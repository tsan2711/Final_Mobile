# 🧪 COMPREHENSIVE TESTING GUIDE

## 🎯 **TESTING OBJECTIVES**

1. **Verify database operations** với nhiều loại dữ liệu
2. **Test API endpoints** với real scenarios
3. **Confirm Android app integration** với backend
4. **Validate production readiness** của toàn bộ hệ thống

---

## 📋 **TESTING PROCEDURE**

### **STEP 1: Start Backend Server**

```bash
# Navigate to backend directory
cd /Users/tsangcuteso1/Documents/GitHub/gamedohoagameplaydinhnhathemattroi/Final_Mobile/backend

# Install axios for testing (if not already installed)
npm install axios

# Start MongoDB (if not running)
# Make sure MongoDB Compass is connected

# Start the backend server
node server.js
```

**Expected Output:**
```
🚀 Server running on http://0.0.0.0:8000
📱 Android app can connect to: http://YOUR_IP:8000/api/
🔗 Health check: http://0.0.0.0:8000/health
✅ MongoDB Connected
```

### **STEP 2: Create Extensive Test Data**

```bash
# Create comprehensive test data
node test-data.js
```

**Expected Output:**
```
✅ MongoDB Connected for extensive testing
🔄 Adding more test data (keeping existing)...
👥 Created additional users: 5
🏦 Created additional accounts: X
💸 Created sample transactions: 15

📊 COMPREHENSIVE TEST DATA CREATED:
==================================================
👥 Total Users: X
🏦 Total Accounts: X
💸 Total Transactions: X
💰 Total Money in System: XXX,XXX VND
```

### **STEP 3: Run API Comprehensive Tests**

```bash
# Run full API test suite
node api-test.js
```

**Expected Output:**
```
🧪 COMPREHENSIVE API TESTING STARTED
==================================================

🔍 Testing Health Check...
Health Check: ✅
API Test: ✅

🔐 Testing Authentication Flow...
👤 Testing customer@example.com:
   Login OTP: ✅ (OTP: XXXXXX)
   OTP Verify: ✅
   User: Nguyen Van A (CUSTOMER)

🏦 Testing Account Management...
💳 Testing accounts for customer@example.com:
   Get Accounts: ✅ (2 accounts)
   Total Balance: X,XXX,XXX VND
   Account Summary: ✅
   Primary Account: ✅

💸 Testing Money Transfer...
💰 Transfer: customer@example.com → user2@example.com
   Initiate Transfer: ✅ (TXN: TXNXXXXXXXX)
   Transfer OTP: XXXXXX
   Complete Transfer: ✅

📋 Testing Transaction History...
📜 History for customer@example.com:
   Transaction History: ✅ (X transactions)

⚠️  Testing Error Handling...
   Invalid Login: ✅
   Unauthorized Access: ✅

📊 TEST SUMMARY
==============================
✅ Passed: 6/6
🎉 ALL TESTS PASSED! API is production ready!
```

### **STEP 4: Test Android App Connection**

#### **Option A: Using Android Emulator**

```bash
# Build Android app
cd /Users/tsangcuteso1/Documents/GitHub/gamedohoagameplaydinhnhathemattroi/Final_Mobile
./gradlew assembleDebug

# Install on emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Test Credentials:**
- Email: `customer@example.com`
- Password: `123456`
- OTP: *Check backend terminal logs*

#### **Option B: Using Real Device**

1. **Find your computer's IP address:**
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

2. **Update ApiConfig.java:**
```java
// Comment out emulator URL
// public static final String BASE_URL = "http://10.0.2.2:8000/api/";

// Uncomment and update with your IP
public static final String BASE_URL = "http://YOUR_REAL_IP:8000/api/";
```

3. **Rebuild and install:**
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 **DETAILED TEST SCENARIOS**

### **1. Authentication Flow Testing**

#### **Test Case: Successful Login**
1. Open Android app
2. Enter email: `customer@example.com`
3. Enter password: `123456`
4. Tap "Đăng nhập"
5. **Expected:** OTP dialog appears
6. Check backend terminal for OTP
7. Enter OTP in app
8. **Expected:** Navigate to Main Activity

#### **Test Case: Invalid Credentials**
1. Enter wrong email/password
2. **Expected:** Error message displayed

#### **Test Case: Wrong OTP**
1. Enter correct credentials
2. Enter wrong OTP
3. **Expected:** Error message với attempts left

### **2. Account Management Testing**

#### **Test Case: View Account Summary**
1. Login successfully
2. Navigate to Home tab
3. **Expected:** 
   - Account balances displayed
   - Account numbers masked
   - Total balance calculated correctly

#### **Test Case: Account Details**
1. In Home fragment
2. Tap on account card (if implemented)
3. **Expected:** Detailed account information

### **3. Money Transfer Testing**

#### **Test Case: Successful Transfer**
1. Navigate to Transaction tab
2. Tap on transfer option
3. Enter account number: `7577590065039952` (from test data)
4. Enter amount: `100000`
5. Enter description: `Test transfer`
6. **Expected:** OTP dialog
7. Enter OTP from backend logs
8. **Expected:** Success message và updated balance

#### **Test Case: Insufficient Balance**
1. Enter transfer amount > account balance
2. **Expected:** Error message

#### **Test Case: Invalid Account Number**
1. Enter non-existent account number
2. **Expected:** Account not found error

### **4. Transaction History Testing**

#### **Test Case: View Transaction History**
1. Navigate to Transaction tab
2. View transaction list
3. **Expected:** List of recent transactions
4. Tap on transaction (if implemented)
5. **Expected:** Transaction details

### **5. Profile Management Testing**

#### **Test Case: View Profile**
1. Navigate to Profile tab
2. **Expected:** User information displayed

#### **Test Case: Update Profile**
1. Tap edit profile option
2. Update name/phone
3. **Expected:** Profile updated successfully

#### **Test Case: Logout**
1. Tap logout option
2. **Expected:** Return to login screen
3. **Expected:** Session cleared

---

## 📊 **DATABASE VERIFICATION**

### **MongoDB Compass Verification**

1. **Open MongoDB Compass**
2. **Connect to:** `mongodb://localhost:27017/banking_app`

