# 🔍 Hướng dẫn Debug Balance không cập nhật

## ✅ Đã thêm Debug Logging

Tất cả logging đã được thêm vào để debug vấn đề balance không cập nhật.

---

## 📱 Cách xem logs trên Android

### 1. **Mở Android Studio Logcat**

1. Mở Android Studio
2. Chạy app trên emulator/device
3. Mở tab **Logcat** ở dưới cùng
4. Filter logs theo tag:
   - `AccountService` - Logs từ AccountService
   - `HomeFragment` - Logs từ HomeFragment  
   - `TransactionFragment` - Logs từ TransactionFragment

### 2. **Xem Debug Logs**

Tìm các logs có prefix `[DEBUG]`:
- 🔄 = Đang load data
- ✅ = Thành công
- ❌ = Lỗi
- ⚠️ = Cảnh báo
- 💰 = Balance information
- 📊 = Account data

### 3. **Các logs quan trọng**

```
🔄 [DEBUG] Getting user accounts from API...
✅ [DEBUG] API Response received: {...}
📊 [DEBUG] Found X accounts
💰 [DEBUG] Account ID: ..., Balance: ..., Type: ...
💵 [DEBUG] Parsed balance: ... -> ...
```

---

## 🖥️ Cách xem logs trên Backend

### 1. **Mở Terminal/Console nơi chạy backend**

```bash
cd backend
npm start
# hoặc
node server.js
```

### 2. **Xem Debug Logs**

Tìm các logs có prefix `[DEBUG]`:
- 🔄 = Đang query database
- 📊 = Số lượng accounts
- 💰 = Balance từ database
- 📤 = Data gửi về client

### 3. **Các logs quan trọng**

```
[DEBUG] 🔄 Getting accounts for userId: ...
[DEBUG] 📊 Found X accounts from database
[DEBUG] 💰 Account ID: ..., Balance: ..., Type: ...
[DEBUG] 💵 Formatted account balance: ... for account ...
[DEBUG] 📤 Sending X formatted accounts to client
[DEBUG] 📤 Account ...: balance=... (type: number)
```

---

## 🔧 Các bước Debug

### Bước 1: Kiểm tra Database

1. Mở MongoDB shell hoặc MongoDB Compass
2. Chạy query:
   ```javascript
   db.accounts.find({userId: ObjectId("YOUR_USER_ID")})
   ```
3. Kiểm tra field `balance` có đúng giá trị không

### Bước 2: Kiểm tra Backend API

