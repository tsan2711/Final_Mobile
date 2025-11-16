# 🧪 Hướng Dẫn Test Tính Năng Utilities

## ✅ Lỗi Đã Sửa

Đã sửa các lỗi compile trong `UtilityService.java`:
- ✅ Sửa `new ApiService()` → `ApiService.getInstance(context)`
- ✅ Loại bỏ `sessionManager.getAuthToken()` (không cần, ApiService tự lấy token)
- ✅ Thay đổi từ `post(url, body, token)` → `post(endpoint, jsonObject, callback)`
- ✅ Sử dụng ApiService.ApiCallback thay vì trả về String
- ✅ Tất cả methods đã được viết lại đúng chuẩn

## 🚀 Cách Test Utilities

### Bước 1: Khởi động Backend

```bash
cd "/Users/tsangcuteso1/Documents/GitHub/CKSOA/My project/Final_Mobile/backend"
node server.js
```

Bạn sẽ thấy:
```
✅ MongoDB Connected
🚀 Server running on http://0.0.0.0:8000
📱 Android app can connect to: http://YOUR_IP:8000/api/
🔗 Health check: http://0.0.0.0:8000/health
```

### Bước 2: Seed Dữ Liệu Test (nếu chưa có)

```bash
cd backend
node seed.js
```

Tài khoản test:
- **Email:** `customer@example.com`
- **Password:** `123456`
- **Type:** CUSTOMER

### Bước 3: Chạy Android App

1. Mở Android Studio
2. Build và Run app trên emulator hoặc thiết bị thật
3. Đăng nhập với tài khoản test

### Bước 4: Test Từng Tính Năng

#### 📱 Test 1: Thanh Toán Tiền Điện

1. Chọn tab **"Tiện ích"** (icon utilities ở bottom navigation)
2. Click vào card **"Tiền điện"**
3. Nhập thông tin:
   - Mã khách hàng: `PD123456` (bất kỳ)
   - Tên khách hàng: `Nguyễn Văn A` (tùy chọn)
   - Kỳ thanh toán: `12/2024` (tùy chọn)
   - Số tiền: `500000` (500k VNĐ)
4. Click **"Thanh toán"**
5. Dialog OTP sẽ hiện ra với:
   - Thông tin giao dịch
   - Phí (1% của số tiền, tối đa 20k)
   - Tổng tiền
   - **OTP (Dev): XXXXXX** ← Mã OTP hiển thị luôn
6. Nhập OTP và click **"Xác nhận"**
7. Thấy thông báo **"Thành công"**

**Expected Results:**
- ✅ Form validation hoạt động (bắt buộc nhập số tiền)
- ✅ Progress dialog hiện khi xử lý
- ✅ OTP được gửi và hiển thị
- ✅ Thanh toán thành công
- ✅ Số dư tài khoản giảm

#### 💧 Test 2: Thanh Toán Tiền Nước

1. Click vào card **"Tiền nước"**
2. Nhập:
   - Mã khách hàng: `PN789012`
   - Tên: `Trần Thị B`
   - Kỳ: `12/2024`
   - Số tiền: `300000`
3. Thanh toán → Nhập OTP → Xác nhận
4. Kiểm tra thành công

#### 🌐 Test 3: Thanh Toán Internet

1. Click vào card **"Internet"**
2. Nhập:
   - Mã khách hàng: `PI345678`
   - Nhà cung cấp: `VNPT` (hoặc `FPT`, `Viettel`)
   - Số tiền: `400000`
3. Thanh toán → OTP → Xác nhận

#### 📞 Test 4: Nạp Tiền Điện Thoại

1. Click vào card **"Nạp tiền"**
2. Nhập:
   - Số điện thoại: `0987654321` (phải đúng format Việt Nam)
   - Số tiền: `50000` (50k, 100k, 200k...)
   - Nhà mạng: `AUTO` (tự nhận diện) hoặc `VIETTEL`, `VINAPHONE`...
3. Nạp tiền → OTP → Xác nhận

**Lưu ý:** 
- Số điện thoại phải đúng format: `0[3|5|7|8|9]XXXXXXXX`
- Nếu sai format, sẽ có lỗi validation

#### ⏳ Test 5: Gói Cước & Thẻ Cào (Chưa hoàn thiện)

- Click vào **"Gói cước"** → Hiện "Tính năng đang phát triển"
- Click vào **"Thẻ cào"** → Hiện "Tính năng đang phát triển"

**Backend API đã có nhưng UI chưa implement.**

---

## 🔍 Kiểm Tra Backend

### Test API trực tiếp bằng curl:

#### 1. Login để lấy token:

```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "123456"
  }'
```

Copy `token` từ response.

#### 2. Test thanh toán tiền điện:

```bash
curl -X POST http://localhost:8000/api/utilities/pay-electricity \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "customerNumber": "PD123456",
    "amount": 500000,
    "customerName": "Nguyen Van A",
    "period": "12/2024"
  }'
```

