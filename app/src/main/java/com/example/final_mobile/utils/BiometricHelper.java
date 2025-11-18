package com.example.final_mobile.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class BiometricHelper {
    private static final String TAG = "BiometricHelper";

    public interface BiometricCallback {
        void onSuccess();
        void onError(String error);
        void onCancel();
    }

    /**
     * Check if biometric authentication is available
     */
    public static boolean isBiometricAvailable(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);

        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Show biometric authentication prompt
     */
    public static void showBiometricPrompt(FragmentActivity activity, String title, String subtitle, BiometricCallback callback) {
        if (!isBiometricAvailable(activity)) {
            callback.onError("Biometric authentication không khả dụng trên thiết bị này");
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Biometric authentication succeeded");
                callback.onSuccess();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.e(TAG, "Biometric authentication error: " + errString);
                
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || 
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    callback.onCancel();
                } else {
                    callback.onError("Xác thực sinh trắc học thất bại: " + errString);
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.w(TAG, "Biometric authentication failed");
                callback.onError("Xác thực không thành công. Vui lòng thử lại.");
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title != null ? title : "Xác thực sinh trắc học")
                .setSubtitle(subtitle != null ? subtitle : "Vui lòng xác thực để tiếp tục giao dịch")
                .setNegativeButtonText("Hủy")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Show biometric prompt for high-value transaction
     */
    public static void showTransactionBiometricPrompt(FragmentActivity activity, double amount, BiometricCallback callback) {
        String title = "Xác thực giao dịch";
        String subtitle = String.format("Giao dịch trị giá %,.0f VND yêu cầu xác thực sinh trắc học", amount);
        showBiometricPrompt(activity, title, subtitle, callback);
    }
}

