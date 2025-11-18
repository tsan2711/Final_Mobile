package com.example.final_mobile;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_mobile.models.Transaction;
import com.example.final_mobile.services.SessionManager;
import com.example.final_mobile.services.TransactionService;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryFragment extends Fragment {
    private static final String TAG = "TransactionHistoryFragment";
    private static final String ARG_ACCOUNT_ID = "account_id";

    private RecyclerView rvTransactions;
    private TextView tvNoTransactions;
    private TransactionService transactionService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private TransactionAdapter adapter;
    private List<Transaction> transactions = new ArrayList<>();
    private int currentPage = 1;
    private int limit = 20;
    private boolean isLoading = false;
    private boolean hasMore = true;

    public static TransactionHistoryFragment newInstance(String accountId) {
        TransactionHistoryFragment fragment = new TransactionHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT_ID, accountId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        transactionService = new TransactionService(getContext());
        sessionManager = SessionManager.getInstance(getContext());
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);

        initViews(view);
        setupRecyclerView();
        loadTransactions();
    }

    private void initViews(View view) {
        rvTransactions = view.findViewById(R.id.rv_transactions);
        tvNoTransactions = view.findViewById(R.id.tv_no_transactions);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void loadTransactions() {
        if (isLoading || !hasMore) return;
        
        isLoading = true;
        if (currentPage == 1) {
            progressDialog.setMessage("Đang tải lịch sử giao dịch...");
            progressDialog.show();
        }

        transactionService.getUserTransactions(currentPage, limit, new TransactionService.TransactionCallback() {
            @Override
            public void onSuccess(List<Transaction> transactionList) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        progressDialog.dismiss();
                        
                        if (transactionList.isEmpty()) {
                            hasMore = false;
                        } else {
                            transactions.addAll(transactionList);
                            adapter.notifyDataSetChanged();
                            currentPage++;
                            hasMore = transactionList.size() == limit;
                        }
                        
                        if (transactions.isEmpty()) {
                            tvNoTransactions.setVisibility(View.VISIBLE);
                            rvTransactions.setVisibility(View.GONE);
                        } else {
                            tvNoTransactions.setVisibility(View.GONE);
                            rvTransactions.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onSingleTransactionSuccess(Transaction transaction) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        progressDialog.dismiss();
                        Log.e(TAG, "Error loading transactions: " + error);
                        if (transactions.isEmpty()) {
                            tvNoTransactions.setVisibility(View.VISIBLE);
                            rvTransactions.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onOtpRequired(String message, String transactionId) {}
        });
    }

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
            holder.bind(transactionList.get(position));
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

    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        return String.format(java.util.Locale.getDefault(), "%,.0f VNĐ", amount.doubleValue());
    }
}

