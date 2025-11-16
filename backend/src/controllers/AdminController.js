const User = require('../models/User');
const Account = require('../models/Account');
const Transaction = require('../models/Transaction');
const { formatUser, formatAccount } = require('../utils/responseFormatter');

class AdminController {
  // Get all customers (with pagination)
  static async getAllCustomers(req, res) {
    try {
      const page = parseInt(req.query.page) || 1;
      const limit = parseInt(req.query.limit) || 20;
      const skip = (page - 1) * limit;

      // Only get CUSTOMER type users
      const customers = await User.find({ customerType: 'CUSTOMER' })
        .select('-password -refreshTokens')
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limit);

      const total = await User.countDocuments({ customerType: 'CUSTOMER' });

      // Get account counts for each customer
      const customersWithAccounts = await Promise.all(
        customers.map(async (customer) => {
          const accountCount = await Account.countDocuments({ 
            userId: customer._id, 
            isActive: true 
          });
          
          const accountData = await Account.findOne({ 
            userId: customer._id, 
            accountType: 'CHECKING',
            isActive: true 
          }).select('accountNumber balance');

          const customerFormatted = formatUser(customer);
          return {
            ...customerFormatted,
            account_count: accountCount || 0,
            primary_account: accountData ? {
              account_number: accountData.accountNumber ? String(accountData.accountNumber) : '',
              balance: (accountData.balance !== null && accountData.balance !== undefined) ? Number(accountData.balance) : 0
            } : null
          };
        })
      );

      res.status(200).json({
        success: true,
        message: 'Customers retrieved successfully',
        data: customersWithAccounts,
        meta: {
          page,
          limit,
          total,
          total_pages: Math.ceil(total / limit)
        }
      });

    } catch (error) {
      console.error('Get all customers error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve customers'
      });
    }
  }

  // Search customers
  static async searchCustomers(req, res) {
    try {
      const { query } = req.query;
      
      if (!query || query.trim().length < 2) {
        return res.status(400).json({
          success: false,
          message: 'Search query must be at least 2 characters'
        });
      }

      const searchRegex = new RegExp(query, 'i');
      
      const customers = await User.find({
        customerType: 'CUSTOMER',
        $or: [
          { email: searchRegex },
          { fullName: searchRegex },
          { phone: searchRegex },
          { accountNumber: searchRegex }
        ]
      })
        .select('-password -refreshTokens')
        .limit(50)
        .sort({ createdAt: -1 });

      res.status(200).json({
        success: true,
        message: 'Search completed successfully',
        data: customers.map(formatUser),
        count: customers.length
      });

    } catch (error) {
      console.error('Search customers error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to search customers'
      });
    }
  }

  // Get customer details with accounts
  static async getCustomerDetails(req, res) {
    try {
      const { customerId } = req.params;

      const customer = await User.findById(customerId)
        .select('-password -refreshTokens');

      if (!customer) {
        return res.status(404).json({
          success: false,
          message: 'Customer not found'
        });
      }

      if (customer.customerType !== 'CUSTOMER') {
        return res.status(403).json({
          success: false,
          message: 'User is not a customer'
        });
      }

      // Get all accounts for this customer
      const accounts = await Account.find({ 
        userId: customer._id 
      }).sort({ accountType: 1, createdAt: -1 });

      // Get transaction summary
      const transactionStats = await Transaction.aggregate([
        {
          $match: {
            $or: [
              { fromAccountId: { $in: accounts.map(acc => acc._id) } },
              { toAccountId: { $in: accounts.map(acc => acc._id) } }
            ]
          }
        },
        {
          $group: {
            _id: '$status',
            count: { $sum: 1 },
            totalAmount: { $sum: '$amount' }
          }
        }
      ]);

      res.status(200).json({
        success: true,
        message: 'Customer details retrieved successfully',
        data: {
          customer: formatUser(customer),
          accounts: accounts.map(formatAccount),
          transaction_stats: transactionStats,
          account_count: accounts.length
        }
      });

    } catch (error) {
      console.error('Get customer details error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve customer details'
      });
    }
  }

  // Create account for customer (officer only)
  static async createCustomerAccount(req, res) {
    try {
      const { customerId, accountType, initialBalance, interestRate } = req.body;

      // Validation
      if (!customerId || !accountType) {
        return res.status(400).json({
          success: false,
          message: 'Customer ID and account type are required'
        });
      }

      const validAccountTypes = ['CHECKING', 'SAVING', 'MORTGAGE'];
      if (!validAccountTypes.includes(accountType)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid account type. Must be CHECKING, SAVING, or MORTGAGE'
        });
      }

      // Check if customer exists
      const customer = await User.findById(customerId);
      if (!customer || customer.customerType !== 'CUSTOMER') {
        return res.status(404).json({
          success: false,
          message: 'Customer not found'
        });
      }

      // Check if account type already exists for this customer
      const existingAccount = await Account.findOne({
        userId: customerId,
        accountType: accountType,
        isActive: true
      });

      if (existingAccount) {
        return res.status(400).json({
          success: false,
          message: `Customer already has an active ${accountType} account`
        });
      }

      // Create account
      const account = new Account({
        userId: customerId,
        accountNumber: Account.generateAccountNumber(),
        accountType: accountType,
        balance: initialBalance || 0,
        interestRate: interestRate || (accountType === 'SAVING' ? 5.5 : accountType === 'MORTGAGE' ? 8.0 : 0.5),
        currency: 'VND',
        isActive: true
      });

      await account.save();

      // If initial balance, create deposit transaction
      if (initialBalance && initialBalance > 0) {
        const transaction = new Transaction({
          transactionId: Transaction.generateTransactionId(),
          fromAccountId: account._id,
          toAccountId: account._id,
          fromAccountNumber: account.accountNumber,
          toAccountNumber: account.accountNumber,
          amount: initialBalance,
          currency: 'VND',
          description: 'Initial deposit - Account created by bank officer',
          transactionType: 'DEPOSIT',
          status: 'COMPLETED',
          initiatedBy: req.userId, // Officer ID
          fee: 0,
          totalAmount: initialBalance,
          otpVerified: true,
          processedAt: new Date()
        });

        await transaction.save();
      }

      res.status(201).json({
        success: true,
        message: 'Account created successfully',
        data: formatAccount(account)
      });

    } catch (error) {
      console.error('Create customer account error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to create account'
      });
    }
  }

  // Update account (officer only)
  static async updateAccount(req, res) {
    try {
      const { accountId } = req.params;
      const { interestRate, isActive, balance } = req.body;

      const account = await Account.findById(accountId);
      
      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      // Update fields
      if (interestRate !== undefined) {
        if (interestRate < 0 || interestRate > 100) {
          return res.status(400).json({
            success: false,
            message: 'Interest rate must be between 0 and 100'
          });
        }
        account.interestRate = interestRate;
      }

      if (isActive !== undefined) {
        account.isActive = isActive;
      }

      // Note: Balance changes should go through transactions, but officers can adjust if needed
      if (balance !== undefined && balance >= 0) {
        const balanceDifference = balance - account.balance;
        account.balance = balance;

        // Create adjustment transaction
        if (balanceDifference !== 0) {
          const transaction = new Transaction({
            transactionId: Transaction.generateTransactionId(),
            fromAccountId: account._id,
            toAccountId: account._id,
            fromAccountNumber: account.accountNumber,
            toAccountNumber: account.accountNumber,
            amount: Math.abs(balanceDifference),
            currency: account.currency,
            description: balanceDifference > 0 
              ? 'Balance adjustment - Credit by bank officer'
              : 'Balance adjustment - Debit by bank officer',
            transactionType: balanceDifference > 0 ? 'DEPOSIT' : 'WITHDRAWAL',
            status: 'COMPLETED',
            initiatedBy: req.userId, // Officer ID
            fee: 0,
            totalAmount: Math.abs(balanceDifference),
            otpVerified: true,
            processedAt: new Date()
          });

          await transaction.save();
        }
      }

      await account.save();

      res.status(200).json({
        success: true,
        message: 'Account updated successfully',
        data: formatAccount(account)
      });

    } catch (error) {
      console.error('Update account error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to update account'
      });
    }
  }

  // Deactivate account
  static async deactivateAccount(req, res) {
    try {
      const { accountId } = req.params;

      const account = await Account.findById(accountId);
      
      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      account.isActive = false;
      await account.save();

      res.status(200).json({
        success: true,
        message: 'Account deactivated successfully',
        data: formatAccount(account)
      });

    } catch (error) {
      console.error('Deactivate account error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to deactivate account'
      });
    }
  }

  // Get officer dashboard stats
  static async getDashboardStats(req, res) {
    try {
      const totalCustomers = await User.countDocuments({ customerType: 'CUSTOMER' });
      const activeAccounts = await Account.countDocuments({ isActive: true });
      const totalBalance = await Account.aggregate([
        { $match: { isActive: true } },
        { $group: { _id: null, total: { $sum: '$balance' } } }
      ]);

      const recentTransactions = await Transaction.find()
        .sort({ createdAt: -1 })
        .limit(10)
        .populate('initiatedBy', 'fullName email')
        .select('transactionId amount transactionType status createdAt description');

      const todayTransactions = await Transaction.countDocuments({
        createdAt: {
          $gte: new Date(new Date().setHours(0, 0, 0, 0))
        }
      });

      const totalBalanceAmount = (totalBalance && totalBalance[0] && totalBalance[0].total) ? totalBalance[0].total : 0;

      res.status(200).json({
        success: true,
        message: 'Dashboard stats retrieved successfully',
        data: {
          total_customers: totalCustomers || 0,
          active_accounts: activeAccounts || 0,
          total_balance: totalBalanceAmount,
          today_transactions: todayTransactions || 0,
          recent_transactions: recentTransactions.map(t => {
            const transaction = {
              transaction_id: t.transactionId ? String(t.transactionId) : '',
              amount: (t.amount !== null && t.amount !== undefined) ? Number(t.amount) : 0,
              type: t.transactionType ? String(t.transactionType) : '',
              status: t.status ? String(t.status) : '',
              description: t.description ? String(t.description) : ''
            };

            // Handle created_at
            if (t.createdAt) {
              transaction.created_at = typeof t.createdAt === 'string' ? t.createdAt : t.createdAt.toISOString();
            } else {
              transaction.created_at = null;
            }

            // Handle initiated_by
            if (t.initiatedBy && t.initiatedBy._id) {
              transaction.initiated_by = {
                name: t.initiatedBy.fullName ? String(t.initiatedBy.fullName) : '',
                email: t.initiatedBy.email ? String(t.initiatedBy.email) : ''
              };
            } else {
              transaction.initiated_by = null;
            }

            return transaction;
          })
        }
      });

    } catch (error) {
      console.error('Get dashboard stats error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve dashboard stats'
      });
    }
  }
}

module.exports = AdminController;

