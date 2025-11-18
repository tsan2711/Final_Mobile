# 🔧 Hướng dẫn cấu hình Payment Gateway (VNPay)

## ✅ Đã loại bỏ Stripe - Chỉ sử dụng VNPay

Hệ thống hiện chỉ hỗ trợ:
- **VNPay** - Nạp tiền qua VNPay
- **Bank Transfer** - Chuyển khoản đến ngân hàng khác

## 🎓 Cho Môn Học / Test Local

**Xem file `VNPAY_TEST_SETUP.md` để có hướng dẫn test nhanh cho local!**

Code đã được cấu hình với **test credentials mặc định**, bạn có thể test ngay mà không cần đăng ký VNPay!

---

## 📋 Bước 1: Cấu hình Backend (.env file)

Tạo hoặc cập nhật file `.env` trong thư mục `backend/`:

```env
# ============================================
# SERVER CONFIGURATION
# ============================================
PORT=8000
HOST=0.0.0.0
NODE_ENV=development

# ============================================
# DATABASE CONFIGURATION
# ============================================
MONGODB_URI=mongodb://localhost:27017/your_database_name

# ============================================
# VNPAY CONFIGURATION (BẮT BUỘC)
# ============================================
# Lấy từ VNPay Merchant Portal: https://sandbox.vnpayment.vn/
VNPAY_TMN_CODE=YOUR_VNPAY_TMN_CODE
VNPAY_HASH_SECRET=YOUR_VNPAY_HASH_SECRET

# Sandbox URL (cho testing)
VNPAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

# Production URL (khi đưa vào production)
# VNPAY_URL=https://www.vnpayment.vn/paymentv2/vpcpay.html

# Callback URL - Backend sẽ nhận callback từ VNPay
VNPAY_RETURN_URL=http://localhost:8000/api/payments/vnpay/callback
# Hoặc nếu deploy:
# VNPAY_RETURN_URL=https://your-backend-domain.com/api/payments/vnpay/callback

# ============================================
# FRONTEND/BACKEND URLs
# ============================================
# URL của frontend (để redirect sau khi thanh toán)
FRONTEND_URL=http://localhost:3000
# Hoặc nếu có mobile app:
# FRONTEND_URL=yourapp://payment

# URL của backend
BACKEND_URL=http://localhost:8000
# Hoặc nếu deploy:
# BACKEND_URL=https://your-backend-domain.com

# ============================================
# JWT CONFIGURATION (nếu chưa có)
# ============================================
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRE=7d
```

---

## 🔑 Bước 2: Lấy VNPay Credentials

### Cách 1: Đăng ký VNPay Sandbox (Cho testing)

1. **Truy cập VNPay Sandbox:**
   - URL: https://sandbox.vnpayment.vn/
   - Đăng ký tài khoản merchant mới

2. **Lấy thông tin:**
   - Đăng nhập vào Merchant Portal
   - Vào mục **"Thông tin kết nối"** hoặc **"Integration"**
   - Copy các thông tin sau:
     - **TMN Code** (Terminal Code)
     - **Hash Secret** (Secret Key)

3. **Cấu hình IPN URL:**
   - Trong VNPay Merchant Portal, cấu hình IPN URL:
   ```
   http://your-backend-url/api/payments/vnpay/callback
   ```
   - Ví dụ cho local: `http://localhost:8000/api/payments/vnpay/callback`

### Cách 2: Sử dụng VNPay Production (Khi deploy)

1. Liên hệ VNPay để đăng ký merchant account production
2. Lấy Production TMN Code và Hash Secret
3. Cập nhật `.env`:
   ```env
   VNPAY_URL=https://www.vnpayment.vn/paymentv2/vpcpay.html
   VNPAY_TMN_CODE=YOUR_PRODUCTION_TMN_CODE
   VNPAY_HASH_SECRET=YOUR_PRODUCTION_HASH_SECRET
   ```

---

## 📱 Bước 3: Cấu hình Android

File `app/src/main/java/com/example/final_mobile/services/ApiConfig.java` đã được cập nhật.

**Kiểm tra BASE_URL:**

```java
// Option 1: Android Emulator kết nối Node.js local
public static final String BASE_URL = "http://10.0.2.2:8000/api/";

// Option 2: Real Device kết nối Node.js local (thay IP của máy tính)
// public static final String BASE_URL = "http://192.168.1.100:8000/api/";

// Option 3: Production Node.js server
// public static final String BASE_URL = "https://yourdomain.com/api/";
```

**Lưu ý:**
- **Emulator**: Dùng `10.0.2.2:8000` (không cần thay đổi)
- **Real Device**: Thay `192.168.1.100` bằng IP máy tính của bạn
  - Tìm IP: `ipconfig` (Windows) hoặc `ifconfig` (Mac/Linux)
