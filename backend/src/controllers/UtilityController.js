const Utility = require('../models/Utility');
const Account = require('../models/Account');
const OtpCode = require('../models/OtpCode');
const OTPUtils = require('../utils/otp');
const { formatUtility } = require('../utils/responseFormatter');

class UtilityController {
  // Pay electricity bill
  static async payElectricityBill(req, res) {
    try {
      const { accountId, customerNumber, amount, customerName, period } = req.body;
      const userId = req.userId;

      // Validate required fields
      if (!userId) {
        console.error('Pay electricity bill: userId is missing', { req: { userId: req.userId, user: req.user } });
        return res.status(401).json({
          success: false,
          message: 'User authentication required'
        });
      }

      if (!customerNumber || !amount) {
        return res.status(400).json({
          success: false,
          message: 'Customer number and amount are required'
        });
      }

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'ELECTRICITY', 'EVN', customerNumber, amount,
        `Thanh toán tiền điện ${period || ''}`,
        { customerName, period }
      );
    } catch (error) {
      console.error('Pay electricity bill error:', error);
      console.error('Error stack:', error.stack);
      console.error('Request body:', req.body);
      console.error('Request user:', req.user);
      console.error('Request userId:', req.userId);
      res.status(500).json({
        success: false,
        message: 'Failed to process electricity bill payment',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // Pay water bill
  static async payWaterBill(req, res) {
    try {
      const { accountId, customerNumber, amount, customerName, period } = req.body;
      const userId = req.userId;

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'WATER', 'SAWACO', customerNumber, amount,
        `Thanh toán tiền nước ${period || ''}`,
        { customerName, period }
      );
    } catch (error) {
      console.error('Pay water bill error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to process water bill payment'
      });
    }
  }

  // Pay internet bill
  static async payInternetBill(req, res) {
    try {
      const { accountId, customerNumber, amount, provider, customerName } = req.body;
      const userId = req.userId;

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'INTERNET', provider || 'VNPT', customerNumber, amount,
        `Thanh toán cước internet ${provider || ''}`,
        { customerName }
      );
    } catch (error) {
      console.error('Pay internet bill error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to process internet bill payment'
      });
    }
  }

  // Mobile topup
  static async mobileTopup(req, res) {
    try {
      const { accountId, phoneNumber, amount, provider } = req.body;
      const userId = req.userId;

      // Validation
      if (!phoneNumber || !amount) {
        return res.status(400).json({
          success: false,
          message: 'Phone number and amount are required'
        });
      }

      // Validate phone number format
      const phoneRegex = /^(0[3|5|7|8|9])+([0-9]{8})$/;
      if (!phoneRegex.test(phoneNumber)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid phone number format'
        });
      }

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'PHONE_TOPUP', provider || 'AUTO', phoneNumber, amount,
        `Nạp tiền điện thoại ${phoneNumber}`,
        { phoneNumber }
      );
    } catch (error) {
      console.error('Mobile topup error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to process mobile topup'
      });
    }
  }

  // Buy data package
  static async buyDataPackage(req, res) {
    try {
      const { accountId, phoneNumber, packageName, amount, provider } = req.body;
      const userId = req.userId;

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'DATA_PACKAGE', provider || 'AUTO', phoneNumber, amount,
        `Mua gói cước ${packageName}`,
        { phoneNumber, packageName }
      );
    } catch (error) {
      console.error('Buy data package error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to buy data package'
      });
    }
  }

  // Buy scratch card
  static async buyScratchCard(req, res) {
    try {
      const { accountId, cardType, amount, provider, quantity } = req.body;
      const userId = req.userId;

      const totalAmount = amount * (quantity || 1);

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'SCRATCH_CARD', provider || 'AUTO', cardType, totalAmount,
        `Mua thẻ cào ${provider} ${amount.toLocaleString()}đ x${quantity || 1}`,
        { cardType, quantity: quantity || 1 }
      );
    } catch (error) {
      console.error('Buy scratch card error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to buy scratch card'
      });
    }
  }

  // Generic utility payment processor
  static async processUtilityPayment(req, res, userId, accountId, serviceType, provider, serviceNumber, amount, description, metadata = {}) {
    try {
      // Validate userId
      if (!userId) {
        console.error('processUtilityPayment: userId is missing');
        return res.status(401).json({
          success: false,
          message: 'User authentication required'
        });
      }

      // Validate req.user
      if (!req.user) {
        console.error('processUtilityPayment: req.user is missing', { userId, serviceType });
        return res.status(401).json({
          success: false,
          message: 'User information not found'
        });
      }

      // Validation
      const amountNumber = Number(amount);
      if (!serviceNumber || Number.isNaN(amountNumber)) {
        return res.status(400).json({
          success: false,
          message: 'Service number and valid amount are required'
        });
      }

      if (amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Amount must be greater than 0'
        });
      }

      // Find account
      let account = null;
      if (accountId) {
        account = await Account.findOne({
          _id: accountId,
          userId,
          isActive: true
        });
      } else {
        // Use primary checking account
        account = await Account.findOne({
          userId,
          accountType: 'CHECKING',
          isActive: true
        }).sort({ createdAt: 1 });
      }

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      // Calculate fee and total
      const fee = Utility.calculateFee(amountNumber, serviceType);
      const totalAmount = amountNumber + fee;

      // Check sufficient balance
      if (!account.canDebit(totalAmount)) {
        return res.status(400).json({
          success: false,
          message: 'Insufficient balance'
        });
      }

      // Create utility transaction
      const transactionId = Utility.generateTransactionId();
      
      const utility = new Utility({
        transactionId,
        userId,
        accountId: account._id,
        serviceType,
        provider,
        serviceNumber,
        amount: amountNumber,
        currency: account.currency,
        fee,
        totalAmount,
        status: 'PENDING',
        description,
        metadata
      });

      await utility.save();

      // Generate OTP for verification
      const otp = OTPUtils.generateOTP(6);
      const otpHash = OTPUtils.hashOTP(otp);

      // Get user email safely
      const userEmail = req.user?.email || req.user?.userEmail || 'unknown@example.com';
      
      const otpCode = new OtpCode({
        userId,
        email: userEmail,
        otpHash,
        otpType: 'UTILITY',
        transactionId,
        expiresAt: OTPUtils.generateExpiryTime(5)
      });

      await otpCode.save();

      // Log OTP for development
      console.log(`🔐 UTILITY OTP for ${userEmail}: ${otp}`);

      res.status(200).json({
        success: true,
        message: 'Payment initiated. OTP sent for verification.',
        data: {
          otp_required: true,
          transaction_id: transactionId,
          service_type: serviceType,
          provider,
          service_number: serviceNumber,
          amount: amountNumber,
          formatted_amount: utility.formattedAmount,
          fee,
          total_amount: totalAmount,
          formatted_total_amount: utility.formattedTotalAmount,
          currency: account.currency,
          description,
          // FOR DEVELOPMENT ONLY
          development_otp: otp,
          developmentOTP: otp
        }
      });

    } catch (error) {
      console.error('Process utility payment error:', error);
      console.error('Error stack:', error.stack);
      console.error('Error details:', {
        userId,
        accountId,
        serviceType,
        serviceNumber,
        amount,
        user: req.user ? { id: req.user._id, email: req.user.email } : 'missing'
      });
      throw error;
    }
  }

  // Verify utility payment OTP
  static async verifyUtilityOTP(req, res) {
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

      // Find utility transaction
      const utility = await Utility.findOne({
        transactionId,
        userId,
        status: 'PENDING'
      }).populate('accountId');

      if (!utility) {
        return res.status(404).json({
          success: false,
          message: 'Transaction not found or already processed'
        });
      }

      // Find valid OTP
      const validOTP = await OtpCode.findValidOTP(userId, 'UTILITY', transactionId);
      
      if (!validOTP) {
        await utility.markAsFailed('OTP expired or invalid');
        return res.status(401).json({
          success: false,
          message: 'Invalid or expired OTP'
        });
      }

      // Verify OTP
      const isValidOTP = OTPUtils.verifyOTP(otpCode, validOTP.otpHash);
      
      if (!isValidOTP) {
        await validOTP.incrementAttempt();
        
        if (validOTP.attempts >= 2) {
          await utility.markAsFailed('Too many OTP attempts');
        }
        
        return res.status(401).json({
          success: false,
          message: 'Invalid OTP',
          attemptsLeft: 3 - validOTP.attempts - 1
        });
      }

      // Mark OTP as used
      await validOTP.markAsUsed();
      await utility.markAsProcessing();

      // Ensure account is populated
      if (!utility.accountId) {
        await utility.populate('accountId');
      }

      if (!utility.accountId) {
        await utility.markAsFailed('Account not found');
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      // Process the payment
      try {
        // Reload account from database to ensure we have latest balance
        const account = await Account.findById(utility.accountId._id);

        if (!account) {
          await utility.markAsFailed('Account not found');
          return res.status(404).json({
            success: false,
            message: 'Account not found'
          });
        }

        // Verify sufficient balance again (in case balance changed)
        if (!account.canDebit(utility.totalAmount)) {
          await utility.markAsFailed('Insufficient balance');
          return res.status(400).json({
            success: false,
            message: `Số dư không đủ. Bạn cần ${utility.totalAmount.toLocaleString()} ${utility.currency || 'VND'} nhưng số dư hiện tại là ${account.balance.toLocaleString()} ${account.currency || 'VND'}`
          });
        }

        // Debit from account (this already calls save())
        await account.debit(utility.totalAmount);
        
        // Generate reference number (simulating external payment)
        utility.referenceNumber = `REF${Date.now()}${Math.floor(Math.random() * 1000)}`;
        
        // Mark as completed
        await utility.markAsCompleted();

        res.status(200).json({
          success: true,
          message: 'Payment completed successfully',
          data: formatUtility(utility)
        });

      } catch (paymentError) {
        console.error('Payment execution error:', paymentError);
        await utility.markAsFailed('Payment execution failed');
        
        res.status(500).json({
          success: false,
          message: 'Payment failed. Please try again.'
        });
      }

    } catch (error) {
      console.error('Verify utility OTP error:', error);
      res.status(500).json({
        success: false,
        message: 'OTP verification failed'
      });
    }
  }

  // Get utility payment history
  static async getUtilityHistory(req, res) {
    try {
      const userId = req.userId;
      const { 
        page = 1, 
        limit = 20, 
        serviceType,
        status,
        startDate,
        endDate 
      } = req.query;

      // Build query
      const query = { userId };

      if (serviceType) {
        query.serviceType = serviceType;
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
      
      const [utilities, total] = await Promise.all([
        Utility.find(query)
          .populate('accountId', 'accountNumber accountType')
          .sort({ createdAt: -1 })
          .skip(skip)
          .limit(parseInt(limit)),
        Utility.countDocuments(query)
      ]);

      const totalPages = Math.ceil(total / limit);

      res.status(200).json({
        success: true,
        message: 'Utility payment history retrieved successfully',
        data: utilities.map(formatUtility),
        meta: {
          pagination: {
            currentPage: parseInt(page),
            totalPages,
            totalRecords: total,
            hasNextPage: page < totalPages,
            hasPrevPage: page > 1
          }
        }
      });

    } catch (error) {
      console.error('Get utility history error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve utility payment history'
      });
    }
  }

  // Get available service providers
  static async getServiceProviders(req, res) {
    try {
      const { serviceType } = req.query;

      const providers = {
        ELECTRICITY: [
          { code: 'EVN', name: 'Điện Lực Việt Nam', logo: 'evn_logo.png' }
        ],
        WATER: [
          { code: 'SAWACO', name: 'Công ty nước Sài Gòn', logo: 'sawaco_logo.png' },
          { code: 'HAWACO', name: 'Công ty nước Hà Nội', logo: 'hawaco_logo.png' }
        ],
        INTERNET: [
          { code: 'VNPT', name: 'VNPT', logo: 'vnpt_logo.png' },
          { code: 'VIETTEL', name: 'Viettel', logo: 'viettel_logo.png' },
          { code: 'FPT', name: 'FPT Telecom', logo: 'fpt_logo.png' }
        ],
        PHONE_TOPUP: [
          { code: 'VIETTEL', name: 'Viettel', logo: 'viettel_logo.png' },
          { code: 'VINAPHONE', name: 'Vinaphone', logo: 'vinaphone_logo.png' },
          { code: 'MOBIFONE', name: 'Mobifone', logo: 'mobifone_logo.png' },
          { code: 'VIETNAMOBILE', name: 'Vietnamobile', logo: 'vietnamobile_logo.png' }
        ]
      };

      if (serviceType && providers[serviceType]) {
        res.status(200).json({
          success: true,
          data: providers[serviceType]
        });
      } else {
        res.status(200).json({
          success: true,
          data: providers
        });
      }

    } catch (error) {
      console.error('Get service providers error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve service providers'
      });
    }
  }
}

module.exports = UtilityController;

