# 🚀 NODE.JS BANKING BACKEND SETUP

## 📁 PROJECT STRUCTURE
```
banking-backend-nodejs/
├── 📁 src/
│   ├── 📁 controllers/
│   │   ├── authController.js
│   │   ├── userController.js
│   │   ├── accountController.js
│   │   ├── transactionController.js
│   │   └── utilityController.js
│   ├── 📁 models/
│   │   ├── User.js
│   │   ├── Account.js
│   │   ├── Transaction.js
│   │   └── OtpCode.js
│   ├── 📁 middleware/
│   │   ├── auth.js
│   │   ├── validation.js
│   │   └── errorHandler.js
│   ├── 📁 routes/
│   │   ├── auth.js
│   │   ├── users.js
│   │   ├── accounts.js
│   │   ├── transactions.js
│   │   └── utilities.js
│   ├── 📁 utils/
│   │   ├── jwt.js
│   │   ├── otp.js
│   │   ├── sms.js
│   │   └── validators.js
│   ├── 📁 config/
│   │   ├── database.js
│   │   └── config.js
│   └── app.js
├── package.json
├── .env
├── .gitignore
└── server.js
```

## 📦 PACKAGE.JSON

```json
{
  "name": "banking-backend-nodejs",
  "version": "1.0.0",
  "description": "Banking API Backend with Node.js",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js",
    "test": "jest",
    "seed": "node src/utils/seeder.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "mongoose": "^7.6.3",
    "bcryptjs": "^2.4.3",
    "jsonwebtoken": "^9.0.2",
    "cors": "^2.8.5",
    "dotenv": "^16.3.1",
    "express-rate-limit": "^7.1.5",
    "express-validator": "^7.0.1",
    "helmet": "^7.1.0",
    "morgan": "^1.10.0",
    "nodemailer": "^6.9.7",
    "twilio": "^4.19.0",
    "crypto": "^1.0.1",
    "moment": "^2.29.4",
    "express-async-errors": "^3.1.1"
  },
  "devDependencies": {
    "nodemon": "^3.0.1",
    "jest": "^29.7.0",
    "supertest": "^6.3.3"
  },
  "keywords": [
    "banking",
    "api",
    "nodejs",
    "express",
    "mongodb",
    "jwt",
    "authentication"
  ],
  "author": "Banking Team",
  "license": "MIT"
}
```

## 🔧 SETUP COMMANDS

```bash
# Tạo project
mkdir banking-backend-nodejs
cd banking-backend-nodejs

# Initialize npm
npm init -y

# Install dependencies
npm install express mongoose bcryptjs jsonwebtoken cors dotenv express-rate-limit express-validator helmet morgan nodemailer twilio crypto moment express-async-errors

# Install dev dependencies  
npm install --save-dev nodemon jest supertest

# Tạo cấu trúc thư mục
mkdir -p src/{controllers,models,middleware,routes,utils,config}
```

## 🔑 ENVIRONMENT VARIABLES (.env)

```env
# Server Configuration
NODE_ENV=development
PORT=8000
HOST=0.0.0.0

# Database Configuration  
MONGODB_URI=mongodb://localhost:27017/banking_app
# For MongoDB Atlas: mongodb+srv://username:password@cluster.mongodb.net/banking_app

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-here-make-it-long-and-random
JWT_EXPIRE=24h
JWT_REFRESH_EXPIRE=7d

# CORS Configuration
CORS_ORIGIN=*
# For production: https://yourdomain.com

# Rate Limiting
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100

# OTP Configuration
OTP_EXPIRE_MINUTES=5
OTP_LENGTH=6

# SMS Configuration (Twilio)
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=your_twilio_phone_number

# Email Configuration
EMAIL_SERVICE=gmail
EMAIL_USER=your_email@gmail.com
EMAIL_PASS=your_app_password
EMAIL_FROM=Banking App <noreply@bankingapp.com>

# Security
BCRYPT_ROUNDS=12
```

## 🗄️ DATABASE MODELS

