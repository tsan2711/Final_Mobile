# 🔑 VNPay Sandbox Credentials

## Thông tin tài khoản

**Terminal ID / Mã Website:**
```
DK9HKBJK
```

**Secret Key / Chuỗi bí mật:**
```
HFR3FEWAWKHFE1TTAVGQAUSK0P1SX7N9
```

**URL thanh toán:**
```
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
```

---

## ⚠️ Lưu ý bảo mật

- File này chỉ để tham khảo
- Không commit file này lên Git
- Credentials đã được cập nhật vào file `.env`
- File `.env` đã được thêm vào `.gitignore`

---

## ✅ Đã cấu hình

Credentials đã được cập nhật vào `backend/.env`:

```env
VNPAY_TMN_CODE=DK9HKBJK
VNPAY_HASH_SECRET=HFR3FEWAWKHFE1TTAVGQAUSK0P1SX7N9
VNPAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8000/api/payments/vnpay/callback
```

---

## 🚀 Sẵn sàng test

Khởi động backend:
```bash
cd backend
node server.js
```

Test từ Android app:
1. Mở app → Transaction → Nạp tiền
2. Chọn VNPay
3. Nhập số tiền (≥ 10,000 VND)
4. Thanh toán với thẻ test: `9704198526191432198`, OTP: `123456`

---

Đã cấu hình xong! 🎉

