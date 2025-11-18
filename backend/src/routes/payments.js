const express = require('express');
const router = express.Router();
const PaymentController = require('../controllers/PaymentController');
const { authenticate } = require('../middleware/auth');

// VNPay routes
router.post('/vnpay/create-payment', authenticate, PaymentController.createVnpayPayment);
router.get('/vnpay/callback', PaymentController.vnpayCallback);
router.post('/vnpay/callback', PaymentController.vnpayCallback);

// Bank transfer routes
router.post('/bank-transfer', authenticate, PaymentController.createBankTransfer);

// Payment status and history (specific routes before parameterized routes)
router.get('/history/list', authenticate, PaymentController.getPaymentHistory);
router.get('/:paymentId', authenticate, PaymentController.getPaymentStatus);

module.exports = router;

