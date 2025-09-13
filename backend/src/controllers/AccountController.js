const Account = require('../models/Account');
const User = require('../models/User');

class AccountController {
  // Get all accounts for logged in user
  static async getUserAccounts(req, res) {
    try {
      const userId = req.userId;

      const accounts = await Account.find({ 
        userId, 
        isActive: true 
      }).sort({ accountType: 1, createdAt: -1 });

      if (!accounts || accounts.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'No accounts found'
        });
      }

      // Calculate totals
      const totals = {
        totalBalance: 0,
        checkingBalance: 0,
        savingBalance: 0,
        mortgageBalance: 0,
        accountCount: accounts.length
      };

      accounts.forEach(account => {
        totals.totalBalance += account.balance;
        
        switch (account.accountType) {
          case 'CHECKING':
            totals.checkingBalance += account.balance;
            break;
          case 'SAVING':
            totals.savingBalance += account.balance;
            break;
          case 'MORTGAGE':
            totals.mortgageBalance += account.balance;
            break;
        }
      });

      res.status(200).json({
        success: true,
        message: 'Accounts retrieved successfully',
        data: {
          accounts,
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
          account,
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
          accountNumber: account.maskedAccountNumber,
          accountType: account.accountType,
          balance: account.balance,
          formattedBalance: account.formattedBalance,
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

      // Only return basic info for security
      const account = await Account.findOne({ 
        accountNumber, 
        isActive: true 
      }).populate('userId', 'fullName email').select('accountNumber accountType userId');

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
          accountNumber: account.maskedAccountNumber,
          accountType: account.accountType,
          ownerName: account.userId.fullName,
          // Don't return balance for security
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
        data: {
          account: primaryAccount
        }
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

      if (!accounts || accounts.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'No accounts found'
        });
      }

      // Group accounts by type
      const summary = {
        checking: accounts.filter(acc => acc.accountType === 'CHECKING'),
        saving: accounts.filter(acc => acc.accountType === 'SAVING'),
        mortgage: accounts.filter(acc => acc.accountType === 'MORTGAGE')
      };

      // Calculate totals
      const totals = {
        totalAssets: summary.checking.reduce((sum, acc) => sum + acc.balance, 0) +
                    summary.saving.reduce((sum, acc) => sum + acc.balance, 0),
        totalLiabilities: summary.mortgage.reduce((sum, acc) => sum + acc.balance, 0),
        netWorth: 0
      };

      totals.netWorth = totals.totalAssets - totals.totalLiabilities;

      // Get primary account for quick access
      const primaryAccount = summary.checking[0] || null;

      res.status(200).json({
        success: true,
        message: 'Account summary retrieved successfully',
        data: {
          summary,
          totals,
          primaryAccount,
          accountCounts: {
            checking: summary.checking.length,
            saving: summary.saving.length,
            mortgage: summary.mortgage.length,
            total: accounts.length
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
}

module.exports = AccountController;
