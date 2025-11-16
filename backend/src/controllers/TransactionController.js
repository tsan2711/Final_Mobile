const Transaction = require('../models/Transaction');
const Account = require('../models/Account');
const OtpCode = require('../models/OtpCode');
const OTPUtils = require('../utils/otp');
const { formatAccount, formatTransaction } = require('../utils/responseFormatter');

class TransactionController {
  // Initiate money transfer
  static async initiateTransfer(req, res) {
    try {
      const { fromAccountId, from_account_id, toAccountNumber, to_account_number, amount, description } = req.body;
      const userId = req.userId;

      const actualFromAccountId = fromAccountId || from_account_id;
      const actualToAccountNumber = toAccountNumber || to_account_number;
      const amountNumber = Number(amount);

      // Validation
      if (!actualToAccountNumber || Number.isNaN(amountNumber)) {
        return res.status(400).json({
          success: false,
          message: 'Vui lòng nhập đầy đủ số tài khoản nhận và số tiền'
        });
      }

      if (amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Số tiền phải lớn hơn 0'
        });
      }

      // Minimum transfer amount: 10,000 VND
      if (amountNumber < 10000) {
        return res.status(400).json({
          success: false,
          message: 'Số tiền chuyển tối thiểu là 10,000 VND'
        });
      }

      // Find from account (use provided ID or fallback to primary checking)
      let fromAccount = null;
      if (actualFromAccountId) {
        fromAccount = await Account.findOne({
          _id: actualFromAccountId,
          userId,
          isActive: true
        });
      } else {
        fromAccount = await Account.findOne({
          userId,
          accountType: 'CHECKING',
          isActive: true
        }).sort({ createdAt: 1 });
      }

      if (!fromAccount) {
        return res.status(404).json({
          success: false,
          message: 'Không tìm thấy tài khoản nguồn hợp lệ. Vui lòng kiểm tra lại hoặc tạo tài khoản thanh toán.'
        });
      }

      // Find to account
      const toAccount = await Account.findOne({
        accountNumber: actualToAccountNumber,
        isActive: true
      }).populate('userId', 'fullName email');

      if (!toAccount) {
        return res.status(404).json({
          success: false,
          message: 'Không tìm thấy tài khoản nhận. Vui lòng kiểm tra lại số tài khoản'
        });
      }

      // Check if same account
      if (fromAccount._id.toString() === toAccount._id.toString()) {
        return res.status(400).json({
          success: false,
          message: 'Không thể chuyển tiền đến cùng tài khoản'
        });
      }

      // Calculate fee and total amount
      const fee = Transaction.calculateFee(amountNumber, 'TRANSFER');
      const totalAmount = amountNumber + fee;

      // Check sufficient balance
      if (!fromAccount.canDebit(totalAmount)) {
        return res.status(400).json({
          success: false,
          message: `Số dư không đủ. Bạn cần ${totalAmount.toLocaleString()} VND (bao gồm phí ${fee.toLocaleString()} VND) nhưng số dư hiện tại là ${fromAccount.balance.toLocaleString()} VND`
        });
      }

      // Create transaction record
      const transactionId = Transaction.generateTransactionId();
      
      // Get recipient name safely
      const recipientName = toAccount.userId && toAccount.userId.fullName 
        ? toAccount.userId.fullName 
        : 'Unknown';
      
      const transaction = new Transaction({
        transactionId,
        fromAccountId: fromAccount._id,
        toAccountId: toAccount._id,
        fromAccountNumber: fromAccount.accountNumber,
        toAccountNumber: toAccount.accountNumber,
        amount: amountNumber,
        currency: fromAccount.currency,
        description: description || `Transfer to ${recipientName}`,
        transactionType: 'TRANSFER',
        status: 'PENDING',
        initiatedBy: userId,
        fee,
        totalAmount,
        metadata: {
          ipAddress: req.ip,
          userAgent: req.headers['user-agent']
        }
      });

      await transaction.save();

      // Get user email (from req.user or fetch from database)
      let userEmail = null;
      if (req.user && req.user.email) {
        userEmail = req.user.email;
      } else {
        // Fallback: fetch user from database
        const User = require('../models/User');
        const user = await User.findById(userId).select('email');
        userEmail = user ? user.email : null;
      }

      // Generate and send OTP
      const otp = OTPUtils.generateOTP(6);
      const otpHash = OTPUtils.hashOTP(otp);

      const otpCode = new OtpCode({
        userId,
        email: userEmail || 'unknown@example.com',
        otpHash,
        otpType: 'TRANSACTION',
        transactionId,
        expiresAt: OTPUtils.generateExpiryTime(5)
      });

      await otpCode.save();

