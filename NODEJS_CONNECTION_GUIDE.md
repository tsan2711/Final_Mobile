# 🚀 NODE.JS CONNECTION GUIDE

## 📋 STEP-BY-STEP SETUP NODEJS BACKEND

### ✅ ANDROID APP (ĐÃ SẴN SÀNG)
- [x] API Service layer architecture
- [x] Authentication với JWT support  
- [x] Session management với SharedPreferences
- [x] Network security config cho HTTP development
- [x] Business logic đầy đủ (Account, Transaction, User)

### 🟢 NODE.JS BACKEND SETUP

#### 1. 📦 **TẠO NODE.JS PROJECT**
```bash
# Tạo thư mục project
mkdir banking-backend-nodejs
cd banking-backend-nodejs

# Initialize npm project
npm init -y

# Install dependencies
npm install express mongoose bcryptjs jsonwebtoken cors dotenv express-rate-limit express-validator helmet morgan nodemailer twilio crypto moment express-async-errors

# Install dev dependencies
npm install --save-dev nodemon jest supertest

# Tạo cấu trúc thư mục
mkdir -p src/{controllers,models,middleware,routes,utils,config}
mkdir logs
```

#### 2. 🗄️ **SETUP MONGODB DATABASE**

**Option A: MongoDB Local**
```bash
# Install MongoDB (macOS)
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community

# Install MongoDB (Ubuntu)
sudo apt update
sudo apt install mongodb
sudo systemctl start mongodb
sudo systemctl enable mongodb

# Test connection
mongo
> show dbs
> exit
```

