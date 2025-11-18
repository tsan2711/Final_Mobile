package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import com.example.final_mobile.models.User;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthService {
    private static final String TAG = "AuthService";
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public AuthService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    // Callback interface for authentication
    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String error);
        void onOtpRequired(String message);
    }

    // Register new user
    public void register(String email, String password, String fullName, String phone, String address, AuthCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("fullName", fullName);
            requestBody.put("phone", phone);
            if (address != null && !address.isEmpty()) {
                requestBody.put("address", address);
            }

            apiService.post(ApiConfig.REGISTER, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        // Parse response
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            String token = data.optString("token", data.optString("accessToken", data.optString("access_token", "")));
                            String refreshToken = data.optString("refreshToken", data.optString("refresh_token", ""));
                            
                            // Parse user data
                            JSONObject userData = data.getJSONObject("user");
                            User user = parseUserFromJson(userData);
                            
                            // Create session
                            sessionManager.createLoginSession(token, refreshToken, user);
                            callback.onSuccess(user);
                        } else {
                            String message = response.optString("message", "Registration failed");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing register response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Register error: " + error + " (Status: " + statusCode + ")");
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating register request", e);
            callback.onError("Failed to create register request");
        }
    }

    // Login with email and password
    public void login(String email, String password, AuthCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("device_name", android.os.Build.MODEL);

            apiService.post(ApiConfig.LOGIN, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        // Parse response
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            String token = data.getString("token");
                            String refreshToken = data.optString("refresh_token", "");
                            
                            // Parse user data
                            JSONObject userData = data.getJSONObject("user");
                            User user = parseUserFromJson(userData);
                            
                            // Check if OTP is required
                            boolean otpRequired = data.optBoolean("otp_required", false);
                            if (otpRequired) {
                                // Save temporary login data
                                sessionManager.saveTemporaryData("pending_token", token);
                                sessionManager.saveTemporaryData("pending_user", userData.toString());
                                callback.onOtpRequired("OTP verification required");
                                return;
                            }
                            
                            // Create session
                            sessionManager.createLoginSession(token, refreshToken, user);
                            callback.onSuccess(user);
                        } else {
                            String message = response.optString("message", "Login failed");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing login response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Login error: " + error + " (Status: " + statusCode + ")");
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating login request", e);
            callback.onError("Failed to create login request");
        }
    }

    // Verify OTP
    public void verifyOtp(String otpCode, AuthCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("otp_code", otpCode);
            
            String pendingToken = sessionManager.getTemporaryData("pending_token");
            if (pendingToken != null) {
                requestBody.put("token", pendingToken);
            }

            apiService.post(ApiConfig.VERIFY_OTP, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            // Get stored user data
                            String userDataStr = sessionManager.getTemporaryData("pending_user");
                            if (userDataStr != null) {
                                JSONObject userData = new JSONObject(userDataStr);
                                User user = parseUserFromJson(userData);
                                
                                String token = sessionManager.getTemporaryData("pending_token");
                                String refreshToken = response.getJSONObject("data").optString("refresh_token", "");
                                
                                // Clear temporary data
                                sessionManager.clearTemporaryData("pending_token");
                                sessionManager.clearTemporaryData("pending_user");
                                
                                // Create session
                                sessionManager.createLoginSession(token, refreshToken, user);
                                callback.onSuccess(user);
                            } else {
                                callback.onError("Session data not found");
                            }
                        } else {
                            String message = response.optString("message", "OTP verification failed");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing OTP response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "OTP verification error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating OTP request", e);
            callback.onError("Failed to create OTP request");
        }
    }

    // Send OTP
    public void sendOtp(String phone, AuthCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("phone", phone);

            apiService.post(ApiConfig.SEND_OTP, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            String message = response.optString("message", "OTP sent successfully");
                            callback.onOtpRequired(message);
                        } else {
                            String message = response.optString("message", "Failed to send OTP");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing send OTP response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Send OTP error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating send OTP request", e);
            callback.onError("Failed to create send OTP request");
        }
    }

    // Logout
    public void logout(AuthCallback callback) {
        apiService.post(ApiConfig.LOGOUT, null, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                sessionManager.logoutUser();
                Log.d(TAG, "Logout successful");
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                // Even if API call fails, clear local session
                sessionManager.logoutUser();
                Log.d(TAG, "Logout completed (with API error): " + error);
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }
        });
    }

    // Refresh token
    public void refreshToken(AuthCallback callback) {
        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            callback.onError("No refresh token available");
            return;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("refresh_token", refreshToken);

            apiService.post(ApiConfig.REFRESH_TOKEN, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            String newToken = data.getString("token");
                            sessionManager.updateToken(newToken);
                            
                            if (callback != null) {
                                callback.onSuccess(sessionManager.getCurrentUser());
                            }
                        } else {
                            sessionManager.logoutUser();
                            callback.onError("Token refresh failed");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing refresh token response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Token refresh error: " + error);
                    sessionManager.logoutUser();
                    callback.onError("Session expired. Please login again.");
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating refresh token request", e);
            callback.onError("Failed to create refresh request");
        }
    }

    // Check if user is authenticated
    public boolean isAuthenticated() {
        return sessionManager.isLoggedIn();
    }

    // Get current user
    public User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }

    // Parse user from JSON
    private User parseUserFromJson(JSONObject userData) throws JSONException {
        User user = new User();
        user.setId(userData.getString("id"));
        user.setEmail(userData.getString("email"));
        user.setFullName(userData.optString("full_name", ""));
        user.setPhone(userData.optString("phone", ""));
        user.setAddress(userData.optString("address", ""));
        user.setAccountNumber(userData.optString("account_number", ""));
        user.setCustomerType(userData.optString("customer_type", "CUSTOMER"));
        user.setActive(userData.optBoolean("is_active", true));
        return user;
    }

    // Get user-friendly error message
    private String getErrorMessage(String error, int statusCode) {
        switch (statusCode) {
            case ApiConfig.UNAUTHORIZED:
                return "Email hoặc mật khẩu không đúng";
            case ApiConfig.FORBIDDEN:
                return "Tài khoản của bạn đã bị khóa";
            case ApiConfig.UNPROCESSABLE_ENTITY:
                return "Thông tin đăng nhập không hợp lệ";
            case ApiConfig.INTERNAL_SERVER_ERROR:
                return "Lỗi hệ thống. Vui lòng thử lại sau";
            default:
                return error != null ? error : "Đã xảy ra lỗi không xác định";
        }
    }
}
