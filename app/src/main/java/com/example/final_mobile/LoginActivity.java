package com.example.final_mobile;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.final_mobile.models.User;
import com.example.final_mobile.services.AuthService;
import com.example.final_mobile.services.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.TextView;
import android.widget.EditText;
import android.view.LayoutInflater;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword;
    private ProgressDialog progressDialog;
    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize services
        authService = new AuthService(this);
        sessionManager = SessionManager.getInstance(this);

        // Always show login screen - user must login every time
        // If you want auto-login, uncomment the code below:
        // if (sessionManager.isLoggedIn()) {
        //     navigateToMainActivity();
        //     return;
        // }

        initViews();
        setupUI();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng nhập...");
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        // Set click listener for login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        // Set click listener for forgot password
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (!validateInput(email, password)) {
            return;
        }

        // Show loading
        progressDialog.show();
        btnLogin.setEnabled(false);

        // Call authentication service
        authService.login(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    btnLogin.setEnabled(true);
                    showErrorDialog("Lỗi đăng nhập", error);
                });
            }

            @Override
            public void onOtpRequired(String message) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    btnLogin.setEnabled(true);
                    showOtpDialog(message);
                });
            }
        });
    }

    private boolean validateInput(String email, String password) {
        // Reset errors
        etEmail.setError(null);
        etPassword.setError(null);

        // Email validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email không được để trống");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return false;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Mật khẩu không được để trống");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showOtpDialog(String message) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verification, null);
        
        TextView tvTransactionInfo = dialogView.findViewById(R.id.tv_transaction_info);
        // For login, show email address
        String email = etEmail.getText().toString().trim();
        tvTransactionInfo.setText(email);
        
        TextInputEditText etOtp = dialogView.findViewById(R.id.et_otp);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String otpCode = etOtp.getText().toString().trim();
            if (otpCode.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (otpCode.length() != 6) {
                Toast.makeText(this, "Mã OTP phải có 6 chữ số", Toast.LENGTH_SHORT).show();
                return;
            }
            
            dialog.dismiss();
            verifyOtp(otpCode);
        });
        
        dialog.show();
    }

    private void verifyOtp(String otpCode) {
        progressDialog.setMessage("Đang xác thực OTP...");
        progressDialog.show();

        authService.verifyOtp(otpCode, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showErrorDialog("Lỗi xác thực OTP", error);
                });
            }

            @Override
            public void onOtpRequired(String message) {
                // This shouldn't happen in OTP verification
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleForgotPassword() {
        // Show forgot password dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quên mật khẩu");
        builder.setMessage("Nhập email để nhận link đặt lại mật khẩu");

        final EditText emailInput = new EditText(this);
        emailInput.setHint("Email của bạn");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(emailInput);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Vui lòng nhập email hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // TODO: Implement forgot password API call
            Toast.makeText(this, "Link đặt lại mật khẩu đã được gửi đến email của bạn", Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
