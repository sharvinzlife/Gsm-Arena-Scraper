package org.example.utils;

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
 * Emergency recovery tool for connecting JSON files with existing images
 * and embedding images directly into the JSON
 */
public class ImageRecoveryTool {

    public static void main(String[] args) {
        // Default directories - modify these if needed
        String jsonDir = "interactive_data/phones";
        String imagesDir = "interactive_data/images";
        String finalDataImagesDir = "final_data/images";
        
        System.out.println("=== Image Recovery Tool ===");
        System.out.println("This tool will find matching images for your phone JSON files and embed them");
        System.out.println("Looking in directories:");
        System.out.println("1. " + jsonDir + " (for JSON files)");
        System.out.println("2. " + imagesDir + " (for images)");
        System.out.println("3. " + finalDataImagesDir + " (for images)");
        
        // Check if directories exist and print detailed info
        File jsonDirFile = new File(jsonDir);
        File imagesDirFile = new File(imagesDir);
        File finalImagesDirFile = new File(finalDataImagesDir);
        
        System.out.println("\nDirectory existence check:");
        System.out.println("- JSON directory exists: " + jsonDirFile.exists() + " (" + jsonDir + ")");
        System.out.println("- Images directory exists: " + imagesDirFile.exists() + " (" + imagesDir + ")");
        System.out.println("- Final images directory exists: " + finalImagesDirFile.exists() + " (" + finalDataImagesDir + ")");
        
        try {
            // Process all JSON files in the jsonDir
            processAllJsonFiles(jsonDir, imagesDir, finalDataImagesDir);
        } catch (Exception e) {
            System.err.println("Error during image recovery: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void processAllJsonFiles(String jsonDir, String... imageDirs) {
        File dir = new File(jsonDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("JSON directory not found: " + jsonDir);
            return;
        }
        
        System.out.println("\nLooking for JSON files in: " + dir.getAbsolutePath());
        
        // List all files in the directory
        File[] allFiles = dir.listFiles();
        if (allFiles != null) {
            System.out.println("All files in directory (" + allFiles.length + "):");
            for (File file : allFiles) {
                System.out.println("  " + file.getName() + (file.isDirectory() ? " [DIR]" : ""));
            }
        } else {
            System.out.println("No files found or unable to list directory contents");
            return;
        }
        
        // Filter for JSON files
        File[] jsonFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json") && !name.equals("summary.json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No JSON files found in " + jsonDir);
            return;
        }
        
        System.out.println("\nFound " + jsonFiles.length + " JSON files to process:");
        for (File file : jsonFiles) {
            System.out.println("  " + file.getName());
        }
        
        int processed = 0;
        int skipped = 0;
        
        for (File jsonFile : jsonFiles) {
            try {
                String phoneName = jsonFile.getName().replace(".json", "");
                System.out.println("\nProcessing: " + phoneName);
                
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
                    System.out.println("  Successfully read JSON file with " + phoneData.length() + " keys");
                    
                    // Print some key fields to verify content
                    if (phoneData.has("name")) {
                        System.out.println("  Phone name in JSON: " + phoneData.getString("name"));
                    }
                    if (phoneData.has("brand")) {
                        System.out.println("  Brand in JSON: " + phoneData.getString("brand"));
                    }
                }
                
                List<String> foundImagePaths = new ArrayList<>();
                
                // Try to find matching images in all provided image directories
                for (String imageDir : imageDirs) {
                    System.out.println("  Searching for images in: " + imageDir);
                    List<String> foundImages = findMatchingImages(phoneName, imageDir);
                    if (!foundImages.isEmpty()) {
                        foundImagePaths.addAll(foundImages);
                        System.out.println("  Found " + foundImages.size() + " images in " + imageDir);
                    } else {
                        System.out.println("  No images found in " + imageDir);
                    }
                }
                
                if (foundImagePaths.isEmpty()) {
                    System.out.println("  No matching images found for " + phoneName);
                    skipped++;
                    continue;
                }
                
                // Update the JSON with the found image paths
                JSONArray localImagesArray = new JSONArray();
                for (String path : foundImagePaths) {
                    localImagesArray.put(path);
                }
                phoneData.put("local_images", localImagesArray);
                
                // Embed the images as base64
                JSONArray base64Images = new JSONArray();
                for (String imagePath : foundImagePaths) {
                    Path path = Paths.get(imagePath);
                    if (Files.exists(path)) {
                        try {
                            byte[] imageData = Files.readAllBytes(path);
                            String base64 = Base64.getEncoder().encodeToString(imageData);
                            
                            JSONObject imageObj = new JSONObject();
                            imageObj.put("path", imagePath);
                            imageObj.put("base64", base64);
                            base64Images.put(imageObj);
                            
                            System.out.println("  Embedded image: " + path.getFileName());
                        } catch (Exception e) {
                            System.out.println("  Error embedding image: " + e.getMessage());
                        }
                    } else {
                        System.out.println("  Warning: Image file not found: " + imagePath);
                    }
                }
                
                // Add embedded images to JSON
                phoneData.put("embedded_images", base64Images);
                
                // Write the updated JSON back to file
                try (FileWriter writer = new FileWriter(jsonFile)) {
                    writer.write(phoneData.toString(2));
                    System.out.println("  Updated JSON file with " + base64Images.length() + " embedded images");
                }
                
                processed++;
                
            } catch (Exception e) {
                System.err.println("Error processing " + jsonFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
                skipped++;
            }
        }
        
        System.out.println("\n=== Summary ===");
        System.out.println("Total JSON files: " + jsonFiles.length);
        System.out.println("Successfully processed: " + processed);
        System.out.println("Skipped: " + skipped);
    }
    
    private static List<String> findMatchingImages(String phoneName, String imageDir) {
        List<String> foundImages = new ArrayList<>();
        File dir = new File(imageDir);
        
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("  Image directory not found: " + imageDir);
            return foundImages;
        }
        
        System.out.println("  Checking directory: " + dir.getAbsolutePath());
        
        // List all contents of directory
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            System.out.println("  Directory contents (" + allContents.length + " items):");
            for (File item : allContents) {
                System.out.println("    " + item.getName() + (item.isDirectory() ? " [DIR]" : ""));
            }
        } else {
            System.out.println("  Directory is empty or inaccessible");
            return foundImages;
        }
        