### User Model (src/models/User.js)
```javascript
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
  email: {
    type: String,
    required: true,
    unique: true,
    lowercase: true,
    trim: true
  },
  password: {
    type: String,
    required: true,
    minlength: 6
  },
  fullName: {
    type: String,
    required: true,
    trim: true
  },
  phone: {
    type: String,
    required: true,
    unique: true,
    trim: true
  },
  address: {
    type: String,
    trim: true
  },
  customerType: {
    type: String,
    enum: ['CUSTOMER', 'BANK_OFFICER'],
    default: 'CUSTOMER'
  },
  isActive: {
    type: Boolean,
    default: true
  },
  emailVerified: {
    type: Boolean,
    default: false
  },
  phoneVerified: {
    type: Boolean,
    default: false
  },
  lastLogin: {
    type: Date
  },
  refreshTokens: [{
    token: String,
    createdAt: {
      type: Date,
      default: Date.now,
      expires: '7d'
    }
  }]
}, {
  timestamps: true,
  toJSON: { virtuals: true },
  toObject: { virtuals: true }
});

// Virtual for accounts
userSchema.virtual('accounts', {
  ref: 'Account',
  localField: '_id',
  foreignField: 'userId'
});

// Hash password before saving
userSchema.pre('save', async function(next) {
  if (!this.isModified('password')) return next();
  
  this.password = await bcrypt.hash(this.password, parseInt(process.env.BCRYPT_ROUNDS) || 12);
  next();
});

// Compare password method
userSchema.methods.comparePassword = async function(candidatePassword) {
  return bcrypt.compare(candidatePassword, this.password);
};

// Check if user is bank officer
userSchema.methods.isBankOfficer = function() {
  return this.customerType === 'BANK_OFFICER';
};

// Get user's primary account
userSchema.methods.getPrimaryAccount = async function() {
  const Account = mongoose.model('Account');
  return await Account.findOne({ 
    userId: this._id, 
    accountType: 'CHECKING', 
    isActive: true 
  });
};

// Remove sensitive data
userSchema.methods.toJSON = function() {
  const user = this.toObject();
  delete user.password;
  delete user.refreshTokens;
  return user;
};

module.exports = mongoose.model('User', userSchema);
```

### Account Model (src/models/Account.js)
```javascript
const mongoose = require('mongoose');

const accountSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  accountNumber: {
    type: String,
    required: true,
    unique: true,
    length: 16
  },
  accountType: {
    type: String,
    enum: ['CHECKING', 'SAVING', 'MORTGAGE'],
    required: true
  },
  balance: {
    type: Number,
    default: 0,
    min: 0
  },
  interestRate: {
    type: Number,
    min: 0,
    max: 100
  },
  currency: {
    type: String,
    default: 'VND',
    enum: ['VND', 'USD', 'EUR']
  },
  isActive: {
    type: Boolean,
    default: true
  }
}, {
  timestamps: true
});

// Virtual for masked account number
accountSchema.virtual('maskedAccountNumber').get(function() {
  if (!this.accountNumber || this.accountNumber.length < 4) {
    return this.accountNumber;
  }
  const lastFour = this.accountNumber.slice(-4);
  return `**** **** **** ${lastFour}`;
});

// Virtual for formatted balance
accountSchema.virtual('formattedBalance').get(function() {
  return `${this.balance.toLocaleString()} ${this.currency}`;
});

// Methods
accountSchema.methods.canDebit = function(amount) {
  return this.isActive && this.balance >= amount;
};

accountSchema.methods.debit = function(amount) {
  if (!this.canDebit(amount)) {
    throw new Error('Insufficient balance or inactive account');
  }
  this.balance -= amount;
  return this.save();
};

accountSchema.methods.credit = function(amount) {
  if (!this.isActive) {
    throw new Error('Cannot credit to inactive account');
  }
  this.balance += amount;
  return this.save();
};

// Static method to generate account number
accountSchema.statics.generateAccountNumber = function() {
  const timestamp = Date.now().toString();
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  return (timestamp + random).slice(-16);
};

module.exports = mongoose.model('Account', accountSchema);
```