1. Test API trực tiếp:
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
        http://localhost:8000/api/accounts
   ```
2. Xem response JSON, kiểm tra field `balance` trong mỗi account
3. Xem backend logs để thấy balance được query và format như thế nào

### Bước 3: Kiểm tra Android App

1. Mở Android Studio Logcat
2. Filter: `AccountService` hoặc `HomeFragment`
3. Xem logs khi app load accounts:
   - Balance từ API response
   - Balance sau khi parse
   - Balance được hiển thị

### Bước 4: So sánh giá trị

So sánh 3 giá trị:
1. **Database balance** (từ MongoDB)
2. **API response balance** (từ backend logs)
3. **App displayed balance** (từ Android logs)

Nếu khác nhau → Tìm xem bước nào bị sai.

---

## 🐛 Các vấn đề thường gặp

### 1. **Balance trong DB là String thay vì Number**

**Triệu chứng:**
- Backend logs: `Balance: "1000000"` (có dấu ngoặc kép)
- API response: `balance: "1000000"` (string)

**Giải pháp:**
```javascript
// Trong MongoDB, update balance thành number
db.accounts.updateOne(
  {_id: ObjectId("...")},
  {$set: {balance: Number("1000000")}}
)
```

### 2. **App cache balance cũ**

**Triệu chứng:**
- Logs cho thấy API trả về balance mới
- Nhưng UI vẫn hiển thị balance cũ

**Giải pháp:**
- Nhấn nút **Refresh** (🔄) ở HomeFragment
- Hoặc đóng app và mở lại
- Hoặc navigate sang tab khác rồi quay lại

### 3. **Balance bị format sai**

**Triệu chứng:**
- Balance trong DB: `1000000`
- Balance trong API: `1000000`
- Balance trong App: `1000000.0` hoặc format khác

**Giải pháp:**
- Kiểm tra `parseAccountFromJson()` trong AccountService
- Kiểm tra `formatAccount()` trong responseFormatter.js

### 4. **Backend không query lại từ DB**

**Triệu chứng:**
- Đã update balance trong DB
- Nhưng API vẫn trả về balance cũ

**Giải pháp:**
- Restart backend server
- Kiểm tra MongoDB connection
- Kiểm tra query có đúng userId không

---

## 🔄 Cách Refresh Balance

### Cách 1: Dùng Refresh Button
1. Vào **Home** tab
2. Nhấn nút **Refresh** (🔄) ở góc trên bên phải của card balance
3. Balance sẽ được reload từ API

### Cách 2: Navigate lại Fragment
1. Chuyển sang tab khác (ví dụ Profile)
2. Quay lại tab Home
3. `onResume()` sẽ tự động reload balance

### Cách 3: Restart App
1. Đóng app hoàn toàn
2. Mở lại app
3. Balance sẽ được load từ đầu

---

## 📋 Checklist Debug

- [ ] Kiểm tra balance trong MongoDB có đúng không
- [ ] Kiểm tra backend logs có query đúng balance không
- [ ] Kiểm tra API response có balance đúng không
- [ ] Kiểm tra Android logs có parse balance đúng không
- [ ] Kiểm tra UI có hiển thị balance đúng không
- [ ] Thử refresh bằng nút Refresh
- [ ] Thử restart app
- [ ] Thử restart backend

---

## 🎯 Quick Test

1. **Update balance trong MongoDB:**
   ```javascript
   db.accounts.updateOne(
     {accountNumber: "YOUR_ACCOUNT_NUMBER"},
     {$set: {balance: 50000000}}
   )
   ```

2. **Xem backend logs:**
   - Có thấy balance mới không?

3. **Refresh app:**
   - Nhấn nút Refresh hoặc navigate lại
   - Xem Android logs

4. **So sánh:**
   - Database balance = ?
   - API response balance = ?
   - App displayed balance = ?

---

## 📝 Log Examples

### Backend Logs (Good):
```
[DEBUG] 🔄 Getting accounts for userId: 507f1f77bcf86cd799439011
[DEBUG] 📊 Found 1 accounts from database
[DEBUG] 💰 Account ID: 507f..., Number: 1234567890, Balance: 50000000, Type: CHECKING
[DEBUG] 💵 Formatted account balance: 50000000 for account 1234567890
[DEBUG] 📤 Sending 1 formatted accounts to client
[DEBUG] 📤 Account 1234567890: balance=50000000 (type: number)
```

### Android Logs (Good):
```
AccountService: 🔄 [DEBUG] Getting user accounts from API...
AccountService: ✅ [DEBUG] API Response received: {...}
AccountService: 📊 [DEBUG] Found 1 accounts
AccountService: 💰 [DEBUG] Account ID: ..., Balance: 50000000, Type: CHECKING
HomeFragment: ✅ [DEBUG] Accounts loaded: 1
HomeFragment: 💰 [DEBUG] Balance from API: 50000000 VND
HomeFragment: 🔄 [DEBUG] Updating display - Balance: 50000000, Formatted: 50,000,000 VND
HomeFragment: ✅ [DEBUG] Balance displayed: 50,000,000 VND
```

---

## 🚨 Nếu vẫn không được

1. **Copy toàn bộ logs** (cả backend và Android)
2. **Chụp screenshot** của:
   - MongoDB query result
   - API response (từ Postman/curl)
   - App UI hiển thị balance
3. **Gửi cho tôi** để phân tích tiếp

---

## ✅ Đã thêm tính năng

- ✅ Debug logging chi tiết ở mọi bước
- ✅ Refresh button ở HomeFragment
- ✅ Logging balance từ DB → API → App
- ✅ Type checking cho balance (string vs number)
- ✅ Error handling tốt hơn

Bây giờ bạn có thể debug dễ dàng hơn! 🎉

