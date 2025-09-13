package com.example.final_mobile.models;

import java.util.Date;

public class User {
    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String accountNumber;
    private String customerType; // CUSTOMER or BANK_OFFICER
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;

    // Constructors
    public User() {}

    public User(String id, String email, String fullName, String phone) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.isActive = true;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean isBankOfficer() {
        return "BANK_OFFICER".equals(customerType);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equals(customerType);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", customerType='" + customerType + '\'' +
                '}';
    }
}
