package Hmm;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class InitMarkov {

    public static void main(String[] args) {
        final int nbStates = 5; // 5 Pages (0 to 4)
        final int size = nbStates + 1; // +1 for the "Somme" Row and Column

        // Initialize Matrix
        Cellule[][] MM = new Cellule[size][size];
        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                MM[i][j] = new Cellule(0);
            }
        }

        Scanner sc = new Scanner(System.in);
        int prev = -1;
        int curr = 0; // The user starts on page 0

        System.out.println("=== Web Navigation Markov Model ===");

        while (true) {
            System.out.println("\nWhere you from: " + (prev == -1 ? "None" : prev));
            System.out.println("Where you are:  " + curr);
            System.out.print("Where you go (enter 0-4 to transition, 5 to quit): ");
            
            if (!sc.hasNextInt()) break;
            int nextPage = sc.nextInt();

            if (nextPage < 0 || nextPage >= nbStates) {
                System.out.println("Generating Markov Dashboard (Graph + Table) and exiting...");
                generateAndOpenHTML(MM, nbStates);
                break;
            }

            prev = curr;
            curr = nextPage;

            // 1. Increment Transition (Nb)
            MM[prev][curr].nb += 1;
            
            // 2. Increment Row Total (Somme Sortante) -> Index nbStates
            MM[prev][nbStates].nb += 1; 

            // 3. Increment Column Total (Somme Entrante) -> Index nbStates
            MM[nbStates][curr].nb += 1;

            // 4. Recalculate Yellow Percentages (Row Probabilities - Next)
            for (int k = 0; k < nbStates; ++k) {
                if (MM[prev][nbStates].nb > 0) {
                    MM[prev][k].statNext = (float) MM[prev][k].nb / MM[prev][nbStates].nb;
                }
            }

            // 5. Recalculate Red Percentages (Column Probabilities - Prev)
            for (int k = 0; k < nbStates; ++k) {
                if (MM[nbStates][curr].nb > 0) {
                    MM[k][curr].statPrev = (float) MM[k][curr].nb / MM[nbStates][curr].nb;
                }
            }

            afficheTab(MM, nbStates);
        }
        sc.close();
    }

    public static void afficheTab(Cellule[][] tab, int n) {
        System.out.println("\n--- Transition Matrix (Matches Prof's Table) ---");
        System.out.print("From \\ To | ");
        for (int j = 0; j < n; j++) System.out.printf("Page %d          | ", j);
        System.out.println("Somme");
        System.out.println("----------+------------------+------------------+------------------+------------------+------------------+-------");

        for (int i = 0; i < n; i++) {
            System.out.printf("Page %d    | ", i);
            for (int j = 0; j < n; j++) {
                System.out.printf("Nb=%-2d Y:%3.0f%% R:%3.0f%% | ", 
                    tab[i][j].nb, 
                    tab[i][j].statNext * 100, 
                    tab[i][j].statPrev * 100);
            }
            System.out.printf("Nb=%d\n", tab[i][n].nb);
        }
        
        System.out.println("----------+------------------+------------------+------------------+------------------+------------------+-------");
        System.out.print("Somme     | ");
        for (int j = 0; j < n; j++) {
            System.out.printf("Nb=%-13d | ", tab[n][j].nb);
        }
        System.out.println("-");
    }

    // ---------- HTML Visualization Generator (Graph + Table) ----------
    private static void generateAndOpenHTML(Cellule[][] tab, int nbL) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>Markov Chain Dashboard</title>")
            .append("<script type=\"text/javascript\" src=\"https://unpkg.com/vis-network/standalone/umd/vis-network.min.js\"></script>")
            // CSS STYLES FOR GRAPH AND TABLE
            .append("<style type=\"text/css\">\n")
            .append("  body { margin:0; background-color:#111; color:#fff; font-family:sans-serif; }\n")
            .append("  #mynetwork { width: 100vw; height: 55vh; border-bottom: 2px solid #444; }\n")
            .append("  .table-container { width: 95vw; margin: 20px auto; overflow-x: auto; padding-bottom: 30px; }\n")
            .append("  table { width: 100%; border-collapse: collapse; font-family: monospace; font-size: 15px; }\n")
            .append("  th, td { border: 1px solid #555; padding: 12px; text-align: center; }\n")
            .append("  th { background-color: #222; color: #ccc; }\n")
            .append("  td { background-color: #1a1a1a; }\n")
            .append("  .y-stat { color: #ffff00; font-weight: bold; }\n") // Yellow percentage
            .append("  .r-stat { color: #ff4444; font-weight: bold; }\n") // Red percentage
            .append("</style>\n")
            .append("</head><body>")
            
            // 1. TOP HALF: THE GRAPH
            .append("<h2 style=\"text-align:center; margin-top: 15px;\">Markov Chain Interactive Graph</h2>")
            .append("<div id=\"mynetwork\"></div>")
            
            // 2. BOTTOM HALF: THE SCHEDULE/TABLE
            .append("<div class=\"table-container\">\n")
            .append("<h2 style=\"text-align:center; color:#ddd;\">Transition Matrix Table</h2>\n")
            .append("<table>\n");

        // Table Header
        html.append("<tr><th>From \\ To</th>");
        for (int j = 0; j < nbL; j++) html.append("<th>Page ").append(j).append("</th>");
        html.append("<th>Somme (Row)</th></tr>\n");

        // Table Body
        for (int i = 0; i < nbL; i++) {
            html.append("<tr><th>Page ").append(i).append("</th>");
            for (int j = 0; j < nbL; j++) {
                html.append("<td>")
                    .append("Nb = ").append(tab[i][j].nb).append("<br>")
                    .append("<span class=\"y-stat\">Next: ").append(String.format(java.util.Locale.US, "%.0f", tab[i][j].statNext * 100)).append("%</span><br>")
                    .append("<span class=\"r-stat\">Prev: ").append(String.format(java.util.Locale.US, "%.0f", tab[i][j].statPrev * 100)).append("%</span>")
                    .append("</td>");
            }
            html.append("<th>Nb = ").append(tab[i][nbL].nb).append("</th></tr>\n");
        }

        // Table Footer (Somme Column)
        html.append("<tr><th>Somme (Col)</th>");
        for (int j = 0; j < nbL; j++) {
            html.append("<th>Nb = ").append(tab[nbL][j].nb).append("</th>");
        }
        html.append("<th>-</th></tr>\n");
        
        html.append("</table>\n</div>\n");

        // 3. JAVASCRIPT FOR VIS.JS GRAPH
        html.append("<script type=\"text/javascript\">\n");
        html.append("var nodes = new vis.DataSet([\n");
        for (int i = 0; i < nbL; i++) {
            html.append("{id: ").append(i).append(", label: 'Page ").append(i)
                .append("', shape: 'circle', color: {background: '#1f77b4', border: '#fff'}, font: {color: '#fff', size: 20}},\n");
        }
        html.append("]);\nvar edges = new vis.DataSet([\n");
        
        for (int i = 0; i < nbL; i++) {
            for (int j = 0; j < nbL; j++) {
                if (tab[i][j].nb > 0) { 
                    double percent = tab[i][j].statNext * 100.0; 
                    String label = String.format(java.util.Locale.US, "%.1f%%", percent);
                    html.append("{from: ").append(i).append(", to: ").append(j)
                        .append(", label: '").append(label).append("'")
                        .append(", arrows: 'to', font: {color: '#00ff00', align: 'top', size: 16}")
                        .append(", color: {color: '#aaaaaa', highlight: '#00ff00'}")
                        .append(", value: ").append(tab[i][j].statNext)
                        .append("},\n");
                }
            }
        }
        html.append("]);\n")
            .append("var container = document.getElementById('mynetwork');\n")
            .append("var data = {nodes: nodes, edges: edges};\n")
            .append("var options = { physics: {barnesHut: {springLength: 250}}, edges: {scaling: {min: 1, max: 8}, smooth: {type: 'dynamic'}} };\n")
            .append("var network = new vis.Network(container, data, options);\n")
            .append("</script></body></html>");

        // 4. SAVE AND OPEN FILE
        try {
            File tempFile = File.createTempFile("markov_dashboard", ".html");
            FileWriter writer = new FileWriter(tempFile, java.nio.charset.StandardCharsets.UTF_8);
            writer.write(html.toString());
            writer.close();
            Desktop.getDesktop().browse(tempFile.toURI());
        } catch (IOException e) {
            System.err.println("Failed to open HTML: " + e.getMessage());
        }
    }
}