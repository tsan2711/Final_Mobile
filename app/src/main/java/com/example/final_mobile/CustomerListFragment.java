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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.final_mobile.services.AdminService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CustomerListFragment extends Fragment {

    private TextView tvFragmentLabel;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
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
        btnCreateAccount = view.findViewById(R.id.btn_create_account);
        customerListContainer = view.findViewById(R.id.ll_customer_list);
        
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        tvFragmentLabel.setText("Danh sách Khách hàng");
        
        btnSearch.setOnClickListener(v -> performSearch());
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

        // Set customer info with null checks
        tvName.setText(customer.getFullName() != null && !customer.getFullName().isEmpty() 
            ? customer.getFullName() : "N/A");
        tvEmail.setText(customer.getEmail() != null && !customer.getEmail().isEmpty() 
            ? customer.getEmail() : "N/A");
        tvPhone.setText(customer.getPhone() != null && !customer.getPhone().isEmpty() 
            ? customer.getPhone() : "N/A");
        tvAccountCount.setText(customer.getAccountCount() + " tài khoản");

        btnCreateAccount.setOnClickListener(v -> showCreateAccountDialog(customer));

        cardView.setOnClickListener(v -> {
            // TODO: Show customer details
            String customerName = customer.getFullName() != null ? customer.getFullName() : "Khách hàng";
            Toast.makeText(getContext(), "Chi tiết khách hàng: " + customerName, Toast.LENGTH_SHORT).show();
        });

        return cardView;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}