        // First, look for a directory named exactly like the phone or similar
        String normalizedPhoneName = phoneName.replaceAll("[/\\\\:*?\"<>|]", "_");
        File phoneDir = new File(dir, normalizedPhoneName);
        
        System.out.println("  Looking for directory: " + phoneDir.getPath());
        
        if (phoneDir.exists() && phoneDir.isDirectory()) {
            System.out.println("  Found exact match directory: " + phoneDir.getName());
            // Found an exact match directory
            File[] imageFiles = phoneDir.listFiles((d, name) -> 
                name.toLowerCase().endsWith(".jpg") || 
                name.toLowerCase().endsWith(".jpeg") || 
                name.toLowerCase().endsWith(".png"));
                
            if (imageFiles != null && imageFiles.length > 0) {
                System.out.println("  Found " + imageFiles.length + " images in directory");
                for (File image : imageFiles) {
                    foundImages.add(image.getAbsolutePath());
                    System.out.println("  Found exact match image: " + image.getName());
                }
                
                if (!foundImages.isEmpty()) {
                    return foundImages; // Return if we found images in the exact match directory
                }
            } else {
                System.out.println("  No images found in exact match directory");
            }
        } else {
            System.out.println("  No exact match directory found");
        }
        
        // If we didn't find an exact match directory, look for similar directories
        String lowerPhoneName = phoneName.toLowerCase();
        System.out.println("  Looking for similar directories containing: " + lowerPhoneName);
        
        File[] allDirs = dir.listFiles(File::isDirectory);
        
        if (allDirs != null && allDirs.length > 0) {
            System.out.println("  Checking " + allDirs.length + " subdirectories for matches");
            
            for (File subdir : allDirs) {
                String subdirName = subdir.getName().toLowerCase();
                
                // Check if the directory name contains our phone name or vice versa
                if (subdirName.contains(lowerPhoneName) || lowerPhoneName.contains(subdirName)) {
                    System.out.println("  Found similar directory: " + subdir.getName());
                    
                    File[] imageFiles = subdir.listFiles((d, name) -> 
                        name.toLowerCase().endsWith(".jpg") || 
                        name.toLowerCase().endsWith(".jpeg") || 
                        name.toLowerCase().endsWith(".png"));
                        
                    if (imageFiles != null && imageFiles.length > 0) {
                        System.out.println("  Found " + imageFiles.length + " images in similar directory");
                        for (File image : imageFiles) {
                            foundImages.add(image.getAbsolutePath());
                            System.out.println("  Found similar match image: " + image.getName());
                        }
                        
                        if (!foundImages.isEmpty()) {
                            return foundImages; // Return if we found images in a similar directory
                        }
                    } else {
                        System.out.println("  No images found in similar directory");
                    }
                }
            }
        } else {
            System.out.println("  No subdirectories found");
        }
        
        // Finally, look in root directory for any files matching the phone name
        System.out.println("  Looking for image files in root directory matching: " + lowerPhoneName);
        
        File[] rootImages = dir.listFiles((d, name) -> {
            String lowerName = name.toLowerCase();
            return (lowerName.endsWith(".jpg") || 
                    lowerName.endsWith(".jpeg") || 
                    lowerName.endsWith(".png")) &&
                   (lowerName.contains(lowerPhoneName) || 
                    lowerPhoneName.contains(lowerName.replace(".jpg", "").replace(".jpeg", "").replace(".png", "")));
        });
        
        if (rootImages != null && rootImages.length > 0) {
            System.out.println("  Found " + rootImages.length + " matching images in root directory");
            for (File image : rootImages) {
                foundImages.add(image.getAbsolutePath());
                System.out.println("  Found matching image: " + image.getName());
            }
        } else {
            System.out.println("  No matching images found in root directory");
        }
        
        return foundImages;
    }
}
