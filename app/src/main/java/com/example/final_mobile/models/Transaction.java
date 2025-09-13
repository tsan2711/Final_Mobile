package com.example.final_mobile.models;

import java.math.BigDecimal;
import java.util.Date;

public class Transaction {
    private String id;
    private String fromAccountId;
    private String toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String transactionType; // TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED
    private String description;
    private String referenceNumber;
    private String otpCode;
    private Date createdAt;
    private Date updatedAt;
    private Date completedAt;

    // Transaction Types
    public static final String TYPE_TRANSFER = "TRANSFER";
    public static final String TYPE_DEPOSIT = "DEPOSIT";
    public static final String TYPE_WITHDRAWAL = "WITHDRAWAL";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_TOPUP = "TOPUP";

    // Transaction Status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Constructors
    public Transaction() {
        this.currency = "VND";
        this.status = STATUS_PENDING;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Transaction(String fromAccountId, String toAccountId, BigDecimal amount, String transactionType) {
        this();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.referenceNumber = generateReferenceNumber();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }

    public String getToAccountId() { return toAccountId; }
    public void setToAccountId(String toAccountId) { this.toAccountId = toAccountId; }

    public String getFromAccountNumber() { return fromAccountNumber; }
    public void setFromAccountNumber(String fromAccountNumber) { this.fromAccountNumber = fromAccountNumber; }

    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    // Helper methods
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    public String getFormattedAmount() {
        return String.format("%,.0f %s", amount.doubleValue(), currency);
    }

    public String getDisplayAmount(String currentAccountId) {
        // If this account is receiving money, show positive
        if (currentAccountId.equals(toAccountId)) {
            return "+" + getFormattedAmount();
        }
        // If this account is sending money, show negative
        else if (currentAccountId.equals(fromAccountId)) {
            return "-" + getFormattedAmount();
        }
        return getFormattedAmount();
    }

    public boolean isIncoming(String currentAccountId) {
        return currentAccountId.equals(toAccountId);
    }

    public boolean isOutgoing(String currentAccountId) {
        return currentAccountId.equals(fromAccountId);
    }

    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                ", transactionType='" + transactionType + '\'' +
                ", status='" + status + '\'' +
                ", referenceNumber='" + referenceNumber + '\'' +
                '}';
    }
}