### Transaction Model (src/models/Transaction.js)
```javascript
const mongoose = require('mongoose');

const transactionSchema = new mongoose.Schema({
  fromAccountId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Account'
  },
  toAccountId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Account'
  },
  fromAccountNumber: {
    type: String
  },
  toAccountNumber: {
    type: String
  },
  amount: {
    type: Number,
    required: true,
    min: 0
  },
  currency: {
    type: String,
    default: 'VND',
    enum: ['VND', 'USD', 'EUR']
  },
  transactionType: {
    type: String,
    enum: ['TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT', 'TOPUP'],
    required: true
  },
  status: {
    type: String,
    enum: ['PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'],
    default: 'PENDING'
  },
  description: {
    type: String,
    trim: true
  },
  referenceNumber: {
    type: String,
    unique: true,
    required: true
  },
  otpRequired: {
    type: Boolean,
    default: false
  },
  otpVerified: {
    type: Boolean,
    default: false
  },
  completedAt: {
    type: Date
  },
  failureReason: {
    type: String
  }
}, {
  timestamps: true
});

// Virtual for formatted amount
transactionSchema.virtual('formattedAmount').get(function() {
  return `${this.amount.toLocaleString()} ${this.currency}`;
});

// Methods
transactionSchema.methods.isPending = function() {
  return this.status === 'PENDING';
};

transactionSchema.methods.isCompleted = function() {
  return this.status === 'COMPLETED';
};

transactionSchema.methods.complete = function() {
  this.status = 'COMPLETED';
  this.completedAt = new Date();
  return this.save();
};

transactionSchema.methods.fail = function(reason) {
  this.status = 'FAILED';
  this.failureReason = reason;
  return this.save();
};

// Static method to generate reference number
transactionSchema.statics.generateReferenceNumber = function() {
  return 'TXN' + Date.now() + Math.floor(Math.random() * 10000);
};

module.exports = mongoose.model('Transaction', transactionSchema);
```

### OTP Model (src/models/OtpCode.js)
```javascript
const mongoose = require('mongoose');
const crypto = require('crypto');

const otpSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  code: {
    type: String,
    required: true,
    length: 6
  },
  type: {
    type: String,
    enum: ['LOGIN', 'TRANSACTION', 'PASSWORD_RESET', 'PHONE_VERIFICATION'],
    required: true
  },
  referenceId: {
    type: String // Transaction ID or other reference
  },
  isUsed: {
    type: Boolean,
    default: false
  },
  expiresAt: {
    type: Date,
    required: true
  },
  attempts: {
    type: Number,
    default: 0,
    max: 3
  }
}, {
  timestamps: true
});

// Index for automatic expiration
otpSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

// Methods
otpSchema.methods.isExpired = function() {
  return new Date() > this.expiresAt;
};

otpSchema.methods.isValid = function() {
  return !this.isUsed && !this.isExpired() && this.attempts < 3;
};

otpSchema.methods.markAsUsed = function() {
  this.isUsed = true;
  return this.save();
};

otpSchema.methods.incrementAttempts = function() {
  this.attempts += 1;
  return this.save();
};

// Static method to generate OTP code
otpSchema.statics.generateCode = function() {
  return crypto.randomInt(100000, 999999).toString();
};

// Static method to create OTP
otpSchema.statics.createOtp = async function(userId, type, referenceId = null) {
  // Delete existing unused OTPs of same type
  await this.deleteMany({ 
    userId, 
    type, 
    isUsed: false 
  });

  const code = this.generateCode();
  const expiresAt = new Date(Date.now() + (parseInt(process.env.OTP_EXPIRE_MINUTES) || 5) * 60 * 1000);

  return this.create({
    userId,
    code,
    type,
    referenceId,
    expiresAt
  });
};

module.exports = mongoose.model('OtpCode', otpSchema);
```

## ⚙️ DATABASE CONNECTION (src/config/database.js)

```javascript
const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    const conn = await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });

    console.log(`✅ MongoDB Connected: ${conn.connection.host}`);
    
    // Handle connection events
    mongoose.connection.on('error', (err) => {
      console.error('❌ MongoDB connection error:', err);
    });

    mongoose.connection.on('disconnected', () => {
      console.log('⚠️  MongoDB disconnected');
    });

    process.on('SIGINT', async () => {
      await mongoose.connection.close();
      console.log('📴 MongoDB connection closed through app termination');
      process.exit(0);
    });

  } catch (error) {
    console.error('❌ Database connection failed:', error.message);
    process.exit(1);
  }
};

module.exports = connectDB;
```

