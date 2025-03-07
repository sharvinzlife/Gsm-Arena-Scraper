package org.example.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PhoneDataParser {

    /**
     * Parse phone details from HTML content
     * @param html HTML content from phone details page
     * @return JSON object with parsed phone details
     */
    public static JSONObject parsePhoneDetails(String html) {
        Document doc = Jsoup.parse(html);
        JSONObject phoneData = new JSONObject();
        
        // Extract basic info
        String phoneName = doc.select("h1.specs-phone-name-title").text();
        if (phoneName.isEmpty()) {
            // Try alternative selector
            phoneName = doc.select("h1.article-info-name").text();
        }
        phoneData.put("name", phoneName);
        
        // Extract image
        Element imgElement = doc.selectFirst("div.specs-photo-main img");
        if (imgElement == null) {
            // Try alternative selector
            imgElement = doc.selectFirst("div.article-photo img");
        }
        if (imgElement != null) {
            phoneData.put("image", imgElement.attr("src"));
        }
        
        // Debug: Get HTML structure of specs tables
        System.out.println("Parsing specs for: " + phoneName);
        
        // Extract specs tables
        Elements specTables = doc.select("table");
        JSONObject specs = new JSONObject();
        String currentCategory = "General";
        
        for (Element table : specTables) {
            // Check if this is a spec table
            if (!table.hasClass("specs-table") && !table.hasClass("ntable")) {
                continue;
            }
            
            // Find the table's category header
            Element categoryHeader = table.previousElementSibling();
            while (categoryHeader != null && 
                  !categoryHeader.tagName().equals("h2") && 
                  !categoryHeader.tagName().equals("h3") && 
                  !categoryHeader.tagName().equals("h4")) {
                categoryHeader = categoryHeader.previousElementSibling();
            }
            
            if (categoryHeader != null) {
                currentCategory = categoryHeader.text().trim();
                System.out.println("Found category: " + currentCategory);
            }
            
            // Create category object if it doesn't exist
            if (!specs.has(currentCategory)) {
                specs.put(currentCategory, new JSONObject());
            }
            JSONObject categorySpecs = specs.getJSONObject(currentCategory);
            
            // Parse rows in this table
            Elements rows = table.select("tr");
            for (Element row : rows) {
                Element ttl = row.selectFirst("td.ttl, th.ttl, td:first-child");
                Element nfo = row.selectFirst("td.nfo, td:nth-child(2)");
                
                if (ttl != null && nfo != null) {
                    String key = ttl.text().replace(":", "").trim();
                    String value = nfo.text().trim();
                    
                    // Skip empty entries
                    if (!key.isEmpty() && !value.isEmpty()) {
                        categorySpecs.put(key, value);
                    }
                }
            }
        }
        
        // If we didn't find any specs with the standard approach, try an alternative
        if (specs.length() == 0) {
            System.out.println("No specs found with standard approach, trying alternative...");
            
            Elements allTables = doc.select("table");
            for (Element table : allTables) {
                Elements rows = table.select("tr");
                for (Element row : rows) {
                    Elements cells = row.select("td");
                    if (cells.size() >= 2) {
                        String key = cells.get(0).text().trim();
                        String value = cells.get(1).text().trim();
                        
                        if (!key.isEmpty() && !value.isEmpty()) {
                            // Try to categorize based on key prefixes
                            String category = determineCategory(key);
                            
                            if (!specs.has(category)) {
                                specs.put(category, new JSONObject());
                            }
                            
                            specs.getJSONObject(category).put(key, value);
                        }
                    }
                }
            }
        }
        
        phoneData.put("specifications", specs);
        System.out.println("Total specification categories: " + specs.length());
        
        return phoneData;
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
    
    private static String determineCategory(String key) {
        if (key.contains("Display") || key.contains("Size") || key.contains("Resolution")) {
            return "Display";
        } else if (key.contains("Camera") || key.contains("MP") || key.contains("Video")) {
            return "Camera";
        } else if (key.contains("CPU") || key.contains("Chipset") || key.contains("RAM")) {
            return "Platform";
        } else if (key.contains("Battery") || key.contains("mAh")) {
            return "Battery";
        } else if (key.contains("SIM") || key.contains("Network") || key.contains("2G") || key.contains("5G")) {
            return "Network";
        } else {
            return "General";
        }
    }
}
