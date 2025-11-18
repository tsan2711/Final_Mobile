# 📱 Hướng dẫn sử dụng eKYC & Biometric

## ✅ Đã implement xong

Tất cả tính năng eKYC & Biometric đã được tích hợp vào ứng dụng.

---

## 🎯 Cách sử dụng

### 1. **Xác thực eKYC (Upload ảnh khuôn mặt)**

**Bước 1:** Mở ứng dụng → Vào tab **"Profile"** (Hồ sơ)

**Bước 2:** Nhấn vào **"Xác thực eKYC"**

**Bước 3:** Chọn một trong hai tùy chọn:
- **"Chụp ảnh khuôn mặt"** - Chụp và upload ảnh mới
- **"Kiểm tra trạng thái"** - Xem trạng thái xác thực hiện tại

**Bước 4:** Nếu chọn "Chụp ảnh khuôn mặt":
- Ứng dụng sẽ mở camera
- Đặt khuôn mặt vào khung
- Nhấn **"Chụp ảnh"**
- Xem lại ảnh, nếu OK thì nhấn **"Tải lên"**, nếu không thì **"Chụp lại"**

**Bước 5:** Đợi hệ thống xác thực (tự động trong demo)

---

### 2. **Biometric Authentication cho giao dịch giá trị cao**

**Khi nào cần xác thực sinh trắc học?**
- Khi chuyển tiền **>= 10,000,000 VND** (10 triệu đồng)

**Cách hoạt động:**
1. Vào tab **"Giao dịch"** (Transactions)
2. Nhấn **"Chuyển tiền"**
3. Nhập thông tin:
   - Số tài khoản nhận
   - Số tiền (>= 10,000,000 VND)
   - Mô tả
4. Nhấn **"Xác nhận"**
5. **Hệ thống tự động hiển thị Biometric Prompt** (vân tay/face unlock)
6. Xác thực bằng vân tay hoặc face unlock
7. Sau khi xác thực thành công, giao dịch sẽ được xử lý

**Lưu ý:**
- Nếu chưa có eKYC, hệ thống sẽ yêu cầu hoàn thành eKYC trước
- Nếu eKYC đã hết hạn, cần xác thực lại

---

### 3. **Kiểm tra trạng thái eKYC**

**Cách kiểm tra:**
1. Vào **Profile** → **"Xác thực eKYC"**
2. Chọn **"Kiểm tra trạng thái"**
3. Xem thông tin:
   - **NOT_STARTED**: Chưa bắt đầu
   - **PENDING**: Đang chờ xác thực
   - **VERIFIED**: Đã xác thực ✓
   - **REJECTED**: Đã từ chối (cần chụp lại)

---

## 🔧 API Endpoints

### Backend (Node.js)

1. **POST `/api/ekyc/upload-face`**
   - Upload ảnh khuôn mặt
   - Method: POST (multipart/form-data)
   - Field: `faceImage` (file)

2. **POST `/api/ekyc/verify-identity`**
   - Xác thực danh tính cho giao dịch giá trị cao
   - Body: `{ transactionId, amount, faceImage? }`

3. **GET `/api/ekyc/verification-status`**
   - Lấy trạng thái xác thực eKYC

---

## 📋 Yêu cầu hệ thống

### Android App
- ✅ Camera permission
- ✅ Biometric permission
- ✅ Thiết bị hỗ trợ camera
- ✅ Thiết bị hỗ trợ biometric (vân tay/face unlock)

### Backend
- ✅ Multer đã được cài đặt
- ✅ Thư mục `backend/uploads/ekyc/` để lưu ảnh
- ✅ MongoDB connection

---

## 🎨 UI Components

### ProfileFragment
- ✅ Nút "Xác thực eKYC" đã được thêm vào
- ✅ Dialog chọn hành động (Chụp ảnh / Kiểm tra trạng thái)
- ✅ Hiển thị trạng thái eKYC chi tiết

### FaceCaptureActivity
- ✅ Camera preview
- ✅ Capture button
- ✅ Retake button
- ✅ Upload button
- ✅ Tự động xoay ảnh

### TransactionFragment
- ✅ Tự động kiểm tra giao dịch giá trị cao
- ✅ Tự động hiển thị Biometric Prompt
- ✅ Xử lý kết quả xác thực

---

## 🚀 Testing

### Test eKYC Upload:
1. Login vào app
2. Vào Profile → Xác thực eKYC → Chụp ảnh khuôn mặt
3. Chụp ảnh và upload
4. Kiểm tra backend logs để xem ảnh đã được lưu

### Test Biometric:
1. Đảm bảo đã có eKYC VERIFIED
2. Vào Transactions → Chuyển tiền
3. Nhập số tiền >= 10,000,000 VND
4. Xác nhận → Biometric prompt sẽ xuất hiện
5. Xác thực bằng vân tay/face unlock

### Test High-Value Transaction:
1. Chuyển tiền >= 10,000,000 VND
2. Backend sẽ kiểm tra eKYC status
3. Nếu chưa có eKYC → Trả về lỗi yêu cầu eKYC
4. Nếu có eKYC → Cho phép giao dịch

---

## 📝 Notes

- **Face Verification**: Hiện tại đang mô phỏng (auto-approve). Trong production cần tích hợp ML service thật.
- **Image Storage**: Ảnh lưu trong `backend/uploads/ekyc/`. Production nên dùng cloud storage.
- **Biometric**: Sử dụng Android BiometricPrompt API (hỗ trợ vân tay và face unlock).
- **eKYC Expiry**: Xác thực eKYC có hiệu lực 1 năm.

---

## ✅ Checklist Implementation

- [x] Backend: EkycVerification model
- [x] Backend: EkycController với 3 endpoints
- [x] Backend: ekyc routes
- [x] Backend: Multer cho file upload
- [x] Backend: Kiểm tra eKYC trong TransactionController
- [x] Android: Camera permission
- [x] Android: Biometric permission
- [x] Android: FaceCaptureActivity
- [x] Android: EkycService
- [x] Android: BiometricHelper
- [x] Android: Tích hợp vào TransactionFragment
- [x] Android: Nút eKYC trong ProfileFragment
- [x] Android: Check eKYC status

---

## 🎉 Hoàn thành!

Tất cả tính năng đã được implement và sẵn sàng sử dụng!

