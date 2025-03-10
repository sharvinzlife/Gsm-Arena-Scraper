package org.example.core;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class OxylabsClient {
    private static final String OXYLABS_API_URL = "https://realtime.oxylabs.io/v1/queries";
    private final String username;
    private final String password;
    private final OkHttpClient client;

    public OxylabsClient(String username, String password) {
        this.username = username;
        this.password = password;
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Main scrape method using the simplified approach as suggested by Oxylabs support
     */
    public String scrape(String url) throws IOException {
        // Create the request body based on Oxylabs representative's example
        JSONObject payload = new JSONObject();
        payload.put("source", "universal");
        payload.put("url", url);
        payload.put("geo_location", "India");
        
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(payload.toString(), mediaType);
        
        String auth = Credentials.basic(username, password);
        
        Request request = new Request.Builder()
                .url(OXYLABS_API_URL)
                .addHeader("Authorization", auth)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        
        // Execute the request
        System.out.println("Sending Oxylabs request for: " + url);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Error: " + response.code() + " - " + response.message());
                throw new IOException("Unexpected response: " + response);
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response body");
            }
            
            // Parse the response
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            if (json.has("results") && json.getJSONArray("results").length() > 0) {
                JSONObject result = json.getJSONArray("results").getJSONObject(0);
                if (result.has("content")) {
                    System.out.println("Successfully retrieved content");
                    return result.getString("content");
                } else if (result.has("body")) {
                    System.out.println("Successfully retrieved body");
                    return result.getString("body");
                } else {
                    // Print the full result to see what we got
                    System.err.println("Unexpected result format: " + result.toString(2));
                    throw new IOException("No content or body in response");
                }
            }
            
            throw new IOException("No results in response: " + json.toString(2));
        }
    }

    /**
     * Scrape a URL using the universal source, which may provide better extraction for complex pages
     */
    public String scrapeUniversal(String url) throws IOException {
        // Structure the payload similar to the Python example
        JSONObject payload = new JSONObject();
        payload.put("source", "universal");
        payload.put("url", url);
        payload.put("geo_location", "India"); // Use location suggested by Oxylabs support
        
        String requestBody = payload.toString();
        
        // Create the HTTP request
        URL apiUrl = new URL("https://realtime.oxylabs.io/v1/queries");
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        
        // Set up Basic Authentication
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
        
        // Enable output for POST
        connection.setDoOutput(true);
        
        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Get response
        int responseCode = connection.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Read the response
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                // Parse JSON response to extract the content
                JSONObject jsonResponse = new JSONObject(response.toString());
                
                // The structure is different for universal source
                if (jsonResponse.has("results")) {
                    JSONArray results = jsonResponse.getJSONArray("results");
                    if (results.length() > 0) {
                        JSONObject result = results.getJSONObject(0);
                        if (result.has("content")) {
                            System.out.println("Successfully retrieved content using universal source");
                            return result.getString("content");
                        }
                    }
                }
                
                throw new IOException("No content in response from universal source");
            }
        } else {
            // Handle error response
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                throw new IOException("HTTP error " + responseCode + ": " + response.toString());
            }
        }
    }
}
