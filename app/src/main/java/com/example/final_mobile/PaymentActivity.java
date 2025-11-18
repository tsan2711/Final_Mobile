package com.example.final_mobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.PaymentGatewayService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";
    
    private TextView tvTitle;
    private Account primaryAccount;
    private AccountService accountService;
    private PaymentGatewayService paymentGatewayService;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize services
        accountService = new AccountService(this);
        paymentGatewayService = new PaymentGatewayService(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        initViews();
        loadAccountInfo();
        setupUI();
    }

    private void initViews() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thanh toán & Nạp tiền");
        }
        
        tvTitle = findViewById(R.id.tv_title);
        
        // Payment method buttons
        MaterialButton btnVnpay = findViewById(R.id.btn_vnpay);
        MaterialButton btnBankTransfer = findViewById(R.id.btn_bank_transfer);
        
        btnVnpay.setOnClickListener(v -> showVnpayDialog());
        btnBankTransfer.setOnClickListener(v -> showBankTransferDialog());
    }

    private void setupUI() {
        tvTitle.setText("Thanh toán & Nạp tiền");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadAccountInfo() {
        accountService.getPrimaryAccount(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(java.util.List<Account> accounts) {
                if (!accounts.isEmpty()) {
                    primaryAccount = accounts.get(0);
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                primaryAccount = account;
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {
                // Not used
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading account: " + error);
            }
        });
    }

    private void showVnpayDialog() {
        if (primaryAccount == null) {
            Toast.makeText(this, "Chưa có thông tin tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_vnpay, null);
        
        TextView tvBalance = dialogView.findViewById(R.id.tv_balance);
        tvBalance.setText(primaryAccount.getFormattedBalance());
        
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (validateAmount(amountStr)) {
                dialog.dismiss();
                createVnpayPayment(new BigDecimal(amountStr), description);
            }
        });
        
        dialog.show();
    }

    private void showBankTransferDialog() {
        if (primaryAccount == null) {
            Toast.makeText(this, "Chưa có thông tin tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bank_transfer, null);
        
        TextView tvBalance = dialogView.findViewById(R.id.tv_balance);
        tvBalance.setText(primaryAccount.getFormattedBalance());
        
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etBankName = dialogView.findViewById(R.id.et_bank_name);
        TextInputEditText etBankCode = dialogView.findViewById(R.id.et_bank_code);
        TextInputEditText etRecipientAccount = dialogView.findViewById(R.id.et_recipient_account);
        TextInputEditText etRecipientName = dialogView.findViewById(R.id.et_recipient_name);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String bankName = etBankName.getText().toString().trim();
            String bankCode = etBankCode.getText().toString().trim();
            String recipientAccount = etRecipientAccount.getText().toString().trim();
            String recipientName = etRecipientName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (validateBankTransfer(amountStr, bankName, recipientAccount, recipientName)) {
                dialog.dismiss();
                createBankTransfer(
                    new BigDecimal(amountStr),
                    bankName,
                    bankCode,
                    recipientAccount,
                    recipientName,
                    description
                );
            }
        });
        
        dialog.show();
    }

    private boolean validateAmount(String amountStr) {
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(new BigDecimal("10000")) < 0) {
                Toast.makeText(this, "Số tiền tối thiểu là 10,000 VND", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validateBankTransfer(String amountStr, String bankName, String recipientAccount, String recipientName) {
        if (!validateAmount(amountStr)) {
            return false;
        }

        if (TextUtils.isEmpty(bankName)) {
            Toast.makeText(this, "Vui lòng nhập tên ngân hàng", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(recipientAccount)) {
            Toast.makeText(this, "Vui lòng nhập số tài khoản người nhận", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(recipientName)) {
            Toast.makeText(this, "Vui lòng nhập tên người nhận", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createVnpayPayment(BigDecimal amount, String description) {
        progressDialog.setMessage("Đang tạo giao dịch VNPay...");
        progressDialog.show();

        paymentGatewayService.createVnpayPayment(
            primaryAccount.getId(),
            amount,
            description,
            null,
            null,
            new PaymentGatewayService.PaymentCallback() {
                @Override
                public void onSuccess(PaymentGatewayService.PaymentResult result) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        
                        // Open VNPay payment URL in browser
                        if (result.getPaymentUrl() != null) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getPaymentUrl()));
                            startActivity(browserIntent);
                            
                            Toast.makeText(PaymentActivity.this, 
                                "Đã mở trang thanh toán VNPay. Vui lòng hoàn tất thanh toán.", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(PaymentActivity.this, "Lỗi: Không có URL thanh toán", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(PaymentActivity.this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }

    private void createBankTransfer(BigDecimal amount, String bankName, String bankCode, 
                                     String recipientAccount, String recipientName, String description) {
        progressDialog.setMessage("Đang xử lý chuyển khoản...");
        progressDialog.show();

        paymentGatewayService.createBankTransfer(
            primaryAccount.getId(),
            amount,
            bankName,
            bankCode,
            recipientAccount,
            recipientName,
            description,
            new PaymentGatewayService.PaymentCallback() {
                @Override
                public void onSuccess(PaymentGatewayService.PaymentResult result) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        
                        String message = String.format(
                            "Chuyển khoản thành công!\n\n" +
                            "Mã giao dịch: %s\n" +
                            "Số tiền: %s VND\n" +
                            "Ngân hàng: %s\n" +
                            "Tài khoản nhận: %s\n" +
                            "Tên người nhận: %s\n" +
                            "Mã tham chiếu: %s",
                            result.getPaymentId(),
                            result.getAmount().toString(),
                            bankName,
                            recipientAccount,
                            recipientName,
                            result.getTransferReference()
                        );
                        
                        new AlertDialog.Builder(PaymentActivity.this)
                            .setTitle("Thành công")
                            .setMessage(message)
                            .setPositiveButton("OK", (dialog, which) -> {
                                finish();
                            })
                            .show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(PaymentActivity.this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }
}

