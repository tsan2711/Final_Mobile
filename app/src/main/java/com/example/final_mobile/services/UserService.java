package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import com.example.final_mobile.models.User;

import org.json.JSONException;
import org.json.JSONObject;

public class UserService {
    private static final String TAG = "UserService";
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public UserService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    // Callback interface for user operations
    public interface UserCallback {
        void onSuccess(User user);
        void onUpdateSuccess(String message);
        void onError(String error);
    }

    // Get user profile
    public void getUserProfile(UserCallback callback) {
        apiService.get(ApiConfig.USER_PROFILE, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        JSONObject userData = response.getJSONObject("data");
                        User user = parseUserFromJson(userData);
                        
                        // Update session with latest user data
                        sessionManager.updateUserProfile(user);
                        
                        callback.onSuccess(user);
                    } else {
                        String message = response.optString("message", "Failed to fetch profile");
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing profile response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Get profile error: " + error);
                
                // Return cached user data if API is not available
                if (statusCode == -1) { // Network error
                    User cachedUser = sessionManager.getCurrentUser();
                    if (cachedUser != null) {
                        callback.onSuccess(cachedUser);
                    } else {
                        callback.onError("Không có dữ liệu người dùng");
                    }
                } else {
                    callback.onError(getErrorMessage(error, statusCode));
                }
            }
        });
    }

    // Update user profile
    public void updateUserProfile(User user, UserCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("full_name", user.getFullName());
            requestBody.put("phone", user.getPhone());
            requestBody.put("address", user.getAddress());

            apiService.put(ApiConfig.UPDATE_PROFILE, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            String message = response.optString("message", "Profile updated successfully");
                            
                            // Update session with new data
                            sessionManager.updateUserProfile(user);
                            
                            callback.onUpdateSuccess(message);
                        } else {
                            String message = response.optString("message", "Failed to update profile");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing update response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Update profile error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating update request", e);
            callback.onError("Failed to create update request");
        }
    }

    // Change password
    public void changePassword(String currentPassword, String newPassword, UserCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("current_password", currentPassword);
            requestBody.put("new_password", newPassword);
            requestBody.put("new_password_confirmation", newPassword);

            apiService.put("user/change-password", requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            String message = response.optString("message", "Password changed successfully");
                            callback.onUpdateSuccess(message);
                        } else {
                            String message = response.optString("message", "Failed to change password");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing change password response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Change password error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating change password request", e);
            callback.onError("Failed to create change password request");
        }
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

    // Validate profile data
    public boolean validateProfile(User user) {
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return false;
        }

        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            return false;
        }

        // Validate phone number format (Vietnamese phone numbers)
        String phone = user.getPhone().replaceAll("\\s", "");
        if (!phone.matches("(\\+84|0)[3-9]\\d{8}")) {
            return false;
        }

        return true;
    }

    // Validate password
    public boolean validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        // Check if password contains at least one digit and one letter
        boolean hasDigit = false;
        boolean hasLetter = false;

        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLetter(c)) {
                hasLetter = true;
            }
        }

        return hasDigit && hasLetter;
    }

    // Get current user from session
    public User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }

    // Check if user is bank officer
    public boolean isBankOfficer() {
        return sessionManager.isBankOfficer();
    }

    // Get user display name
    public String getUserDisplayName() {
        User user = getCurrentUser();
        if (user != null && user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            return user.getFullName();
        }
        return "Người dùng";
    }

    // Get user account status
    public String getAccountStatus() {
        User user = getCurrentUser();
        if (user != null) {
            if (user.isActive()) {
                return user.isBankOfficer() ? "Nhân viên ngân hàng" : "Khách hàng cá nhân";
            } else {
                return "Tài khoản bị khóa";
            }
        }
        return "Không xác định";
    }

    // Format phone number for display
    public String formatPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }

        String cleanPhone = phone.replaceAll("\\s", "");
        
        // Convert to +84 format if starts with 0
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "+84" + cleanPhone.substring(1);
        }

        // Format as +84 XXX XXX XXX
        if (cleanPhone.length() >= 12) {
            return cleanPhone.substring(0, 3) + " " + 
                   cleanPhone.substring(3, 6) + " " + 
                   cleanPhone.substring(6, 9) + " " + 
                   cleanPhone.substring(9);
        }

        return cleanPhone;
    }

    // Get error message
    private String getErrorMessage(String error, int statusCode) {
        switch (statusCode) {
            case ApiConfig.UNAUTHORIZED:
                return "Phiên đăng nhập đã hết hạn";
            case ApiConfig.FORBIDDEN:
                return "Bạn không có quyền thực hiện thao tác này";
            case ApiConfig.BAD_REQUEST:
                return "Thông tin không hợp lệ";
            case ApiConfig.UNPROCESSABLE_ENTITY:
                return "Dữ liệu không đúng định dạng";
            case ApiConfig.INTERNAL_SERVER_ERROR:
                return "Lỗi hệ thống. Vui lòng thử lại sau";
            default:
                return error != null ? error : "Đã xảy ra lỗi không xác định";
        }
    }
}
