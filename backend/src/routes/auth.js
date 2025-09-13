const express = require('express');
const AuthController = require('../controllers/AuthController');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();

// Public routes
router.post('/register', AuthController.register);
router.post('/login', AuthController.login);
router.post('/verify-otp', AuthController.verifyLoginOTP);
router.post('/refresh-token', AuthController.refreshToken);

// Protected routes
router.post('/logout', authMiddleware, AuthController.logout);
router.get('/me', authMiddleware, AuthController.getMe);

module.exports = router;
