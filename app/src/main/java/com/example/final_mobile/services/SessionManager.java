package com.example.final_mobile.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.final_mobile.models.User;

import org.json.JSONException;
import org.json.JSONObject;

public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "BankingAppSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_ACCOUNT_NUMBER = "accountNumber";
    private static final String KEY_LOGIN_TIME = "loginTime";
    private static final String KEY_LAST_ACTIVITY = "lastActivity";

    private static SessionManager instance;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    private SessionManager(Context context) {
        this.context = context.getApplicationContext();
        pref = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // Create login session
    public void createLoginSession(String token, String refreshToken, User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_NAME, user.getFullName());
        editor.putString(KEY_USER_PHONE, user.getPhone());
        editor.putString(KEY_USER_TYPE, user.getCustomerType());
        editor.putString(KEY_ACCOUNT_NUMBER, user.getAccountNumber());
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "Login session created for user: " + user.getEmail());
    }

    // Update last activity timestamp
    public void updateLastActivity() {
        editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
        editor.apply();
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        boolean isLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);
        
        Log.d(TAG, "Checking login status: " + isLoggedIn);
        
        // Check if session is expired (7 days for development)
        if (isLoggedIn) {
            long lastActivity = pref.getLong(KEY_LAST_ACTIVITY, 0);
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastActivity;
            long maxInactiveTime = 7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds (was 24 hours)

            Log.d(TAG, "Last activity: " + lastActivity);
            Log.d(TAG, "Current time: " + currentTime);
            Log.d(TAG, "Time diff: " + timeDiff + " ms (" + (timeDiff / 1000 / 60) + " minutes)");
            Log.d(TAG, "Max inactive time: " + maxInactiveTime + " ms");

            if (timeDiff > maxInactiveTime) {
                Log.w(TAG, "Session expired due to inactivity");
                logoutUser();
                return false;
            }
            
            Log.d(TAG, "Session is valid");
        }

        return isLoggedIn;
    }

    // Get stored token
    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    // Get refresh token
    public String getRefreshToken() {
        return pref.getString(KEY_REFRESH_TOKEN, null);
    }

    // Update token (for refresh token scenario)
    public void updateToken(String newToken) {
        editor.putString(KEY_TOKEN, newToken);
        editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
        editor.apply();
    }

    // Get current user information
    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }

        User user = new User();
        user.setId(pref.getString(KEY_USER_ID, ""));
        user.setEmail(pref.getString(KEY_USER_EMAIL, ""));
        user.setFullName(pref.getString(KEY_USER_NAME, ""));
        user.setPhone(pref.getString(KEY_USER_PHONE, ""));
        user.setCustomerType(pref.getString(KEY_USER_TYPE, "CUSTOMER"));
        user.setAccountNumber(pref.getString(KEY_ACCOUNT_NUMBER, ""));

        return user;
    }

    // Get user ID
    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    // Get user email
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    // Get user name
    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }

    // Get account number
    public String getAccountNumber() {
        return pref.getString(KEY_ACCOUNT_NUMBER, "");
    }

    // Check if user is bank officer
    public boolean isBankOfficer() {
        String userType = pref.getString(KEY_USER_TYPE, "CUSTOMER");
        return "BANK_OFFICER".equals(userType);
    }

    // Get session duration
    public long getSessionDuration() {
        long loginTime = pref.getLong(KEY_LOGIN_TIME, 0);
        return System.currentTimeMillis() - loginTime;
    }

    // Update user profile
    public void updateUserProfile(User user) {
        editor.putString(KEY_USER_NAME, user.getFullName());
        editor.putString(KEY_USER_PHONE, user.getPhone());
        editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
        editor.apply();
    }

    // Logout user
    public void logoutUser() {
        Log.d(TAG, "Logging out user");
        
        // Clear all session data
        editor.clear();
        editor.apply();
    }

    // Clear specific session data
    public void clearSession() {
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    // Save temporary data (for OTP verification, etc.)
    public void saveTemporaryData(String key, String value) {
        editor.putString("temp_" + key, value);
        editor.apply();
    }

    // Get temporary data
    public String getTemporaryData(String key) {
        return pref.getString("temp_" + key, null);
    }

    // Clear temporary data
    public void clearTemporaryData(String key) {
        editor.remove("temp_" + key);
        editor.apply();
    }

    // Get all session info as JSON (for debugging)
    public JSONObject getSessionInfo() {
        JSONObject sessionInfo = new JSONObject();
        try {
            sessionInfo.put("isLoggedIn", isLoggedIn());
            sessionInfo.put("userId", getUserId());
            sessionInfo.put("userEmail", getUserEmail());
            sessionInfo.put("userName", getUserName());
            sessionInfo.put("isBankOfficer", isBankOfficer());
            sessionInfo.put("sessionDuration", getSessionDuration());
            sessionInfo.put("hasToken", getToken() != null);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating session info JSON", e);
        }
        return sessionInfo;
    }
}