## 🔧 MAIN CONFIG (src/config/config.js)

```javascript
module.exports = {
  port: process.env.PORT || 8000,
  host: process.env.HOST || '0.0.0.0',
  nodeEnv: process.env.NODE_ENV || 'development',
  
  jwt: {
    secret: process.env.JWT_SECRET || 'fallback-secret-key',
    expiresIn: process.env.JWT_EXPIRE || '24h',
    refreshExpiresIn: process.env.JWT_REFRESH_EXPIRE || '7d'
  },
  
  database: {
    uri: process.env.MONGODB_URI || 'mongodb://localhost:27017/banking_app'
  },
  
  cors: {
    origin: process.env.CORS_ORIGIN || '*',
    credentials: true
  },
  
  rateLimit: {
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000, // 15 minutes
    max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100
  },
  
  otp: {
    expireMinutes: parseInt(process.env.OTP_EXPIRE_MINUTES) || 5,
    length: parseInt(process.env.OTP_LENGTH) || 6
  },
  
  sms: {
    provider: 'twilio',
    twilio: {
      accountSid: process.env.TWILIO_ACCOUNT_SID,
      authToken: process.env.TWILIO_AUTH_TOKEN,
      phoneNumber: process.env.TWILIO_PHONE_NUMBER
    }
  },
  
  email: {
    service: process.env.EMAIL_SERVICE || 'gmail',
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS,
    from: process.env.EMAIL_FROM || 'Banking App <noreply@bankingapp.com>'
  },
  
  security: {
    bcryptRounds: parseInt(process.env.BCRYPT_ROUNDS) || 12
  }
};
```

## 🔗 JWT UTILITIES (src/utils/jwt.js)

```javascript
const jwt = require('jsonwebtoken');
const config = require('../config/config');

const generateTokens = (payload) => {
  const accessToken = jwt.sign(payload, config.jwt.secret, {
    expiresIn: config.jwt.expiresIn
  });
  
  const refreshToken = jwt.sign(payload, config.jwt.secret, {
    expiresIn: config.jwt.refreshExpiresIn
  });
  
  return { accessToken, refreshToken };
};

const verifyToken = (token) => {
  return jwt.verify(token, config.jwt.secret);
};

const decodeToken = (token) => {
  return jwt.decode(token);
};

module.exports = {
  generateTokens,
  verifyToken,
  decodeToken
};
```

## 📱 OTP UTILITIES (src/utils/otp.js)

```javascript
const nodemailer = require('nodemailer');
const twilio = require('twilio');
const config = require('../config/config');

// Email transporter
const emailTransporter = nodemailer.createTransporter({
  service: config.email.service,
  auth: {
    user: config.email.user,
    pass: config.email.pass
  }
});

// Twilio client
const twilioClient = config.sms.twilio.accountSid && config.sms.twilio.authToken 
  ? twilio(config.sms.twilio.accountSid, config.sms.twilio.authToken)
  : null;

const sendOtpSms = async (phone, code) => {
  if (!twilioClient) {
    console.log(`📱 SMS OTP (Dev Mode): ${code} for ${phone}`);
    return;
  }

  try {
    await twilioClient.messages.create({
      body: `Mã OTP của bạn là: ${code}. Có hiệu lực trong ${config.otp.expireMinutes} phút.`,
      from: config.sms.twilio.phoneNumber,
      to: phone
    });
    console.log(`📱 SMS OTP sent to ${phone}`);
  } catch (error) {
    console.error('❌ SMS OTP send failed:', error.message);
    throw new Error('Failed to send SMS OTP');
  }
};

const sendOtpEmail = async (email, code, type = 'verification') => {
  const subject = type === 'login' ? 'Mã OTP Đăng Nhập' : 'Mã OTP Xác Thực';
  const html = `
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
      <h2 style="color: #333;">Banking App - ${subject}</h2>
      <p>Mã OTP của bạn là:</p>
      <div style="background: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0;">
        <h1 style="color: #007bff; font-size: 32px; margin: 0;">${code}</h1>
      </div>
      <p><strong>Lưu ý:</strong> Mã này có hiệu lực trong ${config.otp.expireMinutes} phút.</p>
      <p>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>
    </div>
  `;

  try {
    await emailTransporter.sendMail({
      from: config.email.from,
      to: email,
      subject,
      html
    });
    console.log(`📧 Email OTP sent to ${email}`);
  } catch (error) {
    console.error('❌ Email OTP send failed:', error.message);
    throw new Error('Failed to send email OTP');
  }
};

module.exports = {
  sendOtpSms,
  sendOtpEmail
};
```

