const express = require('express');
const UserController = require('../controllers/UserController');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();

// All user routes require authentication
router.use(authMiddleware);

// Update user profile
router.put('/update', UserController.updateProfile);

// Change password
router.put('/change-password', UserController.changePassword);

module.exports = router;


