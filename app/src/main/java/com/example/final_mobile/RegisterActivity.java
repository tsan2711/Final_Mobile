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

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword, etConfirmPassword, etFullName, etPhone, etAddress;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;
    private ProgressDialog progressDialog;
    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize services
        authService = new AuthService(this);
        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupUI();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etFullName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng ký...");
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        // Set click listener for register button
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegister();
            }
        });

        // Set click listener for login link
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin();
            }
        });
    }

    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        // Validation
        if (!validateInput(email, password, confirmPassword, fullName, phone)) {
            return;
        }

        // Show loading
        progressDialog.show();
        btnRegister.setEnabled(false);

        // Call authentication service
        authService.register(email, password, fullName, phone, address, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    btnRegister.setEnabled(true);
                    showErrorDialog("Lỗi đăng ký", error);
                });
            }

            @Override
            public void onOtpRequired(String message) {
                // Registration doesn't require OTP
            }
        });
    }

    private boolean validateInput(String email, String password, String confirmPassword, String fullName, String phone) {
        // Reset errors
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
        etFullName.setError(null);
        etPhone.setError(null);

        // Full name validation
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Họ tên không được để trống");
            etFullName.requestFocus();
            return false;
        }

        if (fullName.length() < 2) {
            etFullName.setError("Họ tên phải có ít nhất 2 ký tự");
            etFullName.requestFocus();
            return false;
        }

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

        // Phone validation
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Số điện thoại không được để trống");
            etPhone.requestFocus();
            return false;
        }

        // Vietnamese phone number validation (10 digits, starting with 0)
        String phoneRegex = "^0[0-9]{9}$";
        if (!phone.matches(phoneRegex)) {
            etPhone.setError("Số điện thoại không hợp lệ (ví dụ: 0123456789)");
            etPhone.requestFocus();
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

        // Confirm password validation
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
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

