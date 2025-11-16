package com.example.final_mobile.services;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UtilityService {
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public UtilityService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    public interface UtilityCallback {
        void onInitiateSuccess(String transactionId, String otp, UtilityPayment payment);
        void onVerifySuccess(String message);
        void onError(String error);
    }

    public interface ProviderCallback {
        void onSuccess(List<ServiceProvider> providers);
        void onError(String error);
    }

    // Pay electricity bill
    public void payElectricityBill(String accountId, String customerNumber, BigDecimal amount, 
                                   String customerName, String period, UtilityCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            if (accountId != null && !accountId.isEmpty()) {
                requestBody.put("accountId", accountId);
            }
            requestBody.put("customerNumber", customerNumber);
            requestBody.put("amount", amount.doubleValue());
            if (customerName != null) requestBody.put("customerName", customerName);
            if (period != null) requestBody.put("period", period);

            apiService.post("utilities/pay-electricity", requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            String transactionId = data.getString("transaction_id");
                            String otp = data.optString("developmentOTP", data.optString("development_otp", ""));
                            
                            UtilityPayment payment = parseUtilityPayment(data);
                            callback.onInitiateSuccess(transactionId, otp, payment);
                        } else {
                            callback.onError(response.optString("message", "Payment initiation failed"));
                        }
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    callback.onError(error);
                }
            });
        } catch (JSONException e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    // Pay water bill
    public void payWaterBill(String accountId, String customerNumber, BigDecimal amount,
                            String customerName, String period, UtilityCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            if (accountId != null && !accountId.isEmpty()) {
                requestBody.put("accountId", accountId);
            }
            requestBody.put("customerNumber", customerNumber);
            requestBody.put("amount", amount.doubleValue());
            if (customerName != null) requestBody.put("customerName", customerName);
            if (period != null) requestBody.put("period", period);

            apiService.post("utilities/pay-water", requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            String transactionId = data.getString("transaction_id");
                            String otp = data.optString("developmentOTP", data.optString("development_otp", ""));
                            
                            UtilityPayment payment = parseUtilityPayment(data);
                            callback.onInitiateSuccess(transactionId, otp, payment);
                        } else {
                            callback.onError(response.optString("message", "Payment initiation failed"));
                        }
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    callback.onError(error);
                }
            });
        } catch (JSONException e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    // Pay internet bill
    public void payInternetBill(String accountId, String customerNumber, BigDecimal amount,
                               String provider, String customerName, UtilityCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            if (accountId != null && !accountId.isEmpty()) {
                requestBody.put("accountId", accountId);
            }
            requestBody.put("customerNumber", customerNumber);
            requestBody.put("amount", amount.doubleValue());
            if (provider != null) requestBody.put("provider", provider);
            if (customerName != null) requestBody.put("customerName", customerName);

            apiService.post("utilities/pay-internet", requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            String transactionId = data.getString("transaction_id");
                            String otp = data.optString("developmentOTP", data.optString("development_otp", ""));
                            
                            UtilityPayment payment = parseUtilityPayment(data);
                            callback.onInitiateSuccess(transactionId, otp, payment);
                        } else {
                            callback.onError(response.optString("message", "Payment initiation failed"));
                        }
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    callback.onError(error);
                }
            });
        } catch (JSONException e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    // Mobile topup
    public void mobileTopup(String accountId, String phoneNumber, BigDecimal amount,
                           String provider, UtilityCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            if (accountId != null && !accountId.isEmpty()) {
                requestBody.put("accountId", accountId);
            }
            requestBody.put("phoneNumber", phoneNumber);
            requestBody.put("amount", amount.doubleValue());
            if (provider != null) requestBody.put("provider", provider);

            apiService.post("utilities/mobile-topup", requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            String transactionId = data.getString("transaction_id");
                            String otp = data.optString("developmentOTP", data.optString("development_otp", ""));
                            
                            UtilityPayment payment = parseUtilityPayment(data);
                            callback.onInitiateSuccess(transactionId, otp, payment);
                        } else {
                            callback.onError(response.optString("message", "Payment initiation failed"));
                        }
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    callback.onError(error);
                }
            });
        } catch (JSONException e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    // Verify utility OTP
    public void verifyUtilityOTP(String transactionId, String otpCode, UtilityCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("transactionId", transactionId);
            requestBody.put("otpCode", otpCode);

            apiService.post("utilities/verify-otp", requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Payment completed successfully");
                            callback.onVerifySuccess(message);
                        } else {
                            callback.onError(response.optString("message", "OTP verification failed"));
                        }
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    callback.onError(error);
                }
            });
        } catch (JSONException e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    // Get service providers
    public void getServiceProviders(String serviceType, ProviderCallback callback) {
        String endpoint = "utilities/providers";
        if (serviceType != null && !serviceType.isEmpty()) {
            endpoint += "?serviceType=" + serviceType;
        }

        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        JSONArray providersArray = response.optJSONArray("data");
                        if (providersArray != null) {
                            List<ServiceProvider> providers = new ArrayList<>();
                            for (int i = 0; i < providersArray.length(); i++) {
                                JSONObject providerJson = providersArray.getJSONObject(i);
                                ServiceProvider provider = new ServiceProvider();
                                provider.setCode(providerJson.getString("code"));
                                provider.setName(providerJson.getString("name"));
                                provider.setLogo(providerJson.optString("logo", ""));
                                providers.add(provider);
                            }
                            callback.onSuccess(providers);
                        } else {
                            callback.onError("No providers data");
                        }
                    } else {
                        callback.onError(response.optString("message", "Failed to get providers"));
                    }
                } catch (JSONException e) {
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                callback.onError(error);
            }
        });
    }

    private UtilityPayment parseUtilityPayment(JSONObject data) throws JSONException {
        UtilityPayment payment = new UtilityPayment();
        payment.setTransactionId(data.optString("transaction_id", ""));
        payment.setServiceType(data.optString("service_type", ""));
        payment.setProvider(data.optString("provider", ""));
        payment.setServiceNumber(data.optString("service_number", ""));
        payment.setAmount(BigDecimal.valueOf(data.optDouble("amount", 0)));
        payment.setFee(BigDecimal.valueOf(data.optDouble("fee", 0)));
        payment.setTotalAmount(BigDecimal.valueOf(data.optDouble("total_amount", 0)));
        payment.setCurrency(data.optString("currency", "VND"));
        payment.setDescription(data.optString("description", ""));
        return payment;
    }

    // Inner classes for data models
    public static class UtilityPayment {
        private String transactionId;
        private String serviceType;
        private String provider;
        private String serviceNumber;
        private BigDecimal amount;
        private BigDecimal fee;
        private BigDecimal totalAmount;
        private String currency;
        private String description;

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getServiceNumber() { return serviceNumber; }
        public void setServiceNumber(String serviceNumber) { this.serviceNumber = serviceNumber; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public BigDecimal getFee() { return fee; }
        public void setFee(BigDecimal fee) { this.fee = fee; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getFormattedAmount() {
            return String.format("%,.0f %s", amount.doubleValue(), currency);
        }

        public String getFormattedTotalAmount() {
            return String.format("%,.0f %s", totalAmount.doubleValue(), currency);
        }
    }

    public static class ServiceProvider {
        private String code;
        private String name;
        private String logo;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }
    }
}
