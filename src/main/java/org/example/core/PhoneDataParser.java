package org.example.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PhoneDataParser {

    /**
     * Parse phone details from the GSM Arena page HTML
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
                
                // Extract brand from phone name
                String brand = phoneName.split(" ")[0].toLowerCase();
                phoneDetails.put("brand", brand);
            }
            
            // Extract the main phone image
            Element mainImageElem = doc.selectFirst(".specs-photo-main img");
            if (mainImageElem != null) {
                String imageUrl = mainImageElem.attr("src");
                phoneDetails.put("image", imageUrl);
            }
            
            // Extract specifications from the specs-list div
            Element specsListDiv = doc.selectFirst("#specs-list");
            if (specsListDiv != null) {
                // Get all specification tables
                Elements specsTables = specsListDiv.select("table");
                
                JSONObject allSpecs = new JSONObject();
                
                for (Element table : specsTables) {
                    // Each table represents a category of specifications
                    Elements rows = table.select("tr");
                    if (!rows.isEmpty()) {
                        // Get the category name from the first row's th
                        Element categoryHeader = rows.first().selectFirst("th");
                        if (categoryHeader != null) {
                            String category = categoryHeader.text().trim();
                            JSONObject categorySpecs = new JSONObject();
                            
                            // Process all rows for this category
                            for (Element row : rows) {
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
                                        
                                        categorySpecs.put(title, value);
                                        
                                        // Add key specs to the top level for easy access
                                        if (category.equals("Display") && title.equals("Size")) {
                                            phoneDetails.put("displaySize", value);
                                        } else if (category.equals("Platform") && title.equals("OS")) {
                                            phoneDetails.put("os", value);
                                        } else if (category.equals("Platform") && title.equals("Chipset")) {
                                            phoneDetails.put("chipset", value);
                                        } else if (category.equals("Memory") && title.equals("Internal")) {
                                            phoneDetails.put("memory", value);
                                        } else if (category.equals("Battery") && title.equals("Type")) {
                                            phoneDetails.put("battery", value);
                                        } else if (category.equals("Main Camera") && title.equals("Quad") || title.equals("Triple") || title.equals("Dual") || title.equals("Single")) {
                                            phoneDetails.put("mainCamera", value);
                                        } else if (category.equals("Selfie camera") && title.equals("Single") || title.equals("Dual")) {
                                            phoneDetails.put("selfieCamera", value);
                                        }
                                    }
                                }
                            }
                            
                            // Add the category specs to the allSpecs object
                            if (categorySpecs.length() > 0) {
                                allSpecs.put(category, categorySpecs);
                            }
                        }
                    }
                }
                
                // Add all specifications to the phoneDetails
                if (allSpecs.length() > 0) {
                    phoneDetails.put("specifications", allSpecs);
                    System.out.println("Successfully extracted " + allSpecs.length() + " specification categories");
                }
            } else {
                System.out.println("No specs-list div found on the page");
            }
            
            // Extract basic overview details from the specs-spotlight-features
            Elements spotlightFeatures = doc.select(".specs-spotlight-features span[data-spec]");
            for (Element feature : spotlightFeatures) {
                String key = feature.attr("data-spec");
                String value = feature.text().trim();
                
                if (!key.isEmpty() && !value.isEmpty()) {
                    phoneDetails.put(key, value);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing phone details: " + e.getMessage());
            e.printStackTrace();
        }
        
        return phoneDetails;
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