**Option B: MongoDB Atlas (Cloud)**
1. Đi đến [MongoDB Atlas](https://cloud.mongodb.com)
2. Tạo free cluster
3. Tạo database user
4. Whitelist IP (0.0.0.0/0 for development)
5. Copy connection string

#### 3. 🔧 **ENVIRONMENT SETUP**

Tạo file `.env`:
```env
# Server Configuration
NODE_ENV=development
PORT=8000
HOST=0.0.0.0

# Database Configuration
# Local MongoDB:
MONGODB_URI=mongodb://localhost:27017/banking_app

# MongoDB Atlas:
# MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/banking_app

# JWT Configuration  
JWT_SECRET=banking-app-super-secret-jwt-key-2024-secure
JWT_EXPIRE=24h
JWT_REFRESH_EXPIRE=7d

# CORS Configuration
CORS_ORIGIN=*

# Rate Limiting
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100

# OTP Configuration
OTP_EXPIRE_MINUTES=5
OTP_LENGTH=6

# SMS Configuration (Optional - Twilio)
# TWILIO_ACCOUNT_SID=your_twilio_sid
# TWILIO_AUTH_TOKEN=your_twilio_token  
# TWILIO_PHONE_NUMBER=your_twilio_phone

# Email Configuration (Optional - Gmail)
# EMAIL_SERVICE=gmail
# EMAIL_USER=your_email@gmail.com
# EMAIL_PASS=your_app_password
# EMAIL_FROM=Banking App <noreply@bankingapp.com>

# Security
BCRYPT_ROUNDS=12
```

#### 4. 📝 **COPY CODE FILES**

Copy tất cả code từ `NODEJS_BACKEND_SETUP.md` và `NODEJS_CONTROLLERS.md`:

**Tạo các files chính:**
```bash
# Models
touch src/models/User.js
touch src/models/Account.js  
touch src/models/Transaction.js
touch src/models/OtpCode.js

# Controllers
touch src/controllers/authController.js
touch src/controllers/userController.js
touch src/controllers/accountController.js
touch src/controllers/transactionController.js
touch src/controllers/utilityController.js

# Middleware
touch src/middleware/auth.js
touch src/middleware/errorHandler.js

# Routes
touch src/routes/auth.js
touch src/routes/users.js
touch src/routes/accounts.js
touch src/routes/transactions.js
touch src/routes/utilities.js

# Utils
touch src/utils/jwt.js
touch src/utils/otp.js
touch src/utils/validators.js

# Config
touch src/config/database.js
touch src/config/config.js

# Main files
touch src/app.js
touch server.js
```

#### 5. 📦 **UPDATE PACKAGE.JSON SCRIPTS**
```json
{
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js",
    "test": "jest",
    "seed": "node src/utils/seeder.js"
  }
}
```

#### 6. 🌱 **CREATE SEEDER (OPTIONAL)**

Tạo file `src/utils/seeder.js`:
```javascript
require('dotenv').config();
const mongoose = require('mongoose');
const User = require('../models/User');
const Account = require('../models/Account');
const connectDB = require('../config/database');

const seedData = async () => {
  try {
    await connectDB();
    
    // Clear existing data
    await User.deleteMany({});
    await Account.deleteMany({});
    
    console.log('🗑️  Cleared existing data');
    
    // Create test user
    const testUser = new User({
      email: 'test@example.com',
      password: 'password123',
      fullName: 'Nguyễn Văn Test',
      phone: '0987654321',
      address: '123 Test Street, Ho Chi Minh City',
      customerType: 'CUSTOMER',
      emailVerified: true,
      phoneVerified: true
    });
    
    await testUser.save();
    console.log('👤 Created test user');
    
    // Create test accounts
    const checkingAccount = new Account({
      userId: testUser._id,
      accountNumber: '1234567890123456',
      accountType: 'CHECKING',
      balance: 250000000, // 250M VND
      currency: 'VND'
    });
    
    const savingAccount = new Account({
      userId: testUser._id,
      accountNumber: '1234567890123457', 
      accountType: 'SAVING',
      balance: 150000000, // 150M VND
      interestRate: 6.5,
      currency: 'VND'
    });
    
    await checkingAccount.save();
    await savingAccount.save();
    console.log('🏦 Created test accounts');
    
    console.log('✅ Seeding completed!');
    console.log('📧 Test login: test@example.com');
    console.log('🔑 Test password: password123');
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Seeding failed:', error);
    process.exit(1);
  }
};

seedData();
```

#### 7. 🚀 **START NODE.JS SERVER**
```bash
# Development mode với auto-restart
npm run dev

# Production mode
npm start

# Seed database (optional)
npm run seed
```

Expected output:
```
✅ MongoDB Connected: localhost:27017
🚀 Server running on http://0.0.0.0:8000
📱 Environment: development
📊 API Documentation: http://0.0.0.0:8000/health
```

### 📱 ANDROID APP CONFIGURATION

#### 8. 🔧 **TÌM IP MÁY TÍNH**
```bash
# macOS/Linux
ifconfig | grep "inet " | grep -v 127.0.0.1

# Windows
ipconfig | findstr IPv4
```

Ví dụ: `192.168.1.100`

#### 9. ⚙️ **CẬP NHẬT ANDROID API CONFIG**

Trong file `ApiConfig.java`, thay đổi:
```java
// Option 1: Android Emulator → Node.js local
public static final String BASE_URL = "http://10.0.2.2:8000/api/";

// Option 2: Real Device → Node.js local (thay IP máy tính)
// public static final String BASE_URL = "http://192.168.1.100:8000/api/";

// Option 3: Production server
// public static final String BASE_URL = "https://yourdomain.com/api/";
```

### 🧪 TESTING CONNECTION

#### 10. ✅ **TEST NODE.JS API**
```bash
# Test health endpoint
curl http://localhost:8000/health

# Test register
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123", 
    "fullName": "Test User",
    "phone": "0987654321"
  }'

# Test login
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

#### 11. 📱 **TEST ANDROID CONNECTION**
1. Build Android app
2. Install trên device/emulator
3. Thử đăng nhập với credentials từ seeder
4. Check Node.js console logs

### 🔧 ANDROID VS NODE.JS MAPPING

#### **API Endpoints Compatibility:**
```
Android App Request     →  Node.js Route
POST /auth/login        →  POST /api/auth/login
POST /auth/verify-otp   →  POST /api/auth/verify-otp
GET /user/profile       →  GET /api/user/profile
GET /accounts           →  GET /api/accounts
POST /transactions/transfer → POST /api/transactions/transfer
```

#### **Response Format Consistency:**
```json
// Node.js response format (matches Android expectations)
{
  "success": true,
  "message": "Đăng nhập thành công", 
  "data": {
    "token": "jwt_token_here",
    "user": {
      "id": "user_id",
      "email": "user@example.com",
      "fullName": "User Name",
      "customerType": "CUSTOMER"
    }
  }
}
```

### 🐛 TROUBLESHOOTING

#### **Node.js Server Issues:**
```bash
# Check if MongoDB is running
mongosh # or mongo for older versions

# Check Node.js version (requires 14+)
node --version

# Check npm dependencies
npm install

# Clear npm cache
npm cache clean --force

# Check logs
tail -f logs/app.log # if you setup logging
```

#### **Android Connection Issues:**
1. ✅ Verify IP address is correct
2. ✅ Check `network_security_config.xml` includes your IP
3. ✅ Ensure Android and computer on same Wi-Fi
4. ✅ Test API with Postman/curl first
5. ✅ Check Android Logcat for errors

#### **MongoDB Issues:**
```bash
# Check MongoDB status
brew services list | grep mongodb # macOS
systemctl status mongodb # Linux

# Reset MongoDB data (if needed)
sudo rm -rf /data/db/*
mongod --dbpath /data/db --repair
```

#### **Common Errors & Solutions:**

**Error: "EADDRINUSE: address already in use"**
```bash
# Find process using port 8000
lsof -i :8000
# Kill process
kill -9 <PID>
```

**Error: "MongoServerError: Authentication failed"**
- Check MongoDB URI in .env
- Verify database credentials
- Try without authentication first

**Error: "JsonWebTokenError: invalid token"**
- Check JWT_SECRET in .env
- Verify token format in Android requests
- Check Bearer token prefix

### 🚀 PRODUCTION DEPLOYMENT

#### **Node.js Production Setup:**
```bash
# Install PM2 for process management
npm install -g pm2

# Create ecosystem file
touch ecosystem.config.js
```

**ecosystem.config.js:**
```javascript
module.exports = {
  apps: [{
    name: 'banking-api',
    script: 'server.js',
    instances: 'max',
    exec_mode: 'cluster',
    env: {
      NODE_ENV: 'production',
      PORT: 8000
    }
  }]
};
```

**Start in production:**
```bash
pm2 start ecosystem.config.js
pm2 save
pm2 startup
```

#### **Security for Production:**
1. ✅ Use HTTPS với SSL certificate
2. ✅ Set strong JWT_SECRET
3. ✅ Enable rate limiting
4. ✅ Use MongoDB Atlas với authentication
5. ✅ Setup monitoring (PM2, Datadog)
6. ✅ Enable logging
7. ✅ Update CORS_ORIGIN to your domain

### 📊 FINAL CHECKLIST

- [ ] Node.js server running on port 8000
- [ ] MongoDB connected (local or Atlas)
- [ ] Test endpoints working với curl
- [ ] Android `BASE_URL` updated với correct IP
- [ ] Network security config allows HTTP
- [ ] Both devices on same Wi-Fi
- [ ] Android app connects và shows real data
- [ ] Login flow works end-to-end
- [ ] Money transfer with OTP works
- [ ] All CRUD operations functional

### 🎯 EXPECTED FLOW

```
1. Android → POST /api/auth/login
2. Node.js → Generates OTP → Sends response
3. Android → Shows OTP dialog
4. User enters OTP → POST /api/auth/verify-otp  
5. Node.js → Validates OTP → Returns JWT token
6. Android → Stores token → Navigates to home
7. Android → GET /api/accounts (with Bearer token)
8. Node.js → Returns account data
9. Android → Displays real account info
10. Transfer money → OTP verification → Success
```

**🎉 Khi hoàn thành tất cả bước trên, bạn sẽ có hệ thống Banking hoàn chỉnh với Node.js backend!** 

**Node.js có ưu điểm: nhanh hơn, dễ setup hơn Laravel, và JavaScript ecosystem rất phong phú! 🚀**
