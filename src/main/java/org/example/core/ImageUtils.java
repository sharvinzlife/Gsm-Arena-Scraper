package org.example.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
        BRAND_MAPPING.put("realme", "realme");
        BRAND_MAPPING.put("xiaomi", "xiaomi");
        BRAND_MAPPING.put("redmi", "xiaomi");
        BRAND_MAPPING.put("vivo", "vivo");
        BRAND_MAPPING.put("iqoo", "vivo");
    }

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
    
    public static List<String> generateImageUrls(String originalUrl, String brand, String phoneModel, String fullName) {
        List<String> urls = new ArrayList<>();
        if (originalUrl.isEmpty()) return urls;
        
        // Get base domain
        String domain = originalUrl.substring(0, originalUrl.indexOf("/vv/") + 4);
        
        // Clean model name (remove brand prefix if it exists in the model)
        String modelName = phoneModel;
        if (modelName.startsWith(brand + "_")) {
            modelName = modelName.substring(brand.length() + 1);
        }
        
        // Model-specific patterns
        if (brand.equals("samsung")) {
            // Samsung patterns
            urls.add(domain + "pics/samsung/samsung-galaxy-s24-ultra-5g-sm-s928-0.jpg");
            urls.add(domain + "pics/samsung/samsung-" + modelName.replace("_", "-") + "-5g-1.jpg");
            urls.add(domain + "pics/samsung/samsung-" + modelName.replace("_", "-") + "-1.jpg");
            urls.add(domain + "pics/samsung/samsung-" + modelName.replace("_", "-") + ".jpg");
        } else if (brand.equals("apple")) {
            // Apple patterns
            urls.add(domain + "pics/apple/apple-iphone-15-pro-max-1.jpg");
            urls.add(domain + "pics/apple/apple-" + modelName.replace("_", "-") + "-1.jpg");
            urls.add(domain + "pics/apple/apple-" + modelName.replace("_", "-") + ".jpg");
        } else if (brand.equals("motorola")) {
            // Motorola patterns
            urls.add(domain + "pics/motorola/motorola-razr-50-ultra-1.jpg");
            urls.add(domain + "pics/motorola/motorola-" + modelName.replace("_", "-") + "-1.jpg");
            urls.add(domain + "pics/motorola/motorola-" + modelName.replace("_", "-") + ".jpg");
        } else if (brand.equals("google")) {
            // Google patterns
            urls.add(domain + "pics/google/google-pixel-9-pro-1.jpg");
            urls.add(domain + "pics/google/google-" + modelName.replace("_", "-") + "-1.jpg");
            urls.add(domain + "pics/google/google-" + modelName.replace("_", "-") + ".jpg");
        } else if (brand.equals("vivo") || brand.equals("iqoo")) {
            // iQOO/Vivo patterns
            urls.add(domain + "pics/vivo/vivo-" + modelName.replace("_", "-") + "-1.jpg");
            urls.add(domain + "pics/iqoo/iqoo-" + modelName.replace("_", "-") + "-1.jpg");
            urls.add(domain + "pics/vivo/vivo-" + modelName.replace("_", "-") + ".jpg");
            urls.add(domain + "pics/iqoo/iqoo-" + modelName.replace("_", "-") + ".jpg");
            urls.add(domain + "bigpic/vivo-" + modelName.replace("_", "-") + ".jpg");
        } else if (brand.equals("realme")) {
            // Realme patterns
            urls.add(domain + "pics/realme/realme-" + modelName.replace("_", "-") + "-1.jpg");
            urls.add(domain + "pics/realme/realme-" + modelName.replace("_", "-") + ".jpg");
            urls.add(domain + "bigpic/realme-" + modelName.replace("_", "-") + ".jpg");
        } else {
            // Generic pattern
            urls.add(domain + "pics/" + brand + "/" + brand + "-" + modelName.replace("_", "-") + "-1.jpg");
            urls.add(domain + "pics/" + brand + "/" + brand + "-" + modelName.replace("_", "-") + ".jpg");
        }
        
        // Add original URL as fallback
        if (!urls.contains(originalUrl)) {
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
    
    public static void downloadImage(String imageUrl, String phoneName, String targetDir) {
        try {
            String safeFileName = phoneName.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            java.net.URL url = new java.net.URL(imageUrl);
            java.nio.file.Path targetPath = Paths.get(targetDir + "/" + safeFileName);
            
            try (java.io.InputStream in = url.openStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image downloaded: " + targetPath);
            }
        } catch (Exception e) {
            System.err.println("Error downloading image: " + e.getMessage());
        }
    }
}
