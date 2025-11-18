const Account = require('../models/Account');
const User = require('../models/User');
const { formatAccount } = require('../utils/responseFormatter');

class AccountController {
  // Get all accounts for logged in user
  static async getUserAccounts(req, res) {
    try {
      const userId = req.userId;
      console.log(`[DEBUG] 🔄 Getting accounts for userId: ${userId}`);

      const accounts = await Account.find({ 
        userId, 
        isActive: true 
      }).sort({ accountType: 1, createdAt: -1 });

      console.log(`[DEBUG] 📊 Found ${accounts.length} accounts from database`);
      
      // Log balance for each account
      accounts.forEach(account => {
        console.log(`[DEBUG] 💰 Account ID: ${account._id}, Number: ${account.accountNumber}, Balance: ${account.balance}, Type: ${account.accountType}`);
      });

      // Initialize totals
      const totals = {
        totalBalance: 0,
        checkingBalance: 0,
        savingBalance: 0,
        mortgageBalance: 0,
        accountCount: accounts ? accounts.length : 0
      };

      // Calculate totals if accounts exist
      if (accounts && accounts.length > 0) {
        accounts.forEach(account => {
          totals.totalBalance += account.balance || 0;
          
          switch (account.accountType) {
            case 'CHECKING':
              totals.checkingBalance += account.balance || 0;
              break;
            case 'SAVING':
              totals.savingBalance += account.balance || 0;
              break;
            case 'MORTGAGE':
              totals.mortgageBalance += account.balance || 0;
              break;
          }
        });
      }

      const formattedAccounts = accounts && accounts.length > 0 
        ? accounts.map(formatAccount) 
        : [];

      // Log formatted accounts
      console.log(`[DEBUG] 📤 Sending ${formattedAccounts.length} formatted accounts to client`);
      formattedAccounts.forEach(acc => {
        console.log(`[DEBUG] 📤 Account ${acc.account_number}: balance=${acc.balance} (type: ${typeof acc.balance})`);
      });

      // Always return 200 with empty array if no accounts
      res.status(200).json({
        success: true,
        message: accounts && accounts.length > 0 
          ? 'Accounts retrieved successfully' 
          : 'No accounts found',
        data: formattedAccounts,
        meta: {
          totals
        }
      });

    } catch (error) {
      console.error('Get user accounts error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve accounts'
      });
    }
  }

  // Get specific account by ID
  static async getAccount(req, res) {
    try {
      const { accountId } = req.params;
      const userId = req.userId;

      const account = await Account.findOne({ 
        _id: accountId, 
        userId, 
        isActive: true 
      });

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      // Calculate monthly interest (for savings accounts)
      let monthlyInterest = 0;
      if (account.accountType === 'SAVING' && account.interestRate) {
        monthlyInterest = (account.balance * account.interestRate / 100) / 12;
      }

      // Calculate monthly payment (for mortgage accounts)
      let monthlyPayment = 0;
      if (account.accountType === 'MORTGAGE' && account.interestRate) {
        // Simple calculation - in real app would use proper loan calculation
        monthlyPayment = (account.balance * account.interestRate / 100) / 12;
      }

      res.status(200).json({
        success: true,
        message: 'Account retrieved successfully',
        data: {
          account: formatAccount(account),
          calculations: {
            monthlyInterest,
            monthlyPayment,
            annualInterest: account.balance * (account.interestRate || 0) / 100
          }
        }
      });

    } catch (error) {
      console.error('Get account error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve account'
      });
    }
  }

  // Get account balance
  static async getAccountBalance(req, res) {
    try {
      const { accountId } = req.params;
      const userId = req.userId;

      const account = await Account.findOne({ 
        _id: accountId, 
        userId, 
        isActive: true 
      }).select('accountNumber accountType balance currency');

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      res.status(200).json({
        success: true,
        message: 'Balance retrieved successfully',
        data: {
          account_number: account.maskedAccountNumber,
          account_type: account.accountType,
          balance: account.balance,
          formatted_balance: account.formattedBalance,
          currency: account.currency,
          timestamp: new Date().toISOString()
        }
      });

    } catch (error) {
      console.error('Get account balance error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve balance'
      });
    }
  }

  // Get account by account number (for transfers)
  static async getAccountByNumber(req, res) {
    try {
      const { accountNumber } = req.params;

      // Normalize account number by removing all spaces
      const normalizedAccountNumber = (accountNumber || '').toString().trim().replace(/\s/g, '');

      // Only return basic info for security
      // First try exact match
      let account = await Account.findOne({ 
        accountNumber: normalizedAccountNumber, 
        isActive: true 
      }).populate('userId', 'fullName email').select('accountNumber accountType userId');

      // If not found, try finding by removing spaces from stored account numbers
      if (!account) {
        const allAccounts = await Account.find({ isActive: true }).populate('userId', 'fullName email').select('accountNumber accountType userId');
        account = allAccounts.find(acc => {
          if (!acc.accountNumber) return false;
          const cleanDbNumber = acc.accountNumber.toString().replace(/\s/g, '');
          return cleanDbNumber === normalizedAccountNumber;
        });
      }

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      res.status(200).json({
        success: true,
        message: 'Account found',
        data: {
          account_number: account.maskedAccountNumber,
          account_type: account.accountType,
          owner_name: account.userId.fullName
        }
      });

    } catch (error) {
      console.error('Get account by number error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to find account'
      });
    }
  }

  // Get primary account (checking account)
  static async getPrimaryAccount(req, res) {
    try {
      const userId = req.userId;

      const primaryAccount = await Account.findOne({ 
        userId, 
        accountType: 'CHECKING', 
        isActive: true 
      });

      if (!primaryAccount) {
        return res.status(404).json({
          success: false,
          message: 'Primary account not found'
        });
      }

      res.status(200).json({
        success: true,
        message: 'Primary account retrieved successfully',
        data: formatAccount(primaryAccount)
      });

    } catch (error) {
      console.error('Get primary account error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve primary account'
      });
    }
  }

  // Get account summary for dashboard
  static async getAccountSummary(req, res) {
    try {
      const userId = req.userId;

      const accounts = await Account.find({ 
        userId, 
        isActive: true 
      });

      // Initialize summary structure
      const summary = {
        checking: accounts ? accounts.filter(acc => acc.accountType === 'CHECKING') : [],
        saving: accounts ? accounts.filter(acc => acc.accountType === 'SAVING') : [],
        mortgage: accounts ? accounts.filter(acc => acc.accountType === 'MORTGAGE') : []
      };

      // Calculate totals
      const totals = {
        totalAssets: summary.checking.reduce((sum, acc) => sum + (acc.balance || 0), 0) +
                    summary.saving.reduce((sum, acc) => sum + (acc.balance || 0), 0),
        totalLiabilities: summary.mortgage.reduce((sum, acc) => sum + (acc.balance || 0), 0),
        netWorth: 0
      };

      totals.netWorth = totals.totalAssets - totals.totalLiabilities;

      // Get primary account for quick access
      const primaryAccount = summary.checking.length > 0 ? summary.checking[0] : null;

      // Always return 200 even if no accounts
      res.status(200).json({
        success: true,
        message: accounts && accounts.length > 0
          ? 'Account summary retrieved successfully'
          : 'No accounts found',
        data: {
          summary: {
            checking: summary.checking.map(formatAccount),
            saving: summary.saving.map(formatAccount),
            mortgage: summary.mortgage.map(formatAccount)
          },
          totals,
          primaryAccount: primaryAccount ? formatAccount(primaryAccount) : null,
          accountCounts: {
            checking: summary.checking.length,
            saving: summary.saving.length,
            mortgage: summary.mortgage.length,
            total: accounts ? accounts.length : 0
          }
        }
      });

    } catch (error) {
      console.error('Get account summary error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to retrieve account summary'
      });
    }
  }

  // Deposit money to account
  static async depositMoney(req, res) {
    try {
      const { accountId, amount, description } = req.body;
      const userId = req.userId;

      // Validation
      const amountNumber = Number(amount);
      if (!accountId || Number.isNaN(amountNumber)) {
        return res.status(400).json({
          success: false,
          message: 'Account ID and valid amount are required'
        });
      }

      if (amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Amount must be greater than 0'
        });
      }

      // Find account
      const account = await Account.findOne({
        _id: accountId,
        userId,
        isActive: true
      });

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      // Credit the account
      await account.credit(amountNumber);

      // Create transaction record
      const Transaction = require('../models/Transaction');
      const transactionId = Transaction.generateTransactionId();
      
      const transaction = new Transaction({
        transactionId,
        fromAccountId: account._id,
        toAccountId: account._id,
        fromAccountNumber: account.accountNumber,
        toAccountNumber: account.accountNumber,
        amount: amountNumber,
        currency: account.currency,
        description: description || 'Deposit to account',
        transactionType: 'DEPOSIT',
        status: 'COMPLETED',
        initiatedBy: userId,
        fee: 0,
        totalAmount: amountNumber,
        otpVerified: true,
        processedAt: new Date()
      });

      await transaction.save();

      res.status(200).json({
        success: true,
        message: 'Deposit completed successfully',
        data: {
          transaction_id: transactionId,
          account: formatAccount(account),
          amount: amountNumber,
          new_balance: account.balance,
          formatted_balance: account.formattedBalance
        }
      });

    } catch (error) {
      console.error('Deposit money error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to deposit money'
      });
    }
  }

  // Withdraw money from account
  static async withdrawMoney(req, res) {
    try {
      const { accountId, amount, description } = req.body;
      const userId = req.userId;

      // Validation
      const amountNumber = Number(amount);
      if (!accountId || Number.isNaN(amountNumber)) {
        return res.status(400).json({
          success: false,
          message: 'Account ID and valid amount are required'
        });
      }

      if (amountNumber <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Amount must be greater than 0'
        });
      }

      // Find account
      const account = await Account.findOne({
        _id: accountId,
        userId,
        isActive: true
      });

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      // Check sufficient balance
      if (!account.canDebit(amountNumber)) {
        return res.status(400).json({
          success: false,
          message: 'Insufficient balance'
        });
      }

      // Debit the account
      await account.debit(amountNumber);

      // Create transaction record
      const Transaction = require('../models/Transaction');
      const transactionId = Transaction.generateTransactionId();
      
      const transaction = new Transaction({
        transactionId,
        fromAccountId: account._id,
        toAccountId: account._id,
        fromAccountNumber: account.accountNumber,
        toAccountNumber: account.accountNumber,
        amount: amountNumber,
        currency: account.currency,
        description: description || 'Withdrawal from account',
        transactionType: 'WITHDRAWAL',
        status: 'COMPLETED',
        initiatedBy: userId,
        fee: 0,
        totalAmount: amountNumber,
        otpVerified: true,
        processedAt: new Date()
      });

      await transaction.save();

      res.status(200).json({
        success: true,
        message: 'Withdrawal completed successfully',
        data: {
          transaction_id: transactionId,
          account: formatAccount(account),
          amount: amountNumber,
          new_balance: account.balance,
          formatted_balance: account.formattedBalance
        }
      });

    } catch (error) {
      console.error('Withdraw money error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to withdraw money'
      });
    }
  }

  // Create default accounts for user (if they don't have any)
  static async createDefaultAccounts(req, res) {
    try {
      const userId = req.userId;

      // Check if user already has accounts
      const existingAccounts = await Account.find({ userId, isActive: true });
      
      if (existingAccounts && existingAccounts.length > 0) {
        return res.status(400).json({
          success: false,
          message: 'User already has accounts',
          data: existingAccounts.map(formatAccount)
        });
      }

      // Create checking account with initial balance
      const checkingAccount = new Account({
        userId,
        accountNumber: Account.generateAccountNumber(),
        accountType: 'CHECKING',
        balance: 100000, // Starting balance: 100,000 VND
        interestRate: 0.5,
        currency: 'VND'
      });
      await checkingAccount.save();

      // Create savings account
      const savingsAccount = new Account({
        userId,
        accountNumber: Account.generateAccountNumber(),
        accountType: 'SAVING',
        balance: 0,
        interestRate: 5.5,
        currency: 'VND'
      });
      await savingsAccount.save();

      console.log(`✅ Created default accounts for user: ${userId}`);

      const accounts = [checkingAccount, savingsAccount];

      res.status(201).json({
        success: true,
        message: 'Default accounts created successfully',
        data: accounts.map(formatAccount)
      });

    } catch (error) {
      console.error('Create default accounts error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to create default accounts'
      });
    }
  }

  // Get interest projection for account
  static async getInterestProjection(req, res) {
    try {
      const { accountId } = req.params;
      const userId = req.userId;
      const { months = 12 } = req.query; // Default to 12 months

      // Find account
      const account = await Account.findOne({
        _id: accountId,
        userId,
        isActive: true
      });

      if (!account) {
        return res.status(404).json({
          success: false,
          message: 'Account not found'
        });
      }

      // Only calculate for saving accounts
      if (account.accountType !== 'SAVING') {
        return res.status(400).json({
          success: false,
          message: 'Interest projection is only available for saving accounts'
        });
      }

      const interestRate = account.interestRate || 0;
      const currentBalance = account.balance || 0;
      const monthsNum = parseInt(months);

      if (monthsNum < 1 || monthsNum > 60) {
        return res.status(400).json({
          success: false,
          message: 'Months must be between 1 and 60'
        });
      }

      // Calculate projections
      const projections = [];
      let balance = currentBalance;
      const monthlyRate = interestRate / 100 / 12; // Monthly interest rate

      for (let month = 1; month <= monthsNum; month++) {
        // Simple interest calculation (monthly compounding)
        const monthlyInterest = balance * monthlyRate;
        balance += monthlyInterest;

        projections.push({
          month: month,
          balance: Math.round(balance * 100) / 100,
          monthly_interest: Math.round(monthlyInterest * 100) / 100,
          cumulative_interest: Math.round((balance - currentBalance) * 100) / 100
        });
      }

      const totalInterest = balance - currentBalance;

      res.status(200).json({
        success: true,
        message: 'Interest projection calculated successfully',
        data: {
          account: {
            id: account._id.toString(),
            account_number: account.accountNumber,
            account_type: account.accountType,
            current_balance: currentBalance,
            interest_rate: interestRate
          },
          projection: {
            months: monthsNum,
            current_balance: currentBalance,
            projected_balance: Math.round(balance * 100) / 100,
            total_interest: Math.round(totalInterest * 100) / 100,
            monthly_details: projections
          }
        }
      });

    } catch (error) {
      console.error('Get interest projection error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to calculate interest projection: ' + error.message
      });
    }
  }
}

module.exports = AccountController;
