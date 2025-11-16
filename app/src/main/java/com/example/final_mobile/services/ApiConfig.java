package com.example.final_mobile.services;

public class ApiConfig {
    // ⚠️ IMPORTANT: Thay đổi URL này thành Node.js server của bạn
    
    // 🟢 NODE.JS DEVELOPMENT ENVIRONMENT (Chọn 1 trong các option dưới đây):
    
    // Option 1: Android Emulator kết nối Node.js local
    public static final String BASE_URL = "http://10.0.2.2:8000/api/";
    
    // Option 2: Real Device kết nối Node.js local (thay IP của máy tính)
    // public static final String BASE_URL = "http://192.168.1.100:8000/api/";
    
    // Option 3: Production Node.js server
    // public static final String BASE_URL = "https://yourdomain.com/api/";
    
    // Option 4: Node.js với custom port
    // public static final String BASE_URL = "http://10.0.2.2:3000/api/";
    
    // API Endpoints - Updated to match Node.js backend
    public static final String LOGIN = "auth/login";
    public static final String REGISTER = "auth/register";
    public static final String LOGOUT = "auth/logout";
    public static final String REFRESH_TOKEN = "auth/refresh-token";  // Updated
    public static final String VERIFY_OTP = "auth/verify-otp";
    public static final String SEND_OTP = "auth/send-otp";
    
    // User endpoints - Updated to match Node.js backend
    public static final String USER_PROFILE = "auth/me";  // Updated
    public static final String UPDATE_PROFILE = "user/update";
    
    // Account endpoints - Match Node.js backend
    public static final String GET_ACCOUNTS = "accounts";
    public static final String GET_ACCOUNT_SUMMARY = "accounts/summary";  // Added
    public static final String GET_PRIMARY_ACCOUNT = "accounts/primary";  // Added
    public static final String GET_ACCOUNT_BALANCE = "accounts/{id}/balance";
    public static final String GET_ACCOUNT_BY_NUMBER = "accounts/number/{accountNumber}";  // Added
    public static final String CREATE_ACCOUNT = "accounts/create";
    
    // Transaction endpoints - Updated to match Node.js backend
    public static final String GET_TRANSACTIONS = "transactions/history";  // Updated
    public static final String CREATE_TRANSACTION = "transactions/create";
    public static final String TRANSFER_MONEY = "transactions/transfer";
    public static final String VERIFY_TRANSFER_OTP = "transactions/verify-otp";  // Added
    public static final String GET_TRANSACTION_DETAIL = "transactions/{id}";
    
    // Utility endpoints
    public static final String PAY_BILL = "utilities/pay-bill";
    public static final String TOPUP_PHONE = "utilities/topup";
    public static final String GET_BILL_INFO = "utilities/bill-info";
    
    // eKYC endpoints
    public static final String UPLOAD_FACE_IMAGE = "ekyc/face-verification";
    public static final String VERIFY_IDENTITY = "ekyc/verify-identity";
    
    // Map endpoints
    public static final String GET_BRANCHES = "branches";
    public static final String GET_NEAREST_BRANCH = "branches/nearest";
    
    // Admin endpoints (Bank Officer only)
    public static final String ADMIN_DASHBOARD = "admin/dashboard";
    public static final String ADMIN_GET_CUSTOMERS = "admin/customers";
    public static final String ADMIN_SEARCH_CUSTOMERS = "admin/customers/search";
    public static final String ADMIN_GET_CUSTOMER_DETAILS = "admin/customers/{customerId}";
    public static final String ADMIN_CREATE_ACCOUNT = "admin/accounts/create";
    public static final String ADMIN_UPDATE_ACCOUNT = "admin/accounts/{accountId}";
    public static final String ADMIN_DEACTIVATE_ACCOUNT = "admin/accounts/{accountId}";
    public static final String ADMIN_GET_TRANSACTIONS = "admin/transactions";
    public static final String ADMIN_TRANSFER_MONEY = "admin/transactions/transfer";
    public static final String ADMIN_DEPOSIT_MONEY = "admin/transactions/deposit";
    
    // Headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    
    // Content Types
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    
    // Response codes
    public static final int SUCCESS = 200;
    public static final int CREATED = 201;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int UNPROCESSABLE_ENTITY = 422;
    public static final int INTERNAL_SERVER_ERROR = 500;
    
    // Timeout settings (in milliseconds)
    public static final int CONNECT_TIMEOUT = 30000; // 30 seconds
    public static final int READ_TIMEOUT = 30000; // 30 seconds
    
    // API Keys (should be stored securely in production)
    public static final String VNPAY_API_KEY = "your_vnpay_api_key_here";
    public static final String STRIPE_API_KEY = "your_stripe_api_key_here";
    
    private ApiConfig() {
        // Private constructor to prevent instantiation
    }
}
