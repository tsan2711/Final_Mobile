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
import android.widget.TextView;
import android.widget.Toast;

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
        
        // Try to find buttons (these may not exist in current layout)
        // For now, we'll set them to null and handle clicks differently
        btnTransfer = null; // Not available in current layout
        btnDeposit = null; // Not available in current layout
        btnFilter = null; // Not available in current layout
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        // Setup fragment UI here
        tvFragmentLabel.setText(getString(R.string.transaction_fragment_label));
        
        // For demo purposes, make the label clickable to show transfer dialog
        tvFragmentLabel.setOnClickListener(v -> {
            // Show menu options
            String[] options = {"Chuyển tiền", "Thông tin nạp tiền", "Lọc giao dịch"};
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            builder.setTitle("Chọn chức năng");
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showTransferDialog();
                        break;
                    case 1:
                        showDepositInfo();
                        break;
                    case 2:
                        showFilterDialog();
                        break;
                }
            });
            builder.show();
        });
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chuyển tiền");

        // Create custom layout for transfer
        View dialogView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, null);
        
        // Create a simple vertical layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Account number input
        EditText etAccountNumber = new EditText(getContext());
        etAccountNumber.setHint("Số tài khoản người nhận");
        etAccountNumber.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAccountNumber);

        // Amount input
        EditText etAmount = new EditText(getContext());
        etAmount.setHint("Số tiền");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        // Description input
        EditText etDescription = new EditText(getContext());
        etDescription.setHint("Nội dung chuyển tiền");
        etDescription.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(etDescription);

        // Balance info
        TextView tvBalance = new TextView(getContext());
        tvBalance.setText("Số dư khả dụng: " + primaryAccount.getFormattedBalance());
        tvBalance.setTextSize(14);
        tvBalance.setPadding(0, 20, 0, 0);
        layout.addView(tvBalance);

        builder.setView(layout);

        builder.setPositiveButton("Chuyển tiền", (dialog, which) -> {
            String accountNumber = etAccountNumber.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (validateTransferInput(accountNumber, amountStr, description)) {
                performTransfer(accountNumber, new BigDecimal(amountStr), description);
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
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
            if (!transactionService.isValidTransferAmount(amount, primaryAccount.getBalance())) {
                Toast.makeText(getContext(), "Số tiền không hợp lệ hoặc không đủ số dư", Toast.LENGTH_SHORT).show();
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

        transactionService.transferMoney(toAccountNumber, amount, description, new TransactionService.TransactionCallback() {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Xác thực OTP");
        builder.setMessage(message);

        EditText otpInput = new EditText(getContext());
        otpInput.setHint("Nhập mã OTP");
        otpInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(otpInput);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String otpCode = otpInput.getText().toString().trim();
            if (otpCode.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập mã OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            
            verifyTransactionOtp(transactionId, otpCode);
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
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
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
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
