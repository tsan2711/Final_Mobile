package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

public class PaymentGatewayService {
    private static final String TAG = "PaymentGatewayService";
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public PaymentGatewayService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    // Callback interface for payment operations
    public interface PaymentCallback {
        void onSuccess(PaymentResult result);
        void onError(String error);
    }

    // Payment result model
    public static class PaymentResult {
        private String paymentId;
        private String paymentUrl; // For VNPay
        private String paymentIntentId; // For Stripe
        private String clientSecret; // For Stripe
        private String publishableKey; // For Stripe
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String status;
        private String orderId; // For VNPay
        private String transferReference; // For bank transfer
        private String transactionId; // Linked transaction ID

        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        public String getPaymentUrl() { return paymentUrl; }
        public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

        public String getPaymentIntentId() { return paymentIntentId; }
        public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getPublishableKey() { return publishableKey; }
        public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getTransferReference() { return transferReference; }
        public void setTransferReference(String transferReference) { this.transferReference = transferReference; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    }

    /**
     * Create VNPay payment
     */
    public void createVnpayPayment(String accountId, BigDecimal amount, String description, 
                                    String returnUrl, String cancelUrl, PaymentCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("accountId", accountId);
            requestBody.put("amount", amount.toString());
            if (description != null && !description.isEmpty()) {
                requestBody.put("description", description);
            }
            if (returnUrl != null) {
                requestBody.put("returnUrl", returnUrl);
            }
            if (cancelUrl != null) {
                requestBody.put("cancelUrl", cancelUrl);
            }

            apiService.post(ApiConfig.VNPAY_CREATE_PAYMENT, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            PaymentResult result = new PaymentResult();
                            result.setPaymentId(data.getString("paymentId"));
                            result.setPaymentUrl(data.getString("paymentUrl"));
                            result.setOrderId(data.optString("orderId"));
                            result.setAmount(new BigDecimal(data.getString("amount")));
                            result.setCurrency(data.optString("currency", "VND"));
                            result.setPaymentMethod("VNPAY");
                            result.setStatus("PENDING");
                            callback.onSuccess(result);
                        } else {
                            String message = response.optString("message", "Failed to create VNPay payment");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing VNPay payment response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Create VNPay payment error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating VNPay payment request", e);
            callback.onError("Failed to create payment request");
        }
    }

    /**
     * Create external bank transfer
     */
    public void createBankTransfer(String accountId, BigDecimal amount, String bankName, 
                                    String bankCode, String recipientAccountNumber, 
                                    String recipientName, String description, PaymentCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("accountId", accountId);
            requestBody.put("amount", amount.toString());
            requestBody.put("bankName", bankName);
            if (bankCode != null && !bankCode.isEmpty()) {
                requestBody.put("bankCode", bankCode);
            }
            requestBody.put("recipientAccountNumber", recipientAccountNumber);
            requestBody.put("recipientName", recipientName);
            if (description != null && !description.isEmpty()) {
                requestBody.put("description", description);
            }

            apiService.post(ApiConfig.BANK_TRANSFER, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            PaymentResult result = new PaymentResult();
                            result.setPaymentId(data.getString("paymentId"));
                            result.setTransactionId(data.optString("transactionId"));
                            result.setTransferReference(data.optString("transferReference"));
                            result.setAmount(new BigDecimal(data.getString("amount")));
                            result.setCurrency(data.optString("currency", "VND"));
                            result.setPaymentMethod("BANK_TRANSFER");
                            result.setStatus(data.optString("status", "COMPLETED"));
                            callback.onSuccess(result);
                        } else {
                            String message = response.optString("message", "Failed to create bank transfer");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing bank transfer response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Create bank transfer error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating bank transfer request", e);
            callback.onError("Failed to create transfer request");
        }
    }

    /**
     * Get payment status
     */
    public void getPaymentStatus(String paymentId, PaymentCallback callback) {
        String endpoint = ApiConfig.GET_PAYMENT_STATUS.replace("{paymentId}", paymentId);

        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        JSONObject data = response.getJSONObject("data");
                        PaymentResult result = new PaymentResult();
                        result.setPaymentId(data.getString("paymentId"));
                        result.setAmount(new BigDecimal(data.getString("amount")));
                        result.setCurrency(data.optString("currency", "VND"));
                        result.setPaymentMethod(data.getString("paymentMethod"));
                        result.setStatus(data.getString("status"));
                        if (data.has("transaction")) {
                            JSONObject transaction = data.getJSONObject("transaction");
                            result.setTransactionId(transaction.optString("transactionId"));
                        }
                        callback.onSuccess(result);
                    } else {
                        String message = response.optString("message", "Failed to get payment status");
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing payment status response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Get payment status error: " + error);
                callback.onError(getErrorMessage(error, statusCode));
            }
        });
    }

    /**
     * Get payment history
     */
    public void getPaymentHistory(int page, int limit, String paymentMethod, String status, 
                                  PaymentHistoryCallback callback) {
        StringBuilder endpoint = new StringBuilder(ApiConfig.GET_PAYMENT_HISTORY);
        endpoint.append("?page=").append(page).append("&limit=").append(limit);
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            endpoint.append("&paymentMethod=").append(paymentMethod);
        }
        if (status != null && !status.isEmpty()) {
            endpoint.append("&status=").append(status);
        }

        apiService.get(endpoint.toString(), new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        callback.onSuccess(response.getJSONObject("data"));
                    } else {
                        String message = response.optString("message", "Failed to get payment history");
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing payment history response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Get payment history error: " + error);
                callback.onError(getErrorMessage(error, statusCode));
            }
        });
    }

    // Callback interface for payment history
    public interface PaymentHistoryCallback {
        void onSuccess(org.json.JSONObject data);
        void onError(String error);
    }

    private String getErrorMessage(String error, int statusCode) {
        if (statusCode == 401) {
            return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
        } else if (statusCode == 403) {
            return "Bạn không có quyền thực hiện thao tác này.";
        } else if (statusCode == 404) {
            return "Không tìm thấy thông tin thanh toán.";
        } else if (statusCode == 400) {
            return error;
        } else if (statusCode == -1) {
            return "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.";
        } else {
            return error != null && !error.isEmpty() ? error : "Đã xảy ra lỗi. Vui lòng thử lại.";
        }
    }
}

