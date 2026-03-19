package fingerprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FingerprintBonus {

    // Helper class to store Grid Cells
    static class Cell implements Comparable<Cell> {
        String id;
        double x, y;
        int[] rssi;
        double signalDistance;

        public Cell(String id, double x, double y, int[] rssi) {
            this.id = id; this.x = x; this.y = y; this.rssi = rssi;
        }

        // Calculate Manhattan distance between this cell's RSSI and the target RSSI
        public void calculateDistance(int[] targetRssi) {
            this.signalDistance = 0;
            for (int i = 0; i < rssi.length; i++) {
                this.signalDistance += Math.abs(this.rssi[i] - targetRssi[i]);
            }
        }

        @Override
        public int compareTo(Cell other) {
            return Double.compare(this.signalDistance, other.signalDistance);
        }
    }

    public static void main(String[] args) {
        // 1. Initialize the 9 cells from the whiteboard grid
        List<Cell> grid = new ArrayList<>();
        grid.add(new Cell("C1", 2, 2, new int[]{-38, -27, -54, -13}));
        grid.add(new Cell("C2", 6, 2, new int[]{-74, -62, -48, -33}));
        grid.add(new Cell("C3", 10, 2, new int[]{-13, -28, -12, -40}));
        grid.add(new Cell("C4", 2, 6, new int[]{-34, -27, -38, -41}));
        grid.add(new Cell("C5", 6, 6, new int[]{-64, -48, -72, -35}));
        grid.add(new Cell("C6", 10, 6, new int[]{-45, -37, -20, -15}));
        grid.add(new Cell("C7", 2, 10, new int[]{-17, -50, -44, -33}));
        grid.add(new Cell("C8", 6, 10, new int[]{-27, -28, -32, -45}));
        grid.add(new Cell("C9", 10, 10, new int[]{-30, -20, -60, -40}));

        // 2. The Target Mobile RSSI from the whiteboard
        int[] targetRssi = {-26, -42, -13, -46};

        System.out.println("========== PSTA Fingerprinting Bonus Test ==========\n");

        // Calculate distances and sort to find the Ordered Neighbors
        for (Cell c : grid) {
            c.calculateDistance(targetRssi);
        }
        Collections.sort(grid);

        System.out.println("--- Ordered Neighbors Details (Closest to Farthest) ---");
        for (Cell c : grid) {
            System.out.printf("%-4s : Coordinates (X: %2.0f, Y: %2.0f) | Signal Distance: %3d\n", 
                              c.id, c.x, c.y, (int)c.signalDistance);
        }
        System.out.println("----------------------------------------------------\n");

        // 3. Run the tests for N=4, N=3, and N=5
        runIDWTest(grid, 4);
        runIDWTest(grid, 3);
        runIDWTest(grid, 5);
    }

    // --- BARYCENTRIC INVERSE DISTANCE WEIGHTING (IDW) ---
    private static void runIDWTest(List<Cell> sortedGrid, int n) {
        long startTime = System.nanoTime();

        double sumWeights = 0;
        double sumX = 0;
        double sumY = 0;
        StringBuilder neighborsUsed = new StringBuilder();

        for (int i = 0; i < n; i++) {
            Cell cell = sortedGrid.get(i);
            
            // إضافة الإحداثيات والمسافة معاً بشكل أنيق
            neighborsUsed.append(String.format("%s(X:%d, Y:%d, Dist:%d) ", 
                                 cell.id, (int)cell.x, (int)cell.y, (int)cell.signalDistance));
            
            // Weight = 1 / Distance
            double weight = 1.0 / (cell.signalDistance == 0 ? 0.001 : cell.signalDistance);
            sumWeights += weight;
            sumX += cell.x * weight;
            sumY += cell.y * weight;
        }

        // Final Estimated Position (P^)
        double estimatedX = sumX / sumWeights;
        double estimatedY = sumY / sumWeights;

        long endTime = System.nanoTime();
        double executionTimeMs = (endTime - startTime) / 1_000_000.0;

        // Output formatted for the whiteboard
        System.out.printf(">>> TEST N = %d <<<\n", n);
        System.out.println("Neighbors Used : " + neighborsUsed.toString().trim());
        System.out.printf("Estimated P^   : (X: %.2f, Y: %.2f)\n", estimatedX, estimatedY);
        System.out.printf("Execution Time : %.4f ms\n", executionTimeMs);
        System.out.println("----------------------------------------------------");
    }
}