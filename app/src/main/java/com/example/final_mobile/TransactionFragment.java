package com.example.final_mobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.final_mobile.adapters.TransactionAdapter;
import com.example.final_mobile.models.Account;
import com.example.final_mobile.models.Transaction;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.AdminService;
import com.example.final_mobile.services.EkycService;
import com.example.final_mobile.services.SessionManager;
import com.example.final_mobile.services.TransactionService;
import com.example.final_mobile.utils.BiometricHelper;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionFragment extends Fragment {

    private static final String TAG = "TransactionFragment";
    private TextView tvFragmentLabel;
    private Button btnTransfer, btnDeposit, btnFilter;
    private RecyclerView rvTransactions;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvNoTransactions;
    private TransactionService transactionService;
    private AccountService accountService;
    private AdminService adminService;
    private EkycService ekycService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private Account primaryAccount;
    private TransactionAdapter transactionAdapter;
    
    // Store transfer details for biometric verification
    private String pendingToAccountNumber;
    private BigDecimal pendingAmount;
    private String pendingDescription;
    private boolean isAdmin = false;
    private int currentPage = 1;
    private int limit = 20;
    private boolean hasNextPage = false;
    private boolean isLoading = false;
    private String currentFilterType = null;
    private String currentFilterStatus = null;
    private Date currentFilterDateFrom = null;
    private Date currentFilterDateTo = null;
    private List<Transaction> allTransactions = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize services
        transactionService = new TransactionService(getContext());
        accountService = new AccountService(getContext());
        adminService = new AdminService(getContext());
        ekycService = new EkycService(getContext());
        sessionManager = SessionManager.getInstance(getContext());
        isAdmin = sessionManager.isBankOfficer();
        
        initViews(view);
        setupUI();
        
        if (isAdmin) {
            // Admin: Load all customer transactions
            loadAllTransactions();
        } else {
            // Customer: Load own account and transactions
            loadAccountInfo();
            loadTransactions();
        }
    }

    private void initViews(View view) {
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
        btnTransfer = view.findViewById(R.id.btn_transfer);
        btnDeposit = view.findViewById(R.id.btn_deposit);
        btnFilter = view.findViewById(R.id.btn_filter);
        rvTransactions = view.findViewById(R.id.rv_transactions);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        tvNoTransactions = view.findViewById(R.id.tv_no_transactions);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshTransactions();
        });
        
        // Setup infinite scroll
        rvTransactions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if (!isLoading && hasNextPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {
                            loadMoreTransactions();
                        }
                    }
                }
            }
        });
    }
    
    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(allTransactions);
        transactionAdapter.setOnTransactionClickListener(transaction -> {
            // Open transaction detail
            openTransactionDetail(transaction);
        });
        
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(transactionAdapter);
    }
    
    private void openTransactionDetail(Transaction transaction) {
        // Navigate to TransactionDetailActivity
        // Pass the full transaction object instead of just ID to avoid API call issues
        android.content.Intent intent = new android.content.Intent(getContext(), TransactionDetailActivity.class);
        
        // Pass transaction data directly to avoid API call (backend has issues with detail API)
        intent.putExtra("transaction_id", transaction.getTransactionId() != null ? transaction.getTransactionId() : transaction.getId());
        intent.putExtra("mongo_id", transaction.getMongoId());
        
        // Pass transaction data as Parcelable or via Bundle
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("transaction_id", transaction.getTransactionId() != null ? transaction.getTransactionId() : transaction.getId());
        bundle.putString("mongo_id", transaction.getMongoId());
        bundle.putString("from_account_number", transaction.getFromAccountNumber());
        bundle.putString("to_account_number", transaction.getToAccountNumber());
        bundle.putString("amount", transaction.getAmount() != null ? transaction.getAmount().toString() : "0");
        bundle.putString("currency", transaction.getCurrency() != null ? transaction.getCurrency() : "VND");
        bundle.putString("transaction_type", transaction.getTransactionType());
        bundle.putString("status", transaction.getStatus());
        bundle.putString("description", transaction.getDescription());
        bundle.putString("reference_number", transaction.getReferenceNumber());
        if (transaction.getCreatedAt() != null) {
            bundle.putLong("created_at", transaction.getCreatedAt().getTime());
        }
        if (transaction.getCompletedAt() != null) {
            bundle.putLong("completed_at", transaction.getCompletedAt().getTime());
        }
        intent.putExtras(bundle);
        
        Log.d(TAG, "Opening transaction detail - transaction_id: " + transaction.getTransactionId() + 
              ", mongoId: " + transaction.getMongoId());
        
        startActivity(intent);
    }
    
    private void refreshTransactions() {
        currentPage = 1;
        allTransactions.clear();
        transactionAdapter.setTransactions(allTransactions);
        hasNextPage = true;
        
        if (isAdmin) {
            loadAllTransactions();
        } else {
            loadTransactions();
        }
    }

    private void setupUI() {
        // Setup fragment UI here
        if (isAdmin) {
            tvFragmentLabel.setText("Quản lý giao dịch khách hàng");
            if (btnTransfer != null) {
                btnTransfer.setText("Chuyển tiền");
            }
            if (btnDeposit != null) {
                btnDeposit.setText("Nạp tiền");
            }
        } else {
            tvFragmentLabel.setText(getString(R.string.transaction_fragment_label));
        }
        
        // Setup button click listeners
        if (btnTransfer != null) {
            btnTransfer.setOnClickListener(v -> {
                if (isAdmin) {
                    showAdminTransferDialog();
                } else {
                    showTransferDialog();
                }
            });
        }
        
        if (btnDeposit != null) {
            btnDeposit.setOnClickListener(v -> {
                if (isAdmin) {
                    showAdminDepositDialog();
                } else {
                    showDepositInfo();
                }
            });
        }
        
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }
    }

    private void loadAccountInfo() {
        android.util.Log.d("TransactionFragment", "🔄 [DEBUG] Loading account info...");
        accountService.getPrimaryAccount(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                android.util.Log.d("TransactionFragment", "✅ [DEBUG] Account loaded: " + accounts.size() + " accounts");
                if (!accounts.isEmpty() && getActivity() != null) {
                    primaryAccount = accounts.get(0);
                    if (primaryAccount != null) {
                        android.util.Log.d("TransactionFragment", "💰 [DEBUG] Primary Account Balance: " + 
                            primaryAccount.getBalance() + " VND");
                        android.util.Log.d("TransactionFragment", "📝 [DEBUG] Account Number: " + 
                            primaryAccount.getAccountNumber());
                    }
                } else {
                    android.util.Log.w("TransactionFragment", "⚠️ [DEBUG] No accounts found or activity is null");
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                // Not used here
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {
                // Not used here
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lỗi tải thông tin tài khoản: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void loadTransactions() {
        if (isLoading) return;
        
        isLoading = true;
        transactionAdapter.setLoading(true);
        
        if (currentPage == 1) {
            progressDialog.setMessage("Đang tải lịch sử giao dịch...");
            progressDialog.show();
        }
        
        transactionService.getUserTransactions(currentPage, limit, new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        transactionAdapter.setLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        progressDialog.dismiss();
                        
                        if (currentPage == 1) {
                            allTransactions.clear();
                            transactionAdapter.setTransactions(transactions);
                        } else {
                            transactionAdapter.addTransactions(transactions);
                        }
                        allTransactions.addAll(transactions);
                        
                        hasNextPage = transactions.size() == limit;
                        currentPage++;
                        
                        // Update UI
                        updateTransactionsDisplay();
                        Log.d(TAG, "Loaded " + allTransactions.size() + " transactions, UI updated");
                    });
                }
            }

            @Override
            public void onSingleTransactionSuccess(Transaction transaction) {
                // Not used here
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        transactionAdapter.setLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi tải giao dịch: " + error, Toast.LENGTH_SHORT).show();
                        updateTransactionsDisplay();
                    });
                }
            }

            @Override
            public void onOtpRequired(String message, String transactionId) {
                // Not used here
            }
        });
    }

    private void loadAllTransactions() {
        currentPage = 1;
        allTransactions.clear();
        hasNextPage = false; // Reset hasNextPage
        loadMoreTransactions();
    }

    private void loadMoreTransactions() {
        if (isLoading || !hasNextPage) return;
        
        if (isAdmin) {
            // Use currentPage for the API call
            int pageToLoad = currentPage;
            isLoading = true;
            transactionAdapter.setLoading(true);
            
            adminService.getAllTransactions(pageToLoad, limit, currentFilterType, currentFilterStatus, new AdminService.TransactionListCallback() {
                @Override
                public void onSuccess(List<AdminService.RecentTransaction> transactions, int total, int page, int totalPages, boolean hasNext) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            isLoading = false;
                            transactionAdapter.setLoading(false);
                            hasNextPage = hasNext;
                            
                            if (page == 1) {
                                allTransactions.clear();
                                currentPage = 2; // Set to 2 for next load
                            } else {
                                currentPage = page + 1; // Increment for next load
                            }

                            // Convert RecentTransaction to Transaction
                            List<Transaction> transactionList = new ArrayList<>();
                            for (AdminService.RecentTransaction rt : transactions) {
                                Transaction t = new Transaction();
                                t.setId(rt.getTransactionId());
                                t.setAmount(rt.getAmount());
                                t.setTransactionType(rt.getType());
                                t.setStatus(rt.getStatus());
                                t.setDescription(rt.getDescription());
                                transactionList.add(t);
                            }
                            
                            if (page == 1) {
                                transactionAdapter.setTransactions(transactionList);
                            } else {
                                transactionAdapter.addTransactions(transactionList);
                            }
                            allTransactions.addAll(transactionList);

                            // Update UI
                            updateTransactionsDisplay();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            isLoading = false;
                            transactionAdapter.setLoading(false);
                            Toast.makeText(getContext(), "Lỗi tải giao dịch: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } else {
            // Customer: use regular loadTransactions which handles pagination
            loadTransactions();
        }
    }
    
    private void updateTransactionsDisplay() {
        if (rvTransactions == null || tvNoTransactions == null) {
            return;
        }
        
        if (allTransactions.isEmpty() && !isLoading) {
            tvNoTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvNoTransactions.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
        }
    }

    private void showTransferDialog() {
        if (primaryAccount == null) {
            Toast.makeText(getContext(), "Chưa có thông tin tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_transfer_money, null);
        
        TextView tvBalance = dialogView.findViewById(R.id.tv_balance);
        tvBalance.setText(primaryAccount.getFormattedBalance());
        
        TextInputEditText etAccountNumber = dialogView.findViewById(R.id.et_account_number);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String accountNumber = etAccountNumber.getText().toString().trim();
            String originalAccountNumber = accountNumber;
            // Remove all spaces and non-digit characters from account number to ensure clean format
            accountNumber = accountNumber.replaceAll("\\s", "").replaceAll("[^0-9]", "");
            
            Log.d(TAG, "Transfer - Original account number: \"" + originalAccountNumber + "\"");
            Log.d(TAG, "Transfer - Normalized account number: \"" + accountNumber + "\"");
            
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (validateTransferInput(accountNumber, amountStr, description)) {
                dialog.dismiss();
                performTransfer(accountNumber, new BigDecimal(amountStr), description);
            }
        });
        
        dialog.show();
    }

    private boolean validateTransferInput(String accountNumber, String amountStr, String description) {
        if (TextUtils.isEmpty(accountNumber)) {
            Toast.makeText(getContext(), "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!transactionService.isValidAccountNumber(accountNumber)) {
            Toast.makeText(getContext(), "Số tài khoản không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            
            // Check minimum amount
            BigDecimal minAmount = new BigDecimal("10000");
            if (amount.compareTo(minAmount) < 0) {
                Toast.makeText(getContext(), "Số tiền chuyển tối thiểu là 10,000 VND", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            // Calculate fee and check balance
            BigDecimal fee = transactionService.calculateFee(amount);
            BigDecimal totalAmount = amount.add(fee);
            BigDecimal balance = primaryAccount.getBalance();
            
            if (balance == null || balance.compareTo(totalAmount) < 0) {
                String message = String.format("Số dư không đủ. Bạn cần %s VND (bao gồm phí %s VND) nhưng số dư hiện tại là %s VND",
                    formatAmount(totalAmount), formatAmount(fee), formatAmount(balance));
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(description)) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung chuyển tiền", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void performTransfer(String toAccountNumber, BigDecimal amount, String description) {
        // Check if this is a high-value transaction requiring biometric verification
        double amountDouble = amount.doubleValue();
        if (EkycService.requiresBiometricVerification(amountDouble)) {
            // Store transfer details
            pendingToAccountNumber = toAccountNumber;
            pendingAmount = amount;
            pendingDescription = description;
            
            // Check if biometric is available
            if (getActivity() != null && BiometricHelper.isBiometricAvailable(getContext())) {
                // Device has biometric, use it
                android.util.Log.d("TransactionFragment", "🔐 [DEBUG] Using biometric authentication");
                BiometricHelper.showTransactionBiometricPrompt(
                    getActivity(),
                    amountDouble,
                    new BiometricHelper.BiometricCallback() {
                        @Override
                        public void onSuccess() {
                            // Biometric verified, proceed with transfer
                            android.util.Log.d("TransactionFragment", "✅ [DEBUG] Biometric verified, proceeding with transfer");
                            executeTransfer();
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.e("TransactionFragment", "❌ [DEBUG] Biometric error: " + error);
                            // If biometric fails, try eKYC as fallback
                            checkEkycAndProceed(error);
                        }

                        @Override
                        public void onCancel() {
                            android.util.Log.d("TransactionFragment", "⚠️ [DEBUG] User cancelled biometric");
                            // User cancelled biometric, try eKYC as fallback
                            checkEkycAndProceed("Biometric đã bị hủy");
                        }
                    }
                );
            } else {
                // No biometric available, use eKYC verification instead
                android.util.Log.d("TransactionFragment", "⚠️ [DEBUG] Biometric not available, using eKYC verification");
                checkEkycAndProceed(null);
            }
        } else {
            // Normal transaction, proceed directly
            pendingToAccountNumber = toAccountNumber;
            pendingAmount = amount;
            pendingDescription = description;
            executeTransfer();
        }
    }
    
    private void checkEkycAndProceed(String biometricError) {
        // Check eKYC status first
        android.util.Log.d("TransactionFragment", "🔄 [DEBUG] Checking eKYC status...");
        ekycService.getVerificationStatus(new EkycService.EkycCallback() {
            @Override
            public void onSuccess(org.json.JSONObject data) {
                try {
                    String status = data.optString("verification_status", "NOT_STARTED");
                    boolean isValid = data.optBoolean("is_valid", false);
                    
                    android.util.Log.d("TransactionFragment", "📊 [DEBUG] eKYC Status: " + status + ", Valid: " + isValid);
                    
                    if (isValid && "VERIFIED".equals(status)) {
                        // eKYC is valid, proceed with transfer
                        android.util.Log.d("TransactionFragment", "✅ [DEBUG] eKYC verified, proceeding with transfer");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (biometricError != null) {
                                    Toast.makeText(getContext(), 
                                        "Đã xác thực bằng eKYC. Tiếp tục giao dịch...", 
                                        Toast.LENGTH_SHORT).show();
                                }
                                executeTransfer();
                            });
                        }
                    } else {
                        // eKYC not valid, show error
                        android.util.Log.w("TransactionFragment", "⚠️ [DEBUG] eKYC not valid: " + status);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                String message = "Giao dịch giá trị cao yêu cầu xác thực eKYC. ";
                                if ("NOT_STARTED".equals(status)) {
                                    message += "Vui lòng hoàn thành xác thực eKYC trước.";
                                } else if ("PENDING".equals(status)) {
                                    message += "eKYC đang chờ xác thực. Vui lòng đợi hoặc thử lại sau.";
                                } else if ("REJECTED".equals(status)) {
                                    message += "eKYC đã bị từ chối. Vui lòng xác thực lại.";
                                } else {
                                    message += "Trạng thái eKYC: " + status;
                                }
                                
                                new android.app.AlertDialog.Builder(getContext())
                                    .setTitle("Cần xác thực eKYC")
                                    .setMessage(message)
                                    .setPositiveButton("Xác thực eKYC", (dialog, which) -> {
                                        // Navigate to eKYC
                                        android.content.Intent intent = new android.content.Intent(getActivity(), FaceCaptureActivity.class);
                                        startActivity(intent);
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                            });
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("TransactionFragment", "❌ [DEBUG] Error checking eKYC", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), 
                                "Lỗi kiểm tra eKYC. Vui lòng thử lại.", 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("TransactionFragment", "❌ [DEBUG] eKYC check error: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // If biometric error was shown, don't show another error
                        if (biometricError == null) {
                            Toast.makeText(getContext(), 
                                "Lỗi kiểm tra eKYC: " + error, 
                                Toast.LENGTH_LONG).show();
                        } else {
                            // Show combined message
                            Toast.makeText(getContext(), 
                                "Thiết bị không hỗ trợ biometric và eKYC chưa được xác thực. Vui lòng hoàn thành eKYC.", 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
    
    private void executeTransfer() {
        progressDialog.setMessage("Đang xử lý chuyển tiền...");
        progressDialog.show();

        String fromAccountId = primaryAccount != null ? primaryAccount.getId() : null;

        transactionService.transferMoney(fromAccountId, pendingToAccountNumber, pendingAmount, pendingDescription, new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                // Not used here
            }

            @Override
            public void onSingleTransactionSuccess(Transaction transaction) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Chuyển tiền thành công!", Toast.LENGTH_LONG).show();
                        // Reset to first page and refresh
                        currentPage = 1;
                        loadTransactions(); // Refresh transaction list
                        loadAccountInfo(); // Refresh account balance
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showErrorDialog("Lỗi chuyển tiền", error);
                    });
                }
            }

            @Override
            public void onOtpRequired(String message, String transactionId) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showOtpDialog(message, transactionId);
                    });
                }
            }
        });
    }

    private void showOtpDialog(String message, String transactionId) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_otp_verification, null);
        
        TextView tvTransactionInfo = dialogView.findViewById(R.id.tv_transaction_info);
        tvTransactionInfo.setText(message);
        
        TextInputEditText etOtp = dialogView.findViewById(R.id.et_otp);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(false)
            .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String otpCode = etOtp.getText().toString().trim();
            if (otpCode.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập mã OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            
            dialog.dismiss();
            verifyTransactionOtp(transactionId, otpCode);
        });
        
        dialog.show();
    }

    private void verifyTransactionOtp(String transactionId, String otpCode) {
        progressDialog.setMessage("Đang xác thực OTP...");
        progressDialog.show();

        transactionService.verifyTransactionOtp(transactionId, otpCode, new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                // Not used here
            }

            @Override
            public void onSingleTransactionSuccess(Transaction transaction) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Giao dịch thành công!", Toast.LENGTH_LONG).show();
                        // Reset to first page and refresh
                        currentPage = 1;
                        loadTransactions();
                        loadAccountInfo();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showErrorDialog("Lỗi xác thực OTP", error);
                    });
                }
            }

            @Override
            public void onOtpRequired(String message, String transactionId) {
                // This shouldn't happen in OTP verification
            }
        });
    }

    private void showDepositInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Nạp tiền");
        builder.setMessage("Chọn phương thức nạp tiền:\n\n" +
                "1. Nạp tiền qua VNPay\n" +
                "2. Chuyển khoản từ ngân hàng khác\n\n" +
                "Số tài khoản của bạn:\n" + 
                (primaryAccount != null ? primaryAccount.getAccountNumber() : "Chưa có thông tin"));
        builder.setPositiveButton("Mở trang thanh toán", (dialog, which) -> {
            // Open PaymentActivity
            android.content.Intent intent = new android.content.Intent(getContext(), PaymentActivity.class);
            startActivity(intent);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter_transactions, null);
        
        String[] typeOptions = {"Tất cả", "TRANSFER", "DEPOSIT", "WITHDRAWAL", "PAYMENT"};
        String[] statusOptions = {"Tất cả", "COMPLETED", "PENDING", "FAILED", "CANCELLED"};
        
        com.google.android.material.textfield.MaterialAutoCompleteTextView spinnerType = dialogView.findViewById(R.id.spinner_type);
        com.google.android.material.textfield.MaterialAutoCompleteTextView spinnerStatus = dialogView.findViewById(R.id.spinner_status);
        TextInputEditText etDateFrom = dialogView.findViewById(R.id.et_date_from);
        TextInputEditText etDateTo = dialogView.findViewById(R.id.et_date_to);
        
        android.widget.ArrayAdapter<String> typeAdapter = new android.widget.ArrayAdapter<>(getContext(), 
            android.R.layout.simple_list_item_1, typeOptions);
        spinnerType.setAdapter(typeAdapter);
        
        android.widget.ArrayAdapter<String> statusAdapter = new android.widget.ArrayAdapter<>(getContext(), 
            android.R.layout.simple_list_item_1, statusOptions);
        spinnerStatus.setAdapter(statusAdapter);
        
        // Date formatter
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        // Set current date values
        if (currentFilterDateFrom != null) {
            etDateFrom.setText(dateFormatter.format(currentFilterDateFrom));
        }
        if (currentFilterDateTo != null) {
            etDateTo.setText(dateFormatter.format(currentFilterDateTo));
        }
        
        // Date picker listeners
        etDateFrom.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (currentFilterDateFrom != null) {
                calendar.setTime(currentFilterDateFrom);
            }
            new android.app.DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                currentFilterDateFrom = selectedDate.getTime();
                etDateFrom.setText(dateFormatter.format(currentFilterDateFrom));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        
        etDateTo.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (currentFilterDateTo != null) {
                calendar.setTime(currentFilterDateTo);
            }
            new android.app.DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                currentFilterDateTo = selectedDate.getTime();
                etDateTo.setText(dateFormatter.format(currentFilterDateTo));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        
        // Set current selections - find index and set text
        // Use final variables for lambda expression
        final String selectedTypeValue;
        if (currentFilterType != null) {
            String foundValue = null;
            for (int i = 0; i < typeOptions.length; i++) {
                if (typeOptions[i].equals(currentFilterType)) {
                    foundValue = typeOptions[i];
                    break;
                }
            }
            selectedTypeValue = foundValue != null ? foundValue : typeOptions[0];
        } else {
            selectedTypeValue = typeOptions[0];
        }
        
        final String selectedStatusValue;
        if (currentFilterStatus != null) {
            String foundValue = null;
            for (int i = 0; i < statusOptions.length; i++) {
                if (statusOptions[i].equals(currentFilterStatus)) {
                    foundValue = statusOptions[i];
                    break;
                }
            }
            selectedStatusValue = foundValue != null ? foundValue : statusOptions[0];
        } else {
            selectedStatusValue = statusOptions[0];
        }
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnApply = dialogView.findViewById(R.id.btn_apply);
        MaterialButton btnReset = dialogView.findViewById(R.id.btn_reset);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        // Set text after dialog is shown to ensure view is ready
        dialog.setOnShowListener(dialogInterface -> {
            spinnerType.setText(selectedTypeValue, false);
            spinnerStatus.setText(selectedStatusValue, false);
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnReset.setOnClickListener(v -> {
            spinnerType.setText(typeOptions[0], false);
            spinnerStatus.setText(statusOptions[0], false);
            etDateFrom.setText("");
            etDateTo.setText("");
            currentFilterDateFrom = null;
            currentFilterDateTo = null;
        });
        btnApply.setOnClickListener(v -> {
            String selectedType = spinnerType.getText().toString().trim();
            String selectedStatus = spinnerStatus.getText().toString().trim();
            
            // If empty, use "Tất cả"
            if (selectedType.isEmpty()) {
                selectedType = typeOptions[0];
            }
            if (selectedStatus.isEmpty()) {
                selectedStatus = statusOptions[0];
            }
            
            currentFilterType = selectedType.equals("Tất cả") ? null : selectedType;
            currentFilterStatus = selectedStatus.equals("Tất cả") ? null : selectedStatus;
            
            // Date filters are already set by date pickers
            // Note: Backend may need to be updated to support date filters
            
            dialog.dismiss();
            
            if (isAdmin) {
                loadAllTransactions(); // Reload with filters
            } else {
                currentPage = 1;
                allTransactions.clear();
                loadTransactions();
            }
        });
        
        dialog.show();
    }

    private void showAdminTransferDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_admin_transfer, null);
        
        TextInputEditText etFromAccount = dialogView.findViewById(R.id.et_from_account);
        TextInputEditText etToAccount = dialogView.findViewById(R.id.et_to_account);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String fromAccount = etFromAccount.getText().toString().trim();
            String originalFromAccount = fromAccount;
            // Remove all spaces and non-digit characters from account numbers to ensure clean format
            fromAccount = fromAccount.replaceAll("\\s", "").replaceAll("[^0-9]", "");
            String toAccount = etToAccount.getText().toString().trim();
            String originalToAccount = toAccount;
            toAccount = toAccount.replaceAll("\\s", "").replaceAll("[^0-9]", "");
            
            Log.d(TAG, "Admin Transfer - From: \"" + originalFromAccount + "\" -> \"" + fromAccount + "\"");
            Log.d(TAG, "Admin Transfer - To: \"" + originalToAccount + "\" -> \"" + toAccount + "\"");
            
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (validateAdminTransferInput(fromAccount, toAccount, amountStr, description)) {
                dialog.dismiss();
                performAdminTransfer(fromAccount, toAccount, new BigDecimal(amountStr), description);
            }
        });
        
        dialog.show();
    }

    private void showAdminDepositDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_admin_deposit, null);
        
        TextInputEditText etAccountNumber = dialogView.findViewById(R.id.et_account_number);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String accountNumber = etAccountNumber.getText().toString().trim();
            String originalAccountNumber = accountNumber;
            // Remove all spaces and non-digit characters from account number to ensure clean format
            accountNumber = accountNumber.replaceAll("\\s", "").replaceAll("[^0-9]", "");
            
            Log.d(TAG, "Admin Deposit - Original: \"" + originalAccountNumber + "\" -> Normalized: \"" + accountNumber + "\"");
            
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (validateAdminDepositInput(accountNumber, amountStr, description)) {
                dialog.dismiss();
                performAdminDeposit(accountNumber, new BigDecimal(amountStr), description);
            }
        });
        
        dialog.show();
    }

    private boolean validateAdminTransferInput(String fromAccount, String toAccount, String amountStr, String description) {
        if (TextUtils.isEmpty(fromAccount)) {
            Toast.makeText(getContext(), "Vui lòng nhập số tài khoản nguồn", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(toAccount)) {
            Toast.makeText(getContext(), "Vui lòng nhập số tài khoản đích", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (fromAccount.equals(toAccount)) {
            Toast.makeText(getContext(), "Không thể chuyển tiền đến cùng tài khoản", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(new BigDecimal("10000")) < 0) {
                Toast.makeText(getContext(), "Số tiền chuyển tối thiểu là 10,000 VND", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validateAdminDepositInput(String accountNumber, String amountStr, String description) {
        if (TextUtils.isEmpty(accountNumber)) {
            Toast.makeText(getContext(), "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                Toast.makeText(getContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void performAdminTransfer(String fromAccount, String toAccount, BigDecimal amount, String description) {
        progressDialog.setMessage("Đang xử lý chuyển tiền...");
        progressDialog.show();

        adminService.transferMoney(fromAccount, toAccount, amount, description, new AdminService.AdminCallback() {
            @Override
            public void onSuccess(Object data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Chuyển tiền thành công!", Toast.LENGTH_LONG).show();
                        loadAllTransactions(); // Refresh transaction list
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showErrorDialog("Lỗi chuyển tiền", error);
                    });
                }
            }
        });
    }

    private void performAdminDeposit(String accountNumber, BigDecimal amount, String description) {
        progressDialog.setMessage("Đang xử lý nạp tiền...");
        progressDialog.show();

        adminService.depositMoney(accountNumber, amount, description, new AdminService.AdminCallback() {
            @Override
            public void onSuccess(Object data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Nạp tiền thành công!", Toast.LENGTH_LONG).show();
                        loadAllTransactions(); // Refresh transaction list
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showErrorDialog("Lỗi nạp tiền", error);
                    });
                }
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_error, null);
        
        TextView tvTitle = dialogView.findViewById(R.id.tv_title);
        tvTitle.setText(title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_message);
        tvMessage.setText(message);
        
        MaterialButton btnOk = dialogView.findViewById(R.id.btn_ok);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnOk.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        // Format with thousand separator
        return amount.toPlainString().replaceAll("(\\d)(?=(\\d{3})+(?!\\d))", "$1.");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        if (isAdmin) {
            loadAllTransactions();
        } else {
            loadAccountInfo();
            loadTransactions();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
