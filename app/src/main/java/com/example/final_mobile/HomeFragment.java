package com.example.final_mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.models.User;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.SessionManager;

import java.math.BigDecimal;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvFragmentLabel;
    private TextView tvWelcomeMessage;
    private TextView tvAccountBalance;
    private TextView tvAccountNumber;
    private TextView tvSavingsBalance;
    private TextView tvMonthlyInterest;
    private TextView tvMortgagePayment;
    
    private AccountService accountService;
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
        sessionManager = SessionManager.getInstance(getContext());
        
        initViews(view);
        setupUI();
        loadUserData();
    }

    private void initViews(View view) {
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
        
        // Try to find views for dynamic data (these may not exist in current layout)
        // For now, we'll use the main label to show dynamic content
        tvWelcomeMessage = tvFragmentLabel; // Reuse existing view for welcome message
        tvAccountBalance = tvFragmentLabel; // Will show account info here
        tvAccountNumber = null; // Not available in current layout
        
        // These would be in additional cards for savings and mortgage
        tvSavingsBalance = null; // Not available in current layout
        tvMonthlyInterest = null; // Not available in current layout
        tvMortgagePayment = null; // Not available in current layout
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
    }

    private void loadUserData() {
        // Load account information
        accountService.getUserAccounts(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateAccountDisplay(accounts));
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {
                // Not used in this context
            }

            @Override
            public void onBalanceSuccess(BigDecimal balance) {
                // Not used in this context
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
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
        
        // Update checking account display
        if (checkingAccount != null && tvAccountBalance != null) {
            String accountInfo = getString(R.string.home_fragment_label) + "\n\n" +
                    "Số dư: " + checkingAccount.getFormattedBalance() + "\n" +
                    "Tài khoản: " + checkingAccount.getMaskedAccountNumber();
            tvAccountBalance.setText(accountInfo);
        }
        
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

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadUserData();
    }
}
