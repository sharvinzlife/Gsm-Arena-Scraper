package org.example;

// Remove unused imports
// import org.example.core.OxylabsClient;
// import org.json.JSONArray;
// import org.json.JSONObject;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Use try-with-resources to properly close the scanner
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("=== GSM Arena Phone Data Scraper ===");
            System.out.println("1. Interactive Mode - Search for specific phones");
            System.out.println("2. Batch Mode - Process predefined list of phones");
            System.out.print("\nSelect mode (1 or 2): ");
            
            String choice = scanner.nextLine().trim();
            
            if (choice.equals("1")) {
                InteractiveScraper.main(args);
            } else if (choice.equals("2")) {
                BatchScraper.main(args);
            } else {
                System.out.println("Invalid selection. Exiting.");
            }
        }
    }
}
