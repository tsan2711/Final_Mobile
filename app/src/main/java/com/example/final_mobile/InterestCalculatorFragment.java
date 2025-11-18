package com.example.final_mobile;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.SessionManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InterestCalculatorFragment extends Fragment {
    private static final String TAG = "InterestCalculatorFragment";

    private Spinner spinnerAccount;
    private TextView tvCurrentBalance;
    private TextView tvInterestRate;
    private TextView tvProjectedBalance;
    private TextView tvTotalInterest;
    private RecyclerView rvProjection;
    private Button btnCalculate;
    private AccountService accountService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private List<Account> savingAccounts = new ArrayList<>();
    private ProjectionAdapter projectionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_interest_calculator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        accountService = new AccountService(getContext());
        sessionManager = SessionManager.getInstance(getContext());
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);

        initViews(view);
        setupRecyclerView();
        loadSavingAccounts();
    }

    private void initViews(View view) {
        spinnerAccount = view.findViewById(R.id.spinner_account);
        tvCurrentBalance = view.findViewById(R.id.tv_current_balance);
        tvInterestRate = view.findViewById(R.id.tv_interest_rate);
        tvProjectedBalance = view.findViewById(R.id.tv_projected_balance);
        tvTotalInterest = view.findViewById(R.id.tv_total_interest);
        rvProjection = view.findViewById(R.id.rv_projection);
        btnCalculate = view.findViewById(R.id.btn_calculate);

        btnCalculate.setOnClickListener(v -> calculateProjection());
    }

    private void setupRecyclerView() {
        projectionAdapter = new ProjectionAdapter(new ArrayList<>());
        rvProjection.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProjection.setAdapter(projectionAdapter);
    }

    private void loadSavingAccounts() {
        progressDialog.setMessage("Đang tải tài khoản tiết kiệm...");
        progressDialog.show();

        accountService.getUserAccounts(new AccountService.AccountCallback() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        savingAccounts.clear();
                        
                        for (Account account : accounts) {
                            if (account.isSavingAccount() && account.isActive()) {
                                savingAccounts.add(account);
                            }
                        }
                        
                        if (savingAccounts.isEmpty()) {
                            Toast.makeText(getContext(), "Bạn chưa có tài khoản tiết kiệm", Toast.LENGTH_SHORT).show();
                        } else {
                            setupAccountSpinner();
                            // Auto-calculate for first account after spinner is set up
                            if (!savingAccounts.isEmpty()) {
                                Account firstAccount = savingAccounts.get(0);
                                updateAccountInfo(firstAccount);
                                // Delay calculation slightly to avoid double call
                                spinnerAccount.post(() -> {
                                    spinnerAccount.setSelection(0, false); // false = don't trigger listener
                                    calculateProjection();
                                });
                            }
                        }
                    });
                }
            }

            @Override
            public void onSingleAccountSuccess(Account account) {}

            @Override
            public void onBalanceSuccess(BigDecimal balance) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void setupAccountSpinner() {
        List<String> accountNumbers = new ArrayList<>();
        for (Account account : savingAccounts) {
            accountNumbers.add(account.getMaskedAccountNumber() + " - " + account.getFormattedBalance());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, accountNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccount.setAdapter(adapter);

        spinnerAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Account selectedAccount = savingAccounts.get(position);
                updateAccountInfo(selectedAccount);
                calculateProjection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateAccountInfo(Account account) {
        tvCurrentBalance.setText(account.getFormattedBalance());
        if (account.getInterestRate() != null) {
            tvInterestRate.setText(String.format(Locale.getDefault(), "%.2f%%/năm", 
                account.getInterestRate().doubleValue()));
        } else {
            tvInterestRate.setText("Chưa có");
        }
    }

    private void calculateProjection() {
        int selectedPosition = spinnerAccount.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= savingAccounts.size()) {
            return;
        }

        Account selectedAccount = savingAccounts.get(selectedPosition);
        if (selectedAccount.getInterestRate() == null || selectedAccount.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            Toast.makeText(getContext(), "Tài khoản này chưa có lãi suất", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Đang tính toán dự kiến lãi suất...");
        progressDialog.show();

        accountService.getInterestProjection(selectedAccount.getId(), 12, 
            new AccountService.InterestProjectionCallback() {
                @Override
                public void onSuccess(AccountService.InterestProjection projection) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            displayProjection(projection);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
    }

    private void displayProjection(AccountService.InterestProjection projection) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
        formatter.setMaximumFractionDigits(0);

        tvProjectedBalance.setText(formatter.format(projection.getProjectedBalance()) + " VND");
        tvTotalInterest.setText(formatter.format(projection.getTotalInterest()) + " VND");

        if (projection.getMonthlyDetails() != null && !projection.getMonthlyDetails().isEmpty()) {
            projectionAdapter.setProjections(projection.getMonthlyDetails());
        } else {
            projectionAdapter.setProjections(new ArrayList<>());
        }
        
        // Show message if balance is zero
        if (projection.getCurrentBalance() != null && 
            projection.getCurrentBalance().compareTo(BigDecimal.ZERO) == 0) {
            Toast.makeText(getContext(), 
                "Số dư hiện tại là 0 VND. Vui lòng nạp tiền vào tài khoản để tính lãi suất.", 
                Toast.LENGTH_LONG).show();
        }
    }

    private class ProjectionAdapter extends RecyclerView.Adapter<ProjectionAdapter.ViewHolder> {
        private List<AccountService.MonthlyProjection> projections;

        public ProjectionAdapter(List<AccountService.MonthlyProjection> projections) {
            this.projections = projections;
        }

        public void setProjections(List<AccountService.MonthlyProjection> projections) {
            this.projections = projections;
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
            AccountService.MonthlyProjection projection = projections.get(position);
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            formatter.setMaximumFractionDigits(0);

            holder.tvTitle.setText("Tháng " + projection.getMonth());
            holder.tvSubtitle.setText(String.format(Locale.getDefault(),
                "Số dư: %s VND | Lãi tháng: %s VND | Tổng lãi: %s VND",
                formatter.format(projection.getBalance()),
                formatter.format(projection.getMonthlyInterest()),
                formatter.format(projection.getCumulativeInterest())));
        }

        @Override
        public int getItemCount() {
            return projections.size();
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

