# 🔧 Fix Route Not Found Error

## ❌ Vấn Đề

Endpoint `POST /api/admin/customers` trả về `404 - Route not found`

## ✅ Giải Pháp

### Bước 1: Restart Backend Server

**Quan trọng:** Sau khi thêm routes mới, bạn **PHẢI restart backend server** để server load routes mới!

```bash
cd backend

# Dừng server hiện tại (Ctrl+C trong terminal đang chạy server)

# Start lại server
node server.js
```

### Bước 2: Kiểm Tra Routes Đã Được Đăng Ký

Sau khi restart, bạn sẽ thấy trong console:
```
🚀 Server running on http://0.0.0.0:8000
```

Và khi có request đến `/api/admin/*`, bạn sẽ thấy log:
```
GET /api/admin/customers
POST /api/admin/customers
```

### Bước 3: Test Routes

Chạy test script để kiểm tra:
```bash
cd backend
node test_admin_routes.js
```

Kết quả mong đợi:
```
✅ GET /admin/customers - Status: 200
✅ POST /admin/customers - Status: 201
✅ PUT /admin/customers/:id - Status: 200
```

## 🔍 Kiểm Tra Thêm

### 1. Kiểm Tra Route Order

Trong `backend/src/routes/admin.js`, route cụ thể phải đứng **TRƯỚC** route có parameter:

```javascript
// ✅ ĐÚNG - Route cụ thể trước
router.get('/customers/search', AdminController.searchCustomers);
router.get('/customers', AdminController.getAllCustomers);
router.post('/customers', AdminController.createCustomer);
router.get('/customers/:customerId', AdminController.getCustomerDetails);
router.put('/customers/:customerId', AdminController.updateCustomer);

// ❌ SAI - Route có parameter trước sẽ match "/customers/search" như ":customerId"
```

### 2. Kiểm Tra Authentication

Tất cả admin routes yêu cầu:
- ✅ JWT token trong header: `Authorization: Bearer <token>`
- ✅ User phải là `BANK_OFFICER`

Nếu thiếu auth, sẽ trả về `401 Unauthorized` chứ không phải `404`.

### 3. Kiểm Tra Controller Method

Đảm bảo `AdminController.createCustomer` đã được implement:
```bash
grep -n "createCustomer" backend/src/controllers/AdminController.js
```

Phải thấy:
```javascript
static async createCustomer(req, res) {
  // ... implementation
}
```

### 4. Kiểm Tra Server.js

Đảm bảo admin routes được mount:
```bash
grep "admin" backend/server.js
```

Phải thấy:
```javascript
const adminRoutes = require('./src/routes/admin');
app.use('/api/admin', adminRoutes);
```

## 🚨 Common Issues

### Issue 1: Server chưa restart
**Triệu chứng:** Route mới không hoạt động
**Giải pháp:** Dừng và start lại server

### Issue 2: Route order sai
**Triệu chứng:** `/customers/search` bị match bởi `/customers/:customerId`
**Giải pháp:** Đặt route cụ thể trước route có parameter

### Issue 3: Method không khớp
**Triệu chứng:** `POST /api/admin/customers` trả về 404 nhưng `GET` hoạt động
**Giải pháp:** Kiểm tra xem route có định nghĩa đúng method không:
```javascript
router.post('/customers', ...); // ✅ Đúng
router.get('/customers', ...);  // ❌ Sai cho POST request
```

## 📝 Test Routes Manually

### Test với curl:
```bash
# 1. Login để lấy token
TOKEN=$(curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bank.com","password":"123456"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 2. Test GET customers
curl -X GET http://localhost:8000/api/admin/customers \
  -H "Authorization: Bearer $TOKEN"

# 3. Test POST create customer
curl -X POST http://localhost:8000/api/admin/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "123456",
    "fullName": "Test User",
    "phone": "0901234567"
  }'
```

## ✅ Sau Khi Fix

Nếu vẫn gặp lỗi, kiểm tra:
1. ✅ Backend server đã restart
2. ✅ Token còn valid (chưa expire)
3. ✅ User đang login là BANK_OFFICER
4. ✅ Network connection OK
5. ✅ MongoDB đang chạy

