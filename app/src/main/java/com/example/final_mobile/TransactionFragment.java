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
import com.example.final_mobile.services.TransactionService;

import java.math.BigDecimal;
import java.util.List;

public class TransactionFragment extends Fragment {

    private TextView tvFragmentLabel;
    private Button btnTransfer, btnDeposit, btnFilter;
    private TransactionService transactionService;
    private AccountService accountService;
    private ProgressDialog progressDialog;
    private Account primaryAccount;

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
        
        initViews(view);
        setupUI();
        loadAccountInfo();
        loadTransactions();
    }

    private void initViews(View view) {
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
        btnTransfer = view.findViewById(R.id.btn_transfer);
        btnDeposit = view.findViewById(R.id.btn_deposit);
        btnFilter = view.findViewById(R.id.btn_filter);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        // Setup fragment UI here
        tvFragmentLabel.setText(getString(R.string.transaction_fragment_label));
        
        // Setup button click listeners
        if (btnTransfer != null) {
            btnTransfer.setOnClickListener(v -> showTransferDialog());
        }
        
        if (btnDeposit != null) {
            btnDeposit.setOnClickListener(v -> showDepositInfo());
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
        transactionService.getUserTransactions(1, 20, new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // TODO: Update RecyclerView with real transactions
                        // For now, the static UI shows dummy data
                        Toast.makeText(getContext(), "Đã tải " + transactions.size() + " giao dịch", Toast.LENGTH_SHORT).show();
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
        // Simple filter dialog - in real app this would be more sophisticated
        String[] options = {"Tất cả", "Chuyển tiền", "Nạp tiền", "Thanh toán", "Nạp điện thoại"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Lọc giao dịch");
        builder.setItems(options, (dialog, which) -> {
            String selectedFilter = options[which];
            Toast.makeText(getContext(), "Đã chọn bộ lọc: " + selectedFilter, Toast.LENGTH_SHORT).show();
            // TODO: Implement actual filtering
        });
        builder.show();
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
        loadAccountInfo();
        loadTransactions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
