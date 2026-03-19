package td2;

public class NLaterationBonus {

    // Helper class to store Emitter data
    static class Emitter {
        double x, y, z, d;
        public Emitter(double x, double y, double z, double d) {
            this.x = x; this.y = y; this.z = z; this.d = d;
        }
    }

    public static void main(String[] args) {
        // All 4 Emitters from the whiteboard
        Emitter e0 = new Emitter(0.5, 0.5, 0.5, 3.0);
        Emitter e1 = new Emitter(4.0, 0.0, 0.0, 2.0);
        Emitter e2 = new Emitter(4.0, 5.0, 5.0, 4.2);
        Emitter e3 = new Emitter(3.0, 3.0, 3.0, 2.5);

        Emitter[] test1_N4 = {e0, e1, e2, e3}; // Test 1: All 4 emitters
        Emitter[] test2_N3 = {e1, e2, e3};     // Test 2: Only i=1, 2, 3

        System.out.println("========== PSTA 3rd Session: N-Lateration Tests ==========\n");

        // --- TEST 1: N = 4 ---
        System.out.println(">>> TEST 1: N = 4 (Using E0, E1, E2, E3) <<<");
        double[] bounds1 = calculateBounds(test1_N4);
        printBounds(bounds1);
        runTest(test1_N4, bounds1, 1.0);
        runTest(test1_N4, bounds1, 0.5);
        runTest(test1_N4, bounds1, 0.1);

        System.out.println("\n----------------------------------------------------------\n");

        // --- TEST 2: N = 3 ---
        System.out.println(">>> TEST 2: N = 3 (Using ONLY E1, E2, E3) <<<");
        double[] bounds2 = calculateBounds(test2_N3);
        printBounds(bounds2);
        runTest(test2_N3, bounds2, 1.0);
        runTest(test2_N3, bounds2, 0.5);
        runTest(test2_N3, bounds2, 0.1);
    }

    // --- BRUTE FORCE ALGORITHM ---
    private static void runTest(Emitter[] emitters, double[] bounds, double step) {
        double xmin = bounds[0], xmax = bounds[1];
        double ymin = bounds[2], ymax = bounds[3];
        double zmin = bounds[4], zmax = bounds[5];

        double bestX = 0, bestY = 0, bestZ = 0;
        double minError = Double.POSITIVE_INFINITY;

        long startTime = System.currentTimeMillis();

        // 3D Grid Search
        for (double x = xmin; x <= xmax; x += step) {
            for (double y = ymin; y <= ymax; y += step) {
                for (double z = zmin; z <= zmax; z += step) {
                    
                    double currentError = 0;
                    // Calculate L1 Norm Error for this point
                    for (Emitter e : emitters) {
                        double geometricDist = Math.sqrt(Math.pow(x - e.x, 2) + Math.pow(y - e.y, 2) + Math.pow(z - e.z, 2));
                        currentError += Math.abs(geometricDist - e.d);
                    }

                    if (currentError < minError) {
                        minError = currentError;
                        bestX = x; bestY = y; bestZ = z;
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Print results formatted for the whiteboard
        System.out.printf("Interval Search = %.1f  |  P^ (%.2f, %.2f, %.2f)  |  Exec Time: %d ms  |  (Min Error: %.4f)\n", 
                          step, bestX, bestY, bestZ, executionTime, minError);
    }

    // --- CALCULATE BOUNDING BOX ---
    private static double[] calculateBounds(Emitter[] emitters) {
        double xmin = Double.MAX_VALUE, xmax = -Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE, ymax = -Double.MAX_VALUE;
        double zmin = Double.MAX_VALUE, zmax = -
        Double.MAX_VALUE;

        for (Emitter e : emitters) {
            if (e.x - e.d < xmin) xmin = e.x - e.d;
            if (e.x + e.d > xmax) xmax = e.x + e.d;
            
            if (e.y - e.d < ymin) ymin = e.y - e.d;
            if (e.y + e.d > ymax) ymax = e.y + e.d;
            
            if (e.z - e.d < zmin) zmin = e.z - e.d;
            if (e.z + e.d > zmax) zmax = e.z + e.d;
        }
        return new double[]{xmin, xmax, ymin, ymax, zmin, zmax};
    }

    private static void printBounds(double[] bounds) {
        System.out.printf("Space Research (Bounds): [X: %.1f to %.1f] [Y: %.1f to %.1f] [Z: %.1f to %.1f]\n", 
            bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
    }
}