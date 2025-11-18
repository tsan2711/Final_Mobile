const express = require('express');
const AdminController = require('../controllers/AdminController');
const { authMiddleware, bankOfficerOnly } = require('../middleware/auth');

const router = express.Router();

// All admin routes require authentication and bank officer role
router.use(authMiddleware);
router.use(bankOfficerOnly);

// Dashboard stats
router.get('/dashboard', AdminController.getDashboardStats);

// Customer management
// Note: More specific routes must come before parameterized routes
router.get('/customers/search', AdminController.searchCustomers); // Must come before /customers/:customerId
router.get('/customers', AdminController.getAllCustomers);
router.post('/customers', AdminController.createCustomer);
router.get('/customers/:customerId', AdminController.getCustomerDetails);
router.put('/customers/:customerId', AdminController.updateCustomer);

// Account management
router.post('/accounts/create', AdminController.createCustomerAccount);
router.put('/accounts/:accountId', AdminController.updateAccount);
router.delete('/accounts/:accountId', AdminController.deactivateAccount);

// Transaction management
router.get('/transactions', AdminController.getAllTransactions);
router.post('/transactions/transfer', AdminController.transferMoney);
router.post('/transactions/deposit', AdminController.depositMoney);

// Interest rate management
router.put('/interest-rates', AdminController.updateInterestRate);
router.get('/interest-rates/history', AdminController.getInterestRateHistory);

module.exports = router;

