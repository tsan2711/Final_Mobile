# 🔧 Hướng Dẫn Sửa Lỗi

## 🐛 Các Lỗi Bạn Đang Gặp:

### 1. ❌ "Không tìm thấy tài khoản"
**Nguyên nhân:** Database chưa có dữ liệu hoặc user chưa có accounts

### 2. ❌ "Phải đăng nhập mỗi lần vào app"
**Nguyên nhân:** Session đang được lưu NHƯNG có thể bị xóa khi restart app, hoặc LoginActivity luôn check session và navigate ngay

### 3. ❌ "Crash khi vào Profile/Settings"
**Nguyên nhân:** ProfileFragment đang truy cập null values từ User object

---

## ✅ GIẢI PHÁP

### Bước 1: Seed Lại Database (Quan Trọng!)

```bash
cd "/Users/tsangcuteso1/Documents/GitHub/CKSOA/My project/Final_Mobile/backend"

# Dừng server nếu đang chạy (Ctrl+C)

# Chạy seed để tạo data
node seed.js
```

**Kết quả mong đợi:**
```
✅ MongoDB Connected for seeding
🗑️  Cleared existing data
👥 Created users: 3
🏦 Created accounts: 5

📊 Sample Data Created:
==================================================

👤 Bank Administrator (BANK_OFFICER)
   📧 Email: admin@bank.com
   📱 Phone: 0987654321
   🔑 Password: 123456

👤 Nguyen Van A (CUSTOMER)
   📧 Email: customer@example.com
   📱 Phone: 0123456789
   🔑 Password: 123456
   💳 CHECKING: 1731673234567890 - 5,234,567 VND
   💳 SAVING: 1731673234567891 - 25,123,456 VND
   💳 MORTGAGE: 1731673234567892 - 250,000,000 VND

👤 Tran Thi B (CUSTOMER)
   📧 Email: user2@example.com
   📱 Phone: 0987123456
   🔑 Password: 123456
   💳 CHECKING: 1731673234567893 - 3,456,789 VND
   💳 SAVING: 1731673234567894 - 15,678,901 VND

🎉 Seeding completed successfully!
```

### Bước 2: Khởi Động Lại Backend

```bash
# Trong cùng thư mục backend
node server.js
```

Thấy:
```
✅ MongoDB Connected
🚀 Server running on http://0.0.0.0:8000
```

### Bước 3: Sửa MainActivity để Kiểm Tra Session

MainActivity cần check session và redirect về Login nếu chưa đăng nhập:

**File cần sửa:** `app/src/main/java/com/example/final_mobile/MainActivity.java`

**Thêm vào đầu class:**
```java
import android.content.Intent;
import com.example.final_mobile.services.SessionManager;
```

**Thêm vào onCreate() TRƯỚC setContentView:**
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // CHECK SESSION FIRST!
    SessionManager sessionManager = SessionManager.getInstance(this);
    if (!sessionManager.isLoggedIn()) {
        // Not logged in, go back to login
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
        return;
    }
    
    setContentView(R.layout.activity_main);
    // ... rest of code
}
```

### Bước 4: Sửa ProfileFragment để Tránh Crash

ProfileFragment đang gặp NullPointerException. Cần thêm null checks:

**File:** `app/src/main/java/com/example/final_mobile/ProfileFragment.java`

Tìm method `updateProfileDisplay()` và sửa:

```java
private void updateProfileDisplay() {
    if (currentUser == null) {
        // Load from session if currentUser is null
        currentUser = SessionManager.getInstance(getContext()).getCurrentUser();
    }
    
    if (currentUser != null && tvFragmentLabel != null) {
        String displayName = currentUser.getFullName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "User";
        }
        
        String email = currentUser.getEmail();
        if (email == null) email = "N/A";
        
        String phone = currentUser.getPhone();
        if (phone == null) phone = "N/A";
        
        String profileInfo = getString(R.string.profile_fragment_label) + "\n\n" +
                "Tên: " + displayName + "\n" +
                "Email: " + email + "\n" +
                "Điện thoại: " + phone + "\n" +
                "Trạng thái: Hoạt động\n\n" +
                "Nhấn để xem thêm tùy chọn";
        
        tvFragmentLabel.setText(profileInfo);
    } else {
        // Fallback
        if (tvFragmentLabel != null) {
            tvFragmentLabel.setText("Không thể tải thông tin người dùng");
        }
    }
}
```

### Bước 5: Sửa HomeFragment để Xử Lý "No Account Found"

**File:** `app/src/main/java/com/example/final_mobile/HomeFragment.java`

Tìm method `loadUserData()` và thêm error handling tốt hơn:

```java
@Override
public void onError(String error) {
    if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
            if (error.contains("No accounts found") || error.contains("Account not found")) {
                // Show friendly message
                Toast.makeText(getContext(), 
                    "Tài khoản chưa được kích hoạt. Vui lòng liên hệ ngân hàng.", 
                    Toast.LENGTH_LONG).show();
                
                // Show default message
                if (tvFragmentLabel != null) {
                    User currentUser = sessionManager.getCurrentUser();
                    String name = currentUser != null ? currentUser.getFullName() : "bạn";
                    tvFragmentLabel.setText("Chào " + name + "!\n\nTài khoản của bạn chưa được kích hoạt.\nVui lòng liên hệ ngân hàng để được hỗ trợ.");
                }
            } else {
                Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```

---

## 🚀 HOẶC SỬA NHANH HƠN

Tôi sẽ tạo các file patch cho bạn. Chạy các lệnh sau:

### File 1: MainActivity Fix




