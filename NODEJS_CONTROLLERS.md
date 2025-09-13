# 🎮 NODE.JS CONTROLLERS - BANKING API

## 🔐 AUTH CONTROLLER (src/controllers/authController.js)

```javascript
const User = require('../models/User');
const OtpCode = require('../models/OtpCode');
const { generateTokens } = require('../utils/jwt');
const { sendOtpSms, sendOtpEmail } = require('../utils/otp');
const config = require('../config/config');

// Register User
const register = async (req, res) => {
  try {
    const { email, password, fullName, phone, address } = req.body;

    // Check if user already exists
    const existingUser = await User.findOne({ 
      $or: [{ email }, { phone }] 
    });

    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: 'Email hoặc số điện thoại đã được sử dụng'
      });
    }

    // Create user
    const user = new User({
      email,
      password,
      fullName,
      phone,
      address
    });

    await user.save();

    // Generate tokens
    const { accessToken, refreshToken } = generateTokens({ 
      userId: user._id,
      email: user.email 
    });

    // Save refresh token
    user.refreshTokens.push({ token: refreshToken });
    await user.save();

    res.status(201).json({
      success: true,
      message: 'Đăng ký thành công',
      data: {
        token: accessToken,
        refreshToken,
        user: {
          id: user._id,
          email: user.email,
          fullName: user.fullName,
          phone: user.phone,
          customerType: user.customerType
        }
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Login User
const login = async (req, res) => {
  try {
    const { email, password } = req.body;

    // Find user
    const user = await User.findOne({ email }).select('+password');
    if (!user || !await user.comparePassword(password)) {
      return res.status(401).json({
        success: false,
        message: 'Email hoặc mật khẩu không đúng'
      });
    }

    if (!user.isActive) {
      return res.status(403).json({
        success: false,
        message: 'Tài khoản đã bị khóa'
      });
    }

    // Check if OTP is required
    const requireOtp = true; // You can add logic here

    if (requireOtp) {
      // Generate OTP
      const otp = await OtpCode.createOtp(user._id, 'LOGIN');
      
      // Send OTP
      try {
        await sendOtpSms(user.phone, otp.code);
      } catch (smsError) {
        console.warn('SMS failed, sending email OTP:', smsError.message);
        await sendOtpEmail(user.email, otp.code, 'login');
      }

      // Generate temporary token
      const { accessToken } = generateTokens({ 
        userId: user._id,
        email: user.email,
        temporary: true 
      });

      return res.json({
        success: true,
        message: 'OTP đã được gửi đến số điện thoại của bạn',
        data: {
          otpRequired: true,
          token: accessToken,
          user: {
            id: user._id,
            email: user.email,
            fullName: user.fullName,
            phone: user.phone,
            customerType: user.customerType
          }
        }
      });
    }

    // Direct login without OTP
    const { accessToken, refreshToken } = generateTokens({ 
      userId: user._id,
      email: user.email 
    });

    // Save refresh token and update last login
    user.refreshTokens.push({ token: refreshToken });
    user.lastLogin = new Date();
    await user.save();

    res.json({
      success: true,
      message: 'Đăng nhập thành công',
      data: {
        token: accessToken,
        refreshToken,
        user: {
          id: user._id,
          email: user.email,
          fullName: user.fullName,
          phone: user.phone,
          customerType: user.customerType
        }
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Verify OTP
const verifyOtp = async (req, res) => {
  try {
    const { otpCode } = req.body;
    const user = req.user;

    // Find OTP
    const otp = await OtpCode.findOne({
      userId: user._id,
      code: otpCode,
      type: 'LOGIN',
      isUsed: false
    });

    if (!otp || !otp.isValid()) {
      if (otp) {
        await otp.incrementAttempts();
      }
      return res.status(400).json({
        success: false,
        message: 'Mã OTP không hợp lệ hoặc đã hết hạn'
      });
    }

    // Mark OTP as used
    await otp.markAsUsed();

    // Generate final tokens
    const { accessToken, refreshToken } = generateTokens({ 
      userId: user._id,
      email: user.email 
    });

    // Clean temporary tokens and save refresh token
    user.refreshTokens = user.refreshTokens.filter(rt => !rt.token.includes('temporary'));
    user.refreshTokens.push({ token: refreshToken });
    user.lastLogin = new Date();
    await user.save();

    res.json({
      success: true,
      message: 'Xác thực OTP thành công',
      data: {
        token: accessToken,
        refreshToken,
        user: {
          id: user._id,
          email: user.email,
          fullName: user.fullName,
          phone: user.phone,
          customerType: user.customerType
        }
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Send OTP
const sendOtp = async (req, res) => {
  try {
    const { phone, type = 'LOGIN' } = req.body;

    const user = await User.findOne({ phone });
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'Số điện thoại không tồn tại'
      });
    }

    // Generate OTP
    const otp = await OtpCode.createOtp(user._id, type);
    
    // Send OTP
    try {
      await sendOtpSms(user.phone, otp.code);
    } catch (smsError) {
      console.warn('SMS failed, sending email OTP:', smsError.message);
      await sendOtpEmail(user.email, otp.code);
    }

    res.json({
      success: true,
      message: 'OTP đã được gửi đến số điện thoại của bạn'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Logout
const logout = async (req, res) => {
  try {
    const user = req.user;
    const refreshToken = req.body.refreshToken;

    // Remove refresh token
    if (refreshToken) {
      user.refreshTokens = user.refreshTokens.filter(
        rt => rt.token !== refreshToken
      );
      await user.save();
    }

    res.json({
      success: true,
      message: 'Đăng xuất thành công'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Refresh Token
const refreshToken = async (req, res) => {
  try {
    const { refreshToken: token } = req.body;

    if (!token) {
      return res.status(401).json({
        success: false,
        message: 'Refresh token is required'
      });
    }

    const user = await User.findOne({ 
      'refreshTokens.token': token 
    });

    if (!user) {
      return res.status(401).json({
        success: false,
        message: 'Invalid refresh token'
      });
    }

    // Generate new tokens
    const { accessToken, refreshToken: newRefreshToken } = generateTokens({ 
      userId: user._id,
      email: user.email 
    });

    // Replace old refresh token
    user.refreshTokens = user.refreshTokens.filter(rt => rt.token !== token);
    user.refreshTokens.push({ token: newRefreshToken });
    await user.save();

    res.json({
      success: true,
      data: {
        token: accessToken,
        refreshToken: newRefreshToken
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

module.exports = {
  register,
  login,
  verifyOtp,
  sendOtp,
  logout,
  refreshToken
};
```

