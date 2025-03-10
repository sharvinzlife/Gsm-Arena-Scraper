package org.example.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

public class PhoneDataParser {

    /**
     * Parse phone details from the GSM Arena page HTML with the new JSON structure
     */
    public static JSONObject parsePhoneDetails(String html) {
        JSONObject phoneDetails = new JSONObject();
        
        try {
            Document doc = Jsoup.parse(html);
            
            // Extract phone name
            Element phoneNameElem = doc.selectFirst("h1.specs-phone-name-title");
            if (phoneNameElem != null) {
                String phoneName = phoneNameElem.text().trim();
                phoneDetails.put("name", phoneName);
                
                // Extract brand and model from phone name
                String[] nameParts = phoneName.split(" ", 2);
                if (nameParts.length > 0) {
                    String brand = nameParts[0];
                    phoneDetails.put("brand", brand);
                    
                    if (nameParts.length > 1) {
                        phoneDetails.put("model", nameParts[1]);
                    }
                }
            }
            
            // Extract specifications from the specs-list div
            Element specsListDiv = doc.selectFirst("#specs-list");
            if (specsListDiv != null) {
                // Get all specification tables
                Elements specsTables = specsListDiv.select("table");
                
                // Process each specs table
                for (Element table : specsTables) {
                    Elements rows = table.select("tr");
                    if (!rows.isEmpty()) {
                        // Get the category name from the first row's th
                        Element categoryHeader = rows.first().selectFirst("th");
                        if (categoryHeader != null) {
                            String categoryName = categoryHeader.text().trim();
                            String mappedCategory = mapCategoryName(categoryName);
                            
                            // Create or get the category object
                            JSONObject categoryObj;
                            if (phoneDetails.has(mappedCategory)) {
                                categoryObj = phoneDetails.getJSONObject(mappedCategory);
                            } else {
                                categoryObj = new JSONObject();
                                phoneDetails.put(mappedCategory, categoryObj);
                            }
                            
                            // Process all rows for this category
                            for (int i = 1; i < rows.size(); i++) { // Skip header row
                                Element row = rows.get(i);
                                Elements titleElems = row.select("td.ttl");
                                Elements valueElems = row.select("td.nfo");
                                
                                if (!titleElems.isEmpty() && !valueElems.isEmpty()) {
                                    String title = titleElems.first().text().trim();
                                    String value = valueElems.first().text().trim();
                                    
                                    // Skip empty titles or values
                                    if (!title.isEmpty() && !value.isEmpty()) {
                                        // Remove any trailing colon from title
                                        if (title.endsWith(":")) {
                                            title = title.substring(0, title.length() - 1).trim();
                                        }
                                        
                                        // Map the property name
                                        String mappedProperty = mapPropertyName(title);
                                        
                                        // Special handling for specific fields based on the category
                                        if (mappedCategory.equals("misc") && title.equalsIgnoreCase("Announced")) {
                                            phoneDetails.put("announced", value);
                                        } else if (mappedCategory.equals("misc") && title.equalsIgnoreCase("Status")) {
                                            phoneDetails.put("status", value);
                                        } else {
                                            // Add to the category object
                                            categoryObj.put(mappedProperty, value);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                System.out.println("Successfully extracted specifications into new format");
            } else {
                System.out.println("No specs-list div found on the page");
            }
            
            // Extract the main phone image
            Element mainImageElem = doc.selectFirst(".specs-photo-main img");
            if (mainImageElem != null) {
                String imageUrl = mainImageElem.attr("src");
                phoneDetails.put("image", imageUrl);
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing phone details: " + e.getMessage());
            e.printStackTrace();
        }
        
        return phoneDetails;
    }
    
    /**
     * Map GSM Arena category names to our desired format
     */
    private static String mapCategoryName(String category) {
        category = category.toLowerCase();
        
        switch (category) {
            case "body":
                return "body";
            case "display":
                return "display";
            case "platform":
                return "platform";
            case "memory":
                return "memory";
            case "main camera":
                return "main_camera";
            case "selfie camera":
                return "selfie_camera";
            case "sound":
                return "sound";
            case "network":
                return "network";
            case "comms":
            case "connectivity":
                return "comms";
            case "features":
                return "features";
            case "battery":
                return "battery";
            case "misc":
            case "launch":
                return "misc";
            case "tests":
                return "tests";
            default:
                return category.toLowerCase().replace(" ", "_");
        }
    }
    
    /**
     * Map property names to the desired format
     */
    private static String mapPropertyName(String property) {
        property = property.toLowerCase();
        
        switch (property) {
            // Body properties
            case "dimensions":
            case "weight":
            case "build":
            case "sim":
                return property;
            
            // Display properties
            case "size":
            case "resolution":
            case "protection":
                return property;
                
            // Type is used in multiple categories, so handle it based on context
            case "type":
                return "type";
            
            // Memory properties
            case "card slot":
                return "card_slot";
            case "internal":
                return "internal";
            case "storage type":
                return "storage_type";
                
            // Platform properties
            case "os":
            case "chipset":
            case "cpu":
            case "gpu":
                return property;
                
            // Camera properties
            case "triple":
            case "dual":
            case "single":
            case "quad":
            case "penta":
                return property;
            case "features":
            case "video":
                return property;
                
            // Sound properties
            case "loudspeaker":
                return "loudspeaker";
            case "3.5mm jack":
                return "jack";
                
            // Comms properties
            case "wlan":
            case "bluetooth":
            case "positioning":
            case "gps":
                return property.equals("gps") ? "positioning" : property;
            case "nfc":
            case "radio":
            case "usb":
                return property;
                
            // Features properties
            case "sensors":
            case "other":
                return property;
                
            // Battery properties
            case "charging":
                return "charging";
                
            // Misc properties
            case "colors":
            case "models":
            case "sar":
            case "price":
                return property;
                
            // Tests properties
            case "performance":
            case "camera":
            case "display":
            case "battery life":
                return property.replace(" ", "_");
            case "loudspeaker test":
                return "loudspeaker";
                
            default:
                return property.replace(" ", "_").replace("-", "_");
        }
    }
    
    /**
     * Add downloaded image paths to the phone details
     */
    public static void addImagePathsToPhoneDetails(JSONObject phoneDetails, List<String> localImagePaths) {
        if (phoneDetails != null && localImagePaths != null && !localImagePaths.isEmpty()) {
            JSONArray imagePaths = new JSONArray();
            for (String path : localImagePaths) {
                imagePaths.put(path);
            }
            phoneDetails.put("local_images", imagePaths);
        }
    }
    
    /**
     * Parse phone list from brand page HTML content
     * @param html HTML content from brand page
     * @return JSON array with parsed phone list
     */
    public static JSONArray parsePhoneList(String html) {
        Document doc = Jsoup.parse(html);
        JSONArray phones = new JSONArray();
        
        Elements phoneElements = doc.select("div.makers > ul > li");
        for (Element phone : phoneElements) {
            JSONObject phoneObj = new JSONObject();
            
            Element link = phone.selectFirst("a");
            Element img = phone.selectFirst("img");
            
            if (link != null) {
                phoneObj.put("name", link.text());
                phoneObj.put("url", link.attr("href"));
                
                if (img != null) {
                    phoneObj.put("image", img.attr("src"));
                }
                
                Element description = phone.selectFirst("span.sloucka");
                if (description != null) {
                    phoneObj.put("description", description.text());
                }
                
                phones.put(phoneObj);
            }
        }
        
        return phones;
    }
}
