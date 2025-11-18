package com.example.final_mobile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ImageView;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.models.Transaction;
import com.example.final_mobile.models.User;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.SessionManager;
import com.example.final_mobile.services.TransactionService;
import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private TextView tvFragmentLabel;
    private TextView tvWelcomeMessage;
    private TextView tvAccountBalance;
    private TextView tvAccountNumber;
    private TextView tvSavingsBalance;
    private TextView tvMonthlyInterest;
    private TextView tvMortgagePayment;
    
    private MaterialCardView cardTransfer;
    private MaterialCardView cardPayment;
    private MaterialCardView cardAccount;
    private MaterialCardView cardInterestCalculator;
    private android.widget.ImageButton btnRefreshBalance;
    
    private LinearLayout llRecentTransactions;
    private TextView tvNoRecentTransactions;
    
    private AccountService accountService;
    private TransactionService transactionService;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize services
        accountService = new AccountService(getContext());
        transactionService = new TransactionService(getContext());
        sessionManager = SessionManager.getInstance(getContext());
        
        initViews(view);
        setupUI();
        loadUserData();
        loadRecentTransactions();
    }

    private void initViews(View view) {
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
        
        // Bind views from layout
        tvAccountBalance = view.findViewById(R.id.tv_account_balance);
        tvAccountNumber = view.findViewById(R.id.tv_account_number);
        btnRefreshBalance = view.findViewById(R.id.btn_refresh_balance);
        
        // Quick action cards
        cardTransfer = view.findViewById(R.id.card_transfer);
        cardPayment = view.findViewById(R.id.card_payment);
        cardAccount = view.findViewById(R.id.card_account);
        cardInterestCalculator = view.findViewById(R.id.card_interest_calculator);
        
        // Try to find views for dynamic data (these may not exist in current layout)
        tvWelcomeMessage = tvFragmentLabel; // Reuse existing view for welcome message
        
        // These would be in additional cards for savings and mortgage
        tvSavingsBalance = null; // Not available in current layout
        tvMonthlyInterest = null; // Not available in current layout
        tvMortgagePayment = null; // Not available in current layout
        
        // Recent transactions container
        llRecentTransactions = view.findViewById(R.id.ll_recent_transactions);
        tvNoRecentTransactions = view.findViewById(R.id.tv_no_recent_transactions);
        
        // Setup deposit/withdraw buttons
        Button btnDeposit = view.findViewById(R.id.btn_deposit);
        Button btnWithdraw = view.findViewById(R.id.btn_withdraw);
        
        if (btnDeposit != null) {
            btnDeposit.setOnClickListener(v -> showDepositDialog());
        }
        
        if (btnWithdraw != null) {
            btnWithdraw.setOnClickListener(v -> showWithdrawDialog());
        }
    }

    private void setupUI() {
        // Setup fragment UI here
        tvFragmentLabel.setText(getString(R.string.home_fragment_label));
        
        // Set welcome message
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null && tvWelcomeMessage != null) {
            String welcomeMessage = "Chào " + getFirstName(currentUser.getFullName()) + "!";
            tvWelcomeMessage.setText(welcomeMessage);
        }
        
        // Setup quick action card click listeners
        if (cardTransfer != null) {
            cardTransfer.setOnClickListener(v -> navigateToTransactionFragment());
        }
        
        if (cardPayment != null) {
            cardPayment.setOnClickListener(v -> navigateToUtilitiesFragment());
        }
        
        if (cardAccount != null) {
            cardAccount.setOnClickListener(v -> navigateToProfileFragment());
        }
        
        if (cardInterestCalculator != null) {
            cardInterestCalculator.setOnClickListener(v -> navigateToInterestCalculatorFragment());
        }
        
        // Refresh balance button
        if (btnRefreshBalance != null) {
            btnRefreshBalance.setOnClickListener(v -> {
                Log.d(TAG, "🔄 [DEBUG] User clicked refresh balance button");
                loadUserData();
                Toast.makeText(getContext(), "Đang làm mới số dư...", Toast.LENGTH_SHORT).show();
            });
        }

    }

    private void showDepositDialog() {
        accountService.getPrimaryAccount(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (getActivity() != null && !accounts.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        Account primaryAccount = accounts.get(0);
                        showDepositDialogInternal(primaryAccount);
                    });
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showDepositDialogInternal(account));
                }
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void showDepositDialogInternal(Account account) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_deposit, null);
        
        TextView tvBalance = dialogView.findViewById(R.id.tv_balance);
        tvBalance.setText(account.getFormattedBalance());
        
        com.google.android.material.textfield.TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        com.google.android.material.textfield.TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            
            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    Toast.makeText(getContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                dialog.dismiss();
                performDeposit(account.getId(), amount, description);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }

    private void showWithdrawDialog() {
        accountService.getPrimaryAccount(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (getActivity() != null && !accounts.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        Account primaryAccount = accounts.get(0);
                        showWithdrawDialogInternal(primaryAccount);
                    });
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showWithdrawDialogInternal(account));
                }
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void showWithdrawDialogInternal(Account account) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_withdraw, null);
        
        TextView tvBalance = dialogView.findViewById(R.id.tv_balance);
        tvBalance.setText(account.getFormattedBalance());
        
        com.google.android.material.textfield.TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        com.google.android.material.textfield.TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            
            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    Toast.makeText(getContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (amount.compareTo(account.getBalance()) > 0) {
                    Toast.makeText(getContext(), "Số dư không đủ", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                dialog.dismiss();
                performWithdraw(account.getId(), amount, description);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }

    private void performDeposit(String accountId, BigDecimal amount, String description) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("Đang xử lý nạp tiền...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        accountService.depositMoney(accountId, amount, description, new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Nạp tiền thành công!", Toast.LENGTH_LONG).show();
                        loadUserData();
                        loadRecentTransactions();
                    });
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Nạp tiền thành công!", Toast.LENGTH_LONG).show();
                        loadUserData();
                        loadRecentTransactions();
                    });
                }
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi nạp tiền: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void performWithdraw(String accountId, BigDecimal amount, String description) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("Đang xử lý rút tiền...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        accountService.withdrawMoney(accountId, amount, description, new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Rút tiền thành công!", Toast.LENGTH_LONG).show();
                        loadUserData();
                        loadRecentTransactions();
                    });
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Rút tiền thành công!", Toast.LENGTH_LONG).show();
                        loadUserData();
                        loadRecentTransactions();
                    });
                }
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi rút tiền: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void navigateToTransactionFragment() {
        if (getActivity() == null || !isAdded()) {
            return;
        }
        
        try {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            if (fragmentManager == null) {
                return;
            }
            
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new TransactionFragment());
            transaction.commit();
            
            // Update bottom navigation selection (this may trigger listener, but that's okay)
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.setBottomNavigationSelection(R.id.nav_transactions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi chuyển trang: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void navigateToUtilitiesFragment() {
        if (getActivity() == null || !isAdded()) {
            return;
        }
        
        try {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            if (fragmentManager == null) {
                return;
            }
            
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new UtilitiesFragment());
            transaction.commit();
            
            // Update bottom navigation selection
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.setBottomNavigationSelection(R.id.nav_utilities);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi chuyển trang: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void navigateToProfileFragment() {
        if (getActivity() == null || !isAdded()) {
            return;
        }
        
        try {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            if (fragmentManager == null) {
                return;
            }
            
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new ProfileFragment());
            transaction.commit();
            
            // Update bottom navigation selection
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.setBottomNavigationSelection(R.id.nav_profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi chuyển trang: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void navigateToInterestCalculatorFragment() {
        if (getActivity() == null || !isAdded()) {
            return;
        }
        
        try {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            if (fragmentManager == null) {
                return;
            }
            
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new InterestCalculatorFragment());
            transaction.addToBackStack(null); // Allow back navigation
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi chuyển trang: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserData() {
        Log.d(TAG, "🔄 [DEBUG] Loading user data (balance)...");
        // Load primary account information to ensure balance and account number are synchronized
        accountService.getPrimaryAccount(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                Log.d(TAG, "✅ [DEBUG] Accounts loaded: " + accounts.size());
                if (getActivity() != null && !accounts.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        // Get primary account from list (should be only one)
                        Account primaryAccount = accounts.get(0);
                        if (primaryAccount != null) {
                            Log.d(TAG, "💰 [DEBUG] Balance from API: " + primaryAccount.getBalance() + " VND");
                            Log.d(TAG, "📝 [DEBUG] Account Number: " + primaryAccount.getAccountNumber());
                        }
                        updatePrimaryAccountDisplay(primaryAccount);
                    });
                } else {
                    Log.w(TAG, "⚠️ [DEBUG] No accounts or activity is null");
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                // This can also be used if API returns single account
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updatePrimaryAccountDisplay(account));
                }
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {
                // Not used in this context
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // Additional check to prevent crash
                        if (getContext() == null || !isAdded()) {
                            return;
                        }
                        
                        if (error != null && (error.contains("No accounts found") || 
                                              error.contains("Account not found") ||
                                              error.contains("No primary account") ||
                                              error.contains("404"))) {
                            // User has no accounts yet - show friendly message and default values
                            updatePrimaryAccountDisplay(null);
                            
                            Toast.makeText(getContext(), 
                                "Tài khoản chưa được kích hoạt. Vui lòng liên hệ ngân hàng.", 
                                Toast.LENGTH_LONG).show();
                            
                            // Show default info
                            if (tvFragmentLabel != null && getView() != null) {
                                try {
                                    User currentUser = sessionManager.getCurrentUser();
                                    String name = "bạn";
                                    if (currentUser != null && currentUser.getFullName() != null) {
                                        String[] nameParts = currentUser.getFullName().trim().split("\\s+");
                                        name = nameParts[nameParts.length - 1];
                                    }
                                    
                                    String welcomeMsg = "Chào " + name + "!\n\n" +
                                            "Tài khoản của bạn chưa được kích hoạt.\n\n" +
                                            "Vui lòng liên hệ:\n" +
                                            "☎️ Hotline: 1900 1234\n" +
                                            "📧 Email: support@mybank.vn\n\n" +
                                            "để được hỗ trợ kích hoạt tài khoản.";
                                    tvFragmentLabel.setText(welcomeMsg);
                                } catch (Exception e) {
                                    // Fail silently - fragment may have been destroyed
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    // Update primary account display - ensure balance and account number are synchronized
    private void updatePrimaryAccountDisplay(Account primaryAccount) {
        if (primaryAccount != null) {
            BigDecimal balance = primaryAccount.getBalance();
            String formattedBalance = primaryAccount.getFormattedBalance();
            
            Log.d(TAG, "🔄 [DEBUG] Updating display - Balance: " + balance + 
                ", Formatted: " + formattedBalance);
            
            // Update balance and account number from the same account object
            // This ensures they are always synchronized
            if (tvAccountBalance != null) {
                tvAccountBalance.setText(formattedBalance);
                Log.d(TAG, "✅ [DEBUG] Balance displayed: " + formattedBalance);
            } else {
                Log.w(TAG, "⚠️ [DEBUG] tvAccountBalance is null!");
            }
            
            if (tvAccountNumber != null) {
                tvAccountNumber.setText(primaryAccount.getMaskedAccountNumber());
            }
        } else {
            Log.w(TAG, "⚠️ [DEBUG] Primary account is null, showing default values");
            // No primary account found - show default values
            if (tvAccountBalance != null) {
                tvAccountBalance.setText("0 VNĐ");
            }
            if (tvAccountNumber != null) {
                tvAccountNumber.setText("**** **** **** ****");
            }
        }
    }

    private void updateAccountDisplay(List<Account> accounts) {
        Account checkingAccount = null;
        Account savingsAccount = null;
        Account mortgageAccount = null;
        
        // Categorize accounts
        for (Account account : accounts) {
            switch (account.getAccountType()) {
                case Account.TYPE_CHECKING:
                    checkingAccount = account;
                    break;
                case Account.TYPE_SAVING:
                    savingsAccount = account;
                    break;
                case Account.TYPE_MORTGAGE:
                    mortgageAccount = account;
                    break;
            }
        }
        
        // Update checking account display - synchronize balance and account number
        updatePrimaryAccountDisplay(checkingAccount);
        
        // Update savings account display
        if (savingsAccount != null) {
            if (tvSavingsBalance != null) {
                tvSavingsBalance.setText("Tiết kiệm: " + savingsAccount.getFormattedBalance());
            }
            
            if (tvMonthlyInterest != null) {
                BigDecimal monthlyInterest = accountService.calculateMonthlyInterest(savingsAccount);
                tvMonthlyInterest.setText("Lãi tháng: " + formatCurrency(monthlyInterest));
            }
        }
        
        // Update mortgage account display
        if (mortgageAccount != null && tvMortgagePayment != null) {
            BigDecimal monthlyPayment = accountService.calculateMonthlyMortgagePayment(mortgageAccount);
            tvMortgagePayment.setText("Trả góp tháng: " + formatCurrency(monthlyPayment));
        }
    }

    private String getFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "bạn";
        }
        
        String[] nameParts = fullName.trim().split("\\s+");
        return nameParts[nameParts.length - 1]; // Get last part (first name in Vietnamese)
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f VNĐ", amount.doubleValue());
    }

    private void loadRecentTransactions() {
        // Load only the 5 most recent transactions for home screen
        transactionService.getRecentTransactions(new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateRecentTransactionsDisplay(transactions);
                        Log.d(TAG, "Loaded " + transactions.size() + " recent transactions");
                    });
                }
            }

            @Override
            public void onSingleTransactionSuccess(Transaction transaction) {
                // Not used here
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading recent transactions: " + error);
                // Don't show error toast for home screen - just show empty state
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (llRecentTransactions != null) {
                            llRecentTransactions.removeAllViews();
                        }
                        if (tvNoRecentTransactions != null) {
                            tvNoRecentTransactions.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onOtpRequired(String message, String transactionId) {
                // Not used here
            }
        });
    }

    private void updateRecentTransactionsDisplay(List<Transaction> transactions) {
        if (llRecentTransactions == null || tvNoRecentTransactions == null) {
            return;
        }

        llRecentTransactions.removeAllViews();

        if (transactions == null || transactions.isEmpty()) {
            tvNoRecentTransactions.setVisibility(View.VISIBLE);
            return;
        }

        tvNoRecentTransactions.setVisibility(View.GONE);

        // Show only first 5 transactions
        int maxTransactions = Math.min(5, transactions.size());
        for (int i = 0; i < maxTransactions; i++) {
            Transaction transaction = transactions.get(i);
            View transactionView = createRecentTransactionView(transaction);
            llRecentTransactions.addView(transactionView);
        }
    }

    private View createRecentTransactionView(Transaction transaction) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.item_recent_transaction_home, llRecentTransactions, false);

        ImageView ivIcon = view.findViewById(R.id.iv_transaction_icon);
        TextView tvDescription = view.findViewById(R.id.tv_transaction_description);
        TextView tvTime = view.findViewById(R.id.tv_transaction_time);
        TextView tvAmount = view.findViewById(R.id.tv_transaction_amount);

        // Set icon based on transaction type
        String transactionType = transaction.getTransactionType();
        if ("TRANSFER".equals(transactionType)) {
            ivIcon.setImageResource(R.drawable.ic_transactions);
            ivIcon.setBackgroundResource(R.drawable.circle_background_blue);
        } else if ("DEPOSIT".equals(transactionType)) {
            ivIcon.setImageResource(R.drawable.ic_utilities);
            ivIcon.setBackgroundResource(R.drawable.circle_background_green);
        } else {
            ivIcon.setImageResource(R.drawable.ic_transactions);
            ivIcon.setBackgroundResource(R.drawable.circle_background_blue);
        }

        // Set description
        String description = transaction.getDescription();
        if (description == null || description.isEmpty()) {
            description = getTransactionTypeName(transactionType);
        }
        tvDescription.setText(description);

        // Set time
        if (transaction.getCreatedAt() != null) {
            tvTime.setText(formatTransactionTime(transaction.getCreatedAt()));
        } else {
            tvTime.setText("Vừa xong");
        }

        // Set amount with color
        BigDecimal amount = transaction.getAmount();
        if (amount != null) {
            boolean isNegative = "TRANSFER".equals(transactionType) || "WITHDRAWAL".equals(transactionType);
            String amountText = (isNegative ? "-" : "+") + formatCurrency(amount);
            tvAmount.setText(amountText);
            tvAmount.setTextColor(getResources().getColor(
                isNegative ? R.color.withdrawal_color : R.color.deposit_color, null));
        } else {
            tvAmount.setText("0 VNĐ");
        }

        // Set click listener to navigate to transaction detail
        view.setOnClickListener(v -> {
            // Navigate to transaction fragment
            navigateToTransactionFragment();
        });

        return view;
    }

    private String getTransactionTypeName(String type) {
        if (type == null) return "Giao dịch";
        switch (type) {
            case "TRANSFER": return "Chuyển tiền";
            case "DEPOSIT": return "Nạp tiền";
            case "WITHDRAWAL": return "Rút tiền";
            case "PAYMENT": return "Thanh toán";
            default: return "Giao dịch";
        }
    }

    private String formatTransactionTime(Date date) {
        if (date == null) return "Vừa xong";
        
        Date now = new Date();
        long diff = now.getTime() - date.getTime();
        long diffMinutes = diff / (60 * 1000);
        long diffHours = diff / (60 * 60 * 1000);
        long diffDays = diff / (24 * 60 * 60 * 1000);

        if (diffMinutes < 1) {
            return "Vừa xong";
        } else if (diffMinutes < 60) {
            return diffMinutes + " phút trước";
        } else if (diffHours < 24) {
            return diffHours + " giờ trước";
        } else if (diffDays == 1) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "Hôm qua, " + sdf.format(date);
        } else if (diffDays < 7) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, HH:mm", Locale.getDefault());
            return sdf.format(date);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault());
            return sdf.format(date);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadUserData();
        loadRecentTransactions();
    }
}