## 👤 USER CONTROLLER (src/controllers/userController.js)

```javascript
const User = require('../models/User');
const bcrypt = require('bcryptjs');

// Get User Profile
const getProfile = async (req, res) => {
  try {
    const user = await User.findById(req.userId).populate('accounts');
    
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    const primaryAccount = await user.getPrimaryAccount();

    res.json({
      success: true,
      data: {
        id: user._id,
        email: user.email,
        fullName: user.fullName,
        phone: user.phone,
        address: user.address,
        customerType: user.customerType,
        isActive: user.isActive,
        emailVerified: user.emailVerified,
        phoneVerified: user.phoneVerified,
        accountNumber: primaryAccount?.accountNumber,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Update User Profile
const updateProfile = async (req, res) => {
  try {
    const { fullName, phone, address } = req.body;
    const userId = req.userId;

    // Check if phone is already used by another user
    if (phone) {
      const existingUser = await User.findOne({ 
        phone, 
        _id: { $ne: userId } 
      });
      
      if (existingUser) {
        return res.status(400).json({
          success: false,
          message: 'Số điện thoại đã được sử dụng'
        });
      }
    }

    const user = await User.findByIdAndUpdate(
      userId,
      { 
        fullName, 
        phone, 
        address,
        updatedAt: new Date()
      },
      { new: true, runValidators: true }
    );

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      message: 'Cập nhật thông tin thành công',
      data: {
        id: user._id,
        email: user.email,
        fullName: user.fullName,
        phone: user.phone,
        address: user.address,
        customerType: user.customerType
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Change Password
const changePassword = async (req, res) => {
  try {
    const { currentPassword, newPassword } = req.body;
    const userId = req.userId;

    const user = await User.findById(userId).select('+password');
    
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // Verify current password
    const isValidPassword = await user.comparePassword(currentPassword);
    if (!isValidPassword) {
      return res.status(400).json({
        success: false,
        message: 'Mật khẩu hiện tại không đúng'
      });
    }

    // Update password
    user.password = newPassword;
    user.refreshTokens = []; // Clear all refresh tokens
    await user.save();

    res.json({
      success: true,
      message: 'Đổi mật khẩu thành công. Vui lòng đăng nhập lại.'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

module.exports = {
  getProfile,
  updateProfile,
  changePassword
};
```

## 🏦 ACCOUNT CONTROLLER (src/controllers/accountController.js)

