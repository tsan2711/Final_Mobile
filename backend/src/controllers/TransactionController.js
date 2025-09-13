const Transaction = require('../models/Transaction');
const Account = require('../models/Account');
const OtpCode = require('../models/OtpCode');
const OTPUtils = require('../utils/otp');

class TransactionController {
  // Initiate money transfer
  static async initiateTransfer(req, res) {
    try {
      const { fromAccountId, toAccountNumber, amount, description } = req.body;
      const userId = req.userId;

      // Validation
      if (!fromAccountId || !toAccountNumber || !amount) {
        return res.status(400).json({
          success: false,
          message: 'From account, to account number, and amount are required'
        });
      }

      if (amount <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Amount must be greater than 0'
        });
      }

      // Find from account
      const fromAccount = await Account.findOne({
        _id: fromAccountId,
        userId,
        isActive: true
      });

      if (!fromAccount) {
        return res.status(404).json({
          success: false,
          message: 'Source account not found'
        });
      }

      // Find to account
      const toAccount = await Account.findOne({
        accountNumber: toAccountNumber,
        isActive: true
      }).populate('userId', 'fullName email');

      if (!toAccount) {
        return res.status(404).json({
          success: false,
          message: 'Destination account not found'
        });
      }

      // Check if same account
      if (fromAccount._id.toString() === toAccount._id.toString()) {
        return res.status(400).json({
          success: false,
          message: 'Cannot transfer to the same account'
        });
      }

      // Calculate fee and total amount
      const fee = Transaction.calculateFee(amount, 'TRANSFER');
      const totalAmount = amount + fee;

      // Check sufficient balance
      if (!fromAccount.canDebit(totalAmount)) {
        return res.status(400).json({
          success: false,
          message: 'Insufficient balance'
        });
      }

      // Create transaction record
      const transactionId = Transaction.generateTransactionId();
      
      const transaction = new Transaction({
        transactionId,
        fromAccountId: fromAccount._id,
        toAccountId: toAccount._id,
        fromAccountNumber: fromAccount.accountNumber,
        toAccountNumber: toAccount.accountNumber,
        amount,
        currency: fromAccount.currency,
        description: description || `Transfer to ${toAccount.userId.fullName}`,
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

      // Generate and send OTP
      const otp = OTPUtils.generateOTP(6);
      const otpHash = OTPUtils.hashOTP(otp);

      const otpCode = new OtpCode({
        userId,
        email: req.user.email,
        otpHash,
        otpType: 'TRANSACTION',
        transactionId,
        expiresAt: OTPUtils.generateExpiryTime(5)
      });

      await otpCode.save();

      // In real app, send OTP via SMS/Email
      console.log(`🔐 TRANSACTION OTP for ${req.user.email}: ${otp}`);

      res.status(200).json({
        success: true,
        message: 'Transaction initiated. OTP sent for verification.',
        data: {
          transactionId,
          fromAccount: {
            accountNumber: fromAccount.maskedAccountNumber,
            balance: fromAccount.formattedBalance
          },
          toAccount: {
            accountNumber: toAccount.maskedAccountNumber,
            ownerName: toAccount.userId.fullName
          },
          amount: `${amount.toLocaleString()} ${fromAccount.currency}`,
          fee: `${fee.toLocaleString()} ${fromAccount.currency}`,
          totalAmount: `${totalAmount.toLocaleString()} ${fromAccount.currency}`,
          description: transaction.description,
          // FOR DEVELOPMENT ONLY - Remove in production
          developmentOTP: otp
        }
      });

    } catch (error) {
      console.error('Initiate transfer error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to initiate transfer'
      });
    }
  }

  // Verify OTP and complete transfer
  static async verifyTransferOTP(req, res) {
    try {
      const { transactionId, otpCode } = req.body;
      const userId = req.userId;

      // Validation
      if (!transactionId || !otpCode) {
        return res.status(400).json({
          success: false,
          message: 'Transaction ID and OTP are required'
        });
      }

      // Find transaction
      const transaction = await Transaction.findOne({
        transactionId,
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
      const validOTP = await OtpCode.findValidOTP(userId, 'TRANSACTION', transactionId);
      
      if (!validOTP) {
        await transaction.markAsFailed('OTP expired or invalid');
        return res.status(401).json({
          success: false,
          message: 'Invalid or expired OTP'
        });
      }

      // Verify OTP
      const isValidOTP = OTPUtils.verifyOTP(otpCode, validOTP.otpHash);
      
      if (!isValidOTP) {
        await validOTP.incrementAttempt();
        
        if (validOTP.attempts >= 2) { // Max 3 attempts total
          await transaction.markAsFailed('Too many OTP attempts');
        }
        
        return res.status(401).json({
          success: false,
          message: 'Invalid OTP',
          attemptsLeft: 3 - validOTP.attempts - 1
        });
      }

      // Mark OTP as used and transaction as verified
      await validOTP.markAsUsed();
      await transaction.markOtpVerified();
      await transaction.markAsProcessing();

      // Perform the actual transfer
      try {
        // Debit from source account
        await transaction.fromAccountId.debit(transaction.totalAmount);
        
        // Credit to destination account
        await transaction.toAccountId.credit(transaction.amount);
        
        // Mark transaction as completed
        await transaction.markAsCompleted();

        // Reload accounts for updated balances
        await transaction.fromAccountId.reload();
        await transaction.toAccountId.reload();

        res.status(200).json({
          success: true,
          message: 'Transfer completed successfully',
          data: {
            transactionId: transaction.transactionId,
            status: transaction.status,
            amount: transaction.formattedAmount,
            fee: `${transaction.fee.toLocaleString()} ${transaction.currency}`,
            totalAmount: transaction.formattedTotalAmount,
            fromAccount: {
              accountNumber: transaction.fromAccountId.maskedAccountNumber,
              newBalance: transaction.fromAccountId.formattedBalance
            },
            toAccount: {
              accountNumber: transaction.toAccountId.maskedAccountNumber
            },
            processedAt: transaction.processedAt,
            description: transaction.description
          }
        });

      } catch (transferError) {
        console.error('Transfer execution error:', transferError);
        await transaction.markAsFailed('Transfer execution failed');
        
        res.status(500).json({
          success: false,
          message: 'Transfer failed. Please try again.'
        });
      }

    } catch (error) {
      console.error('Verify transfer OTP error:', error);
      res.status(500).json({
        success: false,
        message: 'OTP verification failed'
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

      // Build query
      const query = {
        $or: [
          { initiatedBy: userId },
          { 
            $and: [
              {
                $or: [
                  { fromAccountId: { $in: await this.getUserAccountIds(userId) } },
                  { toAccountId: { $in: await this.getUserAccountIds(userId) } }
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
        data: {
          transactions,
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

      const transaction = await Transaction.findOne({
        transactionId,
        $or: [
          { initiatedBy: userId },
          { 
            $and: [
              {
                $or: [
                  { fromAccountId: { $in: await this.getUserAccountIds(userId) } },
                  { toAccountId: { $in: await this.getUserAccountIds(userId) } }
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
        data: {
          transaction
        }
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