- **Production**: Dùng domain thực tế

---

## ✅ Bước 4: Kiểm tra cấu hình

### Test Backend:

```bash
cd backend
node server.js
```

Kiểm tra log:
```
🚀 Server running on http://0.0.0.0:8000
✅ MongoDB Connected
```

### Test Payment Endpoint:

```bash
# Test VNPay create payment (cần JWT token)
curl -X POST http://localhost:8000/api/payments/vnpay/create-payment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "accountId": "YOUR_ACCOUNT_ID",
    "amount": 100000,
    "description": "Test payment"
  }'
```

---

## 📝 Checklist cấu hình

### Backend:
- [ ] File `.env` đã được tạo trong `backend/`
- [ ] `VNPAY_TMN_CODE` đã được điền
- [ ] `VNPAY_HASH_SECRET` đã được điền
- [ ] `VNPAY_RETURN_URL` đã được cấu hình đúng
- [ ] `BACKEND_URL` và `FRONTEND_URL` đã được cấu hình
- [ ] Backend server chạy thành công

### VNPay:
- [ ] Đã đăng ký tài khoản VNPay Sandbox
- [ ] IPN URL đã được cấu hình trong VNPay Portal
- [ ] Đã test với sandbox trước khi chuyển production

### Android:
- [ ] `BASE_URL` trong `ApiConfig.java` đã đúng
- [ ] Đã test kết nối với backend
- [ ] PaymentActivity đã được thêm vào AndroidManifest

---

## 🐛 Troubleshooting

### Lỗi: "VNPay secure hash không hợp lệ"
- ✅ Kiểm tra `VNPAY_HASH_SECRET` có đúng không
- ✅ Đảm bảo không có khoảng trắng thừa trong `.env`
- ✅ Kiểm tra lại TMN Code

### Lỗi: "Cannot connect to backend"
- ✅ Kiểm tra `BASE_URL` trong Android
- ✅ Kiểm tra backend có đang chạy không (`node server.js`)
- ✅ Kiểm tra firewall/network
- ✅ Với real device, đảm bảo điện thoại và máy tính cùng mạng WiFi

### Lỗi: "Payment not found" trong callback
- ✅ Kiểm tra `VNPAY_RETURN_URL` có đúng không
- ✅ Kiểm tra database có lưu payment không
- ✅ Kiểm tra VNPay Portal có cấu hình IPN URL đúng không

### Lỗi: "VNPay callback không nhận được"
- ✅ Kiểm tra backend có thể nhận request từ internet không
- ✅ Nếu test local, cần dùng ngrok hoặc deploy lên server
- ✅ Kiểm tra firewall có chặn port 8000 không

---

## 🚀 Production Deployment

Khi deploy lên production:

1. **Thay đổi VNPay sang production:**
   ```env
   VNPAY_URL=https://www.vnpayment.vn/paymentv2/vpcpay.html
   VNPAY_TMN_CODE=YOUR_PRODUCTION_TMN_CODE
   VNPAY_HASH_SECRET=YOUR_PRODUCTION_HASH_SECRET
   ```

2. **Cập nhật URLs:**
   ```env
   BACKEND_URL=https://api.yourdomain.com
   FRONTEND_URL=https://yourdomain.com
   VNPAY_RETURN_URL=https://api.yourdomain.com/api/payments/vnpay/callback
   ```

3. **Bảo mật:**
   - ✅ Không commit file `.env` lên Git
   - ✅ Thêm `.env` vào `.gitignore`
   - ✅ Dùng environment variables trên hosting platform
   - ✅ Sử dụng HTTPS cho production

---

## 📚 Tài liệu tham khảo

- **VNPay Documentation:** https://sandbox.vnpayment.vn/apis/
- **VNPay Sandbox:** https://sandbox.vnpayment.vn/
- **VNPay Production:** https://www.vnpayment.vn/

---

## 💡 Lưu ý quan trọng

1. **Sandbox vs Production:**
   - Sandbox: Dùng để test, không tính phí
   - Production: Cần đăng ký merchant account thật, có phí

2. **Callback URL:**
   - Phải là URL công khai (public URL)
   - Không thể dùng `localhost` cho callback
   - Có thể dùng ngrok để test local: `ngrok http 8000`

3. **Security:**
   - Không bao giờ commit `.env` file
   - Hash Secret phải được bảo mật
   - Sử dụng HTTPS trong production

---

## ✅ Hoàn tất!

Sau khi cấu hình xong, bạn có thể:
- ✅ Nạp tiền qua VNPay từ Android app
- ✅ Chuyển khoản đến ngân hàng khác
- ✅ Xem lịch sử thanh toán

Chúc bạn thành công! 🎉

