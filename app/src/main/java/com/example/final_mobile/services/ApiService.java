package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
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

    // Read response from connection
    private String readResponse(HttpURLConnection connection, int responseCode) throws IOException {
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    // Handle API response
    private void handleResponse(String response, int responseCode, ApiCallback callback) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            
            if (responseCode >= 200 && responseCode < 300) {
                callback.onSuccess(jsonResponse);
            } else {
                String errorMessage = jsonResponse.optString("message", "Unknown error");
                callback.onError(errorMessage, responseCode);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response: " + e.getMessage(), e);
            callback.onError("Invalid response format", responseCode);
        }
    }

    // Cleanup method
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
