package com.example.final_mobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.example.final_mobile.models.Account;
import com.example.final_mobile.models.Transaction;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.AdminService;
import com.example.final_mobile.services.SessionManager;
import com.example.final_mobile.services.TransactionService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TransactionFragment extends Fragment {

    private TextView tvFragmentLabel;
    private Button btnTransfer, btnDeposit, btnFilter, btnLoadMore;
    private LinearLayout llTransactionsContainer;
    private TextView tvNoTransactions;
    private TransactionService transactionService;
    private AccountService accountService;
    private AdminService adminService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private Account primaryAccount;
    private boolean isAdmin = false;
    private int currentPage = 1;
    private int limit = 20;
    private boolean hasNextPage = false;
    private String currentFilterType = null;
    private String currentFilterStatus = null;
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
        btnLoadMore = view.findViewById(R.id.btn_load_more);
        llTransactionsContainer = view.findViewById(R.id.ll_transactions_container);
        tvNoTransactions = view.findViewById(R.id.tv_no_transactions);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
        
        // Setup Load More button
        if (btnLoadMore != null) {
            btnLoadMore.setOnClickListener(v -> {
                if (isAdmin) {
                    loadMoreTransactions();
                }
            });
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
        accountService.getPrimaryAccount(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (!accounts.isEmpty() && getActivity() != null) {
                    primaryAccount = accounts.get(0);
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
        transactionService.getUserTransactions(currentPage, limit, new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (currentPage == 1) {
                            allTransactions.clear();
                        }
                        allTransactions.addAll(transactions);
                        // TODO: Update RecyclerView with real transactions
                        Toast.makeText(getContext(), "Đã tải " + allTransactions.size() + " giao dịch", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "Lỗi tải giao dịch: " + error, Toast.LENGTH_SHORT).show();
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
        // Use currentPage for the API call
        int pageToLoad = currentPage;
        adminService.getAllTransactions(pageToLoad, limit, currentFilterType, currentFilterStatus, new AdminService.TransactionListCallback() {
            @Override
            public void onSuccess(List<AdminService.RecentTransaction> transactions, int total, int page, int totalPages, boolean hasNext) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hasNextPage = hasNext;
                        
                        if (page == 1) {
                            allTransactions.clear();
                            currentPage = 2; // Set to 2 for next load
                        } else {
                            currentPage = page + 1; // Increment for next load
                        }

                        // Convert RecentTransaction to Transaction
                        for (AdminService.RecentTransaction rt : transactions) {
                            Transaction t = new Transaction();
                            t.setId(rt.getTransactionId());
                            t.setAmount(rt.getAmount());
                            t.setTransactionType(rt.getType());
                            t.setStatus(rt.getStatus());
                            t.setDescription(rt.getDescription());
                            allTransactions.add(t);
                        }

                        // Update UI
                        updateTransactionsDisplay();
                        
                        // Update Load More button
                        if (btnLoadMore != null) {
                            if (hasNextPage) {
                                btnLoadMore.setVisibility(View.VISIBLE);
                                int remaining = total - allTransactions.size();
                                if (remaining > 0) {
                                    btnLoadMore.setText("Xem thêm giao dịch (" + remaining + " còn lại)");
                                } else {
                                    btnLoadMore.setText("Xem thêm giao dịch");
                                }
                            } else {
                                btnLoadMore.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lỗi tải giao dịch: " + error, Toast.LENGTH_SHORT).show();
                        if (btnLoadMore != null) {
                            btnLoadMore.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }
    
    private void updateTransactionsDisplay() {
        if (llTransactionsContainer == null || tvNoTransactions == null) {
            return;
        }
        
        if (allTransactions.isEmpty()) {
            tvNoTransactions.setVisibility(View.VISIBLE);
            llTransactionsContainer.setVisibility(View.GONE);
        } else {
            tvNoTransactions.setVisibility(View.GONE);
            llTransactionsContainer.setVisibility(View.VISIBLE);
            llTransactionsContainer.removeAllViews();
            
            for (Transaction transaction : allTransactions) {
                View transactionView = createTransactionView(transaction);
                llTransactionsContainer.addView(transactionView);
            }
        }
    }
    
    private View createTransactionView(Transaction transaction) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.item_recent_transaction, llTransactionsContainer, false);
        
        TextView tvTransactionId = view.findViewById(R.id.tv_transaction_id);
        TextView tvAmount = view.findViewById(R.id.tv_amount);
        TextView tvType = view.findViewById(R.id.tv_type);
        TextView tvStatus = view.findViewById(R.id.tv_status);
        
        String transactionId = transaction.getId();
        if (transactionId != null && transactionId.length() > 0) {
            int displayLength = Math.min(8, transactionId.length());
            tvTransactionId.setText("GD: " + transactionId.substring(0, displayLength));
        } else {
            tvTransactionId.setText("GD: N/A");
        }
        
        if (transaction.getAmount() != null) {
            tvAmount.setText(formatCurrency(transaction.getAmount()));
        } else {
            tvAmount.setText("0 VNĐ");
        }
        
        tvType.setText(getTransactionTypeName(transaction.getTransactionType()));
        
        String status = transaction.getStatus();
        tvStatus.setText(status != null ? status : "N/A");
        
        // Set status color
        if ("COMPLETED".equals(status)) {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        } else if ("PENDING".equals(status)) {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, null));
        } else if ("FAILED".equals(status)) {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
        }
        
        return view;
    }
    
    private String getTransactionTypeName(String type) {
        if (type == null) return "N/A";
        switch (type) {
            case "TRANSFER": return "Chuyển tiền";
            case "DEPOSIT": return "Nạp tiền";
            case "WITHDRAWAL": return "Rút tiền";
            case "PAYMENT": return "Thanh toán";
            default: return type;
        }
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        java.text.NumberFormat formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault());
        return formatter.format(amount) + " VNĐ";
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
        progressDialog.setMessage("Đang xử lý chuyển tiền...");
        progressDialog.show();

        String fromAccountId = primaryAccount != null ? primaryAccount.getId() : null;

        transactionService.transferMoney(fromAccountId, toAccountNumber, amount, description, new TransactionService.TransactionCallback() {
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
        builder.setMessage("Để nạp tiền vào tài khoản, vui lòng:\n\n" +
                "1. Chuyển tiền từ ngân hàng khác\n" +
                "2. Nạp tiền tại ATM\n" +
                "3. Đến chi nhánh gần nhất\n\n" +
                "Số tài khoản của bạn:\n" + 
                (primaryAccount != null ? primaryAccount.getAccountNumber() : "Chưa có thông tin"));
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter_transactions, null);
        
        String[] typeOptions = {"Tất cả", "TRANSFER", "DEPOSIT", "WITHDRAWAL", "PAYMENT"};
        String[] statusOptions = {"Tất cả", "COMPLETED", "PENDING", "FAILED", "CANCELLED"};
        
        com.google.android.material.textfield.MaterialAutoCompleteTextView spinnerType = dialogView.findViewById(R.id.spinner_type);
        com.google.android.material.textfield.MaterialAutoCompleteTextView spinnerStatus = dialogView.findViewById(R.id.spinner_status);
        
        android.widget.ArrayAdapter<String> typeAdapter = new android.widget.ArrayAdapter<>(getContext(), 
            android.R.layout.simple_list_item_1, typeOptions);
        spinnerType.setAdapter(typeAdapter);
        
        android.widget.ArrayAdapter<String> statusAdapter = new android.widget.ArrayAdapter<>(getContext(), 
            android.R.layout.simple_list_item_1, statusOptions);
        spinnerStatus.setAdapter(statusAdapter);
        
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
            String toAccount = etToAccount.getText().toString().trim();
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
