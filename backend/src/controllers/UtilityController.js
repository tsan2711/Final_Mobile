const Utility = require('../models/Utility');
const Account = require('../models/Account');
const OtpCode = require('../models/OtpCode');
const Transaction = require('../models/Transaction');
const User = require('../models/User');
const Branch = require('../models/Branch');
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

  // Book flight
  static async bookFlight(req, res) {
    try {
      const { accountId, flightNumber, amount, airline, departureDate, arrivalDate, passengerName, route } = req.body;
      const userId = req.userId;

      // Validation
      if (!flightNumber || !amount) {
        return res.status(400).json({
          success: false,
          message: 'Flight number and amount are required'
        });
      }

      // Convert amount to number
      const amountNumber = Number(amount);
      if (isNaN(amountNumber) || amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Invalid amount'
        });
      }

      const description = `Mua vé máy bay ${airline || ''} ${flightNumber}${route ? ` (${route})` : ''}`;

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'FLIGHT', airline || 'GENERAL', flightNumber, amountNumber,
        description,
        { flightNumber, airline, departureDate, arrivalDate, passengerName, route }
      );
    } catch (error) {
      console.error('Book flight error:', error);
      console.error('Error stack:', error.stack);
      res.status(500).json({
        success: false,
        message: 'Failed to book flight',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // Buy movie ticket
  static async buyMovieTicket(req, res) {
    try {
      const { accountId, movieName, amount, cinema, showTime, seatNumber, quantity } = req.body;
      const userId = req.userId;

      // Validation
      if (!movieName || !amount) {
        return res.status(400).json({
          success: false,
          message: 'Movie name and amount are required'
        });
      }

      // Convert amount to number
      const amountNumber = Number(amount);
      if (isNaN(amountNumber) || amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Invalid amount'
        });
      }

      const quantityNum = quantity ? Number(quantity) : 1;
      const totalAmount = amountNumber * quantityNum;
      const description = `Mua vé xem phim ${movieName}${cinema ? ` tại ${cinema}` : ''}${showTime ? ` - ${showTime}` : ''}`;

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'MOVIE', cinema || 'GENERAL', movieName, totalAmount,
        description,
        { movieName, cinema, showTime, seatNumber, quantity: quantityNum }
      );
    } catch (error) {
      console.error('Buy movie ticket error:', error);
      console.error('Error stack:', error.stack);
      res.status(500).json({
        success: false,
        message: 'Failed to buy movie ticket',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // Book hotel
  static async bookHotel(req, res) {
    try {
      const { accountId, hotelName, amount, checkInDate, checkOutDate, guestName, roomType } = req.body;
      const userId = req.userId;

      // Validation
      if (!hotelName || !amount) {
        return res.status(400).json({
          success: false,
          message: 'Hotel name and amount are required'
        });
      }

      // Convert amount to number
      const amountNumber = Number(amount);
      if (isNaN(amountNumber) || amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Invalid amount'
        });
      }

      const description = `Đặt phòng khách sạn ${hotelName}${roomType ? ` - ${roomType}` : ''}${checkInDate && checkOutDate ? ` (${checkInDate} - ${checkOutDate})` : ''}`;

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'HOTEL', hotelName, hotelName, amountNumber,
        description,
        { hotelName, checkInDate, checkOutDate, guestName, roomType }
      );
    } catch (error) {
      console.error('Book hotel error:', error);
      console.error('Error stack:', error.stack);
      res.status(500).json({
        success: false,
        message: 'Failed to book hotel',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // E-commerce payment
  static async payEcommerce(req, res) {
    try {
      const { accountId, orderId, amount, platform, productName, merchantName } = req.body;
      const userId = req.userId;

      // Validation
      if (!orderId || !amount) {
        return res.status(400).json({
          success: false,
          message: 'Order ID and amount are required'
        });
      }

      // Convert amount to number
      const amountNumber = Number(amount);
      if (isNaN(amountNumber) || amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Invalid amount'
        });
      }

      const description = `Thanh toán đơn hàng ${orderId}${platform ? ` trên ${platform}` : ''}${productName ? ` - ${productName}` : ''}`;

      return await UtilityController.processUtilityPayment(
        req, res, userId, accountId,
        'ECOMMERCE', platform || merchantName || 'GENERAL', orderId, amountNumber,
        description,
        { orderId, platform, productName, merchantName }
      );
    } catch (error) {
      console.error('E-commerce payment error:', error);
      console.error('Error stack:', error.stack);
      res.status(500).json({
        success: false,
        message: 'Failed to process e-commerce payment',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
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

        // Create Transaction record for transaction history
        try {
          // Find or create a system user for utility payments
          let systemUser = await User.findOne({
            email: 'system@utility.local'
          });

          if (!systemUser) {
            // Create system user if it doesn't exist
            // Use a unique phone number to avoid conflicts
            const timestamp = Date.now().toString().slice(-10);
            systemUser = new User({
              email: 'system@utility.local',
              password: 'system', // Will be hashed by pre-save hook
              fullName: 'System Utility Account',
              phone: `999${timestamp}`, // Unique phone number
              customerType: 'CUSTOMER',
              isActive: true
            });
            await systemUser.save();
          }

          // Find or create a system account for utility payments
          // Account number must be 16 digits
          const systemAccountNumber = '0000000000000000'; // 16 zeros for system account
          let systemAccount = await Account.findOne({
            accountNumber: systemAccountNumber,
            userId: systemUser._id
          });

          if (!systemAccount) {
            // Create system account if it doesn't exist
            systemAccount = new Account({
              userId: systemUser._id,
              accountNumber: systemAccountNumber,
              accountType: 'SAVING',
              currency: 'VND',
              balance: 0,
              isActive: true
            });
            await systemAccount.save();
          }

          // Generate transaction ID
          const transactionId = Transaction.generateTransactionId();

          // Create transaction record
          const transaction = new Transaction({
            transactionId,
            fromAccountId: account._id,
            toAccountId: systemAccount._id,
            fromAccountNumber: account.accountNumber,
            toAccountNumber: `${utility.provider}_${utility.serviceNumber}`,
            amount: utility.amount,
            currency: utility.currency,
            description: utility.description,
            transactionType: 'PAYMENT',
            status: 'COMPLETED',
            initiatedBy: userId,
            fee: utility.fee,
            totalAmount: utility.totalAmount,
            processedAt: new Date(),
            otpVerified: true,
            metadata: {
              utilityTransactionId: utility.transactionId,
              serviceType: utility.serviceType,
              provider: utility.provider
            }
          });

          await transaction.save();
        } catch (transactionError) {
          // Log error but don't fail the payment
          console.error('Failed to create transaction record:', transactionError);
        }

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
        ],
        FLIGHT: [
          { code: 'VIETJET', name: 'VietJet Air', logo: 'vietjet_logo.png' },
          { code: 'VIETNAM_AIRLINES', name: 'Vietnam Airlines', logo: 'vietnam_airlines_logo.png' },
          { code: 'BAMBOO', name: 'Bamboo Airways', logo: 'bamboo_logo.png' },
          { code: 'JETSTAR', name: 'Jetstar Pacific', logo: 'jetstar_logo.png' }
        ],
        MOVIE: [
          { code: 'CGV', name: 'CGV Cinemas', logo: 'cgv_logo.png' },
          { code: 'LOTTE', name: 'Lotte Cinemas', logo: 'lotte_logo.png' },
          { code: 'GALAXY', name: 'Galaxy Cinemas', logo: 'galaxy_logo.png' },
          { code: 'BHD', name: 'BHD Star Cinemas', logo: 'bhd_logo.png' }
        ],
        HOTEL: [
          { code: 'AGODA', name: 'Agoda', logo: 'agoda_logo.png' },
          { code: 'BOOKING', name: 'Booking.com', logo: 'booking_logo.png' },
          { code: 'TRAVELOKA', name: 'Traveloka', logo: 'traveloka_logo.png' },
          { code: 'EXPEDIA', name: 'Expedia', logo: 'expedia_logo.png' }
        ],
        ECOMMERCE: [
          { code: 'SHOPEE', name: 'Shopee', logo: 'shopee_logo.png' },
          { code: 'LAZADA', name: 'Lazada', logo: 'lazada_logo.png' },
          { code: 'TIKI', name: 'Tiki', logo: 'tiki_logo.png' },
          { code: 'SENDO', name: 'Sendo', logo: 'sendo_logo.png' }
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

  // Get all bank branches
  static async getBranches(req, res) {
    try {
      // Query branches from database
      const branches = await Branch.find({ isActive: true })
        .select('name address phone latitude longitude openingHours services')
        .lean();

      // Format response to match expected format
      const formattedBranches = branches.map(branch => ({
        id: branch._id.toString(),
        name: branch.name,
        address: branch.address,
        phone: branch.phone,
        latitude: branch.latitude,
        longitude: branch.longitude,
        openingHours: branch.openingHours || '8:00 - 17:00',
        services: branch.services || []
      }));

      res.status(200).json({
        success: true,
        message: 'Branches retrieved successfully',
        data: formattedBranches
      });
    } catch (error) {
      console.error('Get branches error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve branches'
      });
    }
  }

  // Get nearest branch based on user location
  static async getNearestBranch(req, res) {
    try {
      const { latitude, longitude } = req.query;

      if (!latitude || !longitude) {
        return res.status(400).json({
          success: false,
          message: 'Latitude and longitude are required'
        });
      }

      const userLat = parseFloat(latitude);
      const userLng = parseFloat(longitude);

      if (isNaN(userLat) || isNaN(userLng)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid latitude or longitude'
        });
      }

      // Query branches from database
      const branches = await Branch.find({ isActive: true })
        .select('name address phone latitude longitude openingHours services')
        .lean();

      // Format branches and calculate distance
      const branchesWithDistance = branches.map(branch => {
        const distance = UtilityController.calculateDistance(
          userLat,
          userLng,
          branch.latitude,
          branch.longitude
        );
        return {
          id: branch._id.toString(),
          name: branch.name,
          address: branch.address,
          phone: branch.phone,
          latitude: branch.latitude,
          longitude: branch.longitude,
          openingHours: branch.openingHours || '8:00 - 17:00',
          services: branch.services || [],
          distance: distance, // in kilometers
          distanceText: distance < 1 
            ? `${Math.round(distance * 1000)}m` 
            : `${distance.toFixed(2)}km`
        };
      });

      // Sort by distance
      branchesWithDistance.sort((a, b) => a.distance - b.distance);

      // Get nearest branch
      const nearestBranch = branchesWithDistance[0];

      res.status(200).json({
        success: true,
        message: 'Nearest branch found',
        data: {
          nearest: nearestBranch,
          allBranches: branchesWithDistance
        }
      });
    } catch (error) {
      console.error('Get nearest branch error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to find nearest branch'
      });
    }
  }

  // Calculate distance between two coordinates using Haversine formula
  static calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // Radius of the Earth in kilometers
    const dLat = UtilityController.deg2rad(lat2 - lat1);
    const dLon = UtilityController.deg2rad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(UtilityController.deg2rad(lat1)) * Math.cos(UtilityController.deg2rad(lat2)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c; // Distance in kilometers
    return distance;
  }

  static deg2rad(deg) {
    return deg * (Math.PI / 180);
  }
}

module.exports = UtilityController;

