package com.example.final_mobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.final_mobile.models.Account;
import com.example.final_mobile.services.AdminService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerListFragment extends Fragment {

    private TextView tvFragmentLabel;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private MaterialButton btnCreateCustomer;
    private MaterialButton btnCreateAccount;
    
    private ProgressDialog progressDialog;
    private AdminService adminService;
    
    private List<AdminService.CustomerInfo> customers;
    private ViewGroup customerListContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        adminService = new AdminService(getContext());
        initViews(view);
        setupUI();
        loadCustomers(1, 20);
    }

    private void initViews(View view) {
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        btnCreateCustomer = view.findViewById(R.id.btn_create_customer);
        btnCreateAccount = view.findViewById(R.id.btn_create_account);
        customerListContainer = view.findViewById(R.id.ll_customer_list);
        
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        tvFragmentLabel.setText("Danh sách Khách hàng");
        
        btnSearch.setOnClickListener(v -> performSearch());
        btnCreateCustomer.setOnClickListener(v -> showCreateCustomerDialog());
        btnCreateAccount.setOnClickListener(v -> showCreateAccountDialog());
        
        // Search on Enter key
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            loadCustomers(1, 20);
        } else {
            searchCustomers(query);
        }
    }

    private void loadCustomers(int page, int limit) {
        progressDialog.setMessage("Đang tải danh sách khách hàng...");
        progressDialog.show();

        adminService.getAllCustomers(page, limit, new AdminService.CustomerListCallback() {
            @Override
            public void onSuccess(List<AdminService.CustomerInfo> customersList, int total, int currentPage, int totalPages) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        customers = customersList != null ? customersList : new ArrayList<>();
                        updateCustomerList(customers);
                        if (total > 0) {
                            Toast.makeText(getContext(), "Đã tải " + customers.size() + " khách hàng", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        android.util.Log.e("CustomerListFragment", "Error loading customers: " + error);
                        Toast.makeText(getContext(), "Lỗi tải danh sách: " + error, Toast.LENGTH_LONG).show();
                        customerListContainer.removeAllViews();
                        
                        // Show empty message
                        TextView emptyView = new TextView(getContext());
                        emptyView.setText("Không thể tải danh sách khách hàng.\n" + error);
                        emptyView.setPadding(32, 32, 32, 32);
                        emptyView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                        customerListContainer.addView(emptyView);
                    });
                }
            }
        });
    }

    private void searchCustomers(String query) {
        if (query.length() < 2) {
            Toast.makeText(getContext(), "Vui lòng nhập ít nhất 2 ký tự để tìm kiếm", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Đang tìm kiếm...");
        progressDialog.show();

        adminService.searchCustomers(query, new AdminService.CustomerListCallback() {
            @Override
            public void onSuccess(List<AdminService.CustomerInfo> customersList, int total, int page, int totalPages) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        customers = customersList != null ? customersList : new ArrayList<>();
                        updateCustomerList(customers);
                        if (total > 0) {
                            Toast.makeText(getContext(), "Tìm thấy " + total + " khách hàng", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Không tìm thấy khách hàng nào", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        android.util.Log.e("CustomerListFragment", "Error searching: " + error);
                        Toast.makeText(getContext(), "Lỗi tìm kiếm: " + error, Toast.LENGTH_LONG).show();
                        // Clear list on error
                        customerListContainer.removeAllViews();
                    });
                }
            }
        });
    }

    private void updateCustomerList(List<AdminService.CustomerInfo> customersList) {
        customerListContainer.removeAllViews();

        if (customersList == null || customersList.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("Không có khách hàng nào");
            emptyView.setPadding(32, 32, 32, 32);
            emptyView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            customerListContainer.addView(emptyView);
            return;
        }

        for (AdminService.CustomerInfo customer : customersList) {
            View customerView = createCustomerView(customer);
            customerListContainer.addView(customerView);
        }
    }

    private View createCustomerView(AdminService.CustomerInfo customer) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        MaterialCardView cardView = (MaterialCardView) inflater.inflate(R.layout.item_customer, customerListContainer, false);

        TextView tvName = cardView.findViewById(R.id.tv_customer_name);
        TextView tvEmail = cardView.findViewById(R.id.tv_customer_email);
        TextView tvPhone = cardView.findViewById(R.id.tv_customer_phone);
        TextView tvAccountCount = cardView.findViewById(R.id.tv_account_count);
        MaterialButton btnCreateAccount = cardView.findViewById(R.id.btn_create_account_for_customer);
        
        // Account type buttons
        LinearLayout llAccountButtons = cardView.findViewById(R.id.ll_account_buttons);
        MaterialButton btnChecking = cardView.findViewById(R.id.btn_checking_accounts);
        MaterialButton btnSaving = cardView.findViewById(R.id.btn_saving_accounts);
        MaterialButton btnMortgage = cardView.findViewById(R.id.btn_mortgage_accounts);

        // Set customer info with null checks
        tvName.setText(customer.getFullName() != null && !customer.getFullName().isEmpty() 
            ? customer.getFullName() : "N/A");
        tvEmail.setText(customer.getEmail() != null && !customer.getEmail().isEmpty() 
            ? customer.getEmail() : "N/A");
        tvPhone.setText(customer.getPhone() != null && !customer.getPhone().isEmpty() 
            ? customer.getPhone() : "N/A");
        tvAccountCount.setText(customer.getAccountCount() + " tài khoản");

        btnCreateAccount.setOnClickListener(v -> showCreateAccountDialog(customer));

        // Setup account type buttons
        boolean hasChecking = customer.getCheckingAccounts() != null && !customer.getCheckingAccounts().isEmpty();
        boolean hasSaving = customer.getSavingAccounts() != null && !customer.getSavingAccounts().isEmpty();
        boolean hasMortgage = customer.getMortgageAccounts() != null && !customer.getMortgageAccounts().isEmpty();

        // Debug logging
        android.util.Log.d("CustomerListFragment", "Customer: " + customer.getFullName());
        android.util.Log.d("CustomerListFragment", "  - Checking: " + (customer.getCheckingAccounts() != null ? customer.getCheckingAccounts().size() : "null"));
        android.util.Log.d("CustomerListFragment", "  - Saving: " + (customer.getSavingAccounts() != null ? customer.getSavingAccounts().size() : "null"));
        android.util.Log.d("CustomerListFragment", "  - Mortgage: " + (customer.getMortgageAccounts() != null ? customer.getMortgageAccounts().size() : "null"));

        if (hasChecking || hasSaving || hasMortgage) {
            llAccountButtons.setVisibility(View.VISIBLE);
            android.util.Log.d("CustomerListFragment", "Showing account buttons for: " + customer.getFullName());
            
            // Setup checking button
            if (hasChecking) {
                btnChecking.setVisibility(View.VISIBLE);
                int count = customer.getCheckingAccounts().size();
                btnChecking.setText("CHECKING (" + count + ")");
                btnChecking.setOnClickListener(v -> showAccountListDialog(customer, "CHECKING", customer.getCheckingAccounts()));
            } else {
                btnChecking.setVisibility(View.GONE);
            }
            
            // Setup saving button
            if (hasSaving) {
                btnSaving.setVisibility(View.VISIBLE);
                int count = customer.getSavingAccounts().size();
                btnSaving.setText("SAVING (" + count + ")");
                btnSaving.setOnClickListener(v -> showAccountListDialog(customer, "SAVING", customer.getSavingAccounts()));
            } else {
                btnSaving.setVisibility(View.GONE);
            }
            
            // Setup mortgage button
            if (hasMortgage) {
                btnMortgage.setVisibility(View.VISIBLE);
                int count = customer.getMortgageAccounts().size();
                btnMortgage.setText("MORTGAGE (" + count + ")");
                btnMortgage.setOnClickListener(v -> showAccountListDialog(customer, "MORTGAGE", customer.getMortgageAccounts()));
            } else {
                btnMortgage.setVisibility(View.GONE);
            }
        } else {
            llAccountButtons.setVisibility(View.GONE);
        }

        cardView.setOnClickListener(v -> {
            // Show update customer dialog on click
            showUpdateCustomerDialog(customer);
        });

        return cardView;
    }

    private void showCreateCustomerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_customer, null);

        TextInputEditText etEmail = dialogView.findViewById(R.id.et_email);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_password);
        TextInputEditText etFullName = dialogView.findViewById(R.id.et_full_name);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_phone);
        TextInputEditText etAddress = dialogView.findViewById(R.id.et_address);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (validateCustomerInput(email, password, fullName, phone, true)) {
                dialog.dismiss();
                createCustomer(email, password, fullName, phone, address);
            }
        });

        dialog.show();
    }

    private void showUpdateCustomerDialog(AdminService.CustomerInfo customer) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_customer, null);

        TextInputEditText etEmail = dialogView.findViewById(R.id.et_email);
        TextInputEditText etFullName = dialogView.findViewById(R.id.et_full_name);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_phone);
        TextInputEditText etAddress = dialogView.findViewById(R.id.et_address);
        SwitchMaterial switchIsActive = dialogView.findViewById(R.id.switch_is_active);

        // Pre-fill with existing data
        if (customer != null) {
            etEmail.setText(customer.getEmail() != null ? customer.getEmail() : "");
            etFullName.setText(customer.getFullName() != null ? customer.getFullName() : "");
            etPhone.setText(customer.getPhone() != null ? customer.getPhone() : "");
            // Note: Address might not be in CustomerInfo, so we can't pre-fill it
            switchIsActive.setChecked(true); // Default to active, adjust if needed
        }

        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            boolean isActive = switchIsActive.isChecked();

            if (customer != null && validateCustomerInput(email, null, fullName, phone, false)) {
                dialog.dismiss();
                updateCustomer(customer.getId(), email, fullName, phone, address, isActive);
            }
        });

        dialog.show();
    }

    private boolean validateCustomerInput(String email, String password, String fullName, String phone, boolean isNew) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isNew && (TextUtils.isEmpty(password) || password.length() < 6)) {
            Toast.makeText(getContext(), "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(getContext(), "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            Toast.makeText(getContext(), "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createCustomer(String email, String password, String fullName, String phone, String address) {
        progressDialog.setMessage("Đang tạo khách hàng...");
        progressDialog.show();

        adminService.createCustomer(email, password, fullName, phone, address, new AdminService.AdminCallback() {
            @Override
            public void onSuccess(Object data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Tạo khách hàng thành công!", Toast.LENGTH_LONG).show();
                        loadCustomers(1, 20); // Refresh list
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi tạo khách hàng: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void updateCustomer(String customerId, String email, String fullName, String phone, String address, boolean isActive) {
        progressDialog.setMessage("Đang cập nhật thông tin...");
        progressDialog.show();

        adminService.updateCustomer(customerId, email, fullName, phone, address, isActive, new AdminService.AdminCallback() {
            @Override
            public void onSuccess(Object data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Cập nhật thông tin thành công!", Toast.LENGTH_LONG).show();
                        loadCustomers(1, 20); // Refresh list
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi cập nhật: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void showCreateAccountDialog() {
        // Show dialog to select customer first
        if (customers == null || customers.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng tìm kiếm khách hàng trước", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] customerNames = new String[customers.size()];
        for (int i = 0; i < customers.size(); i++) {
            customerNames[i] = customers.get(i).getFullName() + " (" + customers.get(i).getEmail() + ")";
        }

        new AlertDialog.Builder(getContext())
            .setTitle("Chọn khách hàng")
            .setItems(customerNames, (dialog, which) -> {
                AdminService.CustomerInfo selectedCustomer = customers.get(which);
                showCreateAccountDialog(selectedCustomer);
            })
            .show();
    }

    private void showCreateAccountDialog(AdminService.CustomerInfo customer) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_account, null);

        TextView tvCustomerName = dialogView.findViewById(R.id.tv_customer_name);
        TextInputEditText etAccountType = dialogView.findViewById(R.id.et_account_type);
        TextInputEditText etInitialBalance = dialogView.findViewById(R.id.et_initial_balance);
        TextInputEditText etInterestRate = dialogView.findViewById(R.id.et_interest_rate);

        tvCustomerName.setText(customer.getFullName());

        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String accountType = etAccountType.getText().toString().trim();
            String balanceStr = etInitialBalance.getText().toString().trim();
            String rateStr = etInterestRate.getText().toString().trim();

            if (validateAccountInput(accountType)) {
                dialog.dismiss();
                BigDecimal balance = balanceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(balanceStr);
                BigDecimal rate = rateStr.isEmpty() ? null : new BigDecimal(rateStr);
                createAccount(customer.getId(), accountType, balance, rate);
            }
        });

        dialog.show();
    }

    private boolean validateAccountInput(String accountType) {
        if (TextUtils.isEmpty(accountType)) {
            Toast.makeText(getContext(), "Vui lòng nhập loại tài khoản", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!accountType.equals("CHECKING") && !accountType.equals("SAVING") && !accountType.equals("MORTGAGE")) {
            Toast.makeText(getContext(), "Loại tài khoản không hợp lệ (CHECKING, SAVING, MORTGAGE)", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createAccount(String customerId, String accountType, BigDecimal initialBalance, BigDecimal interestRate) {
        progressDialog.setMessage("Đang tạo tài khoản...");
        progressDialog.show();

        adminService.createCustomerAccount(customerId, accountType, initialBalance, interestRate, 
            new AdminService.AdminCallback() {
                @Override
                public void onSuccess(Object data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Tạo tài khoản thành công!", Toast.LENGTH_LONG).show();
                            loadCustomers(1, 20); // Refresh list
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Lỗi tạo tài khoản: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
    }

    private void showAccountListDialog(AdminService.CustomerInfo customer, String accountType, List<Account> accounts) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_account_list, null);
        
        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_dialog);
        LinearLayout llAccountList = dialogView.findViewById(R.id.ll_account_list);
        TextView tvEmptyAccounts = dialogView.findViewById(R.id.tv_empty_accounts);
        
        // Set title
        String customerName = customer.getFullName() != null ? customer.getFullName() : "Khách hàng";
        tvDialogTitle.setText(customerName + " - " + accountType);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // Display accounts
        if (accounts == null || accounts.isEmpty()) {
            tvEmptyAccounts.setVisibility(View.VISIBLE);
            llAccountList.setVisibility(View.GONE);
        } else {
            tvEmptyAccounts.setVisibility(View.GONE);
            llAccountList.setVisibility(View.VISIBLE);
            llAccountList.removeAllViews();
            
            NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            
            for (Account account : accounts) {
                View accountView = createAccountDetailView(account, currencyFormat);
                llAccountList.addView(accountView);
            }
        }
        
        dialog.show();
    }
    
    private View createAccountDetailView(Account account, NumberFormat currencyFormat) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        MaterialCardView cardView = (MaterialCardView) inflater.inflate(R.layout.item_account_detail, null);
        
        TextView tvAccountNumber = cardView.findViewById(R.id.tv_account_number);
        TextView tvBalance = cardView.findViewById(R.id.tv_account_balance);
        TextView tvInterestRate = cardView.findViewById(R.id.tv_account_interest_rate);
        TextView tvCurrency = cardView.findViewById(R.id.tv_account_currency);
        
        // Set account number
        String accountNumber = account.getAccountNumber() != null && !account.getAccountNumber().isEmpty()
            ? account.getAccountNumber() : "N/A";
        tvAccountNumber.setText("Số tài khoản: " + accountNumber);
        
        // Set balance
        BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        String balanceStr = currencyFormat.format(balance) + " " + (account.getCurrency() != null ? account.getCurrency() : "VND");
        tvBalance.setText(balanceStr);
        
        // Set interest rate
        if (account.getInterestRate() != null) {
            tvInterestRate.setText(account.getInterestRate() + "%");
        } else {
            tvInterestRate.setText("0%");
        }
        
        // Set currency
        tvCurrency.setText(account.getCurrency() != null ? account.getCurrency() : "VND");
        
        // Add click listener to edit account
        cardView.setOnClickListener(v -> {
            // Show update account dialog
            showUpdateAccountDialog(account);
        });
        
        return cardView;
    }

    private void showUpdateAccountDialog(Account account) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_account, null);

        TextInputEditText etAccountNumber = dialogView.findViewById(R.id.et_account_number);
        TextInputEditText etAccountType = dialogView.findViewById(R.id.et_account_type);
        TextInputEditText etInterestRate = dialogView.findViewById(R.id.et_interest_rate);
        TextInputEditText etBalance = dialogView.findViewById(R.id.et_balance);
        SwitchMaterial switchIsActive = dialogView.findViewById(R.id.switch_is_active);

        // Pre-fill with existing data
        if (account != null) {
            etAccountNumber.setText(account.getAccountNumber() != null ? account.getAccountNumber() : "");
            etAccountType.setText(account.getAccountType() != null ? account.getAccountType() : "");
            if (account.getInterestRate() != null) {
                etInterestRate.setText(account.getInterestRate().toString());
            }
            if (account.getBalance() != null) {
                etBalance.setText(account.getBalance().toString());
            }
            switchIsActive.setChecked(account.isActive());
        }

        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String interestRateStr = etInterestRate.getText().toString().trim();
            String balanceStr = etBalance.getText().toString().trim();
            boolean isActive = switchIsActive.isChecked();

            if (account != null) {
                String accountId = account.getId();
                if (accountId == null || accountId.isEmpty()) {
                    // Try to get account ID by account number if id is not available
                    Toast.makeText(getContext(), "Không thể cập nhật: Thiếu ID tài khoản. Vui lòng chọn tài khoản từ danh sách.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    return;
                }
                
                dialog.dismiss();
                BigDecimal interestRate = interestRateStr.isEmpty() ? null : new BigDecimal(interestRateStr);
                BigDecimal balance = balanceStr.isEmpty() ? null : new BigDecimal(balanceStr);
                updateAccount(accountId, interestRate, isActive, balance);
            }
        });

        dialog.show();
    }

    private void updateAccount(String accountId, BigDecimal interestRate, Boolean isActive, BigDecimal balance) {
        progressDialog.setMessage("Đang cập nhật tài khoản...");
        progressDialog.show();

        adminService.updateAccount(accountId, interestRate, isActive, balance, new AdminService.AdminCallback() {
            @Override
            public void onSuccess(Object data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Cập nhật tài khoản thành công!", Toast.LENGTH_LONG).show();
                        loadCustomers(1, 20); // Refresh list
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi cập nhật tài khoản: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}

