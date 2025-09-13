const express = require('express');
const TransactionController = require('../controllers/TransactionController');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();

// All transaction routes require authentication
router.use(authMiddleware);

// Initiate money transfer
router.post('/transfer', TransactionController.initiateTransfer);

// Verify OTP and complete transfer
router.post('/verify-otp', TransactionController.verifyTransferOTP);

// Get transaction history
router.get('/history', TransactionController.getTransactionHistory);

// Get specific transaction details
router.get('/:transactionId', TransactionController.getTransaction);

module.exports = router;