## 🛡️ MIDDLEWARE

### Authentication Middleware (src/middleware/auth.js)
```javascript
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const config = require('../config/config');

const authenticate = async (req, res, next) => {
  try {
    const authHeader = req.header('Authorization');
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'Access token is required'
      });
    }

    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, config.jwt.secret);
    
    const user = await User.findById(decoded.userId);
    if (!user || !user.isActive) {
      return res.status(401).json({
        success: false,
        message: 'Invalid token or user not found'
      });
    }

    req.user = user;
    req.userId = user._id;
    next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({
        success: false,
        message: 'Token expired'
      });
    }
    
    return res.status(401).json({
      success: false,
      message: 'Invalid token'
    });
  }
};

const requireBankOfficer = (req, res, next) => {
  if (!req.user.isBankOfficer()) {
    return res.status(403).json({
      success: false,
      message: 'Bank officer access required'
    });
  }
  next();
};

module.exports = {
  authenticate,
  requireBankOfficer
};
```

### Error Handler Middleware (src/middleware/errorHandler.js)
```javascript
const errorHandler = (err, req, res, next) => {
  console.error('❌ Error:', err);

  // Mongoose validation error
  if (err.name === 'ValidationError') {
    const errors = Object.values(err.errors).map(e => e.message);
    return res.status(400).json({
      success: false,
      message: 'Validation Error',
      errors
    });
  }

  // Mongoose duplicate key error
  if (err.code === 11000) {
    const field = Object.keys(err.keyValue)[0];
    return res.status(400).json({
      success: false,
      message: `${field} already exists`
    });
  }

  // JWT errors
  if (err.name === 'JsonWebTokenError') {
    return res.status(401).json({
      success: false,
      message: 'Invalid token'
    });
  }

  if (err.name === 'TokenExpiredError') {
    return res.status(401).json({
      success: false,
      message: 'Token expired'
    });
  }

  // Default error
  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal Server Error';

  res.status(statusCode).json({
    success: false,
    message,
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack })
  });
};

module.exports = errorHandler;
```

## 📝 VALIDATION UTILITIES (src/utils/validators.js)

```javascript
const { body, validationResult } = require('express-validator');

const handleValidationErrors = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(422).json({
      success: false,
      message: 'Validation failed',
      errors: errors.array()
    });
  }
  next();
};

// User validation rules
const validateLogin = [
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Valid email is required'),
  body('password')
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters'),
  handleValidationErrors
];

const validateRegister = [
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Valid email is required'),
  body('password')
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters'),
  body('fullName')
    .trim()
    .isLength({ min: 2 })
    .withMessage('Full name must be at least 2 characters'),
  body('phone')
    .isMobilePhone('vi-VN')
    .withMessage('Valid Vietnamese phone number is required'),
  handleValidationErrors
];

const validateTransfer = [
  body('toAccountNumber')
    .isLength({ min: 10, max: 16 })
    .isNumeric()
    .withMessage('Valid account number is required'),
  body('amount')
    .isFloat({ min: 10000 })
    .withMessage('Amount must be at least 10,000 VND'),
  body('description')
    .trim()
    .isLength({ min: 1, max: 255 })
    .withMessage('Description is required and must be less than 255 characters'),
  handleValidationErrors
];

const validateOtp = [
  body('otpCode')
    .isLength({ min: 6, max: 6 })
    .isNumeric()
    .withMessage('OTP must be 6 digits'),
  handleValidationErrors
];

module.exports = {
  validateLogin,
  validateRegister,
  validateTransfer,
  validateOtp,
  handleValidationErrors
};
```

Tiếp theo tôi sẽ tạo các Controllers và Routes cho Node.js...
