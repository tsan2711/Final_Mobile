# 🚀 Hướng Dẫn Khởi Động Backend

## ✅ Trạng Thái Hiện Tại

- ✅ MongoDB: Đang chạy trên port 27017
- ✅ Node.js Server: Đang chạy trên port 8000
- ✅ File .env: Đã được tạo với cấu hình đầy đủ

## 📋 Cách Khởi Động Backend

### Cách 1: Sử dụng Script Tự Động (Khuyến nghị)

```bash
cd "/Users/tsangcuteso1/Documents/GitHub/CKSOA/My project/Final_Mobile/backend"
./start-backend.sh
```

Script này sẽ:
- Tự động kiểm tra và khởi động MongoDB nếu chưa chạy
- Kiểm tra và tạo file .env nếu chưa có
- Cài đặt dependencies nếu cần
- Khởi động Node.js server

### Cách 2: Khởi Động Thủ Công

#### Bước 1: Khởi động MongoDB
```bash
# Kiểm tra MongoDB đã chạy chưa
lsof -i :27017

# Nếu chưa chạy, khởi động MongoDB
mongod --dbpath /opt/homebrew/var/mongodb --logpath /opt/homebrew/var/log/mongodb/mongo.log --fork
```

#### Bước 2: Khởi động Node.js Server
```bash
cd "/Users/tsangcuteso1/Documents/GitHub/CKSOA/My project/Final_Mobile/backend"
node server.js
```

## 🧪 Kiểm Tra Server Đang Hoạt Động

### Test Health Endpoint
```bash
curl http://localhost:8000/health
```

Kết quả mong đợi:
```json
{
  "status": "OK",
  "timestamp": "2024-...",
  "message": "Banking API is running!",
  "database": "MongoDB connected"
}
```

### Test API Endpoint
```bash
curl http://localhost:8000/api/test
```

## 📱 Kết Nối Từ Android App

### Android Emulator
App đã được cấu hình để kết nối đến:
```
http://10.0.2.2:8000/api/
```

**Lưu ý:** `10.0.2.2` là địa chỉ đặc biệt của Android Emulator để kết nối đến localhost của máy host.

### Real Device
Nếu dùng thiết bị thật, cần:

1. **Tìm IP máy tính:**
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

2. **Cập nhật ApiConfig.java:**
```java
// Comment dòng emulator
// public static final String BASE_URL = "http://10.0.2.2:8000/api/";

// Uncomment và thay IP của bạn
public static final String BASE_URL = "http://192.168.1.XXX:8000/api/";
```

3. **Đảm bảo Android và máy tính cùng mạng Wi-Fi**

## 🔐 Tài Khoản Test

Sau khi chạy `node seed.js`, bạn có thể đăng nhập với:

### Admin Account
- **Email:** `admin@bank.com`
- **Password:** `123456`
- **Type:** BANK_OFFICER

### Customer Accounts
- **Email:** `customer@example.com`
- **Password:** `123456`
- **Type:** CUSTOMER

## 🛑 Dừng Server

### Dừng Node.js Server
```bash
# Tìm process
lsof -i :8000

# Kill process
kill -9 <PID>
```

Hoặc nhấn `Ctrl+C` nếu đang chạy ở foreground.

### Dừng MongoDB
```bash
# Tìm process
lsof -i :27017

# Kill process
kill -9 <PID>
```

Hoặc:
```bash
pkill mongod
```

## 🐛 Troubleshooting

### Lỗi: "ECONNREFUSED 127.0.0.1:27017"
**Nguyên nhân:** MongoDB chưa chạy
**Giải pháp:** Khởi động MongoDB (xem Cách 2 - Bước 1)

### Lỗi: "EADDRINUSE: address already in use"
**Nguyên nhân:** Port 8000 đã được sử dụng
**Giải pháp:**
```bash
lsof -i :8000
kill -9 <PID>
```

### Lỗi: "Failed to connect to / 10.0.2.2:8000"
**Nguyên nhân:** Node.js server chưa chạy hoặc không lắng nghe đúng interface
**Giải pháp:**
1. Kiểm tra server đang chạy: `lsof -i :8000`
2. Đảm bảo HOST trong .env là `0.0.0.0` (không phải `localhost`)
3. Khởi động lại server

### Lỗi: "Cannot find module"
**Nguyên nhân:** Dependencies chưa được cài đặt
**Giải pháp:**
```bash
cd backend
npm install
```

## 📝 Logs

Server logs sẽ hiển thị trong terminal nơi bạn chạy `node server.js`.

Để xem MongoDB logs:
```bash
tail -f /opt/homebrew/var/log/mongodb/mongo.log
```

## 🔄 Tạo Dữ Liệu Test

Sau khi server chạy, tạo dữ liệu test:
```bash
cd backend
node seed.js
```

Hoặc tạo dữ liệu test mở rộng:
```bash
node test-data.js
```

---

**💡 Tip:** Giữ terminal chạy server mở để xem logs real-time. Nếu đóng terminal, server sẽ dừng.

