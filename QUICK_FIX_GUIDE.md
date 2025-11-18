# 🚀 HƯỚNG DẪN SỬA LỖI NHANH

## ✅ ĐÃ SỬA CÁC LỖI SAU:

### 1. ✅ Lỗi "Không tìm thấy tài khoản"
- **Nguyên nhân:** Database chưa có accounts cho user
- **Giải pháp:** Đã thêm error handling hiển thị thông báo thân thiện
- **Cần làm:** Chạy seed script để tạo dữ liệu test

### 2. ✅ Lỗi "Phải đăng nhập mỗi lần"
- **Nguyên nhân:** MainActivity không check session đúng cách
- **Giải pháp:** Đã thêm session check trong onCreate() và onResume()
- **Kết quả:** Session được lưu và persist giữa các lần mở app

### 3. ✅ Lỗi Crash khi vào Profile
- **Nguyên nhân:** NullPointerException khi access user data
- **Giải pháp:** Đã thêm null checks và fallback values
- **Kết quả:** Profile không còn crash

---

## 🔧 CÁC FILES ĐÃ SỬA:

1. ✅ **MainActivity.java**
   - Thêm session check trong onCreate()
   - Thêm session check trong onResume()
   - Auto redirect về Login nếu session expired
   - Update last activity time

2. ✅ **ProfileFragment.java**
   - Thêm comprehensive null checks
   - Fallback values cho null data
   - Try-catch để prevent crash
   - User-friendly error messages

3. ✅ **HomeFragment.java**
   - Better error handling cho "No accounts found"
   - Hiển thị thông báo thân thiện
   - Hướng dẫn user liên hệ support

4. ✅ **reset-and-seed.sh** (NEW)
   - Script tự động reset và seed database
   - Kiểm tra MongoDB status
   - Tự động start MongoDB nếu cần

---

## 🚀 CÁCH SỬA (3 BƯỚC ĐƠN GIẢN)

### Bước 1: Reset Database (QUAN TRỌNG!)

```bash
cd "/Users/tsangcuteso1/Documents/GitHub/CKSOA/My project/Final_Mobile/backend"

# Option 1: Dùng script tự động (Khuyến nghị)
./reset-and-seed.sh

# Option 2: Chạy seed thủ công
node seed.js
```

**Kết quả mong đợi:**
```
✅ MongoDB Connected for seeding
🗑️  Cleared existing data
👥 Created users: 3
🏦 Created accounts: 5-6

🎉 Seeding completed successfully!

🔧 Test with these credentials:
📧 Email: customer@example.com
🔑 Password: 123456
```

### Bước 2: Start Backend Server

```bash
# Trong cùng thư mục backend
node server.js
```

Thấy:
```
✅ MongoDB Connected
🚀 Server running on http://0.0.0.0:8000
```

### Bước 3: Build và Run Android App

1. **Clean Project** trong Android Studio:
   - Menu → Build → Clean Project
   - Menu → Build → Rebuild Project

2. **Run App:**
   - Click nút Run (▶️) hoặc Shift+F10
   - Chọn emulator hoặc device

3. **Test Login:**
   - Email: `customer@example.com`
   - Password: `123456`
   - Click "Đăng nhập"

---

## ✅ KIỂM TRA LỖI ĐÃ SỬA

### Test 1: Session Persistence ✅
**Trước:** Phải login mỗi lần mở app
**Sau:** 
1. Login một lần
2. Close app (Back button hoặc Home)
3. Mở lại app
4. ✅ **Vẫn đăng nhập, không cần login lại!**

### Test 2: No Crash in Profile ✅
**Trước:** Crash khi vào Profile
**Sau:**
1. Vào tab Profile (icon cuối cùng)
2. ✅ **Hiển thị thông tin user bình thường**
3. Click vào để xem menu
4. ✅ **Menu hiển thị đầy đủ options**

