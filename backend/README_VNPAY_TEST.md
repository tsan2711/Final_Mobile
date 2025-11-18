# 🚀 Quick Start - Test VNPay Local

## ⚡ Test Ngay (Không cần cấu hình)

Code đã có **test credentials mặc định**, bạn có thể test ngay!

### Bước 1: Khởi động Backend
```bash
cd backend
node server.js
```

### Bước 2: Test từ Android App
1. Mở app → Transaction → Nạp tiền
2. Chọn VNPay
3. Nhập số tiền (tối thiểu 10,000 VND)
4. Thanh toán

### Bước 3: Dùng Thẻ Test
- **Số thẻ:** `9704198526191432198`
- **OTP:** `123456`
- **Tên:** NGUYEN VAN A
- **Ngày:** `07/15`

**Xong!** 🎉

---

## 📝 Nếu muốn dùng credentials riêng

1. Copy `.env.example` thành `.env`:
   ```bash
   cp .env.example .env
   ```

2. Lấy credentials từ https://sandbox.vnpayment.vn/
3. Cập nhật vào `.env`

---

Xem chi tiết trong file `VNPAY_TEST_SETUP.md` ở thư mục gốc!

