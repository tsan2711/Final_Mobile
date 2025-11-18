package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import com.example.final_mobile.models.Account;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccountService {
    private static final String TAG = "AccountService";
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public AccountService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    // Callback interface for account operations
    public interface AccountCallback {
        void onSuccess(List<Account> accounts);
        void onSingleAccountSuccess(Account account);
        void onBalanceSuccess(BigDecimal balance);
        void onError(String error);
    }

    // Get all accounts for current user
    public void getUserAccounts(AccountCallback callback) {
        Log.d(TAG, "🔄 [DEBUG] Getting user accounts from API...");
        apiService.get(ApiConfig.GET_ACCOUNTS, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    Log.d(TAG, "✅ [DEBUG] API Response received: " + response.toString());
                    boolean success = response.getBoolean("success");
                    if (success) {
                        JSONArray accountsArray = response.getJSONArray("data");
                        Log.d(TAG, "📊 [DEBUG] Found " + accountsArray.length() + " accounts");
                        List<Account> accounts = parseAccountsFromJson(accountsArray);
                        
                        // Log balance for each account
                        for (Account account : accounts) {
                            Log.d(TAG, "💰 [DEBUG] Account ID: " + account.getId() + 
                                ", Number: " + account.getAccountNumber() + 
                                ", Balance: " + account.getBalance() + 
                                ", Type: " + account.getAccountType());
                        }
                        
                        callback.onSuccess(accounts);
                    } else {
                        String message = response.optString("message", "Failed to fetch accounts");
                        Log.e(TAG, "❌ [DEBUG] API returned success=false: " + message);
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "❌ [DEBUG] Error parsing accounts response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Get accounts error: " + error);
                
                // Return dummy data for demo if API is not available
                if (statusCode == -1) { // Network error
                    List<Account> dummyAccounts = createDummyAccounts();
                    callback.onSuccess(dummyAccounts);
                } else {
                    callback.onError(getErrorMessage(error, statusCode));
                }
            }
        });
    }

    // Get account balance
    public void getAccountBalance(String accountId, AccountCallback callback) {
        String endpoint = ApiConfig.GET_ACCOUNT_BALANCE.replace("{id}", accountId);
        
        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        JSONObject data = response.getJSONObject("data");
                        BigDecimal balance = new BigDecimal(data.getString("balance"));
                        callback.onBalanceSuccess(balance);
                    } else {
                        String message = response.optString("message", "Failed to fetch balance");
                        callback.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing balance response", e);
                    callback.onError("Invalid response format");
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Get balance error: " + error);
                
                // Return dummy balance for demo
                if (statusCode == -1) {
                    callback.onBalanceSuccess(new BigDecimal("250000000")); // 250M VND
                } else {
                    callback.onError(getErrorMessage(error, statusCode));
                }
            }
        });
    }

    // Create new account
    public void createAccount(String accountType, AccountCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("account_type", accountType);
            requestBody.put("currency", "VND");

            apiService.post(ApiConfig.CREATE_ACCOUNT, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject accountData = response.getJSONObject("data");
                            Account account = parseAccountFromJson(accountData);
                            callback.onSingleAccountSuccess(account);
                        } else {
                            String message = response.optString("message", "Failed to create account");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing create account response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Create account error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating account request", e);
            callback.onError("Failed to create account request");
        }
    }

    // Parse accounts from JSON array
    private List<Account> parseAccountsFromJson(JSONArray accountsArray) throws JSONException {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < accountsArray.length(); i++) {
            JSONObject accountJson = accountsArray.getJSONObject(i);
            Account account = parseAccountFromJson(accountJson);
            accounts.add(account);
        }
        return accounts;
    }

    // Parse single account from JSON
    private Account parseAccountFromJson(JSONObject accountJson) throws JSONException {
        Account account = new Account();
        
        // Handle both id and _id fields
        String id = accountJson.optString("id", accountJson.optString("_id", ""));
        account.setId(id);
        
        // user_id is optional (may not exist in some responses like interest projection)
        if (accountJson.has("user_id") && !accountJson.isNull("user_id")) {
            account.setUserId(accountJson.getString("user_id"));
        }
        
        account.setAccountNumber(accountJson.optString("account_number", ""));
        account.setAccountType(accountJson.optString("account_type", ""));
        
        // Parse balance with detailed logging
        String balanceStr = "0";
        if (accountJson.has("balance") && !accountJson.isNull("balance")) {
            // Handle both string and number types
            if (accountJson.get("balance") instanceof Number) {
                balanceStr = String.valueOf(accountJson.getDouble("balance"));
            } else {
                balanceStr = accountJson.getString("balance");
            }
        } else if (accountJson.has("current_balance") && !accountJson.isNull("current_balance")) {
            // Some APIs use current_balance instead of balance
            if (accountJson.get("current_balance") instanceof Number) {
                balanceStr = String.valueOf(accountJson.getDouble("current_balance"));
            } else {
                balanceStr = accountJson.getString("current_balance");
            }
        }
        
        BigDecimal balance = new BigDecimal(balanceStr);
        account.setBalance(balance);
        
        Log.d(TAG, "💵 [DEBUG] Parsed balance: " + balanceStr + " -> " + balance + 
            " for account: " + account.getAccountNumber());
        
        account.setCurrency(accountJson.optString("currency", "VND"));
        account.setActive(accountJson.optBoolean("is_active", true));
        
        // Parse interest_rate - can be number or string
        if (accountJson.has("interest_rate") && !accountJson.isNull("interest_rate")) {
            if (accountJson.get("interest_rate") instanceof Number) {
                account.setInterestRate(BigDecimal.valueOf(accountJson.getDouble("interest_rate")));
            } else {
                account.setInterestRate(new BigDecimal(accountJson.getString("interest_rate")));
            }
        }
        
        return account;
    }

    // Create dummy accounts for demo purposes
    private List<Account> createDummyAccounts() {
        List<Account> accounts = new ArrayList<>();
        
        // Main checking account
        Account checkingAccount = new Account();
        checkingAccount.setId("acc_001");
        checkingAccount.setUserId(sessionManager.getUserId());
        checkingAccount.setAccountNumber("1234567890123456");
        checkingAccount.setAccountType(Account.TYPE_CHECKING);
        checkingAccount.setBalance(new BigDecimal("250000000")); // 250M VND
        checkingAccount.setCurrency("VND");
        checkingAccount.setActive(true);
        checkingAccount.setCreatedAt(new Date());
        accounts.add(checkingAccount);
        
        // Savings account
        Account savingsAccount = new Account();
        savingsAccount.setId("acc_002");
        savingsAccount.setUserId(sessionManager.getUserId());
        savingsAccount.setAccountNumber("1234567890123457");
        savingsAccount.setAccountType(Account.TYPE_SAVING);
        savingsAccount.setBalance(new BigDecimal("150000000")); // 150M VND
        savingsAccount.setInterestRate(new BigDecimal("6.5")); // 6.5% yearly
        savingsAccount.setCurrency("VND");
        savingsAccount.setActive(true);
        savingsAccount.setCreatedAt(new Date());
        accounts.add(savingsAccount);
        
        // Mortgage account (if user has one)
        Account mortgageAccount = new Account();
        mortgageAccount.setId("acc_003");
        mortgageAccount.setUserId(sessionManager.getUserId());
        mortgageAccount.setAccountNumber("1234567890123458");
        mortgageAccount.setAccountType(Account.TYPE_MORTGAGE);
        mortgageAccount.setBalance(new BigDecimal("-500000000")); // -500M VND (loan)
        mortgageAccount.setInterestRate(new BigDecimal("8.2")); // 8.2% yearly
        mortgageAccount.setCurrency("VND");
        mortgageAccount.setActive(true);
        mortgageAccount.setCreatedAt(new Date());
        accounts.add(mortgageAccount);
        
        Log.d(TAG, "Created " + accounts.size() + " dummy accounts");
        return accounts;
    }

    // Get primary checking account
    public void getPrimaryAccount(AccountCallback callback) {
        getUserAccounts(new AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                Account primaryAccount = null;
                for (Account account : accounts) {
                    if (account.isCheckingAccount() && account.isActive()) {
                        primaryAccount = account;
                        break;
                    }
                }
                
                if (primaryAccount != null) {
                    List<Account> singleAccountList = new ArrayList<>();
                    singleAccountList.add(primaryAccount);
                    callback.onSuccess(singleAccountList);
                } else {
                    callback.onError("No primary account found");
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                // Not used in this context
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {
                // Not used in this context
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // Calculate monthly interest for savings account
    public BigDecimal calculateMonthlyInterest(Account account) {
        if (!account.isSavingAccount() || account.getInterestRate() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal yearlyRate = account.getInterestRate().divide(new BigDecimal("100"));
        BigDecimal monthlyRate = yearlyRate.divide(new BigDecimal("12"), 6, BigDecimal.ROUND_HALF_UP);
        return account.getBalance().multiply(monthlyRate);
    }

    // Calculate monthly mortgage payment
    public BigDecimal calculateMonthlyMortgagePayment(Account account) {
        if (!account.isMortgageAccount() || account.getInterestRate() == null) {
            return BigDecimal.ZERO;
        }
        
        // Simple calculation - in real app this would be more complex
        BigDecimal loanAmount = account.getBalance().abs();
        BigDecimal yearlyRate = account.getInterestRate().divide(new BigDecimal("100"));
        BigDecimal monthlyRate = yearlyRate.divide(new BigDecimal("12"), 6, BigDecimal.ROUND_HALF_UP);
        
        // Assuming 20-year term
        int termMonths = 20 * 12;
        
        // Monthly payment formula: P * [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(termMonths);
        BigDecimal numerator = loanAmount.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 0, BigDecimal.ROUND_HALF_UP);
    }

    // Deposit money to account
    public void depositMoney(String accountId, BigDecimal amount, String description, AccountCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("accountId", accountId);
            requestBody.put("amount", amount.toString());
            if (description != null && !description.isEmpty()) {
                requestBody.put("description", description);
            }

            apiService.post(ApiConfig.DEPOSIT_MONEY, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            Account account = parseAccountFromJson(data.getJSONObject("account"));
                            List<Account> accounts = new ArrayList<>();
                            accounts.add(account);
                            callback.onSuccess(accounts);
                        } else {
                            String message = response.optString("message", "Failed to deposit money");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing deposit response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Deposit money error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating deposit request", e);
            callback.onError("Failed to create deposit request");
        }
    }

    // Withdraw money from account
    public void withdrawMoney(String accountId, BigDecimal amount, String description, AccountCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("accountId", accountId);
            requestBody.put("amount", amount.toString());
            if (description != null && !description.isEmpty()) {
                requestBody.put("description", description);
            }

            apiService.post(ApiConfig.WITHDRAW_MONEY, requestBody, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            Account account = parseAccountFromJson(data.getJSONObject("account"));
                            List<Account> accounts = new ArrayList<>();
                            accounts.add(account);
                            callback.onSuccess(accounts);
                        } else {
                            String message = response.optString("message", "Failed to withdraw money");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing withdraw response", e);
                        callback.onError("Invalid response format");
                    }
                }

                @Override
                public void onError(String error, int statusCode) {
                    Log.e(TAG, "Withdraw money error: " + error);
                    callback.onError(getErrorMessage(error, statusCode));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating withdraw request", e);
            callback.onError("Failed to create withdraw request");
        }
    }

    // Interest projection model
    public static class InterestProjection {
        private Account account;
        private int months;
        private BigDecimal currentBalance;
        private BigDecimal projectedBalance;
        private BigDecimal totalInterest;
        private List<MonthlyProjection> monthlyDetails;

        public Account getAccount() { return account; }
        public void setAccount(Account account) { this.account = account; }
        public int getMonths() { return months; }
        public void setMonths(int months) { this.months = months; }
        public BigDecimal getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
        public BigDecimal getProjectedBalance() { return projectedBalance; }
        public void setProjectedBalance(BigDecimal projectedBalance) { this.projectedBalance = projectedBalance; }
        public BigDecimal getTotalInterest() { return totalInterest; }
        public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }
        public List<MonthlyProjection> getMonthlyDetails() { return monthlyDetails; }
        public void setMonthlyDetails(List<MonthlyProjection> monthlyDetails) { this.monthlyDetails = monthlyDetails; }
    }

    public static class MonthlyProjection {
        private int month;
        private BigDecimal balance;
        private BigDecimal monthlyInterest;
        private BigDecimal cumulativeInterest;

        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public BigDecimal getMonthlyInterest() { return monthlyInterest; }
        public void setMonthlyInterest(BigDecimal monthlyInterest) { this.monthlyInterest = monthlyInterest; }
        public BigDecimal getCumulativeInterest() { return cumulativeInterest; }
        public void setCumulativeInterest(BigDecimal cumulativeInterest) { this.cumulativeInterest = cumulativeInterest; }
    }

    public interface InterestProjectionCallback {
        void onSuccess(InterestProjection projection);
        void onError(String error);
    }

    // Get interest projection for account
    public void getInterestProjection(String accountId, int months, InterestProjectionCallback callback) {
        try {
            String endpoint = ApiConfig.GET_INTEREST_PROJECTION.replace("{accountId}", accountId);
            endpoint += "?months=" + months;

            apiService.get(endpoint, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONObject accountObj = data.getJSONObject("account");
                            JSONObject projectionObj = data.getJSONObject("projection");

                            // Parse account
                            Account account = parseAccountFromJson(accountObj);

                            // Parse projection
                            InterestProjection projection = new InterestProjection();
                            projection.setAccount(account);
                            projection.setMonths(projectionObj.getInt("months"));
                            projection.setCurrentBalance(BigDecimal.valueOf(projectionObj.getDouble("current_balance")));
                            projection.setProjectedBalance(BigDecimal.valueOf(projectionObj.getDouble("projected_balance")));
                            projection.setTotalInterest(BigDecimal.valueOf(projectionObj.getDouble("total_interest")));

                            // Parse monthly details
                            JSONArray monthlyArray = projectionObj.getJSONArray("monthly_details");
                            List<MonthlyProjection> monthlyDetails = new ArrayList<>();
                            for (int i = 0; i < monthlyArray.length(); i++) {
                                JSONObject m = monthlyArray.getJSONObject(i);
                                MonthlyProjection monthly = new MonthlyProjection();
                                monthly.setMonth(m.getInt("month"));
                                monthly.setBalance(BigDecimal.valueOf(m.getDouble("balance")));
                                monthly.setMonthlyInterest(BigDecimal.valueOf(m.getDouble("monthly_interest")));
                                monthly.setCumulativeInterest(BigDecimal.valueOf(m.getDouble("cumulative_interest")));
                                monthlyDetails.add(monthly);
                            }
                            projection.setMonthlyDetails(monthlyDetails);

                            callback.onSuccess(projection);
                        } else {
                            callback.onError(response.optString("message", "Failed to get interest projection"));
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

    // Get error message
    private String getErrorMessage(String error, int statusCode) {
        switch (statusCode) {
            case ApiConfig.UNAUTHORIZED:
                return "Phiên đăng nhập đã hết hạn";
            case ApiConfig.FORBIDDEN:
                return "Bạn không có quyền truy cập tài khoản này";
            case ApiConfig.NOT_FOUND:
                return "Không tìm thấy tài khoản";
            case ApiConfig.INTERNAL_SERVER_ERROR:
                return "Lỗi hệ thống. Vui lòng thử lại sau";
            default:
                return error != null ? error : "Đã xảy ra lỗi không xác định";
        }
    }
}