```javascript
const Account = require('../models/Account');
const User = require('../models/User');

// Get User Accounts
const getAccounts = async (req, res) => {
  try {
    const userId = req.userId;
    
    const accounts = await Account.find({ 
      userId, 
      isActive: true 
    }).sort({ accountType: 1 });

    const formattedAccounts = accounts.map(account => ({
      id: account._id,
      userId: account.userId,
      accountNumber: account.accountNumber,
      accountType: account.accountType,
      balance: account.balance.toString(),
      interestRate: account.interestRate,
      currency: account.currency,
      isActive: account.isActive,
      maskedAccountNumber: account.maskedAccountNumber,
      formattedBalance: account.formattedBalance,
      createdAt: account.createdAt,
      updatedAt: account.updatedAt
    }));

    res.json({
      success: true,
      data: formattedAccounts
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Get Account Balance
const getAccountBalance = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.userId;

    const account = await Account.findOne({ 
      _id: id, 
      userId, 
      isActive: true 
    });

    if (!account) {
      return res.status(404).json({
        success: false,
        message: 'Tài khoản không tồn tại'
      });
    }

    res.json({
      success: true,
      data: {
        balance: account.balance.toString(),
        currency: account.currency,
        accountNumber: account.maskedAccountNumber,
        formattedBalance: account.formattedBalance
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Get Account Details
const getAccountDetails = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.userId;

    const account = await Account.findOne({ 
      _id: id, 
      userId 
    });

    if (!account) {
      return res.status(404).json({
        success: false,
        message: 'Tài khoản không tồn tại'
      });
    }

    res.json({
      success: true,
      data: {
        id: account._id,
        accountNumber: account.accountNumber,
        accountType: account.accountType,
        balance: account.balance.toString(),
        interestRate: account.interestRate,
        currency: account.currency,
        isActive: account.isActive,
        formattedBalance: account.formattedBalance,
        createdAt: account.createdAt.toLocaleDateString('vi-VN'),
        updatedAt: account.updatedAt.toLocaleDateString('vi-VN')
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Create Account (for bank officers)
const createAccount = async (req, res) => {
  try {
    const { userId, accountType, initialBalance = 0 } = req.body;

    // Verify user exists
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // Check if user already has this account type
    const existingAccount = await Account.findOne({ 
      userId, 
      accountType 
    });

    if (existingAccount) {
      return res.status(400).json({
        success: false,
        message: 'User already has this account type'
      });
    }

    // Generate account number
    const accountNumber = Account.generateAccountNumber();

    const account = new Account({
      userId,
      accountNumber,
      accountType,
      balance: initialBalance,
      interestRate: accountType === 'SAVING' ? 6.5 : null
    });

    await account.save();

    res.status(201).json({
      success: true,
      message: 'Tài khoản được tạo thành công',
      data: {
        id: account._id,
        accountNumber: account.accountNumber,
        accountType: account.accountType,
        balance: account.balance.toString(),
        currency: account.currency
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

module.exports = {
  getAccounts,
  getAccountBalance,
  getAccountDetails,
  createAccount
};
```

## 💸 TRANSACTION CONTROLLER (src/controllers/transactionController.js)

