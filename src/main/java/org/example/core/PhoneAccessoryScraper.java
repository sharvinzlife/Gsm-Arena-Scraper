package org.example.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for scraping additional phone data like image galleries
 */
public class PhoneAccessoryScraper {
    
    private static final String GSM_ARENA_BASE_URL = "https://www.gsmarena.com";
    
    /**
     * Extracts high-quality images from the phone's pictures page using the universal source
     */
    public static List<String> extractHighQualityImages(String phoneModelId, OxylabsClient oxylabs) {
        List<String> imageUrls = new ArrayList<>();
        String modelNumber = "";
        
        // Extract model ID if present (usually last part of the URL)
        if (phoneModelId.contains("-")) {
            String[] parts = phoneModelId.split("-");
            modelNumber = parts[parts.length - 1];
        }
        
        // Construct the pictures URL
        String picturesUrl = GSM_ARENA_BASE_URL + "/" + phoneModelId.replace(".php", "") + "-pictures-" + modelNumber + ".php";
        
        try {
            System.out.println("Extracting high-quality images from: " + picturesUrl);
            
            // Use universal source for better extraction
            String html = oxylabs.scrapeUniversal(picturesUrl);
            Document doc = Jsoup.parse(html);
            
            // Look for images in various containers and positions
            Elements allImages = doc.select("img[src$=.jpg], img[src$=.png]");
            for (Element img : allImages) {
                String src = img.attr("src");
                // Filter out thumbnails, icons and website UI images
                if (src.contains("/vv/") && !src.contains("gsmarena-logo") && !src.contains("icon-")) {
                    // Convert thumbnail URLs to full-size
                    if (src.contains("/t_")) {
                        src = src.replace("/t_", "/");
                    }
                    
                    // Add the high-quality image
                    if (!imageUrls.contains(src)) {
                        imageUrls.add(src);
                        System.out.println("Found high-quality image: " + src);
                    }
                }
            }
            
            // Also look for picture links
            Elements pictureLinks = doc.select("a[href*=pic]");
            for (Element link : pictureLinks) {
                String href = link.attr("href");
                if (href.contains("pic-") && !href.isEmpty()) {
                    // Complete the URL if it's relative
                    if (href.startsWith("/")) {
                        href = GSM_ARENA_BASE_URL + href;
                    }
                    System.out.println("Found picture link: " + href);
                    
                    // Try to follow this link to get the actual image
                    try {
                        String picPageHtml = oxylabs.scrape(href);
                        Document picDoc = Jsoup.parse(picPageHtml);
                        Elements mainPics = picDoc.select(".main-pic img");
                        for (Element mainPic : mainPics) {
                            String picSrc = mainPic.attr("src");
                            if (!picSrc.isEmpty() && !imageUrls.contains(picSrc)) {
                                imageUrls.add(picSrc);
                                System.out.println("Found main picture: " + picSrc);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error following picture link: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting high-quality images: " + e.getMessage());
        }
        
        return imageUrls;
    }
}
