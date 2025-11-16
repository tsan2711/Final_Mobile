package com.example.final_mobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.final_mobile.models.User;
import com.example.final_mobile.services.AuthService;
import com.example.final_mobile.services.UserService;

public class ProfileFragment extends Fragment {

    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhone;
    private LinearLayout btnPersonalInfo;
    private LinearLayout btnSecurity;
    private LinearLayout btnSupport;
    private LinearLayout btnAbout;
    private Button btnLogout;
    
    private UserService userService;
    private AuthService authService;
    private ProgressDialog progressDialog;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize services
        userService = new UserService(getContext());
        authService = new AuthService(getContext());
        
        initViews(view);
        setupUI();
        loadUserProfile();
    }

    private void initViews(View view) {
        // Find views from the layout
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvUserPhone = view.findViewById(R.id.tv_user_phone);
        btnPersonalInfo = view.findViewById(R.id.btn_personal_info);
        btnSecurity = view.findViewById(R.id.btn_security);
        btnSupport = view.findViewById(R.id.btn_support);
        btnAbout = view.findViewById(R.id.btn_about);
        btnLogout = view.findViewById(R.id.btn_logout);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        // Setup click listeners
        if (btnPersonalInfo != null) {
            btnPersonalInfo.setOnClickListener(v -> showUpdateProfileDialog());
        }
        
        if (btnSecurity != null) {
            btnSecurity.setOnClickListener(v -> showChangePasswordDialog());
        }
        
        if (btnSupport != null) {
            btnSupport.setOnClickListener(v -> showSupportDialog());
        }
        
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> showAccountInfoDialog());
        }
        
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void loadUserProfile() {
        userService.getUserProfile(new UserService.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentUser = user;
                        updateProfileDisplay();
                    });
                }
            }

            @Override
            public void onUpdateSuccess(String message) {
                // Not used in this context
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentUser = userService.getCurrentUser(); // Get cached user
                        updateProfileDisplay();
                        Toast.makeText(getContext(), "Lỗi tải profile: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateProfileDisplay() {
        // Ensure we have user data
        if (currentUser == null && userService != null) {
            currentUser = userService.getCurrentUser();
        }
        
        if (currentUser != null) {
            try {
                String displayName = currentUser.getFullName();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = "User";
                }
                
                String email = currentUser.getEmail();
                if (email == null || email.isEmpty()) {
                    email = "N/A";
                }
                
                String phone = currentUser.getPhone();
                if (phone == null || phone.isEmpty()) {
                    phone = "N/A";
                }
                
                // Update TextViews with user data
                if (tvUserName != null) {
                    tvUserName.setText(displayName);
                }
                
                if (tvUserEmail != null) {
                    tvUserEmail.setText(email);
                }
                
                if (tvUserPhone != null) {
                    tvUserPhone.setText(phone);
                }
            } catch (Exception e) {
                // Fallback in case of any error
                Toast.makeText(getContext(), "Không thể tải thông tin", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Show default values when loading
            if (tvUserName != null) {
                tvUserName.setText("Đang tải...");
            }
            if (tvUserEmail != null) {
                tvUserEmail.setText("Đang tải...");
            }
            if (tvUserPhone != null) {
                tvUserPhone.setText("Đang tải...");
            }
        }
    }


    private void showUpdateProfileDialog() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Chưa có thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_profile, null);
        
        TextInputEditText etFullName = dialogView.findViewById(R.id.et_full_name);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_phone);
        TextInputEditText etAddress = dialogView.findViewById(R.id.et_address);
        
        // Set current values
        etFullName.setText(currentUser.getFullName());
        etPhone.setText(currentUser.getPhone());
        etAddress.setText(currentUser.getAddress());
        
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
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (validateProfileInput(fullName, phone)) {
                dialog.dismiss();
                User updatedUser = new User();
                updatedUser.setId(currentUser.getId());
                updatedUser.setEmail(currentUser.getEmail());
                updatedUser.setFullName(fullName);
                updatedUser.setPhone(phone);
                updatedUser.setAddress(address);
                updatedUser.setCustomerType(currentUser.getCustomerType());

                updateProfile(updatedUser);
            }
        });
        
        dialog.show();
    }

    private boolean validateProfileInput(String fullName, String phone) {
        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(getContext(), "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(getContext(), "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Create temp user for validation
        User tempUser = new User();
        tempUser.setFullName(fullName);
        tempUser.setPhone(phone);

        if (!userService.validateProfile(tempUser)) {
            Toast.makeText(getContext(), "Thông tin không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateProfile(User user) {
        progressDialog.setMessage("Đang cập nhật thông tin...");
        progressDialog.show();

        userService.updateUserProfile(user, new UserService.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // Not used here
            }

            @Override
            public void onUpdateSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        currentUser = user;
                        updateProfileDisplay();
                        Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showErrorDialog("Lỗi cập nhật", error);
                    });
                }
            }
        });
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.et_current_password);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);
        
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
            String currentPassword = etCurrentPassword.getText().toString();
            String newPassword = etNewPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (validatePasswordInput(currentPassword, newPassword, confirmPassword)) {
                dialog.dismiss();
                changePassword(currentPassword, newPassword);
            }
        });
        
        dialog.show();
    }

    private boolean validatePasswordInput(String currentPassword, String newPassword, String confirmPassword) {
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(getContext(), "Vui lòng nhập mật khẩu hiện tại", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(getContext(), "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!userService.validatePassword(newPassword)) {
            Toast.makeText(getContext(), "Mật khẩu mới phải có ít nhất 6 ký tự, bao gồm chữ và số", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void changePassword(String currentPassword, String newPassword) {
        progressDialog.setMessage("Đang đổi mật khẩu...");
        progressDialog.show();

        userService.changePassword(currentPassword, newPassword, new UserService.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // Not used here
            }

            @Override
            public void onUpdateSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showErrorDialog("Lỗi đổi mật khẩu", error);
                    });
                }
            }
        });
    }

    private void showAccountInfoDialog() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Chưa có thông tin tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_account_info, null);
        
        TextView tvEmail = dialogView.findViewById(R.id.tv_email);
        TextView tvFullName = dialogView.findViewById(R.id.tv_full_name);
        TextView tvPhone = dialogView.findViewById(R.id.tv_phone);
        TextView tvAddress = dialogView.findViewById(R.id.tv_address);
        TextView tvAccountNumber = dialogView.findViewById(R.id.tv_account_number);
        TextView tvAccountType = dialogView.findViewById(R.id.tv_account_type);
        
        // Set values
        tvEmail.setText(currentUser.getEmail());
        tvFullName.setText(currentUser.getFullName());
        tvPhone.setText(userService.formatPhoneNumber(currentUser.getPhone()));
        tvAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "Chưa cập nhật");
        tvAccountNumber.setText(currentUser.getAccountNumber() != null ? currentUser.getAccountNumber() : "Chưa có");
        tvAccountType.setText(userService.getAccountStatus());
        
        MaterialButton btnOk = dialogView.findViewById(R.id.btn_ok);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showSupportDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_support, null);
        
        TextView tvHotline = dialogView.findViewById(R.id.tv_hotline);
        TextView tvEmail = dialogView.findViewById(R.id.tv_email);
        TextView tvWebsite = dialogView.findViewById(R.id.tv_website);
        TextView tvWorkingHours = dialogView.findViewById(R.id.tv_working_hours);
        
        // Set values
        tvHotline.setText("1900 1234");
        tvEmail.setText("support@mybank.vn");
        tvWebsite.setText("www.mybank.vn");
        tvWorkingHours.setText("Thứ 2 - Thứ 6: 8:00 - 17:00\nThứ 7: 8:00 - 12:00\nChủ nhật: Nghỉ");
        
        MaterialButton btnOk = dialogView.findViewById(R.id.btn_ok);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Đăng xuất");
        builder.setMessage("Bạn có chắc chắn muốn đăng xuất?");
        
        builder.setPositiveButton("Đăng xuất", (dialog, which) -> {
            performLogout();
        });
        
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void performLogout() {
        progressDialog.setMessage("Đang đăng xuất...");
        progressDialog.show();

        authService.logout(new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
                        
                        // Navigate back to login
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        // Even if logout API fails, clear session and go to login
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onOtpRequired(String message) {
                // Not used in logout
            }
        });
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
    public void onResume() {
        super.onResume();
        // Refresh profile when fragment becomes visible
        loadUserProfile();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