      // In real app, send OTP via SMS/Email
      console.log(`🔐 TRANSACTION OTP for ${userEmail || 'user'}: ${otp}`);

      res.status(200).json({
        success: true,
        message: 'Transaction initiated. OTP sent for verification.',
        data: {
          otp_required: true,
          transaction_id: transactionId,
          transaction_reference: transactionId,
          from_account: formatAccount(fromAccount),
          to_account: {
            account_number: toAccount.maskedAccountNumber,
            owner_name: toAccount.userId && toAccount.userId.fullName 
              ? toAccount.userId.fullName 
              : 'Unknown'
          },
          amount: amountNumber,
          formatted_amount: transaction.formattedAmount,
          fee,
          formatted_fee: `${fee.toLocaleString()} ${fromAccount.currency}`,
          total_amount: totalAmount,
          formatted_total_amount: transaction.formattedTotalAmount,
          currency: fromAccount.currency,
          description: transaction.description,
          // FOR DEVELOPMENT ONLY - Remove in production
          development_otp: otp,
          developmentOTP: otp
        }
      });

    } catch (error) {
      console.error('Initiate transfer error:', error);
      console.error('Error stack:', error.stack);
      res.status(500).json({
        success: false,
        message: error.message || 'Failed to initiate transfer',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // Verify OTP and complete transfer
  static async verifyTransferOTP(req, res) {
    try {
      // Support both snake_case (Android) and camelCase formats
      const { transactionId, transaction_id, otpCode, otp_code } = req.body;
      const userId = req.userId;

      const actualTransactionId = (transactionId || transaction_id || '').trim();
      const actualOtpCode = (otpCode || otp_code || '').toString().trim();

      // Validation
      if (!actualTransactionId || !actualOtpCode) {
        return res.status(400).json({
          success: false,
          message: 'Transaction ID and OTP are required'
        });
      }

      // Find transaction
      const transaction = await Transaction.findOne({
        transactionId: actualTransactionId,
        initiatedBy: userId,
        status: 'PENDING'
      }).populate('fromAccountId toAccountId');

      if (!transaction) {
        return res.status(404).json({
          success: false,
          message: 'Transaction not found or already processed'
        });
      }

      // Find valid OTP
      const validOTP = await OtpCode.findValidOTP(userId, 'TRANSACTION', actualTransactionId);
      
      if (!validOTP) {
        console.log(`[OTP Verification] No valid OTP found for userId: ${userId}, transactionId: ${actualTransactionId}`);
        await transaction.markAsFailed('OTP expired or invalid');
        return res.status(401).json({
          success: false,
          message: 'Mã OTP không hợp lệ hoặc đã hết hạn'
        });
      }

      // Verify OTP (ensure OTP is string and trimmed)
      const cleanOtpCode = actualOtpCode.toString().trim();
      const isValidOTP = OTPUtils.verifyOTP(cleanOtpCode, validOTP.otpHash);
      
      if (!isValidOTP) {
        console.log(`[OTP Verification] Invalid OTP attempt for userId: ${userId}, transactionId: ${actualTransactionId}`);
        await validOTP.incrementAttempt();
        
        if (validOTP.attempts >= 2) { // Max 3 attempts total
          await transaction.markAsFailed('Too many OTP attempts');
        }
        
        return res.status(401).json({
          success: false,
          message: `Mã OTP không đúng. Còn lại ${3 - validOTP.attempts - 1} lần thử`,
          attemptsLeft: 3 - validOTP.attempts - 1
        });
      }
      
      console.log(`[OTP Verification] OTP verified successfully for userId: ${userId}, transactionId: ${actualTransactionId}`);

      // Mark OTP as used and transaction as verified
      await validOTP.markAsUsed();
      await transaction.markOtpVerified();
      await transaction.markAsProcessing();

      // Ensure accounts are populated
      if (!transaction.fromAccountId || !transaction.toAccountId) {
        await transaction.populate('fromAccountId toAccountId');
      }

      // Validate accounts exist
      if (!transaction.fromAccountId || !transaction.toAccountId) {
        await transaction.markAsFailed('Accounts not found');
        return res.status(404).json({
          success: false,
          message: 'Accounts not found'
        });
      }

      // Perform the actual transfer
      try {
        // Reload accounts from database to ensure we have latest balance
        const fromAccount = await Account.findById(transaction.fromAccountId._id);
        const toAccount = await Account.findById(transaction.toAccountId._id);

        if (!fromAccount || !toAccount) {
          await transaction.markAsFailed('Accounts not found');
          return res.status(404).json({
            success: false,
            message: 'Accounts not found'
          });
        }

        // Verify sufficient balance again (in case balance changed)
        if (!fromAccount.canDebit(transaction.totalAmount)) {
          await transaction.markAsFailed('Insufficient balance');
          return res.status(400).json({
            success: false,
            message: `Số dư không đủ. Bạn cần ${transaction.totalAmount.toLocaleString()} VND nhưng số dư hiện tại là ${fromAccount.balance.toLocaleString()} VND`
          });
        }

        // Debit from source account (this already calls save())
        await fromAccount.debit(transaction.totalAmount);
        
        // Credit to destination account (this already calls save())
        await toAccount.credit(transaction.amount);
        
        // Mark transaction as completed
        await transaction.markAsCompleted();

        // Reload transaction with updated account balances
        await transaction.populate('fromAccountId toAccountId');

        res.status(200).json({
          success: true,
          message: 'Transfer completed successfully',
          data: formatTransaction(transaction)
        });

      } catch (transferError) {
        console.error('Transfer execution error:', transferError);
        console.error('Transfer error stack:', transferError.stack);
        await transaction.markAsFailed('Transfer execution failed: ' + transferError.message);
        
        res.status(500).json({
          success: false,
          message: transferError.message || 'Transfer failed. Please try again.',
          error: process.env.NODE_ENV === 'development' ? transferError.message : undefined
        });
      }

    } catch (error) {
      console.error('Verify transfer OTP error:', error);
      console.error('Error stack:', error.stack);
      res.status(500).json({
        success: false,
        message: error.message || 'OTP verification failed',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // Get transaction history
  static async getTransactionHistory(req, res) {
    try {
      const userId = req.userId;
      const { 
        page = 1, 
        limit = 20, 
        accountId, 
        type,
        status,
        startDate,
        endDate 
      } = req.query;

      // Get user account IDs
      const userAccountIds = await TransactionController.getUserAccountIds(userId);
      
      // Build query
      const query = {
        $or: [
          { initiatedBy: userId },
          { 
            $and: [
              {
                $or: [
                  { fromAccountId: { $in: userAccountIds } },
                  { toAccountId: { $in: userAccountIds } }
                ]
              }
            ]
          }
        ]
      };

      // Add filters
      if (accountId) {
        query.$or = [
          { fromAccountId: accountId },
          { toAccountId: accountId }
        ];
      }

      if (type) {
        query.transactionType = type;
      }

      if (status) {
        query.status = status;
      }

      if (startDate || endDate) {
        query.createdAt = {};
        if (startDate) query.createdAt.$gte = new Date(startDate);
        if (endDate) query.createdAt.$lte = new Date(endDate);
      }

      // Execute query with pagination
      const skip = (page - 1) * limit;
      
      const [transactions, total] = await Promise.all([
        Transaction.find(query)
          .populate('fromAccountId', 'accountNumber accountType')
          .populate('toAccountId', 'accountNumber accountType')
          .populate('initiatedBy', 'fullName email')
          .sort({ createdAt: -1 })
          .skip(skip)
          .limit(parseInt(limit)),
        Transaction.countDocuments(query)
      ]);

      // Calculate pagination info
      const totalPages = Math.ceil(total / limit);

      res.status(200).json({
        success: true,
        message: 'Transaction history retrieved successfully',
        data: transactions.map(formatTransaction),
        meta: {
          pagination: {
            currentPage: parseInt(page),
            totalPages,
            totalTransactions: total,
            hasNextPage: page < totalPages,
            hasPrevPage: page > 1
          }
        }
      });

    } catch (error) {
      console.error('Get transaction history error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve transaction history'
      });
    }
  }

  // Get transaction details
  static async getTransaction(req, res) {
    try {
      const { transactionId } = req.params;
      const userId = req.userId;

      const userAccountIds = await TransactionController.getUserAccountIds(userId);
      
      const transaction = await Transaction.findOne({
        transactionId,
        $or: [
          { initiatedBy: userId },
          { 
            $and: [
              {
                $or: [
                  { fromAccountId: { $in: userAccountIds } },
                  { toAccountId: { $in: userAccountIds } }
                ]
              }
            ]
          }
        ]
      })
      .populate('fromAccountId', 'accountNumber accountType')
      .populate('toAccountId', 'accountNumber accountType')
      .populate('initiatedBy', 'fullName email');

      if (!transaction) {
        return res.status(404).json({
          success: false,
          message: 'Transaction not found'
        });
      }

      res.status(200).json({
        success: true,
        message: 'Transaction details retrieved successfully',
        data: formatTransaction(transaction)
      });

    } catch (error) {
      console.error('Get transaction error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve transaction details'
      });
    }
  }

  // Helper method to get user account IDs
  static async getUserAccountIds(userId) {
    const accounts = await Account.find({ userId, isActive: true }).select('_id');
    return accounts.map(acc => acc._id);
  }
}

module.exports = TransactionController;
