package com.example.final_mobile.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_mobile.R;
import com.example.final_mobile.models.Transaction;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_TRANSACTION = 0;
    private static final int TYPE_LOADING = 1;
    
    private List<Transaction> transactions;
    private OnTransactionClickListener listener;
    private boolean isLoading = false;
    // Date format not needed since date view is not in layout
    // private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    
    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }
    
    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }
    
    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }
    
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addTransactions(List<Transaction> newTransactions) {
        if (newTransactions != null && !newTransactions.isEmpty()) {
            int startPosition = transactions.size();
            transactions.addAll(newTransactions);
            notifyItemRangeInserted(startPosition, newTransactions.size());
        }
    }
    
    public void setLoading(boolean loading) {
        if (isLoading != loading) {
            isLoading = loading;
            if (loading) {
                notifyItemInserted(transactions.size());
            } else {
                notifyItemRemoved(transactions.size());
            }
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        if (position == transactions.size() && isLoading) {
            return TYPE_LOADING;
        }
        return TYPE_TRANSACTION;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_recent_transaction, parent, false);
        return new TransactionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TransactionViewHolder) {
            TransactionViewHolder transactionHolder = (TransactionViewHolder) holder;
            Transaction transaction = transactions.get(position);
            transactionHolder.bind(transaction);
        } else if (holder instanceof LoadingViewHolder) {
            // Loading view - no binding needed
        }
    }
    
    @Override
    public int getItemCount() {
        return transactions.size() + (isLoading ? 1 : 0);
    }
    
    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTransactionId;
        private TextView tvAmount;
        private TextView tvType;
        private TextView tvStatus;
        // Date and description views not in current layout
        // private TextView tvDate;
        // private TextView tvDescription;
        
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionId = itemView.findViewById(R.id.tv_transaction_id);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvType = itemView.findViewById(R.id.tv_type);
            tvStatus = itemView.findViewById(R.id.tv_status);
            
            // Date and description views don't exist in current layout
            // tvDate = itemView.findViewById(R.id.tv_date);
            // tvDescription = itemView.findViewById(R.id.tv_description);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTransactionClick(transactions.get(position));
                }
            });
        }
        
        public void bind(Transaction transaction) {
            // Transaction ID
            String transactionId = transaction.getId();
            if (transactionId != null && transactionId.length() > 0) {
                int displayLength = Math.min(8, transactionId.length());
                tvTransactionId.setText("GD: " + transactionId.substring(0, displayLength));
            } else {
                tvTransactionId.setText("GD: N/A");
            }
            
            // Amount
            if (transaction.getAmount() != null) {
                tvAmount.setText(formatCurrency(transaction.getAmount()));
                // Set color based on transaction type
                if (transaction.getToAccountId() != null && 
                    transaction.getFromAccountId() != null &&
                    !transaction.getToAccountId().equals(transaction.getFromAccountId())) {
                    // Incoming transaction - green
                    tvAmount.setTextColor(itemView.getContext().getResources()
                        .getColor(android.R.color.holo_green_dark, null));
                } else {
                    // Outgoing transaction - red/primary
                    tvAmount.setTextColor(itemView.getContext().getResources()
                        .getColor(R.color.primary_color, null));
                }
            } else {
                tvAmount.setText("0 VNĐ");
            }
            
            // Type
            tvType.setText(getTransactionTypeName(transaction.getTransactionType()));
            
            // Status
            String status = transaction.getStatus();
            if (status != null) {
                tvStatus.setText(getStatusName(status));
                setStatusColor(status);
            } else {
                tvStatus.setText("N/A");
            }
            
            // Date (if view exists) - not in current layout, so skip
            // if (tvDate != null && transaction.getCreatedAt() != null) {
            //     tvDate.setText(dateFormat.format(transaction.getCreatedAt()));
            // }
            
            // Description (if view exists) - not in current layout, so skip
            // if (tvDescription != null && transaction.getDescription() != null) {
            //     String desc = transaction.getDescription();
            //     tvDescription.setText(desc.length() > 30 ? desc.substring(0, 30) + "..." : desc);
            // }
        }
        
        private void setStatusColor(String status) {
            int colorRes;
            switch (status) {
                case "COMPLETED":
                    colorRes = android.R.color.holo_green_dark;
                    break;
                case "PENDING":
                case "PROCESSING":
                    colorRes = android.R.color.holo_orange_dark;
                    break;
                case "FAILED":
                    colorRes = android.R.color.holo_red_dark;
                    break;
                case "CANCELLED":
                    colorRes = android.R.color.darker_gray;
                    break;
                default:
                    colorRes = android.R.color.darker_gray;
            }
            tvStatus.setTextColor(itemView.getContext().getResources().getColor(colorRes, null));
        }
        
        private String getTransactionTypeName(String type) {
            if (type == null) return "N/A";
            switch (type) {
                case "TRANSFER": return "Chuyển tiền";
                case "DEPOSIT": return "Nạp tiền";
                case "WITHDRAWAL": return "Rút tiền";
                case "PAYMENT": return "Thanh toán";
                case "TOPUP": return "Nạp tiền điện thoại";
                default: return type;
            }
        }
        
        private String getStatusName(String status) {
            if (status == null) return "N/A";
            switch (status) {
                case "COMPLETED": return "Hoàn thành";
                case "PENDING": return "Chờ xử lý";
                case "PROCESSING": return "Đang xử lý";
                case "FAILED": return "Thất bại";
                case "CANCELLED": return "Đã hủy";
                default: return status;
            }
        }
        
        private String formatCurrency(BigDecimal amount) {
            if (amount == null) return "0 VNĐ";
            return String.format(Locale.getDefault(), "%,.0f VNĐ", amount.doubleValue());
        }
    }
    
    class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}