```javascript
const Transaction = require('../models/Transaction');
const Account = require('../models/Account');
const OtpCode = require('../models/OtpCode');
const { sendOtpSms, sendOtpEmail } = require('../utils/otp');
const mongoose = require('mongoose');

// Get User Transactions
const getTransactions = async (req, res) => {
  try {
    const userId = req.userId;
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const skip = (page - 1) * limit;

    // Get user's account IDs
    const userAccounts = await Account.find({ userId }).select('_id');
    const accountIds = userAccounts.map(acc => acc._id);

    // Get transactions
    const transactions = await Transaction.find({
      $or: [
        { fromAccountId: { $in: accountIds } },
        { toAccountId: { $in: accountIds } }
      ]
    })
    .populate('fromAccountId', 'accountNumber accountType')
    .populate('toAccountId', 'accountNumber accountType')
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit);

    const total = await Transaction.countDocuments({
      $or: [
        { fromAccountId: { $in: accountIds } },
        { toAccountId: { $in: accountIds } }
      ]
    });

    const formattedTransactions = transactions.map(tx => ({
      id: tx._id,
      fromAccountId: tx.fromAccountId?._id,
      toAccountId: tx.toAccountId?._id,
      fromAccountNumber: tx.fromAccountNumber,
      toAccountNumber: tx.toAccountNumber,
      amount: tx.amount.toString(),
      currency: tx.currency,
      transactionType: tx.transactionType,
      status: tx.status,
      description: tx.description,
      referenceNumber: tx.referenceNumber,
      formattedAmount: tx.formattedAmount,
      createdAt: tx.createdAt,
      completedAt: tx.completedAt
    }));

    res.json({
      success: true,
      data: formattedTransactions,
      pagination: {
        currentPage: page,
        totalPages: Math.ceil(total / limit),
        totalItems: total,
        perPage: limit
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Transfer Money
const transferMoney = async (req, res) => {
  const session = await mongoose.startSession();
  
  try {
    session.startTransaction();
    
    const { toAccountNumber, amount, description } = req.body;
    const userId = req.userId;

    // Get sender's primary account
    const fromAccount = await Account.findOne({ 
      userId, 
      accountType: 'CHECKING', 
      isActive: true 
    }).session(session);

    if (!fromAccount) {
      await session.abortTransaction();
      return res.status(404).json({
        success: false,
        message: 'Không tìm thấy tài khoản nguồn'
      });
    }

    // Check balance
    if (!fromAccount.canDebit(amount)) {
      await session.abortTransaction();
      return res.status(400).json({
        success: false,
        message: 'Số dư không đủ'
      });
    }

    // Find destination account
    const toAccount = await Account.findOne({ 
      accountNumber: toAccountNumber, 
      isActive: true 
    }).session(session);

    if (!toAccount) {
      await session.abortTransaction();
      return res.status(404).json({
        success: false,
        message: 'Tài khoản nhận không tồn tại'
      });
    }

    // Check if transferring to same account
    if (fromAccount._id.equals(toAccount._id)) {
      await session.abortTransaction();
      return res.status(400).json({
        success: false,
        message: 'Không thể chuyển tiền đến cùng tài khoản'
      });
    }

    // Create transaction
    const transaction = new Transaction({
      fromAccountId: fromAccount._id,
      toAccountId: toAccount._id,
      fromAccountNumber: fromAccount.accountNumber,
      toAccountNumber: toAccount.accountNumber,
      amount,
      transactionType: 'TRANSFER',
      description,
      referenceNumber: Transaction.generateReferenceNumber()
    });

    await transaction.save({ session });

    // Check if OTP is required (for amounts >= 1M VND)
    const requireOtp = amount >= 1000000;

    if (requireOtp) {
      const user = req.user;
      const otp = await OtpCode.createOtp(user._id, 'TRANSACTION', transaction._id.toString());
      
      // Send OTP
      try {
        await sendOtpSms(user.phone, otp.code);
      } catch (smsError) {
        console.warn('SMS failed, sending email OTP:', smsError.message);
        await sendOtpEmail(user.email, otp.code, 'transaction');
      }

      await session.commitTransaction();

      return res.json({
        success: true,
        message: 'OTP đã được gửi để xác thực giao dịch',
        data: {
          otpRequired: true,
          transactionId: transaction._id,
          referenceNumber: transaction.referenceNumber,
          amount: transaction.formattedAmount
        }
      });
    }

    // Process transfer without OTP
    await processTransfer(transaction, fromAccount, toAccount, session);
    await session.commitTransaction();

    res.json({
      success: true,
      message: 'Chuyển tiền thành công',
      data: {
        id: transaction._id,
        referenceNumber: transaction.referenceNumber,
        amount: transaction.formattedAmount,
        status: transaction.status,
        completedAt: transaction.completedAt
      }
    });
  } catch (error) {
    await session.abortTransaction();
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  } finally {
    session.endSession();
  }
};

// Verify Transaction OTP
const verifyTransactionOtp = async (req, res) => {
  const session = await mongoose.startSession();
  
  try {
    session.startTransaction();
    
    const { transactionId, otpCode } = req.body;
    const userId = req.userId;

    // Find transaction
    const transaction = await Transaction.findById(transactionId).session(session);
    if (!transaction || !transaction.isPending()) {
      await session.abortTransaction();
      return res.status(400).json({
        success: false,
        message: 'Giao dịch không tồn tại hoặc đã được xử lý'
      });
    }

    // Verify OTP
    const otp = await OtpCode.findOne({
      userId,
      code: otpCode,
      type: 'TRANSACTION',
      referenceId: transactionId,
      isUsed: false
    }).session(session);

    if (!otp || !otp.isValid()) {
      if (otp) {
        await otp.incrementAttempts();
      }
      await session.abortTransaction();
      return res.status(400).json({
        success: false,
        message: 'Mã OTP không hợp lệ hoặc đã hết hạn'
      });
    }

    // Mark OTP as used
    await otp.markAsUsed();

    // Get accounts
    const fromAccount = await Account.findById(transaction.fromAccountId).session(session);
    const toAccount = await Account.findById(transaction.toAccountId).session(session);

    // Process transfer
    await processTransfer(transaction, fromAccount, toAccount, session);
    await session.commitTransaction();

    res.json({
      success: true,
      message: 'Giao dịch thành công',
      data: {
        id: transaction._id,
        referenceNumber: transaction.referenceNumber,
        amount: transaction.formattedAmount,
        status: transaction.status,
        completedAt: transaction.completedAt
      }
    });
  } catch (error) {
    await session.abortTransaction();
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  } finally {
    session.endSession();
  }
};

// Get Transaction Details
const getTransactionDetails = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.userId;

    // Get user's account IDs
    const userAccounts = await Account.find({ userId }).select('_id');
    const accountIds = userAccounts.map(acc => acc._id);

    const transaction = await Transaction.findOne({
      _id: id,
      $or: [
        { fromAccountId: { $in: accountIds } },
        { toAccountId: { $in: accountIds } }
      ]
    })
    .populate('fromAccountId', 'accountNumber accountType')
    .populate('toAccountId', 'accountNumber accountType');

    if (!transaction) {
      return res.status(404).json({
        success: false,
        message: 'Giao dịch không tồn tại'
      });
    }

    res.json({
      success: true,
      data: {
        id: transaction._id,
        fromAccountNumber: transaction.fromAccountNumber,
        toAccountNumber: transaction.toAccountNumber,
        amount: transaction.amount.toString(),
        currency: transaction.currency,
        transactionType: transaction.transactionType,
        status: transaction.status,
        description: transaction.description,
        referenceNumber: transaction.referenceNumber,
        formattedAmount: transaction.formattedAmount,
        createdAt: transaction.createdAt,
        completedAt: transaction.completedAt
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Helper function to process transfer
const processTransfer = async (transaction, fromAccount, toAccount, session) => {
  // Update account balances
  await fromAccount.debit(transaction.amount);
  await toAccount.credit(transaction.amount);
  
  // Save accounts
  await fromAccount.save({ session });
  await toAccount.save({ session });
  
  // Complete transaction
  await transaction.complete();
  await transaction.save({ session });
};

module.exports = {
  getTransactions,
  transferMoney,
  verifyTransactionOtp,
  getTransactionDetails
};
```

