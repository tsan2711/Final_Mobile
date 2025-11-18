package com.example.final_mobile;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.models.Transaction;
import com.example.final_mobile.services.AccountService;
import com.example.final_mobile.services.SessionManager;
import com.example.final_mobile.services.TransactionService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountDetailFragment extends Fragment {
    private static final String TAG = "AccountDetailFragment";
    private static final String ARG_ACCOUNT_ID = "account_id";
    private static final String ARG_ACCOUNT = "account";

    private Account account;
    private AccountService accountService;
    private TransactionService transactionService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;

    // Views
    private TextView tvAccountNumber;
    private TextView tvAccountType;
    private TextView tvBalance;
    private TextView tvInterestRate;
    private TextView tvMonthlyInterest;
    private TextView tvAnnualEarnings;
    private TextView tvLoanRemaining;
    private TextView tvMonthlyPayment;
    private TextView tvNextPaymentDate;
    private LinearLayout llSavingsInfo;
    private LinearLayout llMortgageInfo;
    private RecyclerView rvTransactionHistory;
    private TextView tvNoTransactions;
    private Button btnViewAllTransactions;

    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactions = new ArrayList<>();

    public static AccountDetailFragment newInstance(Account account) {
        AccountDetailFragment fragment = new AccountDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            account = (Account) getArguments().getSerializable(ARG_ACCOUNT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        accountService = new AccountService(getContext());
        transactionService = new TransactionService(getContext());
        sessionManager = SessionManager.getInstance(getContext());
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);

        initViews(view);
        setupRecyclerView();
        
        if (account != null) {
            loadAccountDetails();
            loadTransactionHistory();
        } else {
            Toast.makeText(getContext(), "Không tìm thấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews(View view) {
        tvAccountNumber = view.findViewById(R.id.tv_account_number);
        tvAccountType = view.findViewById(R.id.tv_account_type);
        tvBalance = view.findViewById(R.id.tv_balance);
        tvInterestRate = view.findViewById(R.id.tv_interest_rate);
        tvMonthlyInterest = view.findViewById(R.id.tv_monthly_interest);
        tvAnnualEarnings = view.findViewById(R.id.tv_annual_earnings);
        tvLoanRemaining = view.findViewById(R.id.tv_loan_remaining);
        tvMonthlyPayment = view.findViewById(R.id.tv_monthly_payment);
        tvNextPaymentDate = view.findViewById(R.id.tv_next_payment_date);
        llSavingsInfo = view.findViewById(R.id.ll_savings_info);
        llMortgageInfo = view.findViewById(R.id.ll_mortgage_info);
        rvTransactionHistory = view.findViewById(R.id.rv_transaction_history);
        tvNoTransactions = view.findViewById(R.id.tv_no_transactions);
        btnViewAllTransactions = view.findViewById(R.id.btn_view_all_transactions);

        if (btnViewAllTransactions != null) {
            btnViewAllTransactions.setOnClickListener(v -> {
                // Navigate to full transaction history
                if (getActivity() != null) {
                    TransactionHistoryFragment fragment = TransactionHistoryFragment.newInstance(account.getId());
                    getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
                }
            });
        }
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(transactions);
        rvTransactionHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactionHistory.setAdapter(transactionAdapter);
    }

    private void loadAccountDetails() {
        if (account == null) return;

        // Display basic account info
        tvAccountNumber.setText(account.getMaskedAccountNumber());
        tvBalance.setText(account.getFormattedBalance());

        String accountTypeName = getAccountTypeName(account.getAccountType());
        tvAccountType.setText(accountTypeName);

        // Show/hide sections based on account type
        if (account.isSavingAccount()) {
            llSavingsInfo.setVisibility(View.VISIBLE);
            llMortgageInfo.setVisibility(View.GONE);
            displaySavingsInfo();
        } else if (account.isMortgageAccount()) {
            llSavingsInfo.setVisibility(View.GONE);
            llMortgageInfo.setVisibility(View.VISIBLE);
            displayMortgageInfo();
        } else {
            llSavingsInfo.setVisibility(View.GONE);
            llMortgageInfo.setVisibility(View.GONE);
        }
    }

    private void displaySavingsInfo() {
        if (account.getInterestRate() != null) {
            BigDecimal interestRate = account.getInterestRate();
            tvInterestRate.setText(String.format(Locale.getDefault(), "%.2f%%/năm", interestRate.doubleValue()));

            // Calculate monthly interest
            BigDecimal monthlyInterest = accountService.calculateMonthlyInterest(account);
            tvMonthlyInterest.setText(formatCurrency(monthlyInterest));

            // Calculate annual earnings
            BigDecimal annualEarnings = account.getBalance()
                .multiply(interestRate)
                .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            tvAnnualEarnings.setText(formatCurrency(annualEarnings));
        } else {
            tvInterestRate.setText("Chưa có");
            tvMonthlyInterest.setText("0 VNĐ");
            tvAnnualEarnings.setText("0 VNĐ");
        }
    }

    private void displayMortgageInfo() {
        // Loan remaining (balance is negative for mortgage)
        BigDecimal loanRemaining = account.getBalance().abs();
        tvLoanRemaining.setText(formatCurrency(loanRemaining));

        // Monthly payment
        BigDecimal monthlyPayment = accountService.calculateMonthlyMortgagePayment(account);
        tvMonthlyPayment.setText(formatCurrency(monthlyPayment));

        // Next payment date (assuming monthly payments on the 1st)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvNextPaymentDate.setText(sdf.format(calendar.getTime()));
    }

    private void loadTransactionHistory() {
        progressDialog.setMessage("Đang tải lịch sử giao dịch...");
        progressDialog.show();

        transactionService.getUserTransactions(1, 10, new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(List<Transaction> transactionList) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        transactions.clear();
                        // Filter transactions for this account
                        for (Transaction t : transactionList) {
                            if (account.getId().equals(t.getFromAccountId()) || 
                                account.getId().equals(t.getToAccountId())) {
                                transactions.add(t);
                            }
                        }
                        // Limit to 10 most recent
                        if (transactions.size() > 10) {
                            transactions = transactions.subList(0, 10);
                        }
                        transactionAdapter.notifyDataSetChanged();
                        
                        if (transactions.isEmpty()) {
                            tvNoTransactions.setVisibility(View.VISIBLE);
                            rvTransactionHistory.setVisibility(View.GONE);
                        } else {
                            tvNoTransactions.setVisibility(View.GONE);
                            rvTransactionHistory.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onSingleTransactionSuccess(Transaction transaction) {
                // Not used
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error loading transactions: " + error);
                        tvNoTransactions.setVisibility(View.VISIBLE);
                        rvTransactionHistory.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onOtpRequired(String message, String transactionId) {
                // Not used
            }
        });
    }

    private String getAccountTypeName(String type) {
        if (type == null) return "Tài khoản";
        switch (type) {
            case Account.TYPE_CHECKING:
                return "Tài khoản thanh toán";
            case Account.TYPE_SAVING:
                return "Tài khoản tiết kiệm";
            case Account.TYPE_MORTGAGE:
                return "Tài khoản vay";
            default:
                return "Tài khoản";
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        return String.format(Locale.getDefault(), "%,.0f VNĐ", amount.doubleValue());
    }

    // Transaction Adapter
    private class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private List<Transaction> transactionList;

        public TransactionAdapter(List<Transaction> transactionList) {
            this.transactionList = transactionList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction transaction = transactionList.get(position);
            holder.bind(transaction);
        }

        @Override
        public int getItemCount() {
            return transactionList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvTransactionId;
            private TextView tvAmount;
            private TextView tvType;
            private TextView tvStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTransactionId = itemView.findViewById(R.id.tv_transaction_id);
                tvAmount = itemView.findViewById(R.id.tv_amount);
                tvType = itemView.findViewById(R.id.tv_type);
                tvStatus = itemView.findViewById(R.id.tv_status);
            }

            public void bind(Transaction transaction) {
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
            }
        }
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
}

