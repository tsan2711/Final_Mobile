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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.models.User;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.SessionManager;
import com.google.android.material.card.MaterialCardView;

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
    
    private MaterialCardView cardTransfer;
    private MaterialCardView cardPayment;
    private MaterialCardView cardAccount;
    
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
        
        // Bind views from layout
        tvAccountBalance = view.findViewById(R.id.tv_account_balance);
        tvAccountNumber = view.findViewById(R.id.tv_account_number);
        
        // Quick action cards
        cardTransfer = view.findViewById(R.id.card_transfer);
        cardPayment = view.findViewById(R.id.card_payment);
        cardAccount = view.findViewById(R.id.card_account);
        
        // Try to find views for dynamic data (these may not exist in current layout)
        tvWelcomeMessage = tvFragmentLabel; // Reuse existing view for welcome message
        
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

    private void loadUserData() {
        // Load primary account information to ensure balance and account number are synchronized
        accountService.getPrimaryAccount(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (getActivity() != null && !accounts.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        // Get primary account from list (should be only one)
                        Account primaryAccount = accounts.get(0);
                        updatePrimaryAccountDisplay(primaryAccount);
                    });
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
            // Update balance and account number from the same account object
            // This ensures they are always synchronized
            if (tvAccountBalance != null) {
                tvAccountBalance.setText(primaryAccount.getFormattedBalance());
            }
            
            if (tvAccountNumber != null) {
                tvAccountNumber.setText(primaryAccount.getMaskedAccountNumber());
            }
        } else {
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

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadUserData();
    }
}