### Test 3: Handle No Accounts ✅
**Trước:** Lỗi "No accounts found", app không dùng được
**Sau:**
1. Vào Home tab
2. Nếu chưa có accounts:
   - ✅ **Hiển thị thông báo thân thiện**
   - ✅ **Hướng dẫn liên hệ support**
   - ✅ **Không crash, vẫn dùng được các tab khác**

---

## 🎯 TESTING CHECKLIST

Sau khi sửa, hãy test các tình huống sau:

- [ ] **Login thành công** với `customer@example.com` / `123456`
- [ ] **Session persist** - Thoát và mở lại app, vẫn login
- [ ] **Home tab** hiển thị account info hoặc thông báo thân thiện
- [ ] **Transaction tab** không crash (có thể empty nếu chưa có giao dịch)
- [ ] **Utilities tab** hiển thị các service cards
- [ ] **Profile tab** hiển thị user info không crash
- [ ] **Profile menu** mở được và có các options
- [ ] **Logout** hoạt động và quay về login screen
- [ ] **Login lại** sau logout

---

## 🔍 TROUBLESHOOTING

### Vẫn bị lỗi "No accounts found"?

**Kiểm tra:**
```bash
# 1. Vào MongoDB shell
mongosh

# 2. Kiểm tra database
use banking_db
db.users.find().pretty()
db.accounts.find().pretty()
```

**Nếu không có data:**
```bash
# Chạy lại seed
cd backend
node seed.js
```

### Vẫn phải login mỗi lần?

**Kiểm tra trong Logcat:**
```
Filter: SessionManager
```

Tìm dòng:
- ✅ `Login session created for user: customer@example.com`
- ✅ `Session duration: XXX ms`

**Nếu thấy "Session expired":**
- Có thể do thời gian hệ thống sai
- Hoặc SharedPreferences bị xóa
- Thử uninstall và install lại app

### App vẫn crash?

**Xem Logcat để tìm stacktrace:**
```
Filter: AndroidRuntime
```

Copy stacktrace và kiểm tra:
- Dòng nào gây lỗi
- File và line number
- Exception type (NullPointerException, etc.)

---

## 📊 TESTING DATA

Sau khi seed, bạn có các tài khoản test:

### Customer Account 1:
- **Email:** `customer@example.com`
- **Password:** `123456`
- **Accounts:**
  - CHECKING: ~5 triệu VNĐ
  - SAVING: ~25 triệu VNĐ (lãi suất 5.5%/năm)
  - MORTGAGE: ~250 triệu VNĐ (lãi suất 8.5%/năm)

### Customer Account 2:
- **Email:** `user2@example.com`
- **Password:** `123456`
- **Accounts:**
  - CHECKING: ~3 triệu VNĐ
  - SAVING: ~15 triệu VNĐ

### Bank Officer:
- **Email:** `admin@bank.com`
- **Password:** `123456`
- **Role:** BANK_OFFICER (cho các tính năng admin sau này)

---

## 💡 TIP: Kiểm Tra Session Info

Nếu muốn xem thông tin session hiện tại, thêm vào HomeFragment:

```java
// In onViewCreated method
SessionManager sm = SessionManager.getInstance(getContext());
Log.d("SessionInfo", sm.getSessionInfo().toString());
```

Sẽ thấy trong Logcat:
```json
{
  "isLoggedIn": true,
  "userId": "673abc123...",
  "userEmail": "customer@example.com",
  "userName": "Nguyen Van A",
  "isBankOfficer": false,
  "sessionDuration": 123456,
  "hasToken": true
}
```

---

## ✅ KẾT LUẬN

Sau khi sửa:
- ✅ Session được lưu đúng cách
- ✅ Không crash khi vào Profile
- ✅ Xử lý error "No accounts" một cách thân thiện
- ✅ User experience được cải thiện đáng kể

**Tất cả 3 lỗi đã được sửa!** 🎉

---

## 📞 Nếu vẫn gặp vấn đề:

1. Clean và Rebuild project
2. Uninstall app trên device/emulator
3. Chạy lại `node seed.js`
4. Restart backend server
5. Install app lại
6. Test với fresh data

Chúc bạn fix thành công! 🚀





