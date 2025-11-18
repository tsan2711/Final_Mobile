package com.example.final_mobile;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.final_mobile.models.Transaction;
import com.example.final_mobile.services.SessionManager;
import com.example.final_mobile.services.TransactionService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionDetailActivity extends AppCompatActivity {
    private static final String TAG = "TransactionDetailActivity";
    
    private TextView tvTransactionId;
    private TextView tvAmount;
    private TextView tvType;
    private TextView tvStatus;
    private TextView tvDescription;
    private TextView tvFromAccount;
    private TextView tvToAccount;
    private TextView tvCreatedAt;
    private TextView tvCompletedAt;
    private TextView tvReferenceNumber;
    
    private TransactionService transactionService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private String transactionId;
    private String mongoId; // Fallback MongoDB _id
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);
        
        // Get transaction data from intent
        android.os.Bundle bundle = getIntent().getExtras();
        
        // Initialize services first (needed for displayTransaction)
        transactionService = new TransactionService(this);
        sessionManager = SessionManager.getInstance(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết giao dịch");
        }
        
        initViews();
        
        // Check if we have transaction data in bundle (preferred - avoids API call)
        if (bundle != null && bundle.containsKey("amount")) {
            // We have transaction data, use it directly
            Transaction transaction = new Transaction();
            transaction.setId(bundle.getString("transaction_id", ""));
            transaction.setMongoId(bundle.getString("mongo_id", ""));
            transaction.setFromAccountNumber(bundle.getString("from_account_number", ""));
            transaction.setToAccountNumber(bundle.getString("to_account_number", ""));
            
            String amountStr = bundle.getString("amount", "0");
            try {
                transaction.setAmount(new java.math.BigDecimal(amountStr));
            } catch (Exception e) {
                transaction.setAmount(java.math.BigDecimal.ZERO);
            }
            
            transaction.setCurrency(bundle.getString("currency", "VND"));
            transaction.setTransactionType(bundle.getString("transaction_type", ""));
            transaction.setStatus(bundle.getString("status", ""));
            transaction.setDescription(bundle.getString("description", ""));
            transaction.setReferenceNumber(bundle.getString("reference_number", ""));
            
            if (bundle.containsKey("created_at")) {
                transaction.setCreatedAt(new Date(bundle.getLong("created_at")));
            }
            if (bundle.containsKey("completed_at")) {
                transaction.setCompletedAt(new Date(bundle.getLong("completed_at")));
            }
            
            displayTransaction(transaction);
            return;
        }
        
        // Fallback: Get transaction ID and load from API
        transactionId = getIntent().getStringExtra("transaction_id");
        mongoId = getIntent().getStringExtra("mongo_id");
        
        if (transactionId == null || transactionId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin giao dịch", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        loadTransactionDetail();
    }
    
    private void initViews() {
        tvTransactionId = findViewById(R.id.tv_transaction_id);
        tvAmount = findViewById(R.id.tv_amount);
        tvType = findViewById(R.id.tv_type);
        tvStatus = findViewById(R.id.tv_status);
        tvDescription = findViewById(R.id.tv_description);
        tvFromAccount = findViewById(R.id.tv_from_account);
        tvToAccount = findViewById(R.id.tv_to_account);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        tvCompletedAt = findViewById(R.id.tv_completed_at);
        tvReferenceNumber = findViewById(R.id.tv_reference_number);
    }
    
    private void loadTransactionDetail() {
        progressDialog.setMessage("Đang tải chi tiết giao dịch...");
        progressDialog.show();
        
        // Try with transaction_id first
        transactionService.getTransactionDetail(transactionId, new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(java.util.List<Transaction> transactions) {
                // Not used here
            }
            
            @Override
            public void onSingleTransactionSuccess(Transaction transaction) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    displayTransaction(transaction);
                });
            }
            
            @Override
            public void onError(String error) {
                // If transaction_id fails and we have MongoDB _id, try with that
                if (mongoId != null && !mongoId.isEmpty() && !mongoId.equals(transactionId)) {
                    android.util.Log.d(TAG, "Transaction_id failed, trying with MongoDB _id: " + mongoId);
                    progressDialog.setMessage("Đang thử lại...");
                    transactionService.getTransactionDetail(mongoId, new TransactionService.TransactionCallback() {
                        @Override
                        public void onSuccess(java.util.List<Transaction> transactions) {}
                        
                        @Override
                        public void onSingleTransactionSuccess(Transaction transaction) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                displayTransaction(transaction);
                            });
                        }
                        
                        @Override
                        public void onError(String error2) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(TransactionDetailActivity.this, 
                                    "Lỗi tải chi tiết giao dịch: " + error, Toast.LENGTH_LONG).show();
                                finish();
                            });
                        }
                        
                        @Override
                        public void onOtpRequired(String message, String transactionId) {}
                    });
                } else {
                    // No fallback, show error
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(TransactionDetailActivity.this, 
                            "Lỗi tải chi tiết giao dịch: " + error, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            }
            
            @Override
            public void onOtpRequired(String message, String transactionId) {
                // Not used here
            }
        });
    }
    
    private void displayTransaction(Transaction transaction) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        
        // Transaction ID
        if (transaction.getId() != null) {
            tvTransactionId.setText(transaction.getId());
        } else {
            tvTransactionId.setText("N/A");
        }
        
        // Amount
        if (transaction.getAmount() != null) {
            tvAmount.setText(formatCurrency(transaction.getAmount()));
            // Set color based on transaction direction
            String currentAccountId = sessionManager.getUserId(); // Assuming we can get account ID
            if (transaction.getToAccountId() != null && 
                transaction.getFromAccountId() != null &&
                !transaction.getToAccountId().equals(transaction.getFromAccountId())) {
                // Incoming - green
                tvAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
            } else {
                // Outgoing - red/primary
                tvAmount.setTextColor(getResources().getColor(R.color.primary_color, null));
            }
        } else {
            tvAmount.setText("0 VNĐ");
        }
        
        // Type
        tvType.setText(getTransactionTypeName(transaction.getTransactionType()));
        
        // Status
        String status = transaction.getStatus();
        tvStatus.setText(getStatusName(status));
        setStatusColor(status);
        
        // Description
        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            tvDescription.setText(transaction.getDescription());
        } else {
            tvDescription.setText("Không có mô tả");
        }
        
        // From Account
        if (transaction.getFromAccountNumber() != null && !transaction.getFromAccountNumber().isEmpty()) {
            tvFromAccount.setText(transaction.getFromAccountNumber());
        } else {
            tvFromAccount.setText("N/A");
        }
        
        // To Account
        if (transaction.getToAccountNumber() != null && !transaction.getToAccountNumber().isEmpty()) {
            tvToAccount.setText(transaction.getToAccountNumber());
        } else {
            tvToAccount.setText("N/A");
        }
        
        // Created At
        if (transaction.getCreatedAt() != null) {
            tvCreatedAt.setText(dateFormat.format(transaction.getCreatedAt()));
        } else {
            tvCreatedAt.setText("N/A");
        }
        
        // Completed At
        if (transaction.getCompletedAt() != null) {
            tvCompletedAt.setText(dateFormat.format(transaction.getCompletedAt()));
        } else {
            tvCompletedAt.setText("Chưa hoàn thành");
        }
        
        // Reference Number
        if (transaction.getReferenceNumber() != null && !transaction.getReferenceNumber().isEmpty()) {
            tvReferenceNumber.setText(transaction.getReferenceNumber());
        } else {
            tvReferenceNumber.setText("N/A");
        }
    }
    
    private void setStatusColor(String status) {
        int colorRes;
        switch (status) {
            case "COMPLETED":
                colorRes = android.R.color.holo_green_dark;
                break;
            case "PENDING":
            case "PROCESSING":
                colorRes = android.R.color.holo_orange_dark;
                break;
            case "FAILED":
                colorRes = android.R.color.holo_red_dark;
                break;
            case "CANCELLED":
                colorRes = android.R.color.darker_gray;
                break;
            default:
                colorRes = android.R.color.darker_gray;
        }
        tvStatus.setTextColor(getResources().getColor(colorRes, null));
    }
    
    private String getTransactionTypeName(String type) {
        if (type == null) return "N/A";
        switch (type) {
            case "TRANSFER": return "Chuyển tiền";
            case "DEPOSIT": return "Nạp tiền";
            case "WITHDRAWAL": return "Rút tiền";
            case "PAYMENT": return "Thanh toán";
            case "TOPUP": return "Nạp tiền điện thoại";
            default: return type;
        }
    }
    
    private String getStatusName(String status) {
        if (status == null) return "N/A";
        switch (status) {
            case "COMPLETED": return "Hoàn thành";
            case "PENDING": return "Chờ xử lý";
            case "PROCESSING": return "Đang xử lý";
            case "FAILED": return "Thất bại";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        return String.format(Locale.getDefault(), "%,.0f VNĐ", amount.doubleValue());
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

