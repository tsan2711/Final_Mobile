const express = require('express');
const router = express.Router();
const UtilityController = require('../controllers/UtilityController');
const { authenticate } = require('../middleware/auth');

// Bill payments
router.post('/pay-electricity', authenticate, UtilityController.payElectricityBill);
router.post('/pay-water', authenticate, UtilityController.payWaterBill);
router.post('/pay-internet', authenticate, UtilityController.payInternetBill);

// Mobile services
router.post('/mobile-topup', authenticate, UtilityController.mobileTopup);
router.post('/buy-data-package', authenticate, UtilityController.buyDataPackage);
router.post('/buy-scratch-card', authenticate, UtilityController.buyScratchCard);

// Travel & Entertainment services
router.post('/book-flight', authenticate, UtilityController.bookFlight);
router.post('/buy-movie-ticket', authenticate, UtilityController.buyMovieTicket);
router.post('/book-hotel', authenticate, UtilityController.bookHotel);

// E-commerce payment
router.post('/pay-ecommerce', authenticate, UtilityController.payEcommerce);

// OTP verification
router.post('/verify-otp', authenticate, UtilityController.verifyUtilityOTP);

// History
router.get('/history', authenticate, UtilityController.getUtilityHistory);

// Service providers
router.get('/providers', authenticate, UtilityController.getServiceProviders);

module.exports = router;

