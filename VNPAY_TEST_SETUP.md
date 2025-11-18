# 🧪 Hướng dẫn Test VNPay Local (Cho Môn Học)

## ⚠️ Lưu ý: Chỉ dùng cho test local, không dùng cho production!

---

## 📋 Bước 1: Tạo file .env

1. Vào thư mục `backend/`
2. Copy file `.env.example` thành `.env`:
   ```bash
   cd backend
   cp .env.example .env
   ```

3. File `.env` đã có sẵn test credentials, bạn có thể:
   - **Option 1:** Dùng test credentials có sẵn (đã được cấu hình)
   - **Option 2:** Lấy credentials từ VNPay Sandbox (xem bước 2)

---

## 🔑 Bước 2: Lấy Test Credentials từ VNPay (Tùy chọn)

Nếu muốn dùng credentials riêng:

1. **Truy cập:** https://sandbox.vnpayment.vn/
2. **Đăng ký/Đăng nhập** tài khoản
3. **Vào Merchant Portal** → Tìm mục "Thông tin kết nối" hoặc "Integration"
4. **Copy 2 thông tin:**
   - **TMN Code** (Terminal Code)
   - **Hash Secret** (Secret Key)
5. **Cập nhật vào file `.env`:**
   ```env
   VNPAY_TMN_CODE=your_tmn_code_here
   VNPAY_HASH_SECRET=your_hash_secret_here
   ```

**Hoặc** bạn có thể dùng test credentials có sẵn trong code (đã được set mặc định).

---

## 🚀 Bước 3: Khởi động Backend

```bash
cd backend
node server.js
```

Kiểm tra log:
```
🚀 Server running on http://0.0.0.0:8000
✅ MongoDB Connected
```

---

## 📱 Bước 4: Test từ Android App

1. **Mở Android app**
2. **Vào màn hình Transaction** → Click "Nạp tiền"
3. **Chọn "VNPay"**
4. **Nhập số tiền** (tối thiểu 10,000 VND)
5. **Click "Tiếp tục"**

App sẽ mở trình duyệt với trang thanh toán VNPay.

---

## 🧪 Bước 5: Test với Thẻ Test

Khi thanh toán, dùng thông tin thẻ test sau:

### ✅ Thẻ Thành Công:
- **Ngân hàng:** NCB
- **Số thẻ:** `9704198526191432198`
- **Tên chủ thẻ:** NGUYEN VAN A
- **Ngày phát hành:** `07/15`
- **OTP:** `123456`

### ❌ Thẻ Không Đủ Số Dư:
- **Số thẻ:** `9704195798459170488`

### ❌ Thẻ Chưa Kích Hoạt:
- **Số thẻ:** `9704192181368742`

---

## 🔧 Cấu hình Callback URL (Quan trọng cho Test)

### Vấn đề: Localhost không nhận được callback từ VNPay

**Giải pháp 1: Dùng ngrok (Khuyên dùng)**

1. **Cài đặt ngrok:**
   ```bash
   # Mac
   brew install ngrok
   
   # Hoặc download từ: https://ngrok.com/download
   ```

2. **Chạy ngrok:**
   ```bash
   ngrok http 8000
   ```

3. **Copy URL ngrok** (ví dụ: `https://abc123.ngrok.io`)

4. **Cập nhật `.env`:**
   ```env
   VNPAY_RETURN_URL=https://abc123.ngrok.io/api/payments/vnpay/callback
   BACKEND_URL=https://abc123.ngrok.io
   ```

5. **Cấu hình trong VNPay Portal:**
   - Vào VNPay Sandbox → Merchant Portal
   - Tìm mục "IPN URL" hoặc "Callback URL"
   - Nhập: `https://abc123.ngrok.io/api/payments/vnpay/callback`

**Giải pháp 2: Test thủ công callback**

Nếu không dùng ngrok, bạn có thể test callback thủ công:

1. Sau khi thanh toán thành công trên VNPay
2. Copy URL callback từ VNPay
3. Paste vào browser để trigger callback

---

## ✅ Checklist Test

- [ ] File `.env` đã được tạo
- [ ] Backend server đang chạy (port 8000)
- [ ] MongoDB đang chạy
- [ ] Android app có thể kết nối backend
- [ ] Có thể tạo payment qua VNPay
- [ ] Trang thanh toán VNPay hiển thị
- [ ] Có thể test với thẻ test
- [ ] Callback URL đã được cấu hình (nếu dùng ngrok)

---

## 🐛 Troubleshooting

### Lỗi: "Cannot connect to backend"
- ✅ Kiểm tra backend có đang chạy: `node server.js`
- ✅ Kiểm tra `BASE_URL` trong Android: `http://10.0.2.2:8000/api/` (emulator)
- ✅ Với real device: Dùng IP máy tính thay vì `10.0.2.2`

### Lỗi: "VNPay secure hash không hợp lệ"
- ✅ Kiểm tra `VNPAY_HASH_SECRET` trong `.env`
- ✅ Đảm bảo không có khoảng trắng thừa
- ✅ Nếu dùng test credentials mặc định, đảm bảo code đã được cập nhật

### Lỗi: "Payment not found" trong callback
- ✅ Kiểm tra callback URL có đúng không
- ✅ Nếu test local, cần dùng ngrok
- ✅ Kiểm tra database có lưu payment không

### Callback không hoạt động
- ✅ Dùng ngrok để expose local server
- ✅ Cấu hình IPN URL trong VNPay Portal
- ✅ Kiểm tra firewall có chặn không

---

## 📝 Test Credentials Mặc Định

Code đã được cấu hình với test credentials mặc định:

```javascript
VNPAY_TMN_CODE=2QXUI4J4
VNPAY_HASH_SECRET=RAOPSRGEWNYSMDZDEHEQCDDZXLZQJQKT
```

Bạn có thể dùng trực tiếp mà không cần cấu hình thêm!

---

## 🎯 Quick Start (Nhanh nhất)

1. **Copy .env.example:**
   ```bash
   cd backend
   cp .env.example .env
   ```

2. **Khởi động backend:**
   ```bash
   node server.js
   ```

3. **Test từ Android app:**
   - Mở app → Transaction → Nạp tiền → VNPay
   - Nhập số tiền → Thanh toán
   - Dùng thẻ test: `9704198526191432198`, OTP: `123456`

**Xong!** 🎉

---

## ⚠️ Lưu ý Quan Trọng

1. **Chỉ dùng cho test local** - Không dùng credentials này cho production
2. **Callbacks cần public URL** - Dùng ngrok nếu test callback
3. **Test credentials có thể thay đổi** - Nếu không hoạt động, lấy credentials mới từ VNPay Sandbox
4. **Không commit .env** - File `.env` đã được thêm vào `.gitignore`

---

Chúc bạn test thành công! 🚀

