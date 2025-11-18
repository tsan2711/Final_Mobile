package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class EkycService {
    private static final String TAG = "EkycService";
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public EkycService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    // Callback interface for eKYC operations
    public interface EkycCallback {
        void onSuccess(JSONObject data);
        void onError(String error);
    }

    // Upload face image
    public void uploadFaceImage(File imageFile, EkycCallback callback) {
        if (imageFile == null || !imageFile.exists()) {
            callback.onError("Image file not found");
            return;
        }

        apiService.postMultipart(ApiConfig.UPLOAD_FACE_IMAGE, imageFile, "faceImage", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        JSONObject data = response.getJSONObject("data");
                        callback.onSuccess(data);
                    } else {
                        String message = response.optString("message", "Failed to upload face image");
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing upload response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Upload face image error: " + error);
                callback.onError(getErrorMessage(error, statusCode));
            }
        });
    }

    // Verify identity for high-value transaction
    public void verifyIdentity(String transactionId, double amount, String faceImageBase64, EkycCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("transaction_id", transactionId);
            requestBody.put("transactionId", transactionId);
            requestBody.put("amount", amount);
            
            if (faceImageBase64 != null && !faceImageBase64.isEmpty()) {
                requestBody.put("faceImage", faceImageBase64);
            }

            apiService.post(ApiConfig.VERIFY_IDENTITY, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            callback.onSuccess(data);
                        } else {
                            String message = response.optString("message", "Identity verification failed");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing verify identity response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Verify identity error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating verify identity request", e);
            callback.onError("Failed to create verification request");
        }
    }

    // Get verification status
    public void getVerificationStatus(EkycCallback callback) {
        apiService.get(ApiConfig.GET_VERIFICATION_STATUS, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        JSONObject data = response.getJSONObject("data");
                        callback.onSuccess(data);
                    } else {
                        String message = response.optString("message", "Failed to get verification status");
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing verification status response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Get verification status error: " + error);
                callback.onError(getErrorMessage(error, statusCode));
            }
        });
    }

    // Check if transaction requires biometric verification
    public static boolean requiresBiometricVerification(double amount) {
        // High-value threshold: 10,000,000 VND
        return amount >= 10000000;
    }

    // Get error message
    private String getErrorMessage(String error, int statusCode) {
        if (error != null && !error.isEmpty()) {
            String lowerError = error.toLowerCase();
            if (lowerError.contains("ekyc verification not found") || lowerError.contains("upload your face")) {
                return "Vui lòng hoàn thành xác thực eKYC trước khi thực hiện giao dịch";
            }
            if (lowerError.contains("not valid") || lowerError.contains("expired")) {
                return "Xác thực eKYC không hợp lệ hoặc đã hết hạn. Vui lòng xác thực lại";
            }
            if (lowerError.contains("face verification failed") || lowerError.contains("does not match")) {
                return "Xác thực khuôn mặt thất bại. Khuôn mặt không khớp với ảnh đã đăng ký";
            }
            if (error.contains("không") || error.contains("lỗi") || error.contains("vui lòng")) {
                return error;
            }
        }
        
        switch (statusCode) {
            case ApiConfig.UNAUTHORIZED:
                return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại";
            case ApiConfig.FORBIDDEN:
                return "Bạn không có quyền thực hiện thao tác này";
            case ApiConfig.BAD_REQUEST:
                return error != null && !error.isEmpty() ? error : "Thông tin không hợp lệ";
            case ApiConfig.NOT_FOUND:
                return "Không tìm thấy thông tin xác thực";
            case ApiConfig.INTERNAL_SERVER_ERROR:
                return "Lỗi hệ thống. Vui lòng thử lại sau";
            default:
                return error != null && !error.isEmpty() ? error : "Đã xảy ra lỗi. Vui lòng thử lại";
        }
    }
}

