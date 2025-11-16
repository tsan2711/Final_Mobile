# 🔧 Sửa Lỗi Auto Logout - HƯỚNG DẪN ĐƠN GIẢN

## ✅ ĐÃ SỬA:

1. **Tăng session time:** 24 giờ → **7 ngày**
2. **Thêm debug logs** để dễ kiểm tra
3. **Fix logic** trong MainActivity

---

## 🚀 CÁCH SỬA (3 BƯỚC)

### Bước 1: Xóa App Data Cũ

**Chọn 1 trong 3 cách:**

#### Cách 1: Dùng ADB (Nhanh nhất)
```bash
adb shell pm clear com.example.final_mobile
```

#### Cách 2: Trên Device/Emulator
1. Settings → Apps
2. Tìm "Final_Mobile"
3. Storage → Clear Data

#### Cách 3: Uninstall & Reinstall
```bash
adb uninstall com.example.final_mobile
# Sau đó Run lại trong Android Studio
```

### Bước 2: Đảm Bảo Backend Chạy

```bash
cd "/Users/tsangcuteso1/Documents/GitHub/CKSOA/My project/Final_Mobile/backend"

# Nếu chưa seed data
node seed.js

# Start server
node server.js
```

Phải thấy:
```
✅ MongoDB Connected
🚀 Server running on http://0.0.0.0:8000
```

### Bước 3: Rebuild & Test App

1. **Clean Project** trong Android Studio:
   - Build → Clean Project
   - Build → Rebuild Project

2. **Run App:**
   - Click Run (▶️)
   - Đợi build xong

3. **Login:**
   - Email: `customer@example.com`
   - Password: `123456`

4. **Test Session:**
   - Login thành công
   - Nhấn Home button (giữ app chạy background)
   - Mở lại app
   - ✅ **KHÔNG phải login lại!**

---

## 🔍 KIỂM TRA LOGS (Quan Trọng!)

Mở **Logcat** trong Android Studio và filter: `SessionManager`

### Khi Login Thành Công:
```
SessionManager: Login session created for user: customer@example.com
SessionManager: Checking login status: true
SessionManager: Last activity: 1731673234567
SessionManager: Time diff: 0 ms (0 minutes)
SessionManager: Session is valid
```

### Khi Mở App Lại (Có Session):
```
SessionManager: Checking login status: true
SessionManager: Last activity: 1731673234567
SessionManager: Time diff: 30000 ms (0 minutes)
SessionManager: Session is valid
```

### Nếu Bị Logout:
```
SessionManager: Checking login status: true
SessionManager: Time diff: 604800000 ms (10080 minutes)
SessionManager: Session expired due to inactivity
SessionManager: Logging out user
```

---

## ❌ NẾU VẪN BỊ AUTO LOGOUT

### Kiểm Tra 1: Session Có Được Lưu Không?

Sau khi login, check Logcat:
```
Filter: "Login session created"
```

Phải thấy dòng:
```
SessionManager: Login session created for user: customer@example.com
```

**Nếu KHÔNG thấy** → Vấn đề ở AuthService, không lưu session

### Kiểm Tra 2: Session Có Bị Xóa Không?

Check Logcat khi mở app:
```
Filter: "Checking login status"
```

Phải thấy:
```
SessionManager: Checking login status: true
SessionManager: Session is valid
```

**Nếu thấy `false`** → Session bị xóa ở đâu đó

### Kiểm Tra 3: MainActivity Có Logout Không?

Xem `MainActivity.java` line 28-35:

**ĐÚNG:**
```java
if (!sessionManager.isLoggedIn()) {
    // Redirect to login
    Intent intent = new Intent(this, LoginActivity.class);
    startActivity(intent);
    finish();
    return;
}
```

**SAI (nếu có dòng này):**
```java
sessionManager.logoutUser(); // ❌ XÓA DÒNG NÀY!
```

---

## 📊 TEST CHECKLIST

- [ ] **Clear app data** (pm clear)
- [ ] **Backend running** (node server.js)
- [ ] **Database có data** (node seed.js)
- [ ] **Rebuild app** (Clean + Rebuild)
- [ ] **Login thành công** với customer@example.com
- [ ] **Home button** → Mở lại → ✅ Không login lại
- [ ] **Force close** → Mở lại → ✅ Vẫn không login lại
- [ ] **Check Logcat** → "Session is valid"

---

## 🎯 KẾT QUẢ MONG ĐỢI

### ✅ ĐÚNG:
1. Lần đầu mở app → Login screen
2. Login thành công → MainActivity
3. Home button + mở lại → MainActivity (không login)
4. Qua 1 ngày → Vẫn không cần login
5. Chỉ logout khi nhấn "Đăng xuất" trong Profile

### ❌ SAI:
1. Mỗi lần mở app → Phải login lại
2. Sau vài phút → Bị logout tự động
3. Background app → Mở lại phải login

---

## 💡 DEBUG TIP

Thêm code này vào `LoginActivity.onCreate()` để debug:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    sessionManager = SessionManager.getInstance(this);
    
    // DEBUG: Check current session
    android.util.Log.e("LOGIN_DEBUG", "=== SESSION INFO ===");
    android.util.Log.e("LOGIN_DEBUG", "Is logged in: " + sessionManager.isLoggedIn());
    android.util.Log.e("LOGIN_DEBUG", "Has token: " + (sessionManager.getToken() != null));
    android.util.Log.e("LOGIN_DEBUG", "Token: " + sessionManager.getToken());
    android.util.Log.e("LOGIN_DEBUG", "User ID: " + sessionManager.getUserId());
    android.util.Log.e("LOGIN_DEBUG", "==================");
    
    if (sessionManager.isLoggedIn()) {
        navigateToMainActivity();
        return;
    }
    
    setContentView(R.layout.activity_login);
    // ... rest of code
}
```

Xem Logcat filter: `LOGIN_DEBUG`

---

## 🆘 NẾU VẪN KHÔNG ĐƯỢC

Chụp screenshot hoặc copy logs của:

1. **Logcat filter: SessionManager** - Khi login
2. **Logcat filter: SessionManager** - Khi mở lại app
3. **Logcat filter: LOGIN_DEBUG** - Nếu thêm debug code

Và cho tôi xem để debug tiếp!

---

**Session bây giờ tồn tại 7 ngày, đủ để test!** 🚀



