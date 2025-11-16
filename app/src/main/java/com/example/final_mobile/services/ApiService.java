package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiService {
    private static final String TAG = "ApiService";
    private static ApiService instance;
    private ExecutorService executor;
    private Context context;

    private ApiService(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(4);
    }

    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) {
            instance = new ApiService(context);
        }
        return instance;
    }

    // Callback interface for API responses
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error, int statusCode);
    }

    // Generic POST request method
    public void post(String endpoint, JSONObject requestBody, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Set request properties
                connection.setRequestMethod("POST");
                connection.setRequestProperty(ApiConfig.HEADER_CONTENT_TYPE, ApiConfig.CONTENT_TYPE_JSON);
                connection.setRequestProperty(ApiConfig.HEADER_ACCEPT, ApiConfig.CONTENT_TYPE_JSON);
                connection.setDoOutput(true);
                connection.setConnectTimeout(ApiConfig.CONNECT_TIMEOUT);
                connection.setReadTimeout(ApiConfig.READ_TIMEOUT);

                // Add authorization header if token exists
                String token = SessionManager.getInstance(context).getToken();
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty(ApiConfig.HEADER_AUTHORIZATION, "Bearer " + token);
                }

                // Send request body
                if (requestBody != null) {
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                }

                // Get response
                int responseCode = connection.getResponseCode();
                String response = readResponse(connection, responseCode);

                Log.d(TAG, "POST " + endpoint + " - Response Code: " + responseCode);
                Log.d(TAG, "Response: " + response);

                // Parse response and call callback
                handleResponse(response, responseCode, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error in POST request: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage(), -1);
            }
        });
    }

    // Generic GET request method
    public void get(String endpoint, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Set request properties
                connection.setRequestMethod("GET");
                connection.setRequestProperty(ApiConfig.HEADER_ACCEPT, ApiConfig.CONTENT_TYPE_JSON);
                connection.setConnectTimeout(ApiConfig.CONNECT_TIMEOUT);
                connection.setReadTimeout(ApiConfig.READ_TIMEOUT);

                // Add authorization header if token exists
                String token = SessionManager.getInstance(context).getToken();
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty(ApiConfig.HEADER_AUTHORIZATION, "Bearer " + token);
                }

                // Get response
                int responseCode = connection.getResponseCode();
                String response = readResponse(connection, responseCode);

                Log.d(TAG, "GET " + endpoint + " - Response Code: " + responseCode);
                Log.d(TAG, "Response: " + response);

                // Parse response and call callback
                handleResponse(response, responseCode, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error in GET request: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage(), -1);
            }
        });
    }

    // Generic PUT request method
    public void put(String endpoint, JSONObject requestBody, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Set request properties
                connection.setRequestMethod("PUT");
                connection.setRequestProperty(ApiConfig.HEADER_CONTENT_TYPE, ApiConfig.CONTENT_TYPE_JSON);
                connection.setRequestProperty(ApiConfig.HEADER_ACCEPT, ApiConfig.CONTENT_TYPE_JSON);
                connection.setDoOutput(true);
                connection.setConnectTimeout(ApiConfig.CONNECT_TIMEOUT);
                connection.setReadTimeout(ApiConfig.READ_TIMEOUT);

                // Add authorization header if token exists
                String token = SessionManager.getInstance(context).getToken();
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty(ApiConfig.HEADER_AUTHORIZATION, "Bearer " + token);
                }

                // Send request body
                if (requestBody != null) {
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                }

                // Get response
                int responseCode = connection.getResponseCode();
                String response = readResponse(connection, responseCode);

                Log.d(TAG, "PUT " + endpoint + " - Response Code: " + responseCode);
                Log.d(TAG, "Response: " + response);

                // Parse response and call callback
                handleResponse(response, responseCode, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error in PUT request: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage(), -1);
            }
        });
    }

    // Generic DELETE request method
    public void delete(String endpoint, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Set request properties
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty(ApiConfig.HEADER_ACCEPT, ApiConfig.CONTENT_TYPE_JSON);
                connection.setConnectTimeout(ApiConfig.CONNECT_TIMEOUT);
                connection.setReadTimeout(ApiConfig.READ_TIMEOUT);

                // Add authorization header if token exists
                String token = SessionManager.getInstance(context).getToken();
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty(ApiConfig.HEADER_AUTHORIZATION, "Bearer " + token);
                }

                // Get response
                int responseCode = connection.getResponseCode();
                String response = readResponse(connection, responseCode);

                Log.d(TAG, "DELETE " + endpoint + " - Response Code: " + responseCode);
                Log.d(TAG, "Response: " + response);

                // Parse response and call callback
                handleResponse(response, responseCode, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error in DELETE request: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage(), -1);
            }
        });
    }

    // Read response from connection
    private String readResponse(HttpURLConnection connection, int responseCode) throws IOException {
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();
        
        try {
            InputStream inputStream = null;
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (response.length() > 0) {
                        response.append("\n");
                    }
                    response.append(line);
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing reader", e);
                }
            }
        }
        
        String result = response.toString();
        
        // Remove BOM (Byte Order Mark) if present
        if (result.length() > 0 && result.charAt(0) == '\uFEFF') {
            result = result.substring(1);
        }
        
        // Trim whitespace but preserve structure for JSON
        result = result.trim();
        
        if (result.isEmpty()) {
            Log.w(TAG, "Empty response from server for status code: " + responseCode);
        }
        return result;
    }

    // Handle API response
    private void handleResponse(String response, int responseCode, ApiCallback callback) {
        // Check if response is empty
        if (response == null || response.trim().isEmpty()) {
            Log.e(TAG, "Empty response from server. Status code: " + responseCode);
            if (responseCode >= 200 && responseCode < 300) {
                // Empty success response - return empty JSON object
                try {
                    JSONObject emptyResponse = new JSONObject();
                    emptyResponse.put("success", true);
                    emptyResponse.put("data", new JSONObject());
                    callback.onSuccess(emptyResponse);
                } catch (JSONException e) {
                    callback.onError("Empty response from server", responseCode);
                }
            } else {
                callback.onError("Server returned empty response (Status: " + responseCode + ")", responseCode);
            }
            return;
        }

        // Clean the response string before parsing
        String cleanedResponse = response.trim();
        
        // Remove BOM if present
        if (cleanedResponse.length() > 0 && cleanedResponse.charAt(0) == '\uFEFF') {
            cleanedResponse = cleanedResponse.substring(1).trim();
        }
        
        // Check if response looks like JSON (starts with { or [)
        if (!cleanedResponse.startsWith("{") && !cleanedResponse.startsWith("[")) {
            Log.e(TAG, "Response does not appear to be JSON. First 100 chars: " + 
                (cleanedResponse.length() > 100 ? cleanedResponse.substring(0, 100) : cleanedResponse));
            callback.onError("Server returned non-JSON response. Status: " + responseCode + 
                ". Response: " + (cleanedResponse.length() > 200 ? cleanedResponse.substring(0, 200) + "..." : cleanedResponse), responseCode);
            return;
        }

        try {
            // Log response for debugging (truncate if too long)
            String logResponse = cleanedResponse.length() > 500 ? cleanedResponse.substring(0, 500) + "..." : cleanedResponse;
            Log.d(TAG, "Parsing response (length: " + cleanedResponse.length() + "): " + logResponse);
            
            JSONObject jsonResponse = new JSONObject(cleanedResponse);
            
            if (responseCode >= 200 && responseCode < 300) {
                callback.onSuccess(jsonResponse);
            } else {
                String errorMessage = jsonResponse.optString("message", "Unknown error");
                Log.e(TAG, "Error response: " + errorMessage);
                callback.onError(errorMessage, responseCode);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response. Status: " + responseCode, e);
            Log.e(TAG, "JSON Exception details: " + e.getMessage());
            Log.e(TAG, "Response content (first 500 chars): " + 
                (cleanedResponse.length() > 500 ? cleanedResponse.substring(0, 500) + "..." : cleanedResponse));
            
            // Try to extract partial error message if JSON is malformed
            String errorMsg = "Invalid JSON format: " + e.getMessage();
            if (cleanedResponse.length() > 0) {
                errorMsg += ". Response preview: " + 
                    (cleanedResponse.length() > 200 ? cleanedResponse.substring(0, 200) + "..." : cleanedResponse);
            }
            callback.onError(errorMsg, responseCode);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error handling response: " + e.getMessage(), e);
            callback.onError("Unexpected error: " + e.getMessage(), responseCode);
        }
    }

    // Cleanup method
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