## 🔧 UTILITY CONTROLLER (src/controllers/utilityController.js)

```javascript
// Pay Bill
const payBill = async (req, res) => {
  try {
    const { billType, accountNumber, amount, description } = req.body;
    const userId = req.userId;

    // This is a simplified implementation
    // In real app, you would integrate with actual utility providers

    res.json({
      success: true,
      message: `Thanh toán ${billType} thành công`,
      data: {
        billType,
        accountNumber,
        amount: amount.toString(),
        referenceNumber: 'BILL' + Date.now(),
        status: 'COMPLETED'
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Mobile Topup
const mobileTopup = async (req, res) => {
  try {
    const { phoneNumber, amount, provider } = req.body;

    res.json({
      success: true,
      message: 'Nạp tiền điện thoại thành công',
      data: {
        phoneNumber,
        amount: amount.toString(),
        provider,
        referenceNumber: 'TOPUP' + Date.now(),
        status: 'COMPLETED'
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

// Get Branches
const getBranches = async (req, res) => {
  try {
    // Mock data - in real app, this would come from database
    const branches = [
      {
        id: '1',
        name: 'Chi nhánh Hồ Chí Minh',
        address: '123 Nguyễn Huệ, Quận 1, TP.HCM',
        phone: '0283823456',
        latitude: 10.7769,
        longitude: 106.7009
      },
      {
        id: '2', 
        name: 'Chi nhánh Hà Nội',
        address: '456 Hoàng Kiếm, Quận Hoàn Kiếm, Hà Nội',
        phone: '0243823456',
        latitude: 21.0285,
        longitude: 105.8542
      }
    ];

    res.json({
      success: true,
      data: branches
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Lỗi server: ' + error.message
    });
  }
};

module.exports = {
  payBill,
  mobileTopup,
  getBranches
};
```

## 📍 ROUTES SETUP