3. **Verify Collections:**
   - `users` - Should have 8+ users
   - `accounts` - Should have 15+ accounts
   - `transactions` - Should have 15+ transactions
   - `otpcodes` - Should have recent OTP entries

4. **Sample Queries:**
```javascript
// Find all customers
db.users.find({customerType: "CUSTOMER"})

// Find accounts with high balance
db.accounts.find({balance: {$gte: 10000000}})

// Find recent transactions
db.transactions.find().sort({createdAt: -1}).limit(5)

// Find completed transfers
db.transactions.find({status: "COMPLETED", transactionType: "TRANSFER"})
```

---

## 🔍 **PERFORMANCE TESTING**

### **Load Testing với Multiple Users**

```bash
# Create script to test concurrent users
# This can be run manually by opening multiple terminal windows:

# Terminal 1:
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"123456"}'

# Terminal 2:
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user2@example.com","password":"123456"}'

# Terminal 3:
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"nguyen.vana@gmail.com","password":"123456"}'
```

### **Database Performance**
1. Monitor MongoDB connections in Compass
2. Check query execution times
3. Verify no connection leaks

---

## ✅ **SUCCESS CRITERIA**

### **Backend Success Indicators:**
- [ ] All API tests pass (6/6)
- [ ] MongoDB contains diverse test data
- [ ] No memory leaks or connection issues
- [ ] OTP generation và verification working
- [ ] Transaction processing completes successfully
- [ ] Error handling returns appropriate responses

### **Android Success Indicators:**
- [ ] App connects to backend successfully
- [ ] Authentication flow completes end-to-end
- [ ] Account data displays correctly
- [ ] Money transfer works với real backend
- [ ] Transaction history loads
- [ ] Error messages display appropriately
- [ ] Session management works (logout/re-login)

### **Integration Success Indicators:**
- [ ] Real money transfer between test accounts
- [ ] Balance updates reflected immediately
- [ ] Transaction history shows new transfers
- [ ] OTP verification works across platforms
- [ ] Multiple concurrent users supported

---

## 🚨 **TROUBLESHOOTING**

### **Common Issues & Solutions:**

#### **Backend Won't Start:**
```bash
# Check if MongoDB is running
brew services list | grep mongodb

# Start MongoDB if needed
brew services start mongodb/brew/mongodb-community

# Check port 8000 availability
lsof -i :8000
```

#### **Android Can't Connect:**
```bash
# Check firewall settings
# Allow port 8000 in macOS System Preferences > Security & Privacy > Firewall

# Test connectivity from device
# Open browser on device và navigate to: http://YOUR_IP:8000/health
```

#### **Database Connection Issues:**
```bash
# Restart MongoDB
brew services restart mongodb/brew/mongodb-community

# Check MongoDB logs
brew services info mongodb/brew/mongodb-community
```

#### **API Tests Fail:**
```bash
# Check server is running
curl http://localhost:8000/health

# Verify test data exists
node -e "
require('dotenv').config();
const mongoose = require('mongoose');
mongoose.connect(process.env.MONGODB_URI).then(async () => {
  const User = require('./src/models/User');
  const count = await User.countDocuments();
  console.log('Users in DB:', count);
  process.exit(0);
});
"
```

---

## 📈 **NEXT STEPS AFTER TESTING**

### **If All Tests Pass:**
1. ✅ **System is ready for beta deployment**
2. 🔒 **Implement additional security measures**
3. 📊 **Set up monitoring và logging**
4. 🚀 **Prepare for production deployment**

### **If Tests Fail:**
1. 🔍 **Debug specific failing components**
2. 🧪 **Fix issues và re-test**
3. 📝 **Update documentation**
4. 🔄 **Repeat testing cycle**

---

**🎯 GOAL: Achieve 100% test pass rate để confirm production readiness!**
