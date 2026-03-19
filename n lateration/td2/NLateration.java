package td2;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NLateration {

    private static final double SPEED_OF_LIGHT = 299_792_458.0; // m/s

    // ---------- Measurement Models (Pre-processing) ----------

    private static double distanceFromTime(double timeSeconds, double celerity) {
        return celerity * timeSeconds;
    }

    private static double distanceFromFriis(double pt, double pr, double gt, double gr, double lambda) {
        if (pr <= 0 || pt <= 0 || gt <= 0 || gr <= 0 || lambda <= 0) {
            throw new IllegalArgumentException("Friis parameters must be > 0.");
        }
        return (lambda / (4.0 * Math.PI)) * Math.sqrt((pt * gt * gr) / pr);
    }

    private static void printMeasurementModelExamples() {
        System.out.println("--- Measurement Models (Pre-processing Stage) ---");
        double exampleTime = 10e-9;
        double dTime = distanceFromTime(exampleTime, SPEED_OF_LIGHT);
        System.out.printf("Celerity model: t = %.3e s -> d = %.6f m%n", exampleTime, dTime);

        double pt = 1.0;
        double pr = 1e-6;
        double gt = 1.0;
        double gr = 1.0;
        double lambda = 0.345;
        double dFriis = distanceFromFriis(pt, pr, gt, gr, lambda);
        System.out.printf("Friis model: pt=%.3f W, pr=%.3e W -> d = %.6f m%n%n", pt, pr, dFriis);
    }

    // ---------- Geometry Helpers ----------
    private static double distance(IPosition a, IPosition b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double totalError(IPosition candidate, ICell[] emitters) {
        double err = 0.0;
        for (ICell c : emitters) {
            double dGeom = distance(candidate, c.getPosition());
            err += Math.abs(dGeom - c.getDistance());
        }
        return err;
    }

    // ---------- Factory Method ----------
    private static INEmitters buildDatasetFactory() {
        INEmitters emitters = new NEmitters();
        emitters.addEmitter(new Cell(new Position(0.5, 0.5, 0.5), 3.0));
        emitters.addEmitter(new Cell(new Position(4.0, 0.0, 0.0), 2.0));
        emitters.addEmitter(new Cell(new Position(4.0, 5.0, 5.0), 4.2));
        emitters.addEmitter(new Cell(new Position(3.0, 3.0, 3.0), 2.5));
        return emitters;
    }

    // ---------- Bounds & Brute Force Search ----------
    private static double[] computeBoundsFromSpheres(ICell[] emitters) {
        double xmin = Double.POSITIVE_INFINITY, ymin = Double.POSITIVE_INFINITY, zmin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY, zmax = Double.NEGATIVE_INFINITY;

        for (ICell c : emitters) {
            IPosition e = c.getPosition();
            double d = c.getDistance();
            xmin = Math.min(xmin, e.getX() - d);
            xmax = Math.max(xmax, e.getX() + d);
            ymin = Math.min(ymin, e.getY() - d);
            ymax = Math.max(ymax, e.getY() + d);
            zmin = Math.min(zmin, e.getZ() - d);
            zmax = Math.max(zmax, e.getZ() + d);
        }
        return new double[]{xmin, xmax, ymin, ymax, zmin, zmax};
    }

    private static class Result {
        final IPosition bestPos;
        final double bestErr;
        final long evaluatedPoints;
        final double elapsedMs;

        Result(IPosition bestPos, double bestErr, long evaluatedPoints, double elapsedMs) {
            this.bestPos = bestPos;
            this.bestErr = bestErr;
            this.evaluatedPoints = evaluatedPoints;
            this.elapsedMs = elapsedMs;
        }
    }

    private static Result bruteForceSearchTimed(ICell[] emitters, double xmin, double xmax, double ymin, double ymax, double zmin, double zmax, double step) {
        long t0 = System.nanoTime();
        IPosition bestPos = new Position(xmin, ymin, zmin);
        double bestErr = Double.POSITIVE_INFINITY;
        long evaluated = 0;

        for (double x = xmin; x <= xmax; x += step) {
            for (double y = ymin; y <= ymax; y += step) {
                for (double z = zmin; z <= zmax; z += step) {
                    IPosition p = new Position(x, y, z);
                    double e = totalError(p, emitters);
                    evaluated++;
                    if (e < bestErr) {
                        bestErr = e;
                        bestPos = p;
                    }
                }
            }
        }
        long t1 = System.nanoTime();
        return new Result(bestPos, bestErr, evaluated, (t1 - t0) / 1_000_000.0);
    }

    // ---------- Geometric Resolution ----------
    private static IPosition geometricLeastSquares(ICell[] emitters) {
        if (emitters.length < 4) throw new IllegalArgumentException("Need at least 4 emitters.");
        IPosition e0 = emitters[0].getPosition();
        double d0 = emitters[0].getDistance();
        double[][] A = new double[3][3];
        double[] b = new double[3];

        for (int i = 1; i <= 3; i++) {
            IPosition ei = emitters[i].getPosition();
            double di = emitters[i].getDistance();
            A[i - 1][0] = 2.0 * (ei.getX() - e0.getX());
            A[i - 1][1] = 2.0 * (ei.getY() - e0.getY());
            A[i - 1][2] = 2.0 * (ei.getZ() - e0.getZ());
            double ei2 = Math.pow(ei.getX(), 2) + Math.pow(ei.getY(), 2) + Math.pow(ei.getZ(), 2);
            double e02 = Math.pow(e0.getX(), 2) + Math.pow(e0.getY(), 2) + Math.pow(e0.getZ(), 2);
            b[i - 1] = (ei2 - e02) + (d0 * d0 - di * di);
        }

        double[] p = solve3x3(A, b);
        return new Position(p[0], p[1], p[2]);
    }

    private static double[] solve3x3(double[][] A, double[] b) {
        double a = A[0][0], d = A[0][1], g = A[0][2];
        double b1 = A[1][0], e = A[1][1], h = A[1][2];
        double c = A[2][0], f = A[2][1], i = A[2][2];

        double det = a * (e * i - h * f) - d * (b1 * i - h * c) + g * (b1 * f - e * c);
        if (Math.abs(det) < 1e-12) throw new IllegalArgumentException("Singular matrix.");

        double A11 = (e * i - h * f), A12 = -(d * i - g * f), A13 = (d * h - g * e);
        double A21 = -(b1 * i - h * c), A22 = (a * i - g * c), A23 = -(a * h - g * b1);
        double A31 = (b1 * f - e * c), A32 = -(a * f - d * c), A33 = (a * e - d * b1);

        double x = (A11 * b[0] + A12 * b[1] + A13 * b[2]) / det;
        double y = (A21 * b[0] + A22 * b[1] + A23 * b[2]) / det;
        double z = (A31 * b[0] + A32 * b[1] + A33 * b[2]) / det;

        return new double[]{x, y, z};
    }

   // ---------- HTML Visualization Generator ----------
    private static void generateAndOpenHTML(ICell[] emitters, IPosition estimatedPos, IPosition geoPos) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>N-Lateration 3D View</title>")
            .append("<script src=\"https://cdn.plot.ly/plotly-2.24.1.min.js\"></script></head>")
            .append("<body style=\"margin:0; background-color:#111; color:#fff; overflow:hidden;\">")
            .append("<h2 style=\"text-align:center; font-family:sans-serif; margin-top: 10px;\">3D N-Lateration Intersection</h2>")
            .append("<div id=\"plot\" style=\"width:100vw; height:90vh;\"></div>")
            .append("<script>\n")
            .append("var data = [];\n")
            
            // --- JS function to generate a 3D sphere surface ---
            .append("function getSphere(x0, y0, z0, r, color) {\n")
            .append("  var x=[], y=[], z=[];\n")
            .append("  for(var i=0; i<=20; i++) {\n")
            .append("    var v = i * Math.PI / 20;\n")
            .append("    var xRow=[], yRow=[], zRow=[];\n")
            .append("    for(var j=0; j<=40; j++) {\n")
            .append("      var u = j * 2 * Math.PI / 40;\n")
            .append("      xRow.push(x0 + r * Math.cos(u) * Math.sin(v));\n")
            .append("      yRow.push(y0 + r * Math.sin(u) * Math.sin(v));\n")
            .append("      zRow.push(z0 + r * Math.cos(v));\n")
            .append("    }\n")
            .append("    x.push(xRow); y.push(yRow); z.push(zRow);\n")
            .append("  }\n")
            .append("  return {x: x, y: y, z: z, type: 'surface', opacity: 0.12, showscale: false, colorscale: [[0, color], [1, color]], hoverinfo: 'none'};\n")
            .append("}\n");

        String[] colors = {"'#1f77b4'", "'#ff7f0e'", "'#2ca02c'", "'#d62728'"};
        
        StringBuilder ex = new StringBuilder("[");
        StringBuilder ey = new StringBuilder("[");
        StringBuilder ez = new StringBuilder("[");
        StringBuilder textScreen = new StringBuilder("[");
        StringBuilder textHover = new StringBuilder("[");

        StringBuilder lineX = new StringBuilder("[");
        StringBuilder lineY = new StringBuilder("[");
        StringBuilder lineZ = new StringBuilder("[");

        for (int i = 0; i < emitters.length; i++) {
            IPosition p = emitters[i].getPosition();
            double r = emitters[i].getDistance();
            
            ex.append(p.getX()).append(",");
            ey.append(p.getY()).append(",");
            ez.append(p.getZ()).append(",");
            
            // TEXT ON SCREEN: Now includes the actual X, Y, Z values
            textScreen.append("'E").append(i)
                      .append(" (").append(p.getX()).append(", ")
                      .append(p.getY()).append(", ")
                      .append(p.getZ()).append(")',");
            
            // HOVER TOOLTIP
            textHover.append("'<b>Emitter ").append(i).append("</b><br>Radius: ").append(r)
                     .append("<br>X: ").append(p.getX())
                     .append("<br>Y: ").append(p.getY())
                     .append("<br>Z: ").append(p.getZ()).append("',");

            lineX.append(p.getX()).append(",").append(estimatedPos.getX()).append(",null,");
            lineY.append(p.getY()).append(",").append(estimatedPos.getY()).append(",null,");
            lineZ.append(p.getZ()).append(",").append(estimatedPos.getZ()).append(",null,");

            html.append("data.push(getSphere(").append(p.getX()).append(", ")
                .append(p.getY()).append(", ").append(p.getZ()).append(", ")
                .append(r).append(", ").append(colors[i % colors.length]).append("));\n");
        }
        ex.append("]"); ey.append("]"); ez.append("]"); 
        textScreen.append("]"); textHover.append("]");
        lineX.append("]"); lineY.append("]"); lineZ.append("]");

        // 1. Dashed distance lines
        html.append("data.push({x: ").append(lineX).append(", y: ").append(lineY).append(", z: ").append(lineZ)
            .append(", mode: 'lines', type: 'scatter3d', name: 'Distances',")
            .append(" line: {color: '#aaaaaa', width: 2, dash: 'dash'}, hoverinfo: 'none'});\n");

        // 2. Emitters Points & Visible Text
        html.append("data.push({x: ").append(ex).append(", y: ").append(ey).append(", z: ").append(ez)
            .append(", mode: 'markers+text', type: 'scatter3d', name: 'Emitters',")
            .append(" text: ").append(textScreen).append(", hovertext: ").append(textHover)
            .append(", hoverinfo: 'text', textposition: 'top center',")
            .append(" marker: {size: 6, color: 'white', line: {color: '#000', width: 1}}});\n");

        // 3. Brute Force Estimate Point & Visible Text
        // Format to 2 decimal places to keep the label clean using US locale to enforce dot as decimal separator
        String bfScreenText = String.format(java.util.Locale.US, "'BF Est (%.2f, %.2f, %.2f)'", 
            estimatedPos.getX(), estimatedPos.getY(), estimatedPos.getZ());
        String bfHoverText = "'<b>BF Estimate</b><br>X: " + estimatedPos.getX() + "<br>Y: " + estimatedPos.getY() + "<br>Z: " + estimatedPos.getZ() + "'";
        
        html.append("data.push({x: [").append(estimatedPos.getX()).append("], y: [").append(estimatedPos.getY())
            .append("], z: [").append(estimatedPos.getZ()).append("], mode: 'markers+text', type: 'scatter3d', name: 'BF Estimate',")
            .append(" text: [").append(bfScreenText).append("], hovertext: [").append(bfHoverText).append("], hoverinfo: 'text', textposition: 'bottom center',")
            .append(" marker: {size: 12, color: '#00ff00', symbol: 'diamond', line: {color: '#ffffff', width: 2}}});\n");

        // 4. Geometric Estimate Point (if present)
        if (geoPos != null) {
            String geoScreenText = String.format(java.util.Locale.US, "'Geo Est (%.2f, %.2f, %.2f)'", 
                geoPos.getX(), geoPos.getY(), geoPos.getZ());
            String geoHoverText = "'<b>Geo Estimate</b><br>X: " + geoPos.getX() + "<br>Y: " + geoPos.getY() + "<br>Z: " + geoPos.getZ() + "'";
            html.append("data.push({x: [").append(geoPos.getX()).append("], y: [").append(geoPos.getY())
                .append("], z: [").append(geoPos.getZ()).append("], mode: 'markers+text', type: 'scatter3d', name: 'Geo Estimate',")
                .append(" text: [").append(geoScreenText).append("], hovertext: [").append(geoHoverText).append("], hoverinfo: 'text', textposition: 'top center',")
                .append(" marker: {size: 10, color: '#ff00ff', symbol: 'cross', line: {color: '#ffffff', width: 2}}});\n");
        }

        html.append("var layout = {")
            .append("paper_bgcolor: '#111', plot_bgcolor: '#111', font: {color: '#fff'},")
            .append("hoverlabel: {bgcolor: '#222', font: {color: '#fff', family: 'sans-serif', size: 14}, bordercolor: '#555'},")
            .append("scene: {")
            .append("  xaxis: {title: 'X', gridcolor: '#444', zerolinecolor: '#888', showbackground: false},")
            .append("  yaxis: {title: 'Y', gridcolor: '#444', zerolinecolor: '#888', showbackground: false},")
            .append("  zaxis: {title: 'Z', gridcolor: '#444', zerolinecolor: '#888', showbackground: false}")
            .append("}, margin: {l:0, r:0, b:0, t:0}")
            .append("};\n")
            .append("Plotly.newPlot('plot', data, layout);\n")
            .append("</script></body></html>");

        try {
            java.io.File tempFile = java.io.File.createTempFile("nlateration_plot", ".html");
            java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(tempFile), java.nio.charset.StandardCharsets.UTF_8);
            writer.write(html.toString());
            writer.close();
            System.out.println("\nOpening 3D visualization in your browser...");
            java.awt.Desktop.getDesktop().browse(tempFile.toURI());
        } catch (java.io.IOException e) {
            System.err.println("Failed to create or open HTML visualization: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        printMeasurementModelExamples();

        INEmitters dataset = buildDatasetFactory();
        ICell[] emitters = dataset.getEmitters();

        double[] bounds = computeBoundsFromSpheres(emitters);
        double step = 0.1;

        Result bf = bruteForceSearchTimed(emitters, bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5], step);

        IPosition geo = null;
        try {
            geo = geometricLeastSquares(emitters);
        } catch (IllegalArgumentException e) {
            System.err.println("Geometric resolution failed: " + e.getMessage());
        }

        System.out.println("=== TD2 N-Lateration (Official Dataset) ===");
        System.out.println("\n--- Brute-force Spatial Walk (V1) ---");
        System.out.println("Estimated position:     " + bf.bestPos);
        
        System.out.println("\n--- Geometric Resolution (Linearized LS) ---");
        System.out.println("Estimated position:     " + (geo != null ? geo : "N/A"));

        // Trigger the HTML drawing when you run the code
        generateAndOpenHTML(emitters, bf.bestPos, geo);
    }
}