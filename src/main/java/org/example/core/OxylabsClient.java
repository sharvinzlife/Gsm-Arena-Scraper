package org.example.core;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
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
    
    public String scrape(String url) throws IOException {
        // Create the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("source", "universal");
        requestBody.put("url", url);
        requestBody.put("render", "html");
        
        // Create the request
        String auth = Credentials.basic(username, password);
        
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(requestBody.toString(), mediaType);
        
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
}
