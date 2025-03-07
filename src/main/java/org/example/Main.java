package org.example;

import org.example.core.OxylabsClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Main entry point for GSM Arena Scraper
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("GSM Arena Scraper");
        System.out.println("-----------------");
        System.out.println("Choose an option:");
        System.out.println("1. Run batch scraper (scrape predefined phone list)");
        System.out.println("2. Run interactive scraper (enter phone names manually)");
        
        // Read user input
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        int option = scanner.nextInt();
        
        switch (option) {
            case 1:
                BatchScraper.main(args);
                break;
            case 2:
                InteractiveScraper.main(args);
                break;
            default:
                System.out.println("Invalid option. Exiting.");
                break;
        }
    }
}
