package com.example.final_mobile.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class Account implements Serializable {
    private String id;
    private String userId;
    private String accountNumber;
    private String accountType; // CHECKING, SAVING, MORTGAGE
    private BigDecimal balance;
    private BigDecimal interestRate; // For saving accounts
    private String currency;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;

    // Account Types Constants
    public static final String TYPE_CHECKING = "CHECKING";
    public static final String TYPE_SAVING = "SAVING";
    public static final String TYPE_MORTGAGE = "MORTGAGE";

    // Constructors
    public Account() {
        this.currency = "VND";
        this.balance = BigDecimal.ZERO;
        this.isActive = true;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Account(String userId, String accountType, String accountNumber) {
        this();
        this.userId = userId;
        this.accountType = accountType;
        this.accountNumber = accountNumber;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean isCheckingAccount() {
        return TYPE_CHECKING.equals(accountType);
    }

    public boolean isSavingAccount() {
        return TYPE_SAVING.equals(accountType);
    }

    public boolean isMortgageAccount() {
        return TYPE_MORTGAGE.equals(accountType);
    }

    public String getFormattedBalance() {
        return String.format("%,.0f %s", balance.doubleValue(), currency);
    }

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountType='" + accountType + '\'' +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                '}';
    }
}