Response sẽ có:
```json
{
  "success": true,
  "message": "Payment initiated. OTP sent for verification.",
  "data": {
    "otp_required": true,
    "transaction_id": "UTL...",
    "amount": 500000,
    "fee": 5000,
    "total_amount": 505000,
    "developmentOTP": "123456"
  }
}
```

#### 3. Verify OTP:

```bash
curl -X POST http://localhost:8000/api/utilities/verify-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "transactionId": "UTL...",
    "otpCode": "123456"
  }'
```

Response thành công:
```json
{
  "success": true,
  "message": "Payment completed successfully",
  "data": {
    "transaction_id": "UTL...",
    "status": "COMPLETED",
    "reference_number": "REF..."
  }
}
```

#### 4. Kiểm tra lịch sử:

```bash
curl http://localhost:8000/api/utilities/history \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 📊 Kiểm Tra Database

### Vào MongoDB để xem dữ liệu:

```bash
mongosh
use banking_db
db.utilities.find().pretty()
```

Bạn sẽ thấy các records:
```javascript
{
  _id: ObjectId("..."),
  transactionId: "UTL1731672000123456",
  userId: ObjectId("..."),
  serviceType: "ELECTRICITY",
  provider: "EVN",
  serviceNumber: "PD123456",
  amount: 500000,
  fee: 5000,
  totalAmount: 505000,
  status: "COMPLETED",
  referenceNumber: "REF...",
  createdAt: ISODate("..."),
  processedAt: ISODate("...")
}
```

---

## ⚠️ Common Issues & Solutions

### Issue 1: "Failed to connect to 10.0.2.2:8000"
**Nguyên nhân:** Backend chưa chạy
**Giải pháp:** 
```bash
cd backend && node server.js
```

### Issue 2: "Account not found"
**Nguyên nhân:** User chưa có account
**Giải pháp:**
```bash
cd backend && node seed.js
```

### Issue 3: "Insufficient balance"
**Nguyên nhân:** Tài khoản không đủ tiền
**Giải pháp:** Test với số tiền nhỏ hơn hoặc dùng Deposit API để nạp thêm

### Issue 4: "Invalid OTP"
**Nguyên nhân:** Nhập sai OTP
**Giải pháp:** Copy chính xác OTP từ dialog (có hiển thị trong dev mode)

### Issue 5: "Số điện thoại không hợp lệ"
**Nguyên nhân:** Format sai
**Giải pháp:** Phải là `0[3|5|7|8|9]XXXXXXXX` (10 số)
- ✅ `0987654321` 
- ✅ `0912345678`
- ❌ `987654321` (thiếu số 0)
- ❌ `0123456789` (đầu số không hợp lệ)

---

## 📝 Test Checklist

### Backend API Testing:
- [ ] POST `/api/utilities/pay-electricity` - Initiate
- [ ] POST `/api/utilities/pay-water` - Initiate
- [ ] POST `/api/utilities/pay-internet` - Initiate
- [ ] POST `/api/utilities/mobile-topup` - Initiate
- [ ] POST `/api/utilities/verify-otp` - Verify all types
- [ ] GET `/api/utilities/history` - View history
- [ ] GET `/api/utilities/providers` - Get providers list

### Android UI Testing:
- [ ] Tiền điện - Form display
- [ ] Tiền điện - Validation
- [ ] Tiền điện - Payment flow
- [ ] Tiền điện - OTP dialog
- [ ] Tiền điện - Success message
- [ ] Tiền nước - Full flow
- [ ] Internet - Full flow
- [ ] Nạp tiền điện thoại - Full flow
- [ ] Phone number validation
- [ ] Error handling (network, insufficient balance)
- [ ] Progress indicators
- [ ] Toast notifications

### Integration Testing:
- [ ] Backend → Database storage
- [ ] Android → Backend API calls
- [ ] OTP generation and verification
- [ ] Account balance deduction
- [ ] Transaction record creation
- [ ] Fee calculation

---

## 🎉 Success Criteria

Tính năng Utilities được coi là **HOÀN THÀNH** khi:

1. ✅ Tất cả 4 loại thanh toán chính hoạt động:
   - Tiền điện
   - Tiền nước
   - Internet
   - Nạp tiền điện thoại

2. ✅ OTP verification hoạt động đúng

3. ✅ Số dư tài khoản được trừ chính xác (amount + fee)

4. ✅ Transaction records được lưu vào database

5. ✅ UI hiển thị rõ ràng và xử lý lỗi tốt

6. ✅ Không có crash khi test

---

## 📞 Support

Nếu gặp lỗi trong quá trình test:

1. Kiểm tra console logs:
   - Backend: Terminal chạy `node server.js`
   - Android: Logcat trong Android Studio

2. Kiểm tra MongoDB:
   ```bash
   mongosh
   use banking_db
   db.utilities.find().pretty()
   db.accounts.find().pretty()
   ```

3. Check API response trong Logcat (tag: `ApiService`)

---

**Chúc bạn test thành công! 🎊**

