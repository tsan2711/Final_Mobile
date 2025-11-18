package com.example.final_mobile;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.AdminService;
import com.example.final_mobile.services.SessionManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InterestRateManagementFragment extends Fragment {
    private static final String TAG = "InterestRateManagementFragment";

    private RecyclerView rvAccounts;
    private TextView tvNoAccounts;
    private AdminService adminService;
    private AccountService accountService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private AccountAdapter adapter;
    private List<Account> savingsAccounts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_interest_rate_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminService = new AdminService(getContext());
        accountService = new AccountService(getContext());
        sessionManager = SessionManager.getInstance(getContext());
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);

        initViews(view);
        setupRecyclerView();
        loadSavingsAccounts();
    }

    private void initViews(View view) {
        rvAccounts = view.findViewById(R.id.rv_accounts);
        tvNoAccounts = view.findViewById(R.id.tv_no_accounts);
        
        Button btnBulkUpdate = view.findViewById(R.id.btn_bulk_update);
        Button btnViewHistory = view.findViewById(R.id.btn_view_history);
        
        btnBulkUpdate.setOnClickListener(v -> showBulkUpdateDialog());
        btnViewHistory.setOnClickListener(v -> showHistoryDialog());
    }

    private void setupRecyclerView() {
        adapter = new AccountAdapter(savingsAccounts);
        rvAccounts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAccounts.setAdapter(adapter);
    }

    private void loadSavingsAccounts() {
        progressDialog.setMessage("Đang tải danh sách tài khoản tiết kiệm...");
        progressDialog.show();

        // Get all customers and their savings accounts
        adminService.getAllCustomers(1, 100, new AdminService.CustomerListCallback() {
            @Override
            public void onSuccess(List<AdminService.CustomerInfo> customers, int total, int page, int totalPages) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        savingsAccounts.clear();
                        
                        for (AdminService.CustomerInfo customer : customers) {
                            if (customer.getSavingAccounts() != null) {
                                savingsAccounts.addAll(customer.getSavingAccounts());
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        
                        if (savingsAccounts.isEmpty()) {
                            tvNoAccounts.setVisibility(View.VISIBLE);
                            rvAccounts.setVisibility(View.GONE);
                        } else {
                            tvNoAccounts.setVisibility(View.GONE);
                            rvAccounts.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error loading accounts: " + error);
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        tvNoAccounts.setVisibility(View.VISIBLE);
                        rvAccounts.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void showUpdateInterestRateDialog(Account account) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_interest_rate, null);
        
        TextView tvAccountNumber = dialogView.findViewById(R.id.tv_account_number);
        tvAccountNumber.setText(account.getMaskedAccountNumber());
        
        TextView tvCurrentRate = dialogView.findViewById(R.id.tv_current_rate);
        if (account.getInterestRate() != null) {
            tvCurrentRate.setText(String.format("%.2f%%/năm", account.getInterestRate().doubleValue()));
        } else {
            tvCurrentRate.setText("Chưa có");
        }
        
        com.google.android.material.textfield.TextInputEditText etNewRate = dialogView.findViewById(R.id.et_new_rate);
        
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
            String rateStr = etNewRate.getText().toString().trim();
            
            if (TextUtils.isEmpty(rateStr)) {
                Toast.makeText(getContext(), "Vui lòng nhập lãi suất mới", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                BigDecimal newRate = new BigDecimal(rateStr);
                if (newRate.compareTo(BigDecimal.ZERO) < 0 || newRate.compareTo(new BigDecimal("100")) > 0) {
                    Toast.makeText(getContext(), "Lãi suất phải từ 0% đến 100%", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                dialog.dismiss();
                updateInterestRate(account.getId(), newRate);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Lãi suất không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }

    private void updateInterestRate(String accountId, BigDecimal newRate) {
        progressDialog.setMessage("Đang cập nhật lãi suất...");
        progressDialog.show();

        adminService.updateAccount(accountId, newRate, null, null, new AdminService.AdminCallback() {
            @Override
            public void onSuccess(Object data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Cập nhật lãi suất thành công!", Toast.LENGTH_LONG).show();
                        loadSavingsAccounts();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {
        private List<Account> accountList;

        public AccountAdapter(List<Account> accountList) {
            this.accountList = accountList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_interest_rate_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(accountList.get(position));
        }

        @Override
        public int getItemCount() {
            return accountList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvAccountNumber;
            private TextView tvBalance;
            private TextView tvInterestRate;
            private Button btnUpdate;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAccountNumber = itemView.findViewById(R.id.tv_account_number);
                tvBalance = itemView.findViewById(R.id.tv_balance);
                tvInterestRate = itemView.findViewById(R.id.tv_interest_rate);
                btnUpdate = itemView.findViewById(R.id.btn_update);
            }

            public void bind(Account account) {
                tvAccountNumber.setText(account.getMaskedAccountNumber());
                tvBalance.setText(account.getFormattedBalance());
                
                if (account.getInterestRate() != null) {
                    tvInterestRate.setText(String.format("%.2f%%/năm", account.getInterestRate().doubleValue()));
                } else {
                    tvInterestRate.setText("Chưa có");
                }
                
                btnUpdate.setOnClickListener(v -> showUpdateInterestRateDialog(account));
            }
        }
    }

    private void showBulkUpdateDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bulk_update_interest_rate, null);
        
        android.widget.Spinner spinnerAccountType = dialogView.findViewById(R.id.spinner_account_type);
        com.google.android.material.textfield.TextInputEditText etNewRate = dialogView.findViewById(R.id.et_new_rate);
        com.google.android.material.textfield.TextInputEditText etReason = dialogView.findViewById(R.id.et_reason);
        
        String[] accountTypes = {"SAVING", "CHECKING", "MORTGAGE"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, accountTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccountType.setAdapter(adapter);
        
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
            String accountType = (String) spinnerAccountType.getSelectedItem();
            String rateStr = etNewRate.getText().toString().trim();
            String reason = etReason.getText().toString().trim();
            
            if (TextUtils.isEmpty(rateStr)) {
                Toast.makeText(getContext(), "Vui lòng nhập lãi suất mới", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                BigDecimal newRate = new BigDecimal(rateStr);
                if (newRate.compareTo(BigDecimal.ZERO) < 0 || newRate.compareTo(new BigDecimal("100")) > 0) {
                    Toast.makeText(getContext(), "Lãi suất phải từ 0% đến 100%", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                dialog.dismiss();
                updateBulkInterestRate(accountType, newRate, reason);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Lãi suất không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }

    private void updateBulkInterestRate(String accountType, BigDecimal newRate, String reason) {
        progressDialog.setMessage("Đang cập nhật lãi suất cho tất cả tài khoản " + accountType + "...");
        progressDialog.show();

        adminService.updateInterestRate(accountType, newRate.doubleValue(), reason, new AdminService.AdminCallback() {
            @Override
            public void onSuccess(Object data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Cập nhật lãi suất thành công!", Toast.LENGTH_LONG).show();
                        loadSavingsAccounts();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void showHistoryDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_interest_rate_history, null);
        
        android.widget.Spinner spinnerAccountType = dialogView.findViewById(R.id.spinner_account_type);
        androidx.recyclerview.widget.RecyclerView rvHistory = dialogView.findViewById(R.id.rv_history);
        TextView tvNoHistory = dialogView.findViewById(R.id.tv_no_history);
        
        String[] accountTypes = {"Tất cả", "SAVING", "CHECKING", "MORTGAGE"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, accountTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccountType.setAdapter(adapter);
        
        rvHistory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        
        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btn_close);
        com.google.android.material.button.MaterialButton btnRefresh = dialogView.findViewById(R.id.btn_refresh);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        HistoryAdapter historyAdapter = new HistoryAdapter(new ArrayList<>());
        rvHistory.setAdapter(historyAdapter);
        
        spinnerAccountType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedType = position == 0 ? null : accountTypes[position];
                loadHistory(selectedType, historyAdapter, tvNoHistory);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnRefresh.setOnClickListener(v -> {
            String selectedType = spinnerAccountType.getSelectedItemPosition() == 0 ? null : 
                (String) spinnerAccountType.getSelectedItem();
            loadHistory(selectedType, historyAdapter, tvNoHistory);
        });
        
        // Load initial history
        loadHistory(null, historyAdapter, tvNoHistory);
        
        dialog.show();
    }

    private void loadHistory(String accountType, HistoryAdapter adapter, TextView tvNoHistory) {
        adminService.getInterestRateHistory(accountType, null, 1, 50, 
            new AdminService.InterestRateHistoryCallback() {
                @Override
                public void onSuccess(List<AdminService.InterestRateHistoryItem> history, int total, int page, int totalPages) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            adapter.setHistory(history);
                            if (history.isEmpty()) {
                                tvNoHistory.setVisibility(View.VISIBLE);
                            } else {
                                tvNoHistory.setVisibility(View.GONE);
                            }
                        });
                    }
                }

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

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<AdminService.InterestRateHistoryItem> history;

        public HistoryAdapter(List<AdminService.InterestRateHistoryItem> history) {
            this.history = history;
        }

        public void setHistory(List<AdminService.InterestRateHistoryItem> history) {
            this.history = history;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AdminService.InterestRateHistoryItem item = history.get(position);
            holder.tvTitle.setText(item.getAccountNumber() + " - " + item.getAccountType());
            holder.tvSubtitle.setText(String.format(Locale.getDefault(), 
                "%.2f%% → %.2f%% (%s)", 
                item.getOldRate(), 
                item.getNewRate(),
                item.getChangedByName() != null ? item.getChangedByName() : "Unknown"));
        }

        @Override
        public int getItemCount() {
            return history.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvSubtitle;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(android.R.id.text1);
                tvSubtitle = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}

