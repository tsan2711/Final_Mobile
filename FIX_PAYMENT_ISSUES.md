# 🔧 Sửa Lỗi Thanh Toán Tiền Điện Nước

## ✅ ĐÃ SỬA:

### 1. **Sửa UtilitiesFragment.java**
- ✅ Thêm ID cho tất cả các card trong `fragment_utilities.xml`
- ✅ Sửa `setupUtilityCards()` để dùng `findViewById()` thay vì `findCardByText()`
- ✅ Đảm bảo các nút thanh toán có thể click được

**IDs đã thêm:**
- `card_electricity` - Tiền điện
- `card_water` - Tiền nước
- `card_internet` - Internet
- `card_topup` - Nạp tiền
- `card_data_package` - Gói cước
- `card_scratch_card` - Thẻ cào

### 2. **Sửa UtilityController.js (Backend)**
- ✅ Xử lý an toàn cho `req.user.email` với optional chaining
- ✅ Thêm validation cho `userId` và `req.user` trước khi xử lý
- ✅ Thêm error logging chi tiết để debug dễ hơn
- ✅ Trả về error messages rõ ràng hơn

### 3. **Database đã được seed**
- ✅ Tạo lại users và accounts
- ✅ User `customer@example.com` có accounts để test

---

## 🚀 TEST LẠI:

### Bước 1: Đảm bảo Backend đang chạy
```bash
cd "/Users/tsangcuteso1/Documents/GitHub/CKSOA/My project/Final_Mobile/backend"
node server.js
```

Phải thấy:
```
✅ MongoDB Connected
🚀 Server running on http://0.0.0.0:8000
```

### Bước 2: Rebuild App
1. Trong Android Studio:
   - Build → Clean Project
   - Build → Rebuild Project
2. Run app trên emulator/device

### Bước 3: Login và Test
1. **Login:**
   - Email: `customer@example.com`
   - Password: `123456`

2. **Test thanh toán:**
   - Vào tab **Tiện ích** (Utilities)
   - Nhấn card **Tiền điện**
   - Nhập thông tin:
     - Mã khách hàng: `1234567890` (bất kỳ)
     - Số tiền: `100000` (100k VND)
     - Tên khách hàng: `Nguyen Van A` (tùy chọn)
     - Kỳ: `2024/11` (tùy chọn)
   - Nhấn **Xác nhận**
   - ✅ Dialog OTP sẽ hiện ra
   - Nhập OTP (xem console backend log để lấy OTP dev)
   - ✅ Thanh toán thành công

### Bước 4: Test các dịch vụ khác
- ✅ **Tiền nước** - Tương tự tiền điện
- ✅ **Internet** - Nhập mã khách hàng và số tiền
- ✅ **Nạp tiền** - Nhập số điện thoại (phải đúng format 0xxxxxxxxx)

---

## 🔍 DEBUG NẾU VẪN LỖI:

### Kiểm tra Backend Logs:
Khi bấm thanh toán, xem console backend. Bạn sẽ thấy logs chi tiết:
```
Pay electricity bill error: [lỗi cụ thể]
Error stack: [stack trace]
Request body: [data được gửi]
Request user: [user info]
Request userId: [userId]
```

### Kiểm tra App Logs (Logcat):
Filter: `ApiService`
Tìm:
```
POST utilities/pay-electricity - Response Code: 500
Error response: [message]
```

### Lỗi thường gặp:

#### 1. "User authentication required"
**Nguyên nhân:** Token không được gửi hoặc token expired
**Giải pháp:**
- Logout và login lại
- Kiểm tra SessionManager có lưu token không

#### 2. "Account not found"
**Nguyên nhân:** User chưa có account
**Giải pháp:**
```bash
cd backend
node seed.js
```

#### 3. "Insufficient balance"
**Nguyên nhân:** Số dư không đủ
**Giải pháp:**
- Dùng account có số dư cao hơn
- Hoặc seed lại để tạo accounts với số dư lớn hơn

---

## 📊 THÔNG TIN ACCOUNTS SAU KHI SEED:

### Customer: customer@example.com
- **CHECKING:** ~7 triệu VND
- **SAVING:** ~48 triệu VND

### Customer: user2@example.com
- **CHECKING:** ~3 triệu VND
- **SAVING:** ~49 triệu VND
- **MORTGAGE:** ~547 triệu VND

---

## ✅ KẾT QUẢ MONG ĐỢI:

1. ✅ Nhấn card → Dialog thanh toán hiện ra
2. ✅ Nhập thông tin và xác nhận → Dialog OTP hiện ra
3. ✅ Nhập OTP đúng → Thanh toán thành công
4. ✅ Hiển thị thông báo thành công

---

## 💡 LƯU Ý:

- **OTP cho development:** Xem console backend log để lấy OTP
- **Format số điện thoại:** Phải đúng `0xxxxxxxxx` (10 số, bắt đầu bằng 0)
- **Số tiền:** Phải lớn hơn 0

---

**Nếu vẫn còn lỗi, hãy copy backend console logs và Logcat để tôi debug tiếp!** 🚀