### Auth Routes (src/routes/auth.js)
```javascript
const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { authenticate } = require('../middleware/auth');
const { validateLogin, validateRegister, validateOtp } = require('../utils/validators');

router.post('/register', validateRegister, authController.register);
router.post('/login', validateLogin, authController.login);
router.post('/verify-otp', authenticate, validateOtp, authController.verifyOtp);
router.post('/send-otp', authController.sendOtp);
router.post('/logout', authenticate, authController.logout);
router.post('/refresh', authController.refreshToken);

module.exports = router;
```

### User Routes (src/routes/users.js)
```javascript
const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const { authenticate } = require('../middleware/auth');

router.get('/profile', authenticate, userController.getProfile);
router.put('/update', authenticate, userController.updateProfile);
router.put('/change-password', authenticate, userController.changePassword);

module.exports = router;
```

### Account Routes (src/routes/accounts.js)
```javascript
const express = require('express');
const router = express.Router();
const accountController = require('../controllers/accountController');
const { authenticate, requireBankOfficer } = require('../middleware/auth');

router.get('/', authenticate, accountController.getAccounts);
router.get('/:id', authenticate, accountController.getAccountDetails);
router.get('/:id/balance', authenticate, accountController.getAccountBalance);
router.post('/create', authenticate, requireBankOfficer, accountController.createAccount);

module.exports = router;
```

### Transaction Routes (src/routes/transactions.js)
```javascript
const express = require('express');
const router = express.Router();
const transactionController = require('../controllers/transactionController');
const { authenticate } = require('../middleware/auth');
const { validateTransfer, validateOtp } = require('../utils/validators');

router.get('/', authenticate, transactionController.getTransactions);
router.get('/:id', authenticate, transactionController.getTransactionDetails);
router.post('/transfer', authenticate, validateTransfer, transactionController.transferMoney);
router.post('/verify-otp', authenticate, validateOtp, transactionController.verifyTransactionOtp);

module.exports = router;
```

### Utility Routes (src/routes/utilities.js)
```javascript
const express = require('express');
const router = express.Router();
const utilityController = require('../controllers/utilityController');
const { authenticate } = require('../middleware/auth');

router.post('/pay-bill', authenticate, utilityController.payBill);
router.post('/topup', authenticate, utilityController.mobileTopup);
router.get('/branches', utilityController.getBranches);

module.exports = router;
```

## 🚀 MAIN APP FILE (src/app.js)

```javascript
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
require('express-async-errors');

const config = require('./config/config');
const errorHandler = require('./middleware/errorHandler');

// Import routes
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const accountRoutes = require('./routes/accounts');
const transactionRoutes = require('./routes/transactions');
const utilityRoutes = require('./routes/utilities');

const app = express();

// Security middleware
app.use(helmet());
app.use(cors(config.cors));

// Rate limiting
const limiter = rateLimit(config.rateLimit);
app.use('/api/', limiter);

// Logging
if (config.nodeEnv !== 'test') {
  app.use(morgan('combined'));
}

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Health check
app.get('/health', (req, res) => {
  res.json({ 
    status: 'OK', 
    timestamp: new Date().toISOString(),
    environment: config.nodeEnv
  });
});

// API routes
app.use('/api/auth', authRoutes);
app.use('/api/user', userRoutes);
app.use('/api/accounts', accountRoutes);
app.use('/api/transactions', transactionRoutes);
app.use('/api/utilities', utilityRoutes);

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({
    success: false,
    message: 'Route not found'
  });
});

// Error handler
app.use(errorHandler);

module.exports = app;
```

## 🌟 SERVER FILE (server.js)

```javascript
require('dotenv').config();
const app = require('./src/app');
const connectDB = require('./src/config/database');
const config = require('./src/config/config');

// Connect to database
connectDB();

// Start server
const server = app.listen(config.port, config.host, () => {
  console.log(`🚀 Server running on http://${config.host}:${config.port}`);
  console.log(`📱 Environment: ${config.nodeEnv}`);
  console.log(`📊 API Documentation: http://${config.host}:${config.port}/health`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('👋 SIGTERM received, shutting down gracefully');
  server.close(() => {
    console.log('🔌 Process terminated');
  });
});

process.on('uncaughtException', (err) => {
  console.error('💥 Uncaught Exception:', err);
  process.exit(1);
});

process.on('unhandledRejection', (err) => {
  console.error('💥 Unhandled Rejection:', err);
  server.close(() => {
    process.exit(1);
  });
});
```

Node.js backend đã hoàn thành! Tiếp theo sẽ là hướng dẫn setup và test kết nối với Android app. 🎉
