package com.example.final_mobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.final_mobile.models.User;
import com.example.final_mobile.services.AuthService;
import com.example.final_mobile.services.UserService;

public class ProfileFragment extends Fragment {

    private TextView tvFragmentLabel;
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
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        // Setup fragment UI here
        tvFragmentLabel.setText(getString(R.string.profile_fragment_label));
        
        // Make the label clickable to show profile menu
        tvFragmentLabel.setOnClickListener(v -> showProfileMenu());
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
        if (currentUser != null && tvFragmentLabel != null) {
            String profileInfo = getString(R.string.profile_fragment_label) + "\n\n" +
                    "Tên: " + userService.getUserDisplayName() + "\n" +
                    "Email: " + currentUser.getEmail() + "\n" +
                    "Điện thoại: " + userService.formatPhoneNumber(currentUser.getPhone()) + "\n" +
                    "Trạng thái: " + userService.getAccountStatus() + "\n\n" +
                    "Nhấn để xem thêm tùy chọn";
            
            tvFragmentLabel.setText(profileInfo);
        }
    }

    private void showProfileMenu() {
        String[] options = {
            "Cập nhật thông tin cá nhân", 
            "Đổi mật khẩu", 
            "Thông tin tài khoản",
            "Hỗ trợ khách hàng",
            "Đăng xuất"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Tùy chọn profile");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showUpdateProfileDialog();
                    break;
                case 1:
                    showChangePasswordDialog();
                    break;
                case 2:
                    showAccountInfoDialog();
                    break;
                case 3:
                    showSupportDialog();
                    break;
                case 4:
                    showLogoutConfirmation();
                    break;
            }
        });
        builder.show();
    }

    private void showUpdateProfileDialog() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Chưa có thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Cập nhật thông tin");

        // Create layout for form
        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Full name input
        EditText etFullName = new EditText(getContext());
        etFullName.setHint("Họ và tên");
        etFullName.setText(currentUser.getFullName());
        layout.addView(etFullName);

        // Phone input
        EditText etPhone = new EditText(getContext());
        etPhone.setHint("Số điện thoại");
        etPhone.setText(currentUser.getPhone());
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        // Address input
        EditText etAddress = new EditText(getContext());
        etAddress.setHint("Địa chỉ");
        etAddress.setText(currentUser.getAddress());
        layout.addView(etAddress);

        builder.setView(layout);

        builder.setPositiveButton("Cập nhật", (dialog, which) -> {
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (validateProfileInput(fullName, phone)) {
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

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Đổi mật khẩu");

        // Create layout for form
        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Current password input
        EditText etCurrentPassword = new EditText(getContext());
        etCurrentPassword.setHint("Mật khẩu hiện tại");
        etCurrentPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrentPassword);

        // New password input
        EditText etNewPassword = new EditText(getContext());
        etNewPassword.setHint("Mật khẩu mới");
        etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPassword);

        // Confirm password input
        EditText etConfirmPassword = new EditText(getContext());
        etConfirmPassword.setHint("Xác nhận mật khẩu mới");
        etConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmPassword);

        builder.setView(layout);

        builder.setPositiveButton("Đổi mật khẩu", (dialog, which) -> {
            String currentPassword = etCurrentPassword.getText().toString();
            String newPassword = etNewPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (validatePasswordInput(currentPassword, newPassword, confirmPassword)) {
                changePassword(currentPassword, newPassword);
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
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

        String accountInfo = "THÔNG TIN TÀI KHOẢN\n\n" +
                "Email: " + currentUser.getEmail() + "\n" +
                "Họ tên: " + currentUser.getFullName() + "\n" +
                "Điện thoại: " + userService.formatPhoneNumber(currentUser.getPhone()) + "\n" +
                "Địa chỉ: " + (currentUser.getAddress() != null ? currentUser.getAddress() : "Chưa cập nhật") + "\n" +
                "Số tài khoản: " + (currentUser.getAccountNumber() != null ? currentUser.getAccountNumber() : "Chưa có") + "\n" +
                "Loại tài khoản: " + userService.getAccountStatus();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Thông tin tài khoản");
        builder.setMessage(accountInfo);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showSupportDialog() {
        String supportInfo = "HỖ TRỢ KHÁCH HÀNG\n\n" +
                "Hotline: 1900 1234\n" +
                "Email: support@mybank.vn\n" +
                "Website: www.mybank.vn\n\n" +
                "Giờ làm việc:\n" +
                "Thứ 2 - Thứ 6: 8:00 - 17:00\n" +
                "Thứ 7: 8:00 - 12:00\n" +
                "Chủ nhật: Nghỉ";

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Hỗ trợ khách hàng");
        builder.setMessage(supportInfo);
        builder.setPositiveButton("OK", null);
        builder.show();
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
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
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
