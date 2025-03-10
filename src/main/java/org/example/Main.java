package org.example;

import java.util.Scanner;

/**
 * Main menu interface for GSM Arena Scraper
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== GSM Arena Phone Data Scraper ===");
        System.out.println("1. Interactive Mode - Search for specific phones");
        System.out.println("2. Batch Mode - Process predefined list of phones");
        System.out.println("3. Embed images into JSON files");
        System.out.print("\nSelect mode (1-3): ");
        
        try (Scanner scanner = new Scanner(System.in)) {
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    System.out.println("\nStarting Interactive Mode...\n");
                    InteractiveScraper.main(args);
                    break;
                case "2":
                    System.out.println("\nStarting Batch Mode...\n");
                    BatchScraper.main(args);
                    break;
                case "3":
                    System.out.print("\nEnter path to JSON directory (e.g., final_data/phones): ");
                    String path = scanner.nextLine().trim();
                    System.out.println("\nStarting Image Embedding...\n");
                    String[] newArgs = {path};
                    org.example.utils.ImageEmbedder.main(newArgs);
                    break;
                default:
                    System.out.println("Invalid selection. Exiting.");
            }
        }
    }
}
