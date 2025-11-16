package com.example.final_mobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.final_mobile.services.UtilityService;

import java.math.BigDecimal;

public class UtilitiesFragment extends Fragment {

    private TextView tvFragmentLabel;
    private UtilityService utilityService;
    private ProgressDialog progressDialog;
    
    // For OTP verification
    private String currentTransactionId;
    private String currentOtp;
    private UtilityService.UtilityPayment currentPayment;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_utilities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        utilityService = new UtilityService(getContext());
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
        
        initViews(view);
        setupUI(view);
    }

    private void initViews(View view) {
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
    }

    private void setupUI(View view) {
        tvFragmentLabel.setText(getString(R.string.utilities_fragment_label));
        
        // Find all utility cards and set click listeners
        setupUtilityCards(view);
    }
    
    private void setupUtilityCards(View view) {
        // Bill payment cards - use findViewById for reliable access
        View electricityCard = view.findViewById(R.id.card_electricity);
        View waterCard = view.findViewById(R.id.card_water);
        View internetCard = view.findViewById(R.id.card_internet);
        
        // Mobile service cards
        View topupCard = view.findViewById(R.id.card_topup);
        View dataPackageCard = view.findViewById(R.id.card_data_package);
        View scratchCardCard = view.findViewById(R.id.card_scratch_card);
        
        if (electricityCard != null) {
            electricityCard.setOnClickListener(v -> showElectricityBillDialog());
        }
        
        if (waterCard != null) {
            waterCard.setOnClickListener(v -> showWaterBillDialog());
        }
        
        if (internetCard != null) {
            internetCard.setOnClickListener(v -> showInternetBillDialog());
        }
        
        if (topupCard != null) {
            topupCard.setOnClickListener(v -> showMobileTopupDialog());
        }
        
        if (dataPackageCard != null) {
            dataPackageCard.setOnClickListener(v -> showDataPackageDialog());
        }
        
        if (scratchCardCard != null) {
            scratchCardCard.setOnClickListener(v -> showScratchCardDialog());
        }
    }

    private void showElectricityBillDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bill_payment, null);
        
        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_icon);
        ivIcon.setImageResource(R.drawable.ic_electricity);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        tvTitle.setText("Thanh toán tiền điện");
        
        TextInputEditText etCustomerNumber = dialogView.findViewById(R.id.et_customer_number);
        TextInputEditText etCustomerName = dialogView.findViewById(R.id.et_customer_name);
        TextInputEditText etPeriod = dialogView.findViewById(R.id.et_period);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        
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
            String customerNumber = etCustomerNumber.getText().toString().trim();
            String customerName = etCustomerName.getText().toString().trim();
            String period = etPeriod.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            if (validateBillPayment(customerNumber, amountStr)) {
                dialog.dismiss();
                BigDecimal amount = new BigDecimal(amountStr);
                processElectricityBill(customerNumber, amount, customerName, period);
            }
        });
        
        dialog.show();
    }

    private void showWaterBillDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bill_payment, null);
        
        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_icon);
        ivIcon.setImageResource(R.drawable.ic_water);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        tvTitle.setText("Thanh toán tiền nước");
        
        TextInputEditText etCustomerNumber = dialogView.findViewById(R.id.et_customer_number);
        TextInputEditText etCustomerName = dialogView.findViewById(R.id.et_customer_name);
        TextInputEditText etPeriod = dialogView.findViewById(R.id.et_period);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        
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
            String customerNumber = etCustomerNumber.getText().toString().trim();
            String customerName = etCustomerName.getText().toString().trim();
            String period = etPeriod.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            if (validateBillPayment(customerNumber, amountStr)) {
                dialog.dismiss();
                BigDecimal amount = new BigDecimal(amountStr);
                processWaterBill(customerNumber, amount, customerName, period);
            }
        });
        
        dialog.show();
    }

    private void showInternetBillDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bill_payment, null);
        
        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_icon);
        ivIcon.setImageResource(R.drawable.ic_electricity); // Use electricity icon as placeholder
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        tvTitle.setText("Thanh toán cước internet");
        
        // Hide period field for internet
        com.google.android.material.textfield.TextInputLayout tilPeriod = dialogView.findViewById(R.id.til_period);
        tilPeriod.setVisibility(View.GONE);
        
        // Reuse customer name field as provider field
        com.google.android.material.textfield.TextInputLayout tilCustomerName = dialogView.findViewById(R.id.til_customer_name);
        tilCustomerName.setHint("Nhà cung cấp (VNPT, FPT, Viettel)");
        
        TextInputEditText etCustomerNumber = dialogView.findViewById(R.id.et_customer_number);
        TextInputEditText etProvider = dialogView.findViewById(R.id.et_customer_name); // Reuse customer name field for provider
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        
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
            String customerNumber = etCustomerNumber.getText().toString().trim();
            String provider = etProvider.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            if (validateBillPayment(customerNumber, amountStr)) {
                dialog.dismiss();
                BigDecimal amount = new BigDecimal(amountStr);
                processInternetBill(customerNumber, amount, provider);
            }
        });
        
        dialog.show();
    }

    private void showMobileTopupDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_mobile_topup, null);
        
        TextInputEditText etPhoneNumber = dialogView.findViewById(R.id.et_phone_number);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etProvider = dialogView.findViewById(R.id.et_provider);
        
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
            String phoneNumber = etPhoneNumber.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String provider = etProvider.getText().toString().trim();

            if (validatePhoneTopup(phoneNumber, amountStr)) {
                dialog.dismiss();
                BigDecimal amount = new BigDecimal(amountStr);
                processMobileTopup(phoneNumber, amount, provider);
            }
        });
        
        dialog.show();
    }

    private void showDataPackageDialog() {
        Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void showScratchCardDialog() {
        Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private boolean validateBillPayment(String customerNumber, String amount) {
        if (TextUtils.isEmpty(customerNumber)) {
            Toast.makeText(getContext(), "Vui lòng nhập mã khách hàng", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(amount)) {
            Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            BigDecimal amountValue = new BigDecimal(amount);
            if (amountValue.compareTo(BigDecimal.ZERO) <= 0) {
                Toast.makeText(getContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validatePhoneTopup(String phoneNumber, String amount) {
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(getContext(), "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!phoneNumber.matches("^(0[3|5|7|8|9])+([0-9]{8})$")) {
            Toast.makeText(getContext(), "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        return validateBillPayment(phoneNumber, amount);
    }

    private void processElectricityBill(String customerNumber, BigDecimal amount, String customerName, String period) {
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.show();

        utilityService.payElectricityBill(null, customerNumber, amount, customerName, period, 
            new UtilityService.UtilityCallback() {
                @Override
                public void onInitiateSuccess(String transactionId, String otp, UtilityService.UtilityPayment payment) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            currentTransactionId = transactionId;
                            currentOtp = otp;
                            currentPayment = payment;
                            showOTPDialog(payment);
                        });
                    }
                }

                @Override
                public void onVerifySuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showSuccessDialog(message);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showErrorDialog("Lỗi", error);
                        });
                    }
                }
            });
    }

    private void processWaterBill(String customerNumber, BigDecimal amount, String customerName, String period) {
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.show();

        utilityService.payWaterBill(null, customerNumber, amount, customerName, period,
            new UtilityService.UtilityCallback() {
                @Override
                public void onInitiateSuccess(String transactionId, String otp, UtilityService.UtilityPayment payment) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            currentTransactionId = transactionId;
                            currentOtp = otp;
                            currentPayment = payment;
                            showOTPDialog(payment);
                        });
                    }
                }

                @Override
                public void onVerifySuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showSuccessDialog(message);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showErrorDialog("Lỗi", error);
                        });
                    }
                }
            });
    }

    private void processInternetBill(String customerNumber, BigDecimal amount, String provider) {
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.show();

        utilityService.payInternetBill(null, customerNumber, amount, provider, null,
            new UtilityService.UtilityCallback() {
                @Override
                public void onInitiateSuccess(String transactionId, String otp, UtilityService.UtilityPayment payment) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            currentTransactionId = transactionId;
                            currentOtp = otp;
                            currentPayment = payment;
                            showOTPDialog(payment);
                        });
                    }
                }

                @Override
                public void onVerifySuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showSuccessDialog(message);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showErrorDialog("Lỗi", error);
                        });
                    }
                }
            });
    }

    private void processMobileTopup(String phoneNumber, BigDecimal amount, String provider) {
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.show();

        utilityService.mobileTopup(null, phoneNumber, amount, provider,
            new UtilityService.UtilityCallback() {
                @Override
                public void onInitiateSuccess(String transactionId, String otp, UtilityService.UtilityPayment payment) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            currentTransactionId = transactionId;
                            currentOtp = otp;
                            currentPayment = payment;
                            showOTPDialog(payment);
                        });
                    }
                }

                @Override
                public void onVerifySuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showSuccessDialog(message);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showErrorDialog("Lỗi", error);
                        });
                    }
                }
            });
    }

    private void showOTPDialog(UtilityService.UtilityPayment payment) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_otp_verification, null);
        
        TextView tvTransactionInfo = dialogView.findViewById(R.id.tv_transaction_info);
        String info = "Giao dịch: " + payment.getDescription() + "\n" +
                     "Số tiền: " + payment.getFormattedAmount() + "\n" +
                     "Phí: " + String.format("%,.0f %s", payment.getFee().doubleValue(), payment.getCurrency()) + "\n" +
                     "Tổng cộng: " + payment.getFormattedTotalAmount() + "\n\n" +
                     "OTP (Dev): " + currentOtp;
        tvTransactionInfo.setText(info);
        
        TextInputEditText etOtp = dialogView.findViewById(R.id.et_otp);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(false)
            .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (!TextUtils.isEmpty(otp)) {
                dialog.dismiss();
                verifyOTP(otp);
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập OTP", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }

    private void verifyOTP(String otp) {
        progressDialog.setMessage("Đang xác thực OTP...");
        progressDialog.show();

        utilityService.verifyUtilityOTP(currentTransactionId, otp,
            new UtilityService.UtilityCallback() {
                @Override
                public void onInitiateSuccess(String transactionId, String otp, UtilityService.UtilityPayment payment) {
                    // Not used
                }

                @Override
                public void onVerifySuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showSuccessDialog(message);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showErrorDialog("Lỗi xác thực", error);
                        });
                    }
                }
            });
    }

    private void showSuccessDialog(String message) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_success, null);
        
        TextView tvMessage = dialogView.findViewById(R.id.tv_message);
        tvMessage.setText(message);
        
        MaterialButton btnOk = dialogView.findViewById(R.id.btn_ok);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            // Reset current transaction data
            currentTransactionId = null;
            currentOtp = null;
            currentPayment = null;
        });
        
        dialog.show();
        
        // Reset current transaction data
        currentTransactionId = null;
        currentOtp = null;
        currentPayment = null;
    }

    private void showErrorDialog(String title, String message) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_error, null);
        
        TextView tvTitle = dialogView.findViewById(R.id.tv_title);
        tvTitle.setText(title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_message);
        tvMessage.setText(message);
        
        MaterialButton btnOk = dialogView.findViewById(R.id.btn_ok);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnOk.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
