package org.example;

import org.example.core.ImageUtils;
import org.example.core.OxylabsClient;
import org.example.core.PhoneDataParser;
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

public class BatchScraper {
    private static final String GSM_ARENA_BASE_URL = "https://www.gsmarena.com";
    private static final String SEARCH_URL = GSM_ARENA_BASE_URL + "/results.php3?sQuickSearch=yes&sName=";
    private static final String OUTPUT_DIR = "final_data";
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
        
        // Get the phone list
        List<String> phoneNames = getPhoneList();
        
        System.out.println("Starting batch extraction of " + phoneNames.size() + " phones...");
        scrapePhones(phoneNames, oxylabs);
    }
    
    private static List<String> getPhoneList() {
        List<String> phoneNames = new ArrayList<>();
        
        // Add all phones to the list
        phoneNames.add("Motorola G35");
        phoneNames.add("Motorola G45");
        phoneNames.add("iQOO Z9x");
        phoneNames.add("Motorola G64");
        phoneNames.add("realme P1");
        phoneNames.add("CMF by Nothing Phone 1");
        phoneNames.add("Motorola G85");
        phoneNames.add("iQOO Z9");
        phoneNames.add("iQOO Z9s");
        phoneNames.add("Samsung M55s");
        phoneNames.add("Motorola Edge 50 Neo");
        phoneNames.add("OnePlus Nord 3");
        phoneNames.add("realme P2 Pro");
        phoneNames.add("Nothing Phone 2a");
        phoneNames.add("Redmi Note 13 Pro +");
        phoneNames.add("OnePlus Nord CE 4");
        phoneNames.add("Nothing Phone 2a Plus");
        phoneNames.add("iQOO Z9s Pro");
        phoneNames.add("realme 12 Pro +");
        phoneNames.add("Redmi Note 14 Pro");
        phoneNames.add("OnePlus Nord 4");
        phoneNames.add("Realme GT 6T");
        phoneNames.add("Pixel 7");
        phoneNames.add("Motorola Edge 50 Pro");
        phoneNames.add("iQOO Neo 9 Pro");
        phoneNames.add("Realme GT 6");
        phoneNames.add("Nothing Phone 2");
        phoneNames.add("OnePlus 12R");
        phoneNames.add("OnePlus 13R");
        phoneNames.add("Xiaomi 14 CIVI");
        phoneNames.add("Pixel 7 Pro");
        phoneNames.add("iQOO 12");
        phoneNames.add("iPhone 13");
        phoneNames.add("Xiaomi 14");
        phoneNames.add("Motorola Edge 50 Ultra");
        phoneNames.add("Samsung S24");
        phoneNames.add("Motorola Razr 50");
        phoneNames.add("iQOO 13");
        phoneNames.add("Samsung Z Flip 5");
        phoneNames.add("OnePlus 12");
        phoneNames.add("vivo X200");
        phoneNames.add("iPhone 15");
        phoneNames.add("OnePlus 13");
        phoneNames.add("Pixel 8");
        phoneNames.add("Samsung S23 Ultra");
        phoneNames.add("iPhone 16");
        phoneNames.add("Pixel 9");
        phoneNames.add("Motorola Razr 50 Ultra");
        phoneNames.add("vivo X200 Pro");
        phoneNames.add("iPhone 16 Pro");
        phoneNames.add("Samsung S24 Ultra");
        phoneNames.add("iPhone 15 Pro Max");
        phoneNames.add("OnePlus Open");
        phoneNames.add("iPhone 16 Pro Max");
        phoneNames.add("Samsung Fold 5");
        
        return phoneNames;
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
        System.out.println("Batch extraction complete!");
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
        String phoneModel = phoneUrl.substring(0, phoneUrl.lastIndexOf("."));
        String brand = ImageUtils.getBrand(foundPhoneName);
        
        // Step 3: Get the phone details page
        String phoneDetailsHtml = oxylabs.scrape(fullPhoneUrl);
        
        // Step 4: Parse the details
        JSONObject phoneDetails = PhoneDataParser.parsePhoneDetails(phoneDetailsHtml);
        
        // Step 5: Generate proper high-resolution image URL
        String originalImageUrl = phoneDetails.optString("image", "");
        List<String> imageUrls = ImageUtils.generateImageUrls(originalImageUrl, brand, phoneModel, foundPhoneName);
        
        // Try each image URL
        String workingImageUrl = "";
        for (String imageUrl : imageUrls) {
            try {
                System.out.println("Trying image URL: " + imageUrl);
                if (ImageUtils.isImageAvailable(imageUrl)) {
                    workingImageUrl = imageUrl;
                    System.out.println("✅ Found working image URL: " + workingImageUrl);
                    break;
                }
            } catch (Exception e) {
                System.out.println("Image URL not available: " + imageUrl);
            }
        }
        
        // Save the working image URL
        if (!workingImageUrl.isEmpty()) {
            phoneDetails.put("highResImage", workingImageUrl);
            ImageUtils.downloadImage(workingImageUrl, phoneName, IMAGES_DIR);
        } else {
            System.out.println("⚠️ Could not find a working high-res image URL");
            if (!originalImageUrl.isEmpty()) {
                ImageUtils.downloadImage(originalImageUrl, phoneName, IMAGES_DIR);
            }
        }
        
        // Save phone details to JSON
        String safeFileName = phoneName.replaceAll("[^a-zA-Z0-9]", "_") + ".json";
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
