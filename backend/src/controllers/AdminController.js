const User = require('../models/User');
const Account = require('../models/Account');
const Transaction = require('../models/Transaction');
const { formatUser, formatAccount, formatTransaction } = require('../utils/responseFormatter');

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

      // Get account details for each customer by type
      const customersWithAccounts = await Promise.all(
        customers.map(async (customer) => {
          const accountCount = await Account.countDocuments({ 
            userId: customer._id, 
            isActive: true 
          });
          
          // Get all accounts grouped by type
          const checkingAccounts = await Account.find({ 
            userId: customer._id, 
            accountType: 'CHECKING',
            isActive: true 
          }).select('accountNumber balance interestRate currency createdAt').sort({ createdAt: -1 });

          const savingAccounts = await Account.find({ 
            userId: customer._id, 
            accountType: 'SAVING',
            isActive: true 
          }).select('accountNumber balance interestRate currency createdAt').sort({ createdAt: -1 });

          const mortgageAccounts = await Account.find({ 
            userId: customer._id, 
            accountType: 'MORTGAGE',
            isActive: true 
          }).select('accountNumber balance interestRate currency createdAt').sort({ createdAt: -1 });

          // Format accounts
          const formatAccounts = (accounts) => {
            return accounts.map(acc => ({
              account_number: acc.accountNumber ? String(acc.accountNumber) : '',
              balance: (acc.balance !== null && acc.balance !== undefined) ? Number(acc.balance) : 0,
              interest_rate: (acc.interestRate !== null && acc.interestRate !== undefined) ? Number(acc.interestRate) : 0,
              currency: acc.currency || 'VND',
              created_at: acc.createdAt ? acc.createdAt.toISOString() : null
            }));
          };

          const customerFormatted = formatUser(customer);
          const formattedChecking = formatAccounts(checkingAccounts);
          const formattedSaving = formatAccounts(savingAccounts);
          const formattedMortgage = formatAccounts(mortgageAccounts);
          
          console.log(`Customer ${customer.email}: Checking=${formattedChecking.length}, Saving=${formattedSaving.length}, Mortgage=${formattedMortgage.length}`);
          
          return {
            ...customerFormatted,
            account_count: accountCount || 0,
            accounts_by_type: {
              checking: formattedChecking,
              saving: formattedSaving,
              mortgage: formattedMortgage
            },
            // Keep primary_account for backward compatibility
            primary_account: checkingAccounts.length > 0 ? {
              account_number: checkingAccounts[0].accountNumber ? String(checkingAccounts[0].accountNumber) : '',
              balance: (checkingAccounts[0].balance !== null && checkingAccounts[0].balance !== undefined) ? Number(checkingAccounts[0].balance) : 0
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

      // Allow multiple accounts of the same type - removed restriction

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

  // Get all transactions (for admin - all customer transactions)
  static async getAllTransactions(req, res) {
    try {
      const { 
        page = 1, 
        limit = 20,
        type,
        status,
        startDate,
        endDate 
      } = req.query;

      // Build query - get all transactions from CUSTOMER accounts only
      const customerUsers = await User.find({ customerType: 'CUSTOMER' }).select('_id');
      const customerUserIds = customerUsers.map(u => u._id);
      
      const customerAccounts = await Account.find({ userId: { $in: customerUserIds } }).select('_id');
      const customerAccountIds = customerAccounts.map(a => a._id);

      const query = {
        $or: [
          { fromAccountId: { $in: customerAccountIds } },
          { toAccountId: { $in: customerAccountIds } }
        ]
      };

      // Add filters
      if (type) {
        query.transactionType = type;
      }

      if (status) {
        query.status = status;
      }

      if (startDate || endDate) {
        query.createdAt = {};
        if (startDate) query.createdAt.$gte = new Date(startDate);
        if (endDate) query.createdAt.$lte = new Date(endDate);
      }

      // Execute query with pagination
      const skip = (parseInt(page) - 1) * parseInt(limit);
      
      const [transactions, total] = await Promise.all([
        Transaction.find(query)
          .populate('fromAccountId', 'accountNumber accountType userId')
          .populate('toAccountId', 'accountNumber accountType userId')
          .populate('initiatedBy', 'fullName email')
          .sort({ createdAt: -1 })
          .skip(skip)
          .limit(parseInt(limit)),
        Transaction.countDocuments(query)
      ]);

      // Calculate pagination info
      const totalPages = Math.ceil(total / parseInt(limit));

      res.status(200).json({
        success: true,
        message: 'Transactions retrieved successfully',
        data: transactions.map(t => {
          const transaction = {
            transaction_id: t.transactionId ? String(t.transactionId) : '',
            amount: (t.amount !== null && t.amount !== undefined) ? Number(t.amount) : 0,
            type: t.transactionType ? String(t.transactionType) : '',
            status: t.status ? String(t.status) : '',
            description: t.description ? String(t.description) : '',
            from_account_number: t.fromAccountNumber || '',
            to_account_number: t.toAccountNumber || ''
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
        }),
        meta: {
          page: parseInt(page),
          limit: parseInt(limit),
          total,
          total_pages: totalPages,
          has_next_page: parseInt(page) < totalPages,
          has_prev_page: parseInt(page) > 1
        }
      });

    } catch (error) {
      console.error('Get all transactions error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve transactions'
      });
    }
  }

  // Admin transfer money between customer accounts (no OTP required)
  static async transferMoney(req, res) {
    try {
      const { fromAccountNumber, toAccountNumber, amount, description } = req.body;
      const officerId = req.userId;

      const amountNumber = Number(amount);

      // Validation
      if (!fromAccountNumber || !toAccountNumber || Number.isNaN(amountNumber)) {
        return res.status(400).json({
          success: false,
          message: 'From account number, to account number, and valid amount are required'
        });
      }

      if (amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Amount must be greater than 0'
        });
      }

      if (amountNumber < 10000) {
        return res.status(400).json({
          success: false,
          message: 'Minimum transfer amount is 10,000 VND'
        });
      }

      // Find accounts (must be customer accounts)
      const fromAccount = await Account.findOne({
        accountNumber: fromAccountNumber,
        isActive: true
      }).populate('userId', 'customerType');

      const toAccount = await Account.findOne({
        accountNumber: toAccountNumber,
        isActive: true
      }).populate('userId', 'customerType');

      if (!fromAccount || !toAccount) {
        return res.status(404).json({
          success: false,
          message: 'One or both accounts not found'
        });
      }

      // Verify both are customer accounts
      if (fromAccount.userId.customerType !== 'CUSTOMER' || toAccount.userId.customerType !== 'CUSTOMER') {
        return res.status(403).json({
          success: false,
          message: 'Can only transfer between customer accounts'
        });
      }

      // Check if same account
      if (fromAccount._id.toString() === toAccount._id.toString()) {
        return res.status(400).json({
          success: false,
          message: 'Cannot transfer to the same account'
        });
      }

      // Calculate fee and total
      const fee = Transaction.calculateFee(amountNumber, 'TRANSFER');
      const totalAmount = amountNumber + fee;

      // Check sufficient balance
      if (!fromAccount.canDebit(totalAmount)) {
        return res.status(400).json({
          success: false,
          message: `Insufficient balance. Need ${totalAmount.toLocaleString()} VND (including fee ${fee.toLocaleString()} VND) but current balance is ${fromAccount.balance.toLocaleString()} VND`
        });
      }

      // Perform transfer
      await fromAccount.debit(totalAmount);
      await toAccount.credit(amountNumber);

      // Create transaction record
      const transactionId = Transaction.generateTransactionId();
      const transaction = new Transaction({
        transactionId,
        fromAccountId: fromAccount._id,
        toAccountId: toAccount._id,
        fromAccountNumber: fromAccount.accountNumber,
        toAccountNumber: toAccount.accountNumber,
        amount: amountNumber,
        currency: fromAccount.currency,
        description: description || `Transfer by bank officer: ${fromAccount.accountNumber} -> ${toAccount.accountNumber}`,
        transactionType: 'TRANSFER',
        status: 'COMPLETED',
        initiatedBy: officerId,
        fee,
        totalAmount,
        otpVerified: true, // Admin transfers don't need OTP
        processedAt: new Date()
      });

      await transaction.save();

      res.status(200).json({
        success: true,
        message: 'Transfer completed successfully',
        data: formatTransaction(transaction)
      });

    } catch (error) {
      console.error('Admin transfer money error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to transfer money: ' + error.message
      });
    }
  }

  // Admin deposit money to customer account
  static async depositMoney(req, res) {
    try {
      const { accountNumber, amount, description } = req.body;
      const officerId = req.userId;

      const amountNumber = Number(amount);

      // Validation
      if (!accountNumber || Number.isNaN(amountNumber)) {
        return res.status(400).json({
          success: false,
          message: 'Account number and valid amount are required'
        });
      }

      if (amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Amount must be greater than 0'
        });
      }

      // Find account (must be customer account)
      const account = await Account.findOne({
        accountNumber,
        isActive: true
      }).populate('userId', 'customerType');

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      // Verify it's a customer account
      if (account.userId.customerType !== 'CUSTOMER') {
        return res.status(403).json({
          success: false,
          message: 'Can only deposit to customer accounts'
        });
      }

      // Credit the account
      await account.credit(amountNumber);

      // Create transaction record
      const transactionId = Transaction.generateTransactionId();
      const transaction = new Transaction({
        transactionId,
        fromAccountId: account._id,
        toAccountId: account._id,
        fromAccountNumber: account.accountNumber,
        toAccountNumber: account.accountNumber,
        amount: amountNumber,
        currency: account.currency,
        description: description || `Deposit by bank officer to ${account.accountNumber}`,
        transactionType: 'DEPOSIT',
        status: 'COMPLETED',
        initiatedBy: officerId,
        fee: 0,
        totalAmount: amountNumber,
        otpVerified: true, // Admin deposits don't need OTP
        processedAt: new Date()
      });

      await transaction.save();

      res.status(200).json({
        success: true,
        message: 'Deposit completed successfully',
        data: formatTransaction(transaction)
      });

    } catch (error) {
      console.error('Admin deposit money error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to deposit money: ' + error.message
      });
    }
  }
}

module.exports = AdminController;

