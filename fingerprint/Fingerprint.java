package fingerprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Fingerprint {

    // Helper class to store distance metrics for the k-neighbour sort
    static class CellMatch {
        I_Cell cell;
        int diffSignal;

        public CellMatch(I_Cell cell, int diffSignal) {
            this.cell = cell;
            this.diffSignal = diffSignal;
        }
    }

    // Instantiation method from whiteboard: build_example(1)
    public static I_OfflineRSSI build_example() {
        I_OfflineRSSI db = new OfflineRSSI();
        
        // Raw data from TD PDF
        int[][][] rawSignals = {
            {{-38, -27, -54, -13}, {-74, -62, -48, -33}, {-13, -28, -12, -40}},
            {{-34, -27, -38, -41}, {-64, -48, -72, -35}, {-45, -37, -20, -15}},
            {{-17, -50, -44, -33}, {-27, -28, -32, -45}, {-30, -20, -60, -40}}
        };

        float cellWidth = 4.0f;
        float cellHeight = 4.0f;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // Calculate center points for a 4x4m grid starting at (0,0)
                double cx = j * cellWidth + (cellWidth / 2.0);
                double cy = i * cellHeight + (cellHeight / 2.0);
                
                I_Position pos = new Position(cx, cy);
                I_RSSIV rssiv = new RSSIV(rawSignals[i][j]);
                I_Cell cell = new Cell(pos, cellWidth, cellHeight, rssiv);
                
                db.addCell(cell);
            }
        }
        return db;
    }

    // Whiteboard function: k-neighbour (exa - OfflineRSSI) -> returns array of top cells
    public static List<I_Cell> k_neighbour(I_OfflineRSSI offlineDB, I_RSSIV phoneRssi, int k) {
        List<CellMatch> matches = new ArrayList<>();
        int tmSignal = phoneRssi.getSum();

        for (I_Cell cell : offlineDB.getCells()) {
            int diff = Math.abs(cell.getRssiv().getSum() - tmSignal);
            matches.add(new CellMatch(cell, diff));
        }

        // Sort by smallest difference in signal
        matches.sort(Comparator.comparingInt(m -> m.diffSignal));

        List<I_Cell> topK = new ArrayList<>();
        System.out.println("--- Top " + k + " Nearest Neighbours ---");
        for (int i = 0; i < k; i++) {
            CellMatch match = matches.get(i);
            topK.add(match.cell);
            System.out.printf("Neighbor %d -> Center: (%.1f, %.1f) | Signal Diff: %d%n", 
                i+1, match.cell.getCentrum().getX(), match.cell.getCentrum().getY(), match.diffSignal);
        }
        return topK;
    }
    // ---------- HTML Visualization Generator (2D Grid) ----------
    private static void generateAndOpenHTML(I_OfflineRSSI offlineDB, List<I_Cell> top4, double finalX, double finalY, int tmSignal) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>Fingerprint 2D Map</title>")
            .append("<script src=\"https://cdn.plot.ly/plotly-2.24.1.min.js\"></script></head>")
            .append("<body style=\"margin:0; background-color:#111; color:#fff; font-family:sans-serif;\">")
            .append("<h2 style=\"text-align:center; margin-top: 20px;\">Indoor Fingerprinting Map (12m x 12m)</h2>")
            .append("<div id=\"plot\" style=\"width:80vw; height:80vh; margin: auto;\"></div>")
            .append("<script>\n")
            .append("var data = [];\n");

        StringBuilder unselectedX = new StringBuilder("[");
        StringBuilder unselectedY = new StringBuilder("[");
        StringBuilder unselectedText = new StringBuilder("[");

        StringBuilder topX = new StringBuilder("[");
        StringBuilder topY = new StringBuilder("[");
        StringBuilder topText = new StringBuilder("[");

        // Separate cells into "Top 4" and "Unselected" for plotting
        for (I_Cell cell : offlineDB.getCells()) {
            double cx = cell.getCentrum().getX();
            double cy = cell.getCentrum().getY();
            int diff = Math.abs(cell.getRssiv().getSum() - tmSignal);
            
            String hoverInfo = "'<b>Cell Center</b><br>X: " + cx + "<br>Y: " + cy + "<br>Signal Diff: " + diff + "',";

            if (top4.contains(cell)) {
                topX.append(cx).append(",");
                topY.append(cy).append(",");
                topText.append(hoverInfo);
            } else {
                unselectedX.append(cx).append(",");
                unselectedY.append(cy).append(",");
                unselectedText.append(hoverInfo);
            }
        }
        unselectedX.append("]"); unselectedY.append("]"); unselectedText.append("]");
        topX.append("]"); topY.append("]"); topText.append("]");

        // 1. Draw Unselected Cells (Gray)
        html.append("data.push({x: ").append(unselectedX).append(", y: ").append(unselectedY)
            .append(", mode: 'markers', type: 'scatter', name: 'Other Cells', hovertext: ").append(unselectedText)
            .append(", hoverinfo: 'text', marker: {size: 10, color: '#555'}});\n");

        // 2. Draw Top 4 K-Nearest Neighbors (Blue)
        html.append("data.push({x: ").append(topX).append(", y: ").append(topY)
            .append(", mode: 'markers', type: 'scatter', name: 'K-Nearest Cells', hovertext: ").append(topText)
            .append(", hoverinfo: 'text', marker: {size: 14, color: '#1f77b4', line: {color: '#fff', width: 2}}});\n");

        // 3. Draw the Final Estimated Position (Green Star)
        String estText = String.format(java.util.Locale.US, "'<b>Estimated Target</b><br>X: %.2f<br>Y: %.2f'", finalX, finalY);
        html.append("data.push({x: [").append(finalX).append("], y: [").append(finalY)
            .append("], mode: 'markers+text', type: 'scatter', name: 'Estimated Position', hovertext: [").append(estText).append("]")
            .append(", hoverinfo: 'text', text: ['TM Estimate'], textposition: 'bottom center',")
            .append(" marker: {size: 20, color: '#00ff00', symbol: 'star', line: {color: '#fff', width: 2}}});\n");

        // Layout styling (Notice the Y-axis is reversed so 0,0 is top-left)
        html.append("var layout = {")
            .append("paper_bgcolor: '#111', plot_bgcolor: '#222', font: {color: '#fff'},")
            .append("hoverlabel: {bgcolor: '#222', font: {color: '#fff', family: 'sans-serif', size: 14}, bordercolor: '#555'},")
            .append("xaxis: {title: 'Distance X (m)', range: [0, 12], dtick: 4, gridcolor: '#444', showline: true, side: 'top'},")
            .append("yaxis: {title: 'Distance Y (m)', range: [0, 12], dtick: 4, gridcolor: '#444', showline: true, autorange: 'reversed'},")
            .append("shapes: ["); 
        
        // Add shape lines to draw the 3x3 grid clearly
        for(int i = 4; i <= 8; i += 4) {
            html.append("{type: 'line', x0: ").append(i).append(", y0: 0, x1: ").append(i).append(", y1: 12, line: {color: '#555', width: 2, dash: 'dot'}},");
            html.append("{type: 'line', x0: 0, y0: ").append(i).append(", x1: 12, y1: ").append(i).append(", line: {color: '#555', width: 2, dash: 'dot'}},");
        }
        html.append("]};\n");

        html.append("Plotly.newPlot('plot', data, layout);\n")
            .append("</script></body></html>");

        try {
            File tempFile = File.createTempFile("fingerprint_map", ".html");
            FileWriter writer = new FileWriter(tempFile, java.nio.charset.StandardCharsets.UTF_8);
            writer.write(html.toString());
            writer.close();
            System.out.println("\nOpening 2D Fingerprint Map in your browser...");
            Desktop.getDesktop().browse(tempFile.toURI());
        } catch (IOException e) {
            System.err.println("Failed to open HTML visualization: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        // 1. Fingerprint instantiation
        I_OfflineRSSI offlineDB = build_example();

        // 2. RSSI receive S Phone
        I_RSSIV phoneRssi = new RSSIV(new int[]{-26, -42, -13, -46});

        // 3. k-neighbour execution (k = 4)
        List<I_Cell> top4 = k_neighbour(offlineDB, phoneRssi, 4);

        // 4. Barycentric Position Estimation (Inverse Distance Weighting)
        double posX = 0.0;
        double posY = 0.0;
        double sommeDesPoids = 0.0;
        int tmSignal = phoneRssi.getSum();

        for (I_Cell cell : top4) {
            double distance = Math.abs(cell.getRssiv().getSum() - tmSignal);
            if (distance == 0) distance = 0.001; // Prevent division by zero
            
            double poids = 1.0 / distance;
            
            posX += cell.getCentrum().getX() * poids;
            posY += cell.getCentrum().getY() * poids;
            sommeDesPoids += poids;
        }

        double finalX = posX / sommeDesPoids;
        double finalY = posY / sommeDesPoids;

        System.out.println("\n=== FINGERPRINTING ESTIMATED POSITION ===");
        System.out.printf("Mobile Terminal: X = %.2fm, Y = %.2fm%n", finalX, finalY);
    
    // Generate the visual map
    generateAndOpenHTML(offlineDB, top4, finalX, finalY, tmSignal);
    }
}