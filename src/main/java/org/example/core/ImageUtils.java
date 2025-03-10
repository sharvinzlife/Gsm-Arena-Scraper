package org.example.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageUtils {
    // Brand mapping to handle brand name variations
    private static final Map<String, String> BRAND_MAPPING = new HashMap<>();
    static {
        BRAND_MAPPING.put("samsung", "samsung");
        BRAND_MAPPING.put("apple", "apple");
        BRAND_MAPPING.put("iphone", "apple");
        BRAND_MAPPING.put("motorola", "motorola");
        BRAND_MAPPING.put("moto", "motorola");
        BRAND_MAPPING.put("google", "google");
        BRAND_MAPPING.put("pixel", "google");
        BRAND_MAPPING.put("oneplus", "oneplus");
        BRAND_MAPPING.put("nothing", "nothing");
        BRAND_MAPPING.put("cmf", "nothing");
        BRAND_MAPPING.put("realme", "realme");
        BRAND_MAPPING.put("xiaomi", "xiaomi");
        BRAND_MAPPING.put("redmi", "xiaomi");
        BRAND_MAPPING.put("vivo", "vivo");
        BRAND_MAPPING.put("iqoo", "vivo");
    }

    // Remove GSM_ARENA_BASE_URL if not used or keep it and use it
    // private static final String GSM_ARENA_BASE_URL = "https://www.gsmarena.com";

    public static String getBrand(String phoneName) {
        String lowerName = phoneName.toLowerCase();
        
        for (Map.Entry<String, String> entry : BRAND_MAPPING.entrySet()) {
            if (lowerName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Default to first word of phone name if no match
        String[] nameParts = phoneName.split(" ");
        if (nameParts.length > 0) {
            return nameParts[0].toLowerCase();
        }
        
        return "";
    }

    /**
     * Extract images from the phone's pictures page using the universal source
     * This revised version only collects official product images
     */
    public static List<String> extractImagesFromPicturesPage(String picturesUrl, OxylabsClient oxylabs) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            System.out.println("Fetching pictures from: " + picturesUrl);
            
            // Use the universal source for better extraction
            String html = oxylabs.scrape(picturesUrl);
            Document doc = Jsoup.parse(html);
            
            // First look for all images in the pictures-list section which contains official images
            Element picturesListDiv = doc.selectFirst("div#pictures-list");
            if (picturesListDiv != null) {
                // Get all direct img tags (not inside other containers)
                Elements directImages = picturesListDiv.select("img[src]");
                
                // Filter to keep only official product images (not lifestyle/review images)
                List<Element> officialImages = new ArrayList<>();
                for (Element img : directImages) {
                    String src = img.attr("src");
                    if (isOfficialProductImage(src)) {
                        officialImages.add(img);
                    }
                }
                
                System.out.println("Found " + officialImages.size() + " official images");
                
                for (Element img : officialImages) {
                    String src = img.attr("src");
                    // Skip lazy-loaded images with data-src
                    if (img.hasAttr("data-src") && img.attr("src").isEmpty()) {
                        src = img.attr("data-src");
                    }
                    
                    if (!src.isEmpty() && !isIconOrBanner(src)) {
                        if (!imageUrls.contains(src)) {
                            imageUrls.add(src);
                            System.out.println("Found official image: " + src);
                        }
                    }
                }
            }
            
            // If still no official images found, try to find them with more specific patterns
            if (imageUrls.isEmpty()) {
                Elements allImages = doc.select("img[src*=/vv/pics/], img[src*=/vv/bigpic/]");
                for (Element img : allImages) {
                    String src = img.attr("src");
                    if (!src.isEmpty() && isOfficialProductImage(src) && !isIconOrBanner(src)) {
                        if (!imageUrls.contains(src)) {
                            imageUrls.add(src);
                            System.out.println("Found additional official image: " + src);
                        }
                    }
                }
            }
            
            if (imageUrls.isEmpty()) {
                System.out.println("No official images found on pictures page, trying alternative sources");
            }
        } catch (Exception e) {
            System.err.println("Error extracting images from pictures page: " + e.getMessage());
            e.printStackTrace();
        }
        
        return imageUrls;
    }
    
    /**
     * Check if the URL is for an official product image rather than a lifestyle/review image
     */
    private static boolean isOfficialProductImage(String url) {
        String lowerUrl = url.toLowerCase();
        
        // Official images are typically in these directories and don't contain "review" or "lifestyle"
        boolean isOfficialPath = (lowerUrl.contains("/vv/pics/") || lowerUrl.contains("/vv/bigpic/"));
        boolean notReviewImage = !lowerUrl.contains("review") && !lowerUrl.contains("lifestyle");
        
        return isOfficialPath && notReviewImage;
    }
    
    /**
     * Check if the URL is for an icon, logo, or banner rather than a phone image
     */
    private static boolean isIconOrBanner(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("logo") || 
               lowerUrl.contains("icon") || 
               lowerUrl.contains("banner") || 
               lowerUrl.contains("button") || 
               lowerUrl.contains("ads") ||
               lowerUrl.contains("thumbnail");
    }
    
    /**
     * Fallback method to generate image URLs based on patterns
     */
    public static List<String> generateImageUrlsFromPatterns(String originalUrl, String phoneModelId, String brand) {
        List<String> urls = new ArrayList<>();
        if (originalUrl.isEmpty()) return urls;
        
        // Get base domain
        String domain = originalUrl.substring(0, originalUrl.indexOf("/vv/") + 4);
        
        // Model ID from URL (e.g., "samsung_galaxy_m55s-13354")
        String modelName = phoneModelId;
        String modelNumber = "";
        
        // Extract model number if present
        if (modelName.contains("-")) {
            String[] parts = modelName.split("-");
            modelNumber = parts[parts.length - 1];
            modelName = parts[0];
        }
        
        // Replace underscores with hyphens for URL construction
        String modelNameHyphen = modelName.replace("_", "-");
        
        // Base model name without brand prefix
        String baseModelName = modelNameHyphen;
        if (baseModelName.startsWith(brand + "-")) {
            baseModelName = baseModelName.substring(brand.length() + 1);
        }
        
        // Add patterns with numbered suffixes (0, 1, 2) as requested
        urls.add(domain + "pics/" + brand + "/" + modelNameHyphen + "-0.jpg");
        urls.add(domain + "pics/" + brand + "/" + modelNameHyphen + "-1.jpg");
        urls.add(domain + "pics/" + brand + "/" + modelNameHyphen + "-2.jpg");
        
        // For Samsung phones with specific model pattern
        if (brand.equals("samsung")) {
            if (baseModelName.contains("galaxy-s")) {
                urls.add(domain + "pics/" + brand + "/" + brand + "-" + baseModelName + "-5g-sm-s" + modelNumber + "-0.jpg");
                urls.add(domain + "pics/" + brand + "/" + brand + "-" + baseModelName + "-5g-sm-s" + modelNumber + "-1.jpg");
                urls.add(domain + "pics/" + brand + "/" + brand + "-" + baseModelName + "-5g-sm-s" + modelNumber + "-2.jpg");
            }
        }
        
        // Add additional general patterns
        urls.add(domain + "pics/" + brand + "/" + modelNameHyphen + ".jpg");
        
        // Add model number specific pattern
        if (!modelNumber.isEmpty()) {
            urls.add(domain + "pics/" + brand + "/" + modelNameHyphen + "-" + modelNumber + ".jpg");
        }
        
        // Add BigPic pattern (used less frequently now)
        urls.add(domain + "bigpic/" + modelNameHyphen + ".jpg");
        
        // Brand-specific patterns
        if (brand.equals("samsung")) {
            urls.add(domain + "pics/samsung/samsung-" + baseModelName + "-0.jpg");
            urls.add(domain + "pics/samsung/samsung-" + baseModelName + "-1.jpg");
        } else if (brand.equals("motorola")) {
            urls.add(domain + "pics/motorola/motorola-" + baseModelName + "-0.jpg");
            urls.add(domain + "pics/motorola/motorola-" + baseModelName + "-1.jpg");
        } else if (brand.equals("vivo") || brand.contains("iqoo")) {
            urls.add(domain + "pics/vivo/vivo-" + baseModelName + "-0.jpg");
            urls.add(domain + "pics/vivo/vivo-" + baseModelName + "-1.jpg");
            urls.add(domain + "pics/iqoo/iqoo-" + baseModelName + "-0.jpg");
            urls.add(domain + "pics/iqoo/iqoo-" + baseModelName + "-1.jpg");
        } else if (brand.equals("realme")) {
            urls.add(domain + "pics/realme/realme-" + baseModelName + "-0.jpg");
            urls.add(domain + "pics/realme/realme-" + baseModelName + "-1.jpg");
        } else if (brand.equals("nothing")) {
            urls.add(domain + "pics/nothing/nothing-" + baseModelName + "-0.jpg");
            urls.add(domain + "pics/nothing/nothing-" + baseModelName + "-1.jpg");
            urls.add(domain + "pics/nothing/cmf-" + baseModelName + "-0.jpg");
            urls.add(domain + "pics/nothing/cmf-" + baseModelName + "-1.jpg");
        }
        
        // Add original URL as last fallback
        if (!urls.contains(originalUrl) && !originalUrl.isEmpty()) {
            urls.add(originalUrl);
        }
        
        return urls;
    }
    
    public static boolean isImageAvailable(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            return (responseCode == 200);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Download multiple images of a phone and return local paths with proper naming
     */
    public static List<String> downloadImages(List<String> imageUrls, String phoneName, String targetDir) {
        List<String> localImagePaths = new ArrayList<>();
        
        if (imageUrls.isEmpty()) {
            System.out.println("No images to download for " + phoneName);
            return localImagePaths;
        }
        
        // Use exact phone name for directory, preserving special characters
        String phoneDir = targetDir + "/" + phoneName.replaceAll("[/\\\\:*?\"<>|]", "_");
        
        try {
            // Create directory for this phone
            Files.createDirectories(Paths.get(phoneDir));
            
            int successful = 0;
            for (int i = 0; i < imageUrls.size(); i++) {
                String imageUrl = imageUrls.get(i);
                try {
                    // Use a better naming convention for images
                    String filename = phoneName.replaceAll("[/\\\\:*?\"<>|]", "_") + "_" + (i + 1) + ".jpg";
                    Path targetPath = Paths.get(phoneDir, filename);
                    
                    java.net.URL url = new java.net.URL(imageUrl);
                    try (java.io.InputStream in = url.openStream()) {
                        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Image downloaded: " + targetPath);
                        localImagePaths.add(targetPath.toString());
                        successful++;
                    }
                } catch (Exception e) {
                    System.err.println("Error downloading image " + (i + 1) + ": " + e.getMessage());
                }
            }
            
            System.out.println("Downloaded " + successful + " of " + imageUrls.size() + " images for " + phoneName);
            
        } catch (IOException e) {
            System.err.println("Error creating directory for phone images: " + e.getMessage());
        }
        
        return localImagePaths;
    }
    
    /**
     * Get base64 encoded image data from a URL
     */
    public static String getBase64EncodedImage(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            try (java.io.InputStream in = url.openStream()) {
                byte[] imageBytes = in.readAllBytes();
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            System.err.println("Error encoding image: " + e.getMessage());
            return "";
        }
    }

    /**
     * Download a single image (for backward compatibility)
     */
    public static void downloadImage(String imageUrl, String phoneName, String targetDir) {
        try {
            String safeFileName = phoneName.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            java.net.URL url = new java.net.URL(imageUrl);
            Path targetPath = Paths.get(targetDir, safeFileName);
            
            try (java.io.InputStream in = url.openStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image downloaded: " + targetPath);
            }
        } catch (Exception e) {
            System.err.println("Error downloading image: " + e.getMessage());
        }
    }
}