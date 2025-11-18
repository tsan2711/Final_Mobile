const express = require('express');
const AccountController = require('../controllers/AccountController');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();

// All account routes require authentication
router.use(authMiddleware);

// Get all user accounts
router.get('/', AccountController.getUserAccounts);

// Get account summary for dashboard
router.get('/summary', AccountController.getAccountSummary);

// Get primary account
router.get('/primary', AccountController.getPrimaryAccount);

// Get interest projection (must come before /:accountId route)
router.get('/:accountId/interest-projection', AccountController.getInterestProjection);

// Get specific account by ID
router.get('/:accountId', AccountController.getAccount);

// Get account balance
router.get('/:accountId/balance', AccountController.getAccountBalance);

// Get account by account number (for transfers)
router.get('/number/:accountNumber', AccountController.getAccountByNumber);

// Deposit and withdrawal
router.post('/deposit', AccountController.depositMoney);
router.post('/withdraw', AccountController.withdrawMoney);

// Create default accounts for user
router.post('/create-defaults', AccountController.createDefaultAccounts);

module.exports = router;
