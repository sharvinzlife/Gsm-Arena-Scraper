package org.example.utils;

import org.example.core.ImageUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Utility to embed local images as base64 in JSON files
 */
public class ImageEmbedder {

    private static final String IMAGES_DIR = "embedded_images";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -cp target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar " +
                               "org.example.utils.ImageEmbedder <json_directory>");
            return;
        }

        String jsonDir = args[0];
        File dir = new File(jsonDir);
        
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Error: " + jsonDir + " is not a valid directory");
            return;
        }
        
        // Determine the parent directory for existing/new images
        String parentDir = new File(jsonDir).getParent();
        String imagesDir = parentDir + "/images";
        System.out.println("Looking for images in: " + imagesDir);
        
        // Create the embedded_images directory if needed
        String embeddedImagesDir = parentDir + "/" + IMAGES_DIR;
        try {
            Files.createDirectories(Paths.get(embeddedImagesDir));
            System.out.println("Embedded image directory ready: " + embeddedImagesDir);
        } catch (Exception e) {
            System.out.println("Warning: Could not create embedded_images directory: " + e.getMessage());
        }
        
        File[] jsonFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json") && !name.equals("summary.json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No JSON files found in " + jsonDir);
            return;
        }
        
        System.out.println("Found " + jsonFiles.length + " JSON files. Processing...");
        
        int processed = 0;
        int skipped = 0;
        
        for (File jsonFile : jsonFiles) {
            try {
                System.out.println("Processing " + jsonFile.getName() + "...");
                String phoneName = jsonFile.getName().replace(".json", "");
                
                // Read the JSON file
                JSONObject phoneData;
                try (FileReader reader = new FileReader(jsonFile)) {
                    StringBuilder content = new StringBuilder();
                    char[] buffer = new char[1024];
                    int length;
                    while ((length = reader.read(buffer)) != -1) {
                        content.append(buffer, 0, length);
                    }
                    phoneData = new JSONObject(content.toString());
                }
                
                List<String> localImagePaths = new ArrayList<>();
                
                // Check for local_images field
                if (phoneData.has("local_images")) {
                    JSONArray localImages = phoneData.getJSONArray("local_images");
                    for (int i = 0; i < localImages.length(); i++) {
                        localImagePaths.add(localImages.getString(i));
                    }
                    System.out.println("  Found " + localImagePaths.size() + " local image paths in JSON");
                }
                
                // If no valid local images, look for existing images in the images directory
                if (localImagePaths.isEmpty() || !Files.exists(Paths.get(localImagePaths.get(0)))) {
                    System.out.println("  No valid local image paths. Looking for existing images...");
                    File phoneImagesDir = new File(imagesDir, phoneName.replaceAll("[/\\\\:*?\"<>|]", "_"));
                    
                    if (phoneImagesDir.exists() && phoneImagesDir.isDirectory()) {
                        File[] imageFiles = phoneImagesDir.listFiles((d, name) -> 
                            name.toLowerCase().endsWith(".jpg") || 
                            name.toLowerCase().endsWith(".jpeg") || 
                            name.toLowerCase().endsWith(".png"));
                            
                        if (imageFiles != null && imageFiles.length > 0) {
                            localImagePaths.clear();
                            for (File imageFile : imageFiles) {
                                localImagePaths.add(imageFile.getAbsolutePath());
                                System.out.println("  Found existing image: " + imageFile.getName());
                            }
                            
                            // Update the JSON with the found local image paths
                            JSONArray localImagesArray = new JSONArray();
                            for (String path : localImagePaths) {
                                localImagesArray.put(path);
                            }
                            phoneData.put("local_images", localImagesArray);
                            System.out.println("  Updated JSON with " + localImagePaths.size() + " existing image paths");
                        }
                    }
                }
                
                // If we still have no local images but we have image URLs, try to download them
                if (localImagePaths.isEmpty() && phoneData.has("images")) {
                    System.out.println("  No local images found, but found image URLs. Downloading...");
                    
                    JSONArray imageUrls = phoneData.getJSONArray("images");
                    List<String> urls = new ArrayList<>();
                    
                    for (int i = 0; i < imageUrls.length(); i++) {
                        urls.add(imageUrls.getString(i));
                    }
                    
                    localImagePaths = ImageUtils.downloadImages(urls, phoneName, embeddedImagesDir);
                    
                    // Add the local_images array to the JSON
                    JSONArray localImagesArray = new JSONArray();
                    for (String path : localImagePaths) {
                        localImagesArray.put(path);
                    }
                    phoneData.put("local_images", localImagesArray);
                    System.out.println("  Downloaded " + localImagePaths.size() + " new images");
                }
                
                // If we still have no images, try the highResImage field
                if (localImagePaths.isEmpty() && phoneData.has("highResImage")) {
                    System.out.println("  No images array found, but found highResImage. Downloading...");
                    
                    List<String> urls = new ArrayList<>();
                    urls.add(phoneData.getString("highResImage"));
                    
                    localImagePaths = ImageUtils.downloadImages(urls, phoneName, embeddedImagesDir);
                    
                    JSONArray localImagesArray = new JSONArray();
                    for (String path : localImagePaths) {
                        localImagesArray.put(path);
                    }
                    phoneData.put("local_images", localImagesArray);
                    System.out.println("  Downloaded high-res image");
                }
                
                // If we still have no images, try the image field
                if (localImagePaths.isEmpty() && phoneData.has("image")) {
                    System.out.println("  No images array found, but found main image. Downloading...");
                    
                    List<String> urls = new ArrayList<>();
                    urls.add(phoneData.getString("image"));
                    
                    localImagePaths = ImageUtils.downloadImages(urls, phoneName, embeddedImagesDir);
                    
                    JSONArray localImagesArray = new JSONArray();
                    for (String path : localImagePaths) {
                        localImagesArray.put(path);
                    }
                    phoneData.put("local_images", localImagesArray);
                    System.out.println("  Downloaded main image");
                }
                
                if (localImagePaths.isEmpty()) {
                    System.out.println("  No images found to embed. Skipping.");
                    skipped++;
                    continue;
                }
                
                // Embed the images
                JSONArray base64Images = new JSONArray();
                
                // Process each local image
                for (String imagePath : localImagePaths) {
                    Path path = Paths.get(imagePath);
                    
                    if (Files.exists(path)) {
                        try {
                            // Read the image file and convert to base64
                            byte[] imageData = Files.readAllBytes(path);
                            String base64 = Base64.getEncoder().encodeToString(imageData);
                            
                            JSONObject imageObj = new JSONObject();
                            imageObj.put("path", imagePath);
                            imageObj.put("base64", base64);
                            base64Images.put(imageObj);
                            
                            System.out.println("  Embedded image: " + path.getFileName());
                        } catch (Exception e) {
                            System.out.println("  Error embedding image " + path + ": " + e.getMessage());
                        }
                    } else {
                        System.out.println("  Warning: Image file not found: " + imagePath);
                    }
                }
                
                // Add embedded images to the JSON
                phoneData.put("embedded_images", base64Images);
                
                // Write the updated JSON back to file
                try (FileWriter writer = new FileWriter(jsonFile)) {
                    writer.write(phoneData.toString(2));
                }
                
                System.out.println("  Successfully updated " + jsonFile.getName() + " with " + base64Images.length() + " embedded images");
                processed++;
                
            } catch (Exception e) {
                System.err.println("Error processing " + jsonFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
                skipped++;
            }
        }
        
        System.out.println("\nSummary:");
        System.out.println("  Total JSON files: " + jsonFiles.length);
        System.out.println("  Successfully processed: " + processed);
        System.out.println("  Skipped: " + skipped);
    }
}
