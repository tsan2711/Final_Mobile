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
router.get('/customers', AdminController.getAllCustomers);
router.get('/customers/search', AdminController.searchCustomers);
router.get('/customers/:customerId', AdminController.getCustomerDetails);

// Account management
router.post('/accounts/create', AdminController.createCustomerAccount);
router.put('/accounts/:accountId', AdminController.updateAccount);
router.delete('/accounts/:accountId', AdminController.deactivateAccount);

module.exports = router;

