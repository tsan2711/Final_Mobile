# 🔧 Sửa Lỗi Auto Logout

## 🐛 Vấn Đề Hiện Tại:

Bạn đang gặp 2 vấn đề:
1. **Không bắt buộc đăng nhập** - Session cũ còn lưu
2. **Auto logout khi vào app** - Session bị expired hoặc conflict

## ✅ GIẢI PHÁP NHANH

### Option 1: Xóa Data App (Khuyến nghị - Đơn giản nhất)

**Trên Android Emulator/Device:**

1. Vào **Settings** trên device
2. Chọn **Apps** hoặc **Applications**
3. Tìm app **"Final_Mobile"** (hoặc tên app của bạn)
4. Chọn **Storage**
5. Click **"Clear Data"** hoặc **"Clear Storage"**
6. Confirm

**Hoặc dùng ADB:**
```bash
adb shell pm clear com.example.final_mobile
```

### Option 2: Uninstall và Install Lại

**Trong Android Studio:**
1. Uninstall app từ device/emulator
2. Run lại app (sẽ tự động install)

**Hoặc dùng ADB:**
```bash
# Uninstall
adb uninstall com.example.final_mobile

# Sau đó Run lại trong Android Studio
```

### Option 3: Thêm Nút "Clear Session" Trong App (Development)

Thêm code này vào `LoginActivity.java` để test:

**Thêm vào `initViews()`:**
```java
// FOR DEVELOPMENT ONLY - Remove in production
Button btnClearSession = new Button(this);
btnClearSession.setText("Clear Session (Dev)");
btnClearSession.setOnClickListener(v -> {
    SessionManager.getInstance(this).logoutUser();
    Toast.makeText(this, "Session cleared!", Toast.LENGTH_SHORT).show();
    recreate(); // Restart activity
});
```

---

## 🔍 KIỂM TRA SESSION HIỆN TẠI

### Cách 1: Xem Logcat

Filter: `SessionManager`

Tìm các dòng:
- `Login session created for user: ...`
- `Session expired due to inactivity`
- `Logging out user`

### Cách 2: Thêm Debug Code

**Trong LoginActivity onCreate(), sau line 38:**
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    sessionManager = SessionManager.getInstance(this);
    
    // DEBUG: Check session info
    android.util.Log.d("LoginDebug", "Session info: " + sessionManager.getSessionInfo().toString());
    android.util.Log.d("LoginDebug", "Is logged in: " + sessionManager.isLoggedIn());
    android.util.Log.d("LoginDebug", "Has token: " + (sessionManager.getToken() != null));
    
    if (sessionManager.isLoggedIn()) {
        android.util.Log.d("LoginDebug", "User is logged in, navigating to main");
        navigateToMainActivity();
        return;
    }
    
    // ... rest of code
}
```

Xem Logcat để debug.

---

## 🛠️ SỬA VĨNh VIỄN

Vấn đề có thể do:

### 1. Session Expire Time Quá Ngắt

**File:** `SessionManager.java` (line 78)

Hiện tại: 24 giờ
```java
long maxInactiveTime = 24 * 60 * 60 * 1000; // 24 hours
```

Tăng lên 7 ngày cho development:
```java
long maxInactiveTime = 7 * 24 * 60 * 60 * 1000; // 7 days
```

### 2. Session Bị Clear Khi App Restart

Kiểm tra không có code nào gọi `logoutUser()` ở:
- MainActivity
- Application class
- Splash screen (nếu có)

---

## 📝 TEST FLOW ĐúNG

Sau khi clear data, test theo thứ tự:

### Step 1: Fresh Start
```bash
# Clear app data
adb shell pm clear com.example.final_mobile

# Hoặc uninstall
adb uninstall com.example.final_mobile
```

### Step 2: First Login
1. Run app
2. Thấy màn hình Login
3. Nhập:
   - Email: `customer@example.com`
   - Password: `123456`
4. Click Login
5. ✅ Vào được MainActivity

### Step 3: Test Session Persistence
1. Nhấn Home button (không phải Back)
2. Mở lại app
3. ✅ **Phải vào thẳng MainActivity, KHÔNG thấy Login**

### Step 4: Test After Restart
1. Force close app (swipe away từ recent apps)
2. Mở lại app
3. ✅ **Vẫn phải vào thẳng MainActivity**

### Step 5: Test Logout
1. Vào Profile tab
2. Click để mở menu
3. Chọn "Đăng xuất"
4. ✅ Quay về Login screen
5. Session đã bị xóa

---

## 🔧 NẾU VẪN AUTO LOGOUT

Có thể do MainActivity đang gọi logout. Kiểm tra:

**File:** `MainActivity.java`

Tìm các dòng có `logout` hoặc `clearSession`:
```java
// KHÔNG NÊN CÓ DÒNG NÀY Ở ĐÂU NGOÀI onResume check
sessionManager.logoutUser();  // ❌ XÓA DÒNG NÀY
sessionManager.clearSession(); // ❌ XÓA DÒNG NÀY
```

Chỉ để session check:
```java
@Override
protected void onResume() {
    super.onResume();
    // Chỉ check, KHÔNG logout
    if (!sessionManager.isLoggedIn()) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    } else {
        sessionManager.updateLastActivity(); // Chỉ update time
    }
}
```

---

## 🎯 TÓM TẮT NHANH

**Làm theo thứ tự:**

1. **Clear app data:**
   ```bash
   adb shell pm clear com.example.final_mobile
   ```

2. **Đảm bảo backend đang chạy:**
   ```bash
   cd backend
   node server.js
   ```

3. **Đảm bảo có data trong DB:**
   ```bash
   cd backend
   node seed.js
   ```

4. **Run app fresh:**
   - Uninstall old app
   - Run từ Android Studio
   - Login với `customer@example.com` / `123456`

5. **Test session:**
   - Home button
   - Mở lại app
   - ✅ Không phải login lại

---

## ✅ KẾT QUẢ MONG ĐỢI

Sau khi fix:
- ✅ Lần đầu mở app → Thấy Login screen
- ✅ Login thành công → Vào MainActivity
- ✅ Home button và mở lại → Vào thẳng MainActivity (không login)
- ✅ Force close và mở lại → Vẫn vào thẳng MainActivity
- ✅ Chỉ logout khi user chọn "Đăng xuất" trong Profile

---

**Hãy thử clear app data và test lại nhé!** 🚀




