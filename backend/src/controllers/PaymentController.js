const Payment = require('../models/Payment');
const Account = require('../models/Account');
const Transaction = require('../models/Transaction');
const crypto = require('crypto');
const axios = require('axios');

class PaymentController {
  /**
   * Create VNPay payment
   * POST /api/payments/vnpay/create-payment
   */
  static async createVnpayPayment(req, res) {
    try {
      const { accountId, amount, description, returnUrl, cancelUrl } = req.body;
      const userId = req.userId;

      // Validation
      if (!accountId || !amount) {
        return res.status(400).json({
          success: false,
          message: 'Vui lòng nhập đầy đủ thông tin tài khoản và số tiền'
        });
      }

      const amountNumber = Number(amount);
      if (isNaN(amountNumber) || amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Số tiền không hợp lệ'
        });
      }

      // Minimum amount: 10,000 VND
      if (amountNumber < 10000) {
        return res.status(400).json({
          success: false,
          message: 'Số tiền tối thiểu là 10,000 VND'
        });
      }

      // Find account
      const account = await Account.findOne({
        _id: accountId,
        userId,
        isActive: true
      });

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Không tìm thấy tài khoản hợp lệ'
        });
      }

      // Create payment record
      const paymentId = Payment.generatePaymentId();
      const orderId = `ORDER${Date.now()}`;
      
      const payment = new Payment({
        paymentId,
        userId,
        accountId: account._id,
        accountNumber: account.accountNumber,
        amount: amountNumber,
        currency: 'VND',
        paymentMethod: 'VNPAY',
        paymentType: 'DEPOSIT',
        description: description || 'Nạp tiền qua VNPay',
        status: 'PENDING',
        vnpay: {
          orderId,
          transactionId: paymentId
        },
        metadata: {
          ipAddress: req.ip || req.connection.remoteAddress,
          userAgent: req.get('user-agent'),
          returnUrl: returnUrl || `${process.env.FRONTEND_URL || 'http://localhost:3000'}/payment/success`,
          cancelUrl: cancelUrl || `${process.env.FRONTEND_URL || 'http://localhost:3000'}/payment/cancel`
        }
      });

      await payment.save();

      // VNPay configuration (from environment variables)
      // For local testing, you can use test credentials from VNPay Sandbox
      const vnp_TmnCode = process.env.VNPAY_TMN_CODE || '2QXUI4J4'; // Test TMN Code
      const vnp_HashSecret = process.env.VNPAY_HASH_SECRET || 'RAOPSRGEWNYSMDZDEHEQCDDZXLZQJQKT'; // Test Hash Secret
      const vnp_Url = process.env.VNPAY_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html';
      const vnp_ReturnUrl = process.env.VNPAY_RETURN_URL || `${process.env.BACKEND_URL || 'http://localhost:8000'}/api/payments/vnpay/callback`;
      
      // Check if using default test credentials (for development only)
      if (vnp_TmnCode === '2QXUI4J4' && vnp_HashSecret === 'RAOPSRGEWNYSMDZDEHEQCDDZXLZQJQKT') {
        console.log('⚠️  Using VNPay test credentials - For local testing only!');
      }

      // Create VNPay payment URL
      const date = new Date();
      const createDate = date.toISOString().replace(/[-:]/g, '').split('.')[0] + '00';
      const expireDate = new Date(date.getTime() + 15 * 60 * 1000).toISOString().replace(/[-:]/g, '').split('.')[0] + '00';

      const vnp_Params = {
        vnp_Version: '2.1.0',
        vnp_Command: 'pay',
        vnp_TmnCode: vnp_TmnCode,
        vnp_Locale: 'vn',
        vnp_CurrCode: 'VND',
        vnp_TxnRef: orderId,
        vnp_OrderInfo: description || `Nạp tiền ${amountNumber.toLocaleString('vi-VN')} VND`,
        vnp_OrderType: 'other',
        vnp_Amount: amountNumber * 100, // VNPay expects amount in cents
        vnp_ReturnUrl: vnp_ReturnUrl,
        vnp_IpAddr: req.ip || req.connection.remoteAddress,
        vnp_CreateDate: createDate,
        vnp_ExpireDate: expireDate
      };

      // Sort params and create secure hash
      const sortedParams = Object.keys(vnp_Params)
        .sort()
        .reduce((result, key) => {
          result[key] = vnp_Params[key];
          return result;
        }, {});

      const signData = new URLSearchParams(sortedParams).toString();
      const hmac = crypto.createHmac('sha512', vnp_HashSecret);
      const signed = hmac.update(signData, 'utf-8').digest('hex');
      vnp_Params.vnp_SecureHash = signed;

      // Build payment URL
      const paymentUrl = `${vnp_Url}?${new URLSearchParams(vnp_Params).toString()}`;

      // Update payment with VNPay info
      payment.vnpay.paymentUrl = paymentUrl;
      payment.vnpay.secureHash = signed;
      await payment.save();

      res.json({
        success: true,
        data: {
          paymentId: payment.paymentId,
          paymentUrl,
          orderId,
          amount: amountNumber,
          currency: 'VND',
          expiresAt: expireDate
        }
      });
    } catch (error) {
      console.error('Create VNPay payment error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi tạo thanh toán VNPay: ' + error.message
      });
    }
  }

  /**
   * VNPay callback handler
   * POST /api/payments/vnpay/callback
   */
  static async vnpayCallback(req, res) {
    try {
      const vnp_Params = req.query;
      const vnp_HashSecret = process.env.VNPAY_HASH_SECRET || 'RAOPSRGEWNYSMDZDEHEQCDDZXLZQJQKT'; // Test Hash Secret

      // Verify secure hash
      const secureHash = vnp_Params.vnp_SecureHash;
      delete vnp_Params.vnp_SecureHash;
      delete vnp_Params.vnp_SecureHashType;

      const signData = new URLSearchParams(
        Object.keys(vnp_Params)
          .sort()
          .reduce((result, key) => {
            result[key] = vnp_Params[key];
            return result;
          }, {})
      ).toString();

      const hmac = crypto.createHmac('sha512', vnp_HashSecret);
      const signed = hmac.update(signData, 'utf-8').digest('hex');

      if (secureHash !== signed) {
        return res.status(400).json({
          success: false,
          message: 'Invalid secure hash'
        });
      }

      const orderId = vnp_Params.vnp_TxnRef;
      const responseCode = vnp_Params.vnp_ResponseCode;
      const transactionStatus = vnp_Params.vnp_TransactionStatus;

      // Find payment by orderId
      const payment = await Payment.findOne({ 'vnpay.orderId': orderId });

      if (!payment) {
        return res.status(404).json({
          success: false,
          message: 'Payment not found'
        });
      }

      // Update payment with VNPay response
      payment.vnpay.responseCode = responseCode;
      payment.vnpay.transactionStatus = transactionStatus;

      // Check if payment is successful
      if (responseCode === '00' && transactionStatus === '00') {
        // Payment successful
        payment.status = 'PROCESSING';
        await payment.save();

        // Credit account
        const account = await Account.findById(payment.accountId);
        if (account) {
          account.balance = (account.balance || 0) + payment.amount;
          await account.save();

          // Create transaction record
          const transaction = new Transaction({
            transactionId: Transaction.generateTransactionId(),
            fromAccountId: account._id,
            toAccountId: account._id,
            fromAccountNumber: account.accountNumber,
            toAccountNumber: account.accountNumber,
            amount: payment.amount,
            currency: payment.currency,
            transactionType: 'DEPOSIT',
            status: 'COMPLETED',
            description: payment.description || 'Nạp tiền qua VNPay',
            initiatedBy: payment.userId,
            fee: 0,
            totalAmount: payment.amount,
            processedAt: new Date()
          });

          await transaction.save();

          // Link transaction to payment
          payment.transactionId = transaction._id;
          payment.status = 'COMPLETED';
          payment.completedAt = new Date();
          await payment.save();

          // Return success response
          return res.redirect(`${payment.metadata.returnUrl}?paymentId=${payment.paymentId}&status=success`);
        }
      } else {
        // Payment failed
        payment.status = 'FAILED';
        payment.failureReason = `VNPay response code: ${responseCode}, status: ${transactionStatus}`;
        payment.completedAt = new Date();
        await payment.save();

        return res.redirect(`${payment.metadata.cancelUrl}?paymentId=${payment.paymentId}&status=failed&reason=${payment.failureReason}`);
      }

      res.json({
        success: true,
        message: 'Payment processed'
      });
    } catch (error) {
      console.error('VNPay callback error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi xử lý callback VNPay: ' + error.message
      });
    }
  }

  /**
   * External bank transfer
   * POST /api/payments/bank-transfer
   */
  static async createBankTransfer(req, res) {
    try {
      const { 
        accountId, 
        amount, 
        bankName, 
        bankCode, 
        recipientAccountNumber, 
        recipientName, 
        description 
      } = req.body;
      const userId = req.userId;

      // Validation
      if (!accountId || !amount || !bankName || !recipientAccountNumber || !recipientName) {
        return res.status(400).json({
          success: false,
          message: 'Vui lòng nhập đầy đủ thông tin chuyển khoản'
        });
      }

      const amountNumber = Number(amount);
      if (isNaN(amountNumber) || amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Số tiền không hợp lệ'
        });
      }

      // Minimum amount: 10,000 VND
      if (amountNumber < 10000) {
        return res.status(400).json({
          success: false,
          message: 'Số tiền chuyển tối thiểu là 10,000 VND'
        });
      }

      // Find account
      const account = await Account.findOne({
        _id: accountId,
        userId,
        isActive: true
      });

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Không tìm thấy tài khoản hợp lệ'
        });
      }

      // Check balance
      const fee = Transaction.calculateFee(amountNumber, 'TRANSFER');
      const totalAmount = amountNumber + fee;

      if ((account.balance || 0) < totalAmount) {
        return res.status(400).json({
          success: false,
          message: `Số dư không đủ. Cần ${totalAmount.toLocaleString('vi-VN')} VND (bao gồm phí ${fee.toLocaleString('vi-VN')} VND)`
        });
      }

      // Create payment record
      const paymentId = Payment.generatePaymentId();
      
      const payment = new Payment({
        paymentId,
        userId,
        accountId: account._id,
        accountNumber: account.accountNumber,
        amount: amountNumber,
        currency: 'VND',
        paymentMethod: 'BANK_TRANSFER',
        paymentType: 'EXTERNAL_TRANSFER',
        description: description || `Chuyển khoản đến ${bankName}`,
        status: 'PENDING',
        bankTransfer: {
          bankName,
          bankCode: bankCode || '',
          recipientAccountNumber,
          recipientName,
          transferReference: `REF${Date.now()}`
        },
        metadata: {
          ipAddress: req.ip || req.connection.remoteAddress,
          userAgent: req.get('user-agent')
        }
      });

      await payment.save();

      // Process transfer
      payment.status = 'PROCESSING';
      await payment.save();

      // Deduct from account
      account.balance = (account.balance || 0) - totalAmount;
      await account.save();

      // Create transaction record
      const transaction = new Transaction({
        transactionId: Transaction.generateTransactionId(),
        fromAccountId: account._id,
        toAccountId: account._id, // External transfer, no internal toAccount
        fromAccountNumber: account.accountNumber,
        toAccountNumber: recipientAccountNumber,
        amount: amountNumber,
        currency: 'VND',
        transactionType: 'TRANSFER',
        status: 'COMPLETED',
        description: payment.description,
        initiatedBy: payment.userId,
        fee,
        totalAmount,
        processedAt: new Date()
      });

      await transaction.save();

      // Link transaction to payment
      payment.transactionId = transaction._id;
      payment.status = 'COMPLETED';
      payment.completedAt = new Date();
      await payment.save();

      res.json({
        success: true,
        data: {
          paymentId: payment.paymentId,
          transactionId: transaction.transactionId,
          amount: amountNumber,
          fee,
          totalAmount,
          recipientAccountNumber,
          recipientName,
          bankName,
          transferReference: payment.bankTransfer.transferReference,
          status: 'COMPLETED'
        }
      });
    } catch (error) {
      console.error('Bank transfer error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi chuyển khoản: ' + error.message
      });
    }
  }

  /**
   * Get payment status
   * GET /api/payments/:paymentId
   */
  static async getPaymentStatus(req, res) {
    try {
      const { paymentId } = req.params;
      const userId = req.userId;

      const payment = await Payment.findOne({
        paymentId,
        userId
      }).populate('transactionId', 'transactionId status amount');

      if (!payment) {
        return res.status(404).json({
          success: false,
          message: 'Không tìm thấy giao dịch thanh toán'
        });
      }

      res.json({
        success: true,
        data: {
          paymentId: payment.paymentId,
          amount: payment.amount,
          currency: payment.currency,
          paymentMethod: payment.paymentMethod,
          status: payment.status,
          description: payment.description,
          createdAt: payment.createdAt,
          completedAt: payment.completedAt,
          transaction: payment.transactionId
        }
      });
    } catch (error) {
      console.error('Get payment status error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi lấy trạng thái thanh toán: ' + error.message
      });
    }
  }

  /**
   * Get user payment history
   * GET /api/payments/history
   */
  static async getPaymentHistory(req, res) {
    try {
      const userId = req.userId;
      const { page = 1, limit = 20, paymentMethod, status } = req.query;

      const query = { userId };
      if (paymentMethod) query.paymentMethod = paymentMethod;
      if (status) query.status = status;

      const payments = await Payment.find(query)
        .sort({ createdAt: -1 })
        .limit(limit * 1)
        .skip((page - 1) * limit)
        .populate('transactionId', 'transactionId status amount')
        .select('-vnpay.secureHash -stripe.clientSecret');

      const total = await Payment.countDocuments(query);

      res.json({
        success: true,
        data: {
          payments,
          pagination: {
            page: Number(page),
            limit: Number(limit),
            total,
            totalPages: Math.ceil(total / limit)
          }
        }
      });
    } catch (error) {
      console.error('Get payment history error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi lấy lịch sử thanh toán: ' + error.message
      });
    }
  }
}

module.exports = PaymentController;

