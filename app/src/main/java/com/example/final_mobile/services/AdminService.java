package com.example.final_mobile.services;

import android.content.Context;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminService {
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public AdminService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    // Callback interfaces
    public interface AdminCallback {
        void onSuccess(Object data);
        void onError(String error);
    }

    public interface CustomerListCallback {
        void onSuccess(List<CustomerInfo> customers, int total, int page, int totalPages);
        void onError(String error);
    }

    public interface DashboardStatsCallback {
        void onSuccess(DashboardStats stats);
        void onError(String error);
    }

    public interface TransactionListCallback {
        void onSuccess(List<RecentTransaction> transactions, int total, int page, int totalPages, boolean hasNextPage);
        void onError(String error);
    }

    // Customer info model
    public static class CustomerInfo {
        private String id;
        private String email;
        private String fullName;
        private String phone;
        private int accountCount;
        private Account primaryAccount;
        private List<Account> checkingAccounts;
        private List<Account> savingAccounts;
        private List<Account> mortgageAccounts;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public int getAccountCount() { return accountCount; }
        public void setAccountCount(int accountCount) { this.accountCount = accountCount; }
        public Account getPrimaryAccount() { return primaryAccount; }
        public void setPrimaryAccount(Account primaryAccount) { this.primaryAccount = primaryAccount; }
        public List<Account> getCheckingAccounts() { return checkingAccounts; }
        public void setCheckingAccounts(List<Account> checkingAccounts) { this.checkingAccounts = checkingAccounts; }
        public List<Account> getSavingAccounts() { return savingAccounts; }
        public void setSavingAccounts(List<Account> savingAccounts) { this.savingAccounts = savingAccounts; }
        public List<Account> getMortgageAccounts() { return mortgageAccounts; }
        public void setMortgageAccounts(List<Account> mortgageAccounts) { this.mortgageAccounts = mortgageAccounts; }
    }

    // Dashboard stats model
    public static class DashboardStats {
        private int totalCustomers;
        private int activeAccounts;
        private BigDecimal totalBalance;
        private int todayTransactions;
        private List<RecentTransaction> recentTransactions;

        public int getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(int totalCustomers) { this.totalCustomers = totalCustomers; }
        public int getActiveAccounts() { return activeAccounts; }
        public void setActiveAccounts(int activeAccounts) { this.activeAccounts = activeAccounts; }
        public BigDecimal getTotalBalance() { return totalBalance; }
        public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }
        public int getTodayTransactions() { return todayTransactions; }
        public void setTodayTransactions(int todayTransactions) { this.todayTransactions = todayTransactions; }
        public List<RecentTransaction> getRecentTransactions() { return recentTransactions; }
        public void setRecentTransactions(List<RecentTransaction> recentTransactions) { this.recentTransactions = recentTransactions; }
    }

    public static class RecentTransaction {
        private String transactionId;
        private BigDecimal amount;
        private String type;
        private String status;
        private String description;
        private String createdAt;

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // Get dashboard stats
    public void getDashboardStats(DashboardStatsCallback callback) {
        android.util.Log.d("AdminService", "Calling dashboard API: " + ApiConfig.ADMIN_DASHBOARD);
        apiService.get(ApiConfig.ADMIN_DASHBOARD, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                android.util.Log.d("AdminService", "Dashboard API response received: " + response.toString());
                try {
                    if (response == null) {
                        android.util.Log.e("AdminService", "Response is null");
                        callback.onError("Response is null");
                        return;
                    }
                    
                    boolean success = response.optBoolean("success", false);
                    android.util.Log.d("AdminService", "Success flag: " + success);
                    
                    if (success) {
                        JSONObject data = response.optJSONObject("data");
                        if (data == null) {
                            android.util.Log.e("AdminService", "Data object is null in response");
                            callback.onError("Data object is null in response");
                            return;
                        }
                        
                        android.util.Log.d("AdminService", "Data object: " + data.toString());
                        
                        DashboardStats stats = new DashboardStats();
                        stats.setTotalCustomers(data.optInt("total_customers", 0));
                        stats.setActiveAccounts(data.optInt("active_accounts", 0));
                        
                        android.util.Log.d("AdminService", "Total customers: " + stats.getTotalCustomers());
                        android.util.Log.d("AdminService", "Active accounts: " + stats.getActiveAccounts());
                        
                        // Handle total_balance as number
                        BigDecimal totalBalance = BigDecimal.ZERO;
                        if (data.has("total_balance") && !data.isNull("total_balance")) {
                            try {
                                if (data.get("total_balance") instanceof Number) {
                                    totalBalance = BigDecimal.valueOf(data.getDouble("total_balance"));
                                } else {
                                    String balanceStr = data.getString("total_balance");
                                    if (balanceStr != null && !balanceStr.isEmpty()) {
                                        totalBalance = new BigDecimal(balanceStr);
                                    }
                                }
                            } catch (Exception e) {
                                android.util.Log.e("AdminService", "Error parsing total_balance: " + e.getMessage());
                                totalBalance = BigDecimal.ZERO;
                            }
                        }
                        stats.setTotalBalance(totalBalance);
                        
                        stats.setTodayTransactions(data.optInt("today_transactions", 0));

                        // Parse recent transactions
                        JSONArray transactionsArray = data.optJSONArray("recent_transactions");
                        List<RecentTransaction> transactions = new ArrayList<>();
                        if (transactionsArray != null) {
                            for (int i = 0; i < transactionsArray.length(); i++) {
                                JSONObject t = transactionsArray.getJSONObject(i);
                                RecentTransaction transaction = new RecentTransaction();
                                transaction.setTransactionId(t.optString("transaction_id", ""));
                                
                                // Handle amount as number or string
                                if (t.has("amount")) {
                                    if (t.get("amount") instanceof Number) {
                                        transaction.setAmount(BigDecimal.valueOf(t.getDouble("amount")));
                                    } else {
                                        transaction.setAmount(new BigDecimal(t.getString("amount")));
                                    }
                                } else {
                                    transaction.setAmount(BigDecimal.ZERO);
                                }
                                
                                transaction.setType(t.optString("type", ""));
                                transaction.setStatus(t.optString("status", ""));
                                transaction.setDescription(t.optString("description", ""));
                                
                                // Handle created_at - might be date object or string
                                String createdAt = "";
                                if (t.has("created_at")) {
                                    if (t.get("created_at") instanceof String) {
                                        createdAt = t.getString("created_at");
                                    } else {
                                        // If it's a date object, convert to string
                                        createdAt = t.optString("created_at", "");
                                    }
                                }
                                transaction.setCreatedAt(createdAt);
                                
                                transactions.add(transaction);
                            }
                        }
                        stats.setRecentTransactions(transactions);
                        
                        android.util.Log.d("AdminService", "Dashboard stats parsed successfully. Customers: " + stats.getTotalCustomers());

                        callback.onSuccess(stats);
                    } else {
                        String errorMsg = response.optString("message", "Failed to get dashboard stats");
                        android.util.Log.e("AdminService", "API returned success=false: " + errorMsg);
                        callback.onError(errorMsg);
                    }
                } catch (JSONException e) {
                    android.util.Log.e("AdminService", "JSONException parsing dashboard response: " + e.getMessage(), e);
                    android.util.Log.e("AdminService", "Response was: " + (response != null ? response.toString() : "null"));
                    callback.onError("Error parsing response: " + e.getMessage());
                } catch (Exception e) {
                    android.util.Log.e("AdminService", "Unexpected error parsing dashboard response: " + e.getMessage(), e);
                    callback.onError("Unexpected error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                android.util.Log.e("AdminService", "Dashboard API error: " + error + " (Status: " + statusCode + ")");
                callback.onError(error);
            }
        });
    }

    // Get all customers with pagination
    public void getAllCustomers(int page, int limit, CustomerListCallback callback) {
        String endpoint = ApiConfig.ADMIN_GET_CUSTOMERS + "?page=" + page + "&limit=" + limit;
        android.util.Log.d("AdminService", "Calling getAllCustomers: " + endpoint);
        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                android.util.Log.d("AdminService", "getAllCustomers response received: " + response.toString());
                try {
                    if (response == null) {
                        android.util.Log.e("AdminService", "getAllCustomers response is null");
                        callback.onError("Response is null");
                        return;
                    }
                    
                    boolean success = response.optBoolean("success", false);
                    android.util.Log.d("AdminService", "getAllCustomers success flag: " + success);
                    
                    if (success) {
                        JSONArray customersArray = response.optJSONArray("data");
                        JSONObject meta = response.optJSONObject("meta");
                        
                        if (customersArray == null) {
                            android.util.Log.e("AdminService", "customersArray is null");
                            callback.onError("Data array is null in response");
                            return;
                        }
                        
                        if (meta == null) {
                            android.util.Log.e("AdminService", "meta object is null");
                            callback.onError("Meta object is null in response");
                            return;
                        }

                        android.util.Log.d("AdminService", "Found " + customersArray.length() + " customers in array");

                        List<CustomerInfo> customers = new ArrayList<>();
                        for (int i = 0; i < customersArray.length(); i++) {
                            try {
                                JSONObject customerJson = customersArray.getJSONObject(i);
                                CustomerInfo customer = parseCustomerInfo(customerJson);
                                customers.add(customer);
                                android.util.Log.d("AdminService", "Parsed customer " + (i+1) + ": " + customer.getFullName());
                                android.util.Log.d("AdminService", "  - Checking accounts: " + (customer.getCheckingAccounts() != null ? customer.getCheckingAccounts().size() : 0));
                                android.util.Log.d("AdminService", "  - Saving accounts: " + (customer.getSavingAccounts() != null ? customer.getSavingAccounts().size() : 0));
                                android.util.Log.d("AdminService", "  - Mortgage accounts: " + (customer.getMortgageAccounts() != null ? customer.getMortgageAccounts().size() : 0));
                            } catch (Exception e) {
                                android.util.Log.e("AdminService", "Error parsing customer " + i + ": " + e.getMessage(), e);
                                // Continue with next customer
                            }
                        }

                        int total = meta.optInt("total", 0);
                        int currentPage = meta.optInt("page", page);
                        int totalPages = meta.optInt("total_pages", 1);
                        
                        android.util.Log.d("AdminService", "Total customers: " + total + ", page: " + currentPage + ", totalPages: " + totalPages);
                        android.util.Log.d("AdminService", "Parsed " + customers.size() + " customers successfully");

                        callback.onSuccess(customers, total, currentPage, totalPages);
                    } else {
                        String errorMsg = response.optString("message", "Failed to get customers");
                        android.util.Log.e("AdminService", "getAllCustomers API returned success=false: " + errorMsg);
                        callback.onError(errorMsg);
                    }
                } catch (Exception e) {
                    android.util.Log.e("AdminService", "Unexpected error parsing getAllCustomers response: " + e.getMessage(), e);
                    callback.onError("Unexpected error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                android.util.Log.e("AdminService", "getAllCustomers API error: " + error + " (Status: " + statusCode + ")");
                callback.onError(error);
            }
        });
    }

    // Search customers
    public void searchCustomers(String query, CustomerListCallback callback) {
        String endpoint = ApiConfig.ADMIN_SEARCH_CUSTOMERS + "?query=" + java.net.URLEncoder.encode(query);
        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        JSONArray customersArray = response.getJSONArray("data");

                        List<CustomerInfo> customers = new ArrayList<>();
                        for (int i = 0; i < customersArray.length(); i++) {
                            JSONObject customerJson = customersArray.getJSONObject(i);
                            CustomerInfo customer = parseCustomerInfo(customerJson);
                            customers.add(customer);
                        }

                        int total = customers.size();
                        callback.onSuccess(customers, total, 1, 1);
                    } else {
                        callback.onError(response.optString("message", "Search failed"));
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

    // Create account for customer
    public void createCustomerAccount(String customerId, String accountType, BigDecimal initialBalance, 
                                     BigDecimal interestRate, AdminCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("customerId", customerId);
            requestBody.put("accountType", accountType);
            if (initialBalance != null) {
                requestBody.put("initialBalance", initialBalance.doubleValue());
            }
            if (interestRate != null) {
                requestBody.put("interestRate", interestRate.doubleValue());
            }

            apiService.post(ApiConfig.ADMIN_CREATE_ACCOUNT, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject accountData = response.getJSONObject("data");
                            Account account = parseAccountFromJson(accountData);
                            callback.onSuccess(account);
                        } else {
                            callback.onError(response.optString("message", "Failed to create account"));
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
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    // Update account
    public void updateAccount(String accountId, BigDecimal interestRate, Boolean isActive, 
                             BigDecimal balance, AdminCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            if (interestRate != null) {
                requestBody.put("interestRate", interestRate.doubleValue());
            }
            if (isActive != null) {
                requestBody.put("isActive", isActive);
            }
            if (balance != null) {
                requestBody.put("balance", balance.doubleValue());
            }

            String endpoint = ApiConfig.ADMIN_UPDATE_ACCOUNT.replace("{accountId}", accountId);
            apiService.put(endpoint, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject accountData = response.getJSONObject("data");
                            Account account = parseAccountFromJson(accountData);
                            callback.onSuccess(account);
                        } else {
                            callback.onError(response.optString("message", "Failed to update account"));
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
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    // Deactivate account
    public void deactivateAccount(String accountId, AdminCallback callback) {
        String endpoint = ApiConfig.ADMIN_DEACTIVATE_ACCOUNT.replace("{accountId}", accountId);
        apiService.delete(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        callback.onSuccess("Account deactivated successfully");
                    } else {
                        callback.onError(response.optString("message", "Failed to deactivate account"));
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

    // Helper method to parse account from JSON
    private Account parseAccountFromJson(JSONObject accountJson) throws JSONException {
        Account account = new Account();
        // Handle both id and _id
        String id = accountJson.optString("id", accountJson.optString("_id", ""));
        account.setId(id);
        account.setUserId(accountJson.optString("user_id", ""));
        account.setAccountNumber(accountJson.optString("account_number", ""));
        account.setAccountType(accountJson.optString("account_type", ""));
        
        // Handle balance as number or string
        if (accountJson.has("balance")) {
            if (accountJson.get("balance") instanceof Number) {
                account.setBalance(BigDecimal.valueOf(accountJson.getDouble("balance")));
            } else {
                account.setBalance(new BigDecimal(accountJson.getString("balance")));
            }
        } else {
            account.setBalance(BigDecimal.ZERO);
        }
        
        account.setCurrency(accountJson.optString("currency", "VND"));
        account.setActive(accountJson.optBoolean("is_active", true));
        
        // Handle interest_rate as number or string
        if (accountJson.has("interest_rate") && !accountJson.isNull("interest_rate")) {
            if (accountJson.get("interest_rate") instanceof Number) {
                account.setInterestRate(BigDecimal.valueOf(accountJson.getDouble("interest_rate")));
            } else {
                account.setInterestRate(new BigDecimal(accountJson.getString("interest_rate")));
            }
        }
        
        return account;
    }

    // Helper method to parse customer info from JSON
    private CustomerInfo parseCustomerInfo(JSONObject json) throws JSONException {
        CustomerInfo customer = new CustomerInfo();
        // Handle both id and _id
        String id = json.optString("id", json.optString("_id", ""));
        customer.setId(id);
        customer.setEmail(json.optString("email", ""));
        // Backend returns full_name (snake_case)
        customer.setFullName(json.optString("full_name", json.optString("fullName", "")));
        customer.setPhone(json.optString("phone", ""));
        customer.setAccountCount(json.optInt("account_count", 0));

        // Parse primary account if exists
        if (json.has("primary_account") && !json.isNull("primary_account")) {
            JSONObject primaryAccountJson = json.getJSONObject("primary_account");
            Account primaryAccount = new Account();
            primaryAccount.setAccountNumber(primaryAccountJson.optString("account_number", ""));
            // Handle balance as number or string
            if (primaryAccountJson.has("balance")) {
                if (primaryAccountJson.get("balance") instanceof Number) {
                    primaryAccount.setBalance(BigDecimal.valueOf(primaryAccountJson.getDouble("balance")));
                } else {
                    primaryAccount.setBalance(new BigDecimal(primaryAccountJson.getString("balance")));
                }
            } else {
                primaryAccount.setBalance(BigDecimal.ZERO);
            }
            customer.setPrimaryAccount(primaryAccount);
        }

        // Initialize lists first
        customer.setCheckingAccounts(new ArrayList<>());
        customer.setSavingAccounts(new ArrayList<>());
        customer.setMortgageAccounts(new ArrayList<>());
        
        // Parse accounts by type
        if (json.has("accounts_by_type") && !json.isNull("accounts_by_type")) {
            try {
                JSONObject accountsByType = json.getJSONObject("accounts_by_type");
                android.util.Log.d("AdminService", "Found accounts_by_type for customer: " + customer.getFullName());
                
                // Parse checking accounts
                if (accountsByType.has("checking") && !accountsByType.isNull("checking")) {
                    JSONArray checkingArray = accountsByType.getJSONArray("checking");
                    customer.setCheckingAccounts(parseAccountArray(checkingArray));
                    android.util.Log.d("AdminService", "  - Parsed " + customer.getCheckingAccounts().size() + " checking accounts");
                }
                
                // Parse saving accounts
                if (accountsByType.has("saving") && !accountsByType.isNull("saving")) {
                    JSONArray savingArray = accountsByType.getJSONArray("saving");
                    customer.setSavingAccounts(parseAccountArray(savingArray));
                    android.util.Log.d("AdminService", "  - Parsed " + customer.getSavingAccounts().size() + " saving accounts");
                }
                
                // Parse mortgage accounts
                if (accountsByType.has("mortgage") && !accountsByType.isNull("mortgage")) {
                    JSONArray mortgageArray = accountsByType.getJSONArray("mortgage");
                    customer.setMortgageAccounts(parseAccountArray(mortgageArray));
                    android.util.Log.d("AdminService", "  - Parsed " + customer.getMortgageAccounts().size() + " mortgage accounts");
                }
            } catch (Exception e) {
                android.util.Log.e("AdminService", "Error parsing accounts_by_type: " + e.getMessage(), e);
            }
        } else {
            android.util.Log.d("AdminService", "No accounts_by_type found for customer: " + customer.getFullName());
        }

        return customer;
    }

    // Helper method to parse account array
    private List<Account> parseAccountArray(JSONArray accountArray) throws JSONException {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < accountArray.length(); i++) {
            JSONObject accountJson = accountArray.getJSONObject(i);
            Account account = new Account();
            
            // Handle id (might be _id or id)
            String id = accountJson.optString("id", accountJson.optString("_id", ""));
            account.setId(id);
            
            account.setAccountNumber(accountJson.optString("account_number", ""));
            
            // Handle balance
            if (accountJson.has("balance")) {
                if (accountJson.get("balance") instanceof Number) {
                    account.setBalance(BigDecimal.valueOf(accountJson.getDouble("balance")));
                } else {
                    account.setBalance(new BigDecimal(accountJson.getString("balance")));
                }
            } else {
                account.setBalance(BigDecimal.ZERO);
            }
            
            // Handle interest_rate
            if (accountJson.has("interest_rate") && !accountJson.isNull("interest_rate")) {
                if (accountJson.get("interest_rate") instanceof Number) {
                    account.setInterestRate(BigDecimal.valueOf(accountJson.getDouble("interest_rate")));
                } else {
                    account.setInterestRate(new BigDecimal(accountJson.getString("interest_rate")));
                }
            }
            
            account.setCurrency(accountJson.optString("currency", "VND"));
            
            // Handle account_type
            account.setAccountType(accountJson.optString("account_type", ""));
            
            // Handle is_active
            account.setActive(accountJson.optBoolean("is_active", true));
            
            accounts.add(account);
        }
        return accounts;
    }

    // Get all transactions with pagination
    public void getAllTransactions(int page, int limit, String type, String status, TransactionListCallback callback) {
        String endpoint = ApiConfig.ADMIN_GET_TRANSACTIONS + "?page=" + page + "&limit=" + limit;
        if (type != null && !type.isEmpty() && !type.equals("Tất cả")) {
            try {
                endpoint += "&type=" + java.net.URLEncoder.encode(type, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                android.util.Log.e("AdminService", "Error encoding type: " + e.getMessage());
            }
        }
        if (status != null && !status.isEmpty() && !status.equals("Tất cả")) {
            try {
                endpoint += "&status=" + java.net.URLEncoder.encode(status, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                android.util.Log.e("AdminService", "Error encoding status: " + e.getMessage());
            }
        }
        android.util.Log.d("AdminService", "Calling getAllTransactions: " + ApiConfig.BASE_URL + endpoint);
        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                android.util.Log.d("AdminService", "getAllTransactions response received: " + response.toString());
                try {
                    if (response == null) {
                        android.util.Log.e("AdminService", "getAllTransactions response is null");
                        callback.onError("Response is null");
                        return;
                    }
                    
                    boolean success = response.optBoolean("success", false);
                    android.util.Log.d("AdminService", "getAllTransactions success flag: " + success);
                    
                    if (success) {
                        JSONArray transactionsArray = response.optJSONArray("data");
                        JSONObject meta = response.optJSONObject("meta");
                        
                        if (transactionsArray == null) {
                            android.util.Log.e("AdminService", "transactionsArray is null");
                            callback.onError("Data array is null in response");
                            return;
                        }
                        
                        if (meta == null) {
                            android.util.Log.e("AdminService", "meta object is null");
                            callback.onError("Meta object is null in response");
                            return;
                        }

                        android.util.Log.d("AdminService", "Found " + transactionsArray.length() + " transactions in array");

                        List<RecentTransaction> transactions = new ArrayList<>();
                        for (int i = 0; i < transactionsArray.length(); i++) {
                            try {
                                JSONObject t = transactionsArray.getJSONObject(i);
                                RecentTransaction transaction = new RecentTransaction();
                                transaction.setTransactionId(t.optString("transaction_id", ""));
                                
                                // Handle amount as number or string
                                if (t.has("amount")) {
                                    if (t.get("amount") instanceof Number) {
                                        transaction.setAmount(BigDecimal.valueOf(t.getDouble("amount")));
                                    } else {
                                        transaction.setAmount(new BigDecimal(t.getString("amount")));
                                    }
                                } else {
                                    transaction.setAmount(BigDecimal.ZERO);
                                }
                                
                                transaction.setType(t.optString("type", ""));
                                transaction.setStatus(t.optString("status", ""));
                                transaction.setDescription(t.optString("description", ""));
                                
                                // Handle created_at
                                String createdAt = "";
                                if (t.has("created_at")) {
                                    if (t.get("created_at") instanceof String) {
                                        createdAt = t.getString("created_at");
                                    } else {
                                        createdAt = t.optString("created_at", "");
                                    }
                                }
                                transaction.setCreatedAt(createdAt);
                                
                                transactions.add(transaction);
                            } catch (Exception e) {
                                android.util.Log.e("AdminService", "Error parsing transaction " + i + ": " + e.getMessage(), e);
                                // Continue with next transaction
                            }
                        }

                        int total = meta.optInt("total", 0);
                        int currentPage = meta.optInt("page", page);
                        int totalPages = meta.optInt("total_pages", 1);
                        boolean hasNextPage = meta.optBoolean("has_next_page", false);
                        
                        android.util.Log.d("AdminService", "Total transactions: " + total + ", page: " + currentPage + ", totalPages: " + totalPages);
                        android.util.Log.d("AdminService", "Parsed " + transactions.size() + " transactions successfully");

                        callback.onSuccess(transactions, total, currentPage, totalPages, hasNextPage);
                    } else {
                        String errorMsg = response.optString("message", "Failed to get transactions");
                        android.util.Log.e("AdminService", "getAllTransactions API returned success=false: " + errorMsg);
                        callback.onError(errorMsg);
                    }
                } catch (Exception e) {
                    android.util.Log.e("AdminService", "Unexpected error parsing getAllTransactions response: " + e.getMessage(), e);
                    callback.onError("Unexpected error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                android.util.Log.e("AdminService", "getAllTransactions API error: " + error + " (Status: " + statusCode + ")");
                callback.onError(error);
            }
        });
    }

    // Admin transfer money between customer accounts
    public void transferMoney(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String description, AdminCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("fromAccountNumber", fromAccountNumber);
            requestBody.put("toAccountNumber", toAccountNumber);
            requestBody.put("amount", amount.doubleValue());
            if (description != null && !description.isEmpty()) {
                requestBody.put("description", description);
            }

            apiService.post(ApiConfig.ADMIN_TRANSFER_MONEY, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            callback.onSuccess(response.optString("message", "Transfer completed successfully"));
                        } else {
                            callback.onError(response.optString("message", "Failed to transfer money"));
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
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    // Admin deposit money to customer account
    public void depositMoney(String accountNumber, BigDecimal amount, String description, AdminCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("accountNumber", accountNumber);
            requestBody.put("amount", amount.doubleValue());
            if (description != null && !description.isEmpty()) {
                requestBody.put("description", description);
            }

            apiService.post(ApiConfig.ADMIN_DEPOSIT_MONEY, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            callback.onSuccess(response.optString("message", "Deposit completed successfully"));
                        } else {
                            callback.onError(response.optString("message", "Failed to deposit money"));
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
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    // Create new customer
    public void createCustomer(String email, String password, String fullName, String phone, String address, AdminCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("fullName", fullName);
            requestBody.put("phone", phone);
            if (address != null && !address.isEmpty()) {
                requestBody.put("address", address);
            }

            apiService.post(ApiConfig.ADMIN_CREATE_CUSTOMER, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject customerData = response.getJSONObject("data");
                            CustomerInfo customer = parseCustomerInfo(customerData);
                            callback.onSuccess(customer);
                        } else {
                            callback.onError(response.optString("message", "Failed to create customer"));
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
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    // Update customer information
    public void updateCustomer(String customerId, String email, String fullName, String phone, String address, Boolean isActive, AdminCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            if (email != null && !email.isEmpty()) {
                requestBody.put("email", email);
            }
            if (fullName != null && !fullName.isEmpty()) {
                requestBody.put("fullName", fullName);
            }
            if (phone != null && !phone.isEmpty()) {
                requestBody.put("phone", phone);
            }
            if (address != null) {
                requestBody.put("address", address);
            }
            if (isActive != null) {
                requestBody.put("isActive", isActive);
            }

            String endpoint = ApiConfig.ADMIN_UPDATE_CUSTOMER.replace("{customerId}", customerId);
            apiService.put(endpoint, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject customerData = response.getJSONObject("data");
                            CustomerInfo customer = parseCustomerInfo(customerData);
                            callback.onSuccess(customer);
                        } else {
                            callback.onError(response.optString("message", "Failed to update customer"));
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
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    // Interest rate history model
    public static class InterestRateHistoryItem {
        private String id;
        private String accountId;
        private String accountNumber;
        private String accountType;
        private double oldRate;
        private double newRate;
        private String changedByName;
        private String changedByEmail;
        private String reason;
        private String effectiveDate;
        private String createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        public double getOldRate() { return oldRate; }
        public void setOldRate(double oldRate) { this.oldRate = oldRate; }
        public double getNewRate() { return newRate; }
        public void setNewRate(double newRate) { this.newRate = newRate; }
        public String getChangedByName() { return changedByName; }
        public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
        public String getChangedByEmail() { return changedByEmail; }
        public void setChangedByEmail(String changedByEmail) { this.changedByEmail = changedByEmail; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public interface InterestRateHistoryCallback {
        void onSuccess(List<InterestRateHistoryItem> history, int total, int page, int totalPages);
        void onError(String error);
    }

    // Update interest rate for account type
    public void updateInterestRate(String accountType, double newRate, String reason, AdminCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("accountType", accountType);
            requestBody.put("newRate", newRate);
            if (reason != null && !reason.isEmpty()) {
                requestBody.put("reason", reason);
            }

            apiService.put(ApiConfig.ADMIN_UPDATE_INTEREST_RATE, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            callback.onSuccess(response.optString("message", "Interest rate updated successfully"));
                        } else {
                            callback.onError(response.optString("message", "Failed to update interest rate"));
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
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    // Get interest rate history
    public void getInterestRateHistory(String accountType, String accountId, int page, int limit, InterestRateHistoryCallback callback) {
        try {
            StringBuilder endpoint = new StringBuilder(ApiConfig.ADMIN_GET_INTEREST_RATE_HISTORY);
            endpoint.append("?page=").append(page).append("&limit=").append(limit);
            if (accountType != null && !accountType.isEmpty()) {
                endpoint.append("&accountType=").append(java.net.URLEncoder.encode(accountType, "UTF-8"));
            }
            if (accountId != null && !accountId.isEmpty()) {
                endpoint.append("&accountId=").append(java.net.URLEncoder.encode(accountId, "UTF-8"));
            }

            apiService.get(endpoint.toString(), new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray historyArray = response.getJSONArray("data");
                            JSONObject meta = response.optJSONObject("meta");

                            List<InterestRateHistoryItem> history = new ArrayList<>();
                            for (int i = 0; i < historyArray.length(); i++) {
                                JSONObject h = historyArray.getJSONObject(i);
                                InterestRateHistoryItem item = new InterestRateHistoryItem();
                                item.setId(h.optString("id", ""));
                                item.setAccountId(h.optString("account_id", ""));
                                item.setAccountNumber(h.optString("account_number", ""));
                                item.setAccountType(h.optString("account_type", ""));
                                item.setOldRate(h.optDouble("old_rate", 0));
                                item.setNewRate(h.optDouble("new_rate", 0));
                                item.setReason(h.optString("reason", ""));
                                item.setEffectiveDate(h.optString("effective_date", ""));
                                item.setCreatedAt(h.optString("created_at", ""));

                                // Parse changed_by
                                if (h.has("changed_by") && !h.isNull("changed_by")) {
                                    JSONObject changedBy = h.getJSONObject("changed_by");
                                    item.setChangedByName(changedBy.optString("name", ""));
                                    item.setChangedByEmail(changedBy.optString("email", ""));
                                }

                                history.add(item);
                            }

                            int total = meta != null ? meta.optInt("total", 0) : history.size();
                            int currentPage = meta != null ? meta.optInt("page", page) : page;
                            int totalPages = meta != null ? meta.optInt("total_pages", 1) : 1;

                            callback.onSuccess(history, total, currentPage, totalPages);
                        } else {
                            callback.onError(response.optString("message", "Failed to get interest rate history"));
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
        } catch (Exception e) {
            callback.onError("Error creating request: " + e.getMessage());
        }
    }
}

