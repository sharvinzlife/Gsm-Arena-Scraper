package org.example;

import org.example.core.ImageUtils;
import org.example.core.OxylabsClient;
import org.example.core.PhoneDataParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class InteractiveScraper {
    private static final String GSM_ARENA_BASE_URL = "https://www.gsmarena.com";
    private static final String SEARCH_URL = GSM_ARENA_BASE_URL + "/results.php3?sQuickSearch=yes&sName=";
    private static final String OUTPUT_DIR = "interactive_data";
    private static final String PHONES_DIR = OUTPUT_DIR + "/phones";
    private static final String IMAGES_DIR = OUTPUT_DIR + "/images";

    // Oxylabs credentials
    private static final String USERNAME = "curious69_mOxQL";
    private static final String PASSWORD = "J7J~Q5EOggCk+0rIN";
    
    public static void main(String[] args) {
        // Create output directories
        createDirectories();
        
        // Initialize Oxylabs client
        OxylabsClient oxylabs = new OxylabsClient(USERNAME, PASSWORD);
        
        // Get list of phones to scrape interactively
        List<String> phoneNames = getPhoneListInteractively();
        
        if (phoneNames.isEmpty()) {
            System.out.println("No phones specified. Exiting.");
            return;
        }
        
        System.out.println("\nStarting extraction of " + phoneNames.size() + " phones...");
        scrapePhones(phoneNames, oxylabs);
    }
    
    private static List<String> getPhoneListInteractively() {
        List<String> phones = new ArrayList<>();
        // Use try-with-resources to properly close the scanner
        try (Scanner scanner = new Scanner(System.in)) {
            boolean addingPhones = true;
            
            System.out.println("=== GSM Arena Phone Data Scraper ===");
            System.out.println("Enter phone names to scrape (one at a time)");
            System.out.println("Examples: 'Samsung S24 Ultra', 'iPhone 15 Pro Max', 'Pixel 8'");
            
            while (addingPhones) {
                System.out.print("\nEnter phone name (or type 'done' to finish, 'exit' to quit): ");
                String input = scanner.nextLine().trim();
                
                if (input.equalsIgnoreCase("done")) {
                    addingPhones = false;
                } else if (input.equalsIgnoreCase("exit")) {
                    return new ArrayList<>();
                } else if (!input.isEmpty()) {
                    phones.add(input);
                    System.out.println("Added: " + input + " (Total phones: " + phones.size() + ")");
                }
            }
            
            System.out.println("\nSummary of phones to scrape:");
            for (int i = 0; i < phones.size(); i++) {
                System.out.println((i + 1) + ". " + phones.get(i));
            }
            
            System.out.print("\nProceed with scraping these phones? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if (!confirm.startsWith("y")) {
                System.out.println("Operation cancelled. Exiting.");
                return new ArrayList<>();
            }
        }
        
        return phones;
    }

    private static void scrapePhones(List<String> phoneNames, OxylabsClient oxylabs) {
        int successful = 0;
        int failed = 0;
        List<String> successfulPhones = new ArrayList<>();
        List<String> failedPhones = new ArrayList<>();
        
        for (String phoneName : phoneNames) {
            try {
                System.out.println("\n------------------------------");
                System.out.println("Processing: " + phoneName);
                
                JSONObject result = scrapePhone(phoneName, oxylabs);
                
                if (result != null) {
                    successful++;
                    successfulPhones.add(phoneName);
                } else {
                    failed++;
                    failedPhones.add(phoneName);
                }
                
                // Don't hammer the server
                Thread.sleep(5000);
                
            } catch (Exception e) {
                System.out.println("❌ Error processing " + phoneName + ": " + e.getMessage());
                e.printStackTrace();
                failed++;
                failedPhones.add(phoneName);
                
                try {
                    Thread.sleep(10000); // Longer delay after an error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // Save summary
        JSONObject summary = new JSONObject();
        summary.put("total", phoneNames.size());
        summary.put("successful", successful);
        summary.put("failed", failed);
        summary.put("successful_phones", successfulPhones);
        summary.put("failed_phones", failedPhones);
        
        try (FileWriter file = new FileWriter(PHONES_DIR + "/summary.json")) {
            file.write(summary.toString(2));
            System.out.println("\n------------------------------");
            System.out.println("Summary saved to " + PHONES_DIR + "/summary.json");
        } catch (IOException e) {
            System.out.println("Error saving summary: " + e.getMessage());
        }
        
        System.out.println("\n------------------------------");
        System.out.println("Interactive extraction complete!");
        System.out.println("Total phones: " + phoneNames.size());
        System.out.println("Successful: " + successful);
        System.out.println("Failed: " + failed);
        
        if (successful > 0) {
            System.out.println("\nData saved to:");
            System.out.println("- JSON files: " + PHONES_DIR);
            System.out.println("- Images: " + IMAGES_DIR);
        }
    }
    
    private static JSONObject scrapePhone(String phoneName, OxylabsClient oxylabs) throws Exception {
        // First try direct search
        JSONObject result = searchAndScrapePhone(phoneName, oxylabs);
        
        // If direct search fails and it's a special case like "CMF by Nothing", try alternatives
        if (result == null && phoneName.toLowerCase().contains("cmf by nothing")) {
            String alternativeName = phoneName.toLowerCase().replace("cmf by nothing", "nothing").trim();
            System.out.println("Trying alternative search: " + alternativeName);
            result = searchAndScrapePhone(alternativeName, oxylabs);
        }
        
        return result;
    }
    
    private static JSONObject searchAndScrapePhone(String phoneName, OxylabsClient oxylabs) throws Exception {
        // Step 1: Search for the phone
        String searchUrl = SEARCH_URL + phoneName.replace(" ", "+");
        String searchResultsHtml = oxylabs.scrape(searchUrl);
        
        // Step 2: Parse the search results
        Document searchResults = Jsoup.parse(searchResultsHtml);
        Element firstResult = searchResults.selectFirst("div.makers ul li a");
        
        if (firstResult == null) {
            System.out.println("❌ No search results found for: " + phoneName);
            return null;
        }
        
        String phoneUrl = firstResult.attr("href");
        String fullPhoneUrl = GSM_ARENA_BASE_URL + "/" + phoneUrl;
        String foundPhoneName = firstResult.select("span").text();
        System.out.println("Found phone: " + foundPhoneName);
        System.out.println("URL: " + fullPhoneUrl);
        
        // Extract phone model ID from URL for image path construction
        String phoneModelId = phoneUrl.substring(0, phoneUrl.lastIndexOf("."));
        String brand = ImageUtils.getBrand(foundPhoneName);
        
        // Step 3: Get the phone details page
        String phoneDetailsHtml = oxylabs.scrape(fullPhoneUrl);
        
        // Step 4: Parse phone details with enhanced parser
        System.out.println("Parsing specifications for: " + foundPhoneName);
        JSONObject phoneDetails = PhoneDataParser.parsePhoneDetails(phoneDetailsHtml);
        
        // Step 5: Extract images from pictures page
        // Get the model identifier without the file extension
        String modelId = phoneUrl.substring(phoneUrl.lastIndexOf("-") + 1, phoneUrl.lastIndexOf("."));
        
        // Extract the base phone model name WITHOUT the model ID at the end
        String basePhoneModel = phoneUrl.substring(0, phoneUrl.lastIndexOf("-"));
        
        // Construct pictures URL using the correct GSM Arena format: {base_model}-pictures-{id}.php
        String picturesUrl = GSM_ARENA_BASE_URL + "/" + basePhoneModel + "-pictures-" + modelId + ".php";
        
        System.out.println("Fetching pictures from: " + picturesUrl);
        List<String> imageUrls = ImageUtils.extractImagesFromPicturesPage(picturesUrl, oxylabs);
        
        // If no images found, try pattern-based approach
        if (imageUrls.isEmpty()) {
            String originalImageUrl = phoneDetails.optString("image", "");
            imageUrls = ImageUtils.generateImageUrlsFromPatterns(originalImageUrl, phoneModelId, brand);
            
            // Try each image URL
            List<String> workingImageUrls = new ArrayList<>();
            for (String imageUrl : imageUrls) {
                try {
                    System.out.println("Trying image URL: " + imageUrl);
                    if (ImageUtils.isImageAvailable(imageUrl)) {
                        workingImageUrls.add(imageUrl);
                        System.out.println("✅ Found working image URL: " + imageUrl);
                        if (workingImageUrls.size() >= 3) {  // Limit to 3 images
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Image URL not available: " + imageUrl);
                }
            }
            
            imageUrls = workingImageUrls;
        }
        
        // Add image URLs to details
        JSONArray imageUrlsArray = new JSONArray();
        for (String url : imageUrls) {
            imageUrlsArray.put(url);
        }
        
        phoneDetails.put("images", imageUrlsArray);
        if (!imageUrls.isEmpty()) {
            phoneDetails.put("highResImage", imageUrls.get(0));
        }
        
        // Download images and get local paths
        List<String> localImagePaths = ImageUtils.downloadImages(imageUrls, phoneName, IMAGES_DIR);
        
        // Add local image paths to phone details
        PhoneDataParser.addImagePathsToPhoneDetails(phoneDetails, localImagePaths);
        
        // Save phone details to JSON - use exact phone name, just replace invalid file chars
        String safeFileName = phoneName.replaceAll("[/\\\\:*?\"<>|]", "_") + ".json";
        try (FileWriter file = new FileWriter(PHONES_DIR + "/" + safeFileName)) {
            file.write(phoneDetails.toString(2));
            System.out.println("✅ Saved: " + PHONES_DIR + "/" + safeFileName);
        }
        
        return phoneDetails;
    }
    
    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(PHONES_DIR));
            Files.createDirectories(Paths.get(IMAGES_DIR));
            System.out.println("Created output directories in: " + OUTPUT_DIR);
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
        }
    }
}