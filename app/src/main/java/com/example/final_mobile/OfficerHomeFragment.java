package com.example.final_mobile;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.final_mobile.services.AdminService;
import com.example.final_mobile.services.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OfficerHomeFragment extends Fragment {

    private TextView tvFragmentLabel;
    private TextView tvWelcomeMessage;
    private TextView tvTotalCustomers;
    private TextView tvActiveAccounts;
    private TextView tvTotalBalance;
    private TextView tvTodayTransactions;
    private TextView tvNoTransactions;
    
    private MaterialCardView cardCustomers;
    private MaterialCardView cardCreateAccount;
    
    private LinearLayout llRecentTransactions;
    
    private AdminService adminService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_officer_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize services
        adminService = new AdminService(getContext());
        sessionManager = SessionManager.getInstance(getContext());
        
        initViews(view);
        setupUI();
        loadDashboardStats();
    }

    private void initViews(View view) {
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
        tvWelcomeMessage = view.findViewById(R.id.tv_welcome_message);
        tvTotalCustomers = view.findViewById(R.id.tv_total_customers);
        tvActiveAccounts = view.findViewById(R.id.tv_active_accounts);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvTodayTransactions = view.findViewById(R.id.tv_today_transactions);
        tvNoTransactions = view.findViewById(R.id.tv_no_transactions);
        
        cardCustomers = view.findViewById(R.id.card_customers);
        cardCreateAccount = view.findViewById(R.id.card_create_account);
        
        llRecentTransactions = view.findViewById(R.id.ll_recent_transactions);
        
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        // Set welcome message
        String userName = sessionManager.getUserName();
        if (userName != null && !userName.isEmpty()) {
            tvWelcomeMessage.setText("Chào " + getFirstName(userName) + "!");
        }
        
        // Setup click listeners
        cardCustomers.setOnClickListener(v -> navigateToCustomersFragment());
        cardCreateAccount.setOnClickListener(v -> showCreateAccountDialog());
    }

    private void loadDashboardStats() {
        progressDialog.setMessage("Đang tải dữ liệu...");
        progressDialog.show();

        adminService.getDashboardStats(new AdminService.DashboardStatsCallback() {
            @Override
            public void onSuccess(AdminService.DashboardStats stats) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (stats != null) {
                            updateDashboardStats(stats);
                        } else {
                            // Show empty state
                            tvTotalCustomers.setText("0");
                            tvActiveAccounts.setText("0");
                            tvTotalBalance.setText("0 VNĐ");
                            tvTodayTransactions.setText("0");
                            tvNoTransactions.setVisibility(View.VISIBLE);
                            llRecentTransactions.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        android.util.Log.e("OfficerHomeFragment", "Error loading dashboard: " + error);
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + error, Toast.LENGTH_LONG).show();
                        // Show error state
                        tvTotalCustomers.setText("0");
                        tvActiveAccounts.setText("0");
                        tvTotalBalance.setText("0 VNĐ");
                        tvTodayTransactions.setText("0");
                        tvNoTransactions.setVisibility(View.VISIBLE);
                        llRecentTransactions.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void updateDashboardStats(AdminService.DashboardStats stats) {
        // Update stats
        tvTotalCustomers.setText(String.valueOf(stats.getTotalCustomers()));
        tvActiveAccounts.setText(String.valueOf(stats.getActiveAccounts()));
        tvTotalBalance.setText(formatCurrency(stats.getTotalBalance()));
        tvTodayTransactions.setText(String.valueOf(stats.getTodayTransactions()));

        // Update recent transactions
        List<AdminService.RecentTransaction> transactions = stats.getRecentTransactions();
        if (transactions == null || transactions.isEmpty()) {
            tvNoTransactions.setVisibility(View.VISIBLE);
            llRecentTransactions.setVisibility(View.GONE);
        } else {
            tvNoTransactions.setVisibility(View.GONE);
            llRecentTransactions.setVisibility(View.VISIBLE);
            llRecentTransactions.removeAllViews();

            for (AdminService.RecentTransaction transaction : transactions) {
                View transactionView = createTransactionView(transaction);
                llRecentTransactions.addView(transactionView);
            }
        }
    }

    private View createTransactionView(AdminService.RecentTransaction transaction) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.item_recent_transaction, llRecentTransactions, false);

        TextView tvTransactionId = view.findViewById(R.id.tv_transaction_id);
        TextView tvAmount = view.findViewById(R.id.tv_amount);
        TextView tvType = view.findViewById(R.id.tv_type);
        TextView tvStatus = view.findViewById(R.id.tv_status);

        String transactionId = transaction.getTransactionId();
        if (transactionId != null && transactionId.length() > 0) {
            int displayLength = Math.min(8, transactionId.length());
            tvTransactionId.setText("GD: " + transactionId.substring(0, displayLength));
        } else {
            tvTransactionId.setText("GD: N/A");
        }
        
        tvAmount.setText(formatCurrency(transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO));
        tvType.setText(getTransactionTypeName(transaction.getType()));
        
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
        switch (type) {
            case "TRANSFER": return "Chuyển tiền";
            case "DEPOSIT": return "Nạp tiền";
            case "WITHDRAWAL": return "Rút tiền";
            case "UTILITY": return "Tiện ích";
            default: return type;
        }
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
        return formatter.format(amount) + " VNĐ";
    }

    private String getFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "bạn";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private void navigateToCustomersFragment() {
        if (getActivity() == null || !isAdded()) {
            return;
        }
        
        try {
            Fragment fragment = new CustomerListFragment();
            getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khi chuyển trang: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreateAccountDialog() {
        // This will be implemented with CustomerListFragment
        Toast.makeText(getContext(), "Vui lòng chọn khách hàng từ danh sách để tạo tài khoản", Toast.LENGTH_LONG).show();
        navigateToCustomersFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh stats when fragment becomes visible
        loadDashboardStats();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}

