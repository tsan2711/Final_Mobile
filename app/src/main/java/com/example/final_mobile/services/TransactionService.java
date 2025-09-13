package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import com.example.final_mobile.models.Transaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TransactionService {
    private static final String TAG = "TransactionService";
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public TransactionService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    // Callback interface for transaction operations
    public interface TransactionCallback {
        void onSuccess(List<Transaction> transactions);
        void onSingleTransactionSuccess(Transaction transaction);
        void onError(String error);
        void onOtpRequired(String message, String transactionId);
    }

    // Get user transactions
    public void getUserTransactions(int page, int limit, TransactionCallback callback) {
        String endpoint = ApiConfig.GET_TRANSACTIONS + "?page=" + page + "&limit=" + limit;
        
        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        JSONArray transactionsArray = response.getJSONArray("data");
                        List<Transaction> transactions = parseTransactionsFromJson(transactionsArray);
                        callback.onSuccess(transactions);
                    } else {
                        String message = response.optString("message", "Failed to fetch transactions");
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing transactions response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Get transactions error: " + error);
                
                // Return dummy data for demo if API is not available
                if (statusCode == -1) { // Network error
                    List<Transaction> dummyTransactions = createDummyTransactions();
                    callback.onSuccess(dummyTransactions);
                } else {
                    callback.onError(getErrorMessage(error, statusCode));
                }
            }
        });
    }

    // Transfer money
    public void transferMoney(String toAccountNumber, BigDecimal amount, String description, TransactionCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("to_account_number", toAccountNumber);
            requestBody.put("amount", amount.toString());
            requestBody.put("description", description);
            requestBody.put("transaction_type", Transaction.TYPE_TRANSFER);

            apiService.post(ApiConfig.TRANSFER_MONEY, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            
                            // Check if OTP is required
                            boolean otpRequired = data.optBoolean("otp_required", false);
                            if (otpRequired) {
                                String transactionId = data.getString("transaction_id");
                                String message = data.optString("message", "OTP verification required");
                                callback.onOtpRequired(message, transactionId);
                                return;
                            }
                            
                            // Parse transaction result
                            Transaction transaction = parseTransactionFromJson(data);
                            callback.onSingleTransactionSuccess(transaction);
                        } else {
                            String message = response.optString("message", "Transfer failed");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing transfer response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Transfer money error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating transfer request", e);
            callback.onError("Failed to create transfer request");
        }
    }

    // Verify transaction with OTP
    public void verifyTransactionOtp(String transactionId, String otpCode, TransactionCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("transaction_id", transactionId);
            requestBody.put("otp_code", otpCode);

            apiService.post(ApiConfig.VERIFY_OTP, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            Transaction transaction = parseTransactionFromJson(data);
                            callback.onSingleTransactionSuccess(transaction);
                        } else {
                            String message = response.optString("message", "OTP verification failed");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing OTP verification response", e);
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
            Log.e(TAG, "Error creating OTP verification request", e);
            callback.onError("Failed to create OTP verification request");
        }
    }

    // Get transaction detail
    public void getTransactionDetail(String transactionId, TransactionCallback callback) {
        String endpoint = ApiConfig.GET_TRANSACTION_DETAIL.replace("{id}", transactionId);
        
        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        JSONObject transactionData = response.getJSONObject("data");
                        Transaction transaction = parseTransactionFromJson(transactionData);
                        callback.onSingleTransactionSuccess(transaction);
                    } else {
                        String message = response.optString("message", "Failed to fetch transaction detail");
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing transaction detail response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Get transaction detail error: " + error);
                callback.onError(getErrorMessage(error, statusCode));
            }
        });
    }

    // Parse transactions from JSON array
    private List<Transaction> parseTransactionsFromJson(JSONArray transactionsArray) throws JSONException {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < transactionsArray.length(); i++) {
            JSONObject transactionJson = transactionsArray.getJSONObject(i);
            Transaction transaction = parseTransactionFromJson(transactionJson);
            transactions.add(transaction);
        }
        return transactions;
    }

    // Parse single transaction from JSON
    private Transaction parseTransactionFromJson(JSONObject transactionJson) throws JSONException {
        Transaction transaction = new Transaction();
        transaction.setId(transactionJson.getString("id"));
        transaction.setFromAccountId(transactionJson.optString("from_account_id", ""));
        transaction.setToAccountId(transactionJson.optString("to_account_id", ""));
        transaction.setFromAccountNumber(transactionJson.optString("from_account_number", ""));
        transaction.setToAccountNumber(transactionJson.optString("to_account_number", ""));
        transaction.setAmount(new BigDecimal(transactionJson.getString("amount")));
        transaction.setCurrency(transactionJson.optString("currency", "VND"));
        transaction.setTransactionType(transactionJson.getString("transaction_type"));
        transaction.setStatus(transactionJson.getString("status"));
        transaction.setDescription(transactionJson.optString("description", ""));
        transaction.setReferenceNumber(transactionJson.optString("reference_number", ""));
        
        // Parse dates
        // In real app, you'd parse these from ISO date strings
        transaction.setCreatedAt(new Date());
        
        return transaction;
    }

    // Create dummy transactions for demo
    private List<Transaction> createDummyTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        
        // Today's transactions
        Transaction tx1 = new Transaction();
        tx1.setId("tx_001");
        tx1.setFromAccountId("acc_001");
        tx1.setToAccountNumber("9876543210123456");
        tx1.setAmount(new BigDecimal("1500000"));
        tx1.setTransactionType(Transaction.TYPE_TRANSFER);
        tx1.setStatus(Transaction.STATUS_COMPLETED);
        tx1.setDescription("Chuyển tiền đến Nguyễn Văn B");
        tx1.setReferenceNumber("TXN1234567890");
        tx1.setCreatedAt(cal.getTime());
        transactions.add(tx1);
        
        cal.add(Calendar.HOUR, -4);
        Transaction tx2 = new Transaction();
        tx2.setId("tx_002");
        tx2.setFromAccountId("acc_001");
        tx2.setAmount(new BigDecimal("850000"));
        tx2.setTransactionType(Transaction.TYPE_PAYMENT);
        tx2.setStatus(Transaction.STATUS_COMPLETED);
        tx2.setDescription("Thanh toán hóa đơn điện EVN");
        tx2.setReferenceNumber("HD123456");
        tx2.setCreatedAt(cal.getTime());
        transactions.add(tx2);
        
        // Yesterday's transactions
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 15);
        
        Transaction tx3 = new Transaction();
        tx3.setId("tx_003");
        tx3.setToAccountId("acc_001");
        tx3.setFromAccountNumber("VCB****1234");
        tx3.setAmount(new BigDecimal("5000000"));
        tx3.setTransactionType(Transaction.TYPE_DEPOSIT);
        tx3.setStatus(Transaction.STATUS_COMPLETED);
        tx3.setDescription("Nạp tiền từ ngân hàng VCB");
        tx3.setReferenceNumber("VCB987654321");
        tx3.setCreatedAt(cal.getTime());
        transactions.add(tx3);
        
        cal.set(Calendar.HOUR_OF_DAY, 16);
        cal.set(Calendar.MINUTE, 45);
        
        Transaction tx4 = new Transaction();
        tx4.setId("tx_004");
        tx4.setFromAccountId("acc_001");
        tx4.setAmount(new BigDecimal("200000"));
        tx4.setTransactionType(Transaction.TYPE_TOPUP);
        tx4.setStatus(Transaction.STATUS_COMPLETED);
        tx4.setDescription("Nạp tiền điện thoại Viettel");
        tx4.setReferenceNumber("0987654321");
        tx4.setCreatedAt(cal.getTime());
        transactions.add(tx4);
        
        // This week's transactions
        cal.add(Calendar.DAY_OF_MONTH, -3);
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 30);
        
        Transaction tx5 = new Transaction();
        tx5.setId("tx_005");
        tx5.setFromAccountId("acc_001");
        tx5.setToAccountNumber("5555666777888999");
        tx5.setAmount(new BigDecimal("3200000"));
        tx5.setTransactionType(Transaction.TYPE_TRANSFER);
        tx5.setStatus(Transaction.STATUS_COMPLETED);
        tx5.setDescription("Chuyển tiền đến Trần Thị C");
        tx5.setReferenceNumber("TXN9876543210");
        tx5.setCreatedAt(cal.getTime());
        transactions.add(tx5);
        
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 11);
        cal.set(Calendar.MINUTE, 20);
        
        Transaction tx6 = new Transaction();
        tx6.setId("tx_006");
        tx6.setToAccountId("acc_001");
        tx6.setFromAccountNumber("5555666777999888");
        tx6.setAmount(new BigDecimal("800000"));
        tx6.setTransactionType(Transaction.TYPE_TRANSFER);
        tx6.setStatus(Transaction.STATUS_COMPLETED);
        tx6.setDescription("Nhận tiền từ Lê Văn D");
        tx6.setReferenceNumber("TXN5555666777");
        tx6.setCreatedAt(cal.getTime());
        transactions.add(tx6);
        
        Log.d(TAG, "Created " + transactions.size() + " dummy transactions");
        return transactions;
    }

    // Get recent transactions (last 5)
    public void getRecentTransactions(TransactionCallback callback) {
        getUserTransactions(1, 5, callback);
    }

    // Validate account number format
    public boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove spaces and check if it's all digits and has correct length
        String cleanAccountNumber = accountNumber.replaceAll("\\s", "");
        return cleanAccountNumber.matches("\\d{10,16}"); // 10-16 digits
    }

    // Validate transfer amount
    public boolean isValidTransferAmount(BigDecimal amount, BigDecimal availableBalance) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Minimum transfer amount: 10,000 VND
        BigDecimal minAmount = new BigDecimal("10000");
        if (amount.compareTo(minAmount) < 0) {
            return false;
        }
        
        // Check if sufficient balance
        return availableBalance != null && availableBalance.compareTo(amount) >= 0;
    }

    // Get error message
    private String getErrorMessage(String error, int statusCode) {
        switch (statusCode) {
            case ApiConfig.UNAUTHORIZED:
                return "Phiên đăng nhập đã hết hạn";
            case ApiConfig.FORBIDDEN:
                return "Bạn không có quyền thực hiện giao dịch này";
            case ApiConfig.BAD_REQUEST:
                return "Thông tin giao dịch không hợp lệ";
            case ApiConfig.UNPROCESSABLE_ENTITY:
                return "Số dư không đủ hoặc tài khoản không tồn tại";
            case ApiConfig.INTERNAL_SERVER_ERROR:
                return "Lỗi hệ thống. Vui lòng thử lại sau";
            default:
                return error != null ? error : "Đã xảy ra lỗi không xác định";
        }
    }
}
