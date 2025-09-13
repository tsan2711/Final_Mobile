require('dotenv').config();
const mongoose = require('mongoose');
const User = require('./src/models/User');
const Account = require('./src/models/Account');
const Transaction = require('./src/models/Transaction');

const createExtensiveTestData = async () => {
  try {
    // Connect to MongoDB
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });
    console.log('✅ MongoDB Connected for extensive testing');

    // DON'T clear existing data - just add more
    console.log('🔄 Adding more test data (keeping existing)...');

    // Create more diverse users
    const newUsers = [
      {
        email: 'nguyen.vana@gmail.com',
        password: '123456',
        fullName: 'Nguyễn Văn An',
        phone: '0901234567',
        address: 'Quận 1, TP.HCM',
        customerType: 'CUSTOMER',
        emailVerified: true,
        phoneVerified: true
      },
      {
        email: 'tran.thib@yahoo.com',
        password: '123456',
        fullName: 'Trần Thị Bình',
        phone: '0902345678',
        address: 'Quận 3, TP.HCM',
        customerType: 'CUSTOMER',
        emailVerified: true,
        phoneVerified: false
      },
      {
        email: 'le.vanc@outlook.com',
        password: '123456',
        fullName: 'Lê Văn Cường',
        phone: '0903456789',
        address: 'Hà Đông, Hà Nội',
        customerType: 'CUSTOMER',
        emailVerified: false,
        phoneVerified: true
      },
      {
        email: 'pham.thid@company.com',
        password: '123456',
        fullName: 'Phạm Thị Dung',
        phone: '0904567890',
        address: 'Cầu Giấy, Hà Nội',
        customerType: 'CUSTOMER',
        emailVerified: true,
        phoneVerified: true
      },
      {
        email: 'officer2@bank.com',
        password: '123456',
        fullName: 'Nguyễn Thị Loan',
        phone: '0905678901',
        address: 'Bank Head Office',
        customerType: 'BANK_OFFICER',
        emailVerified: true,
        phoneVerified: true
      }
    ];

    const createdUsers = await User.create(newUsers);
    console.log('👥 Created additional users:', createdUsers.length);

    // Create more accounts with realistic balances
    const newAccounts = [];
    
    for (let user of createdUsers) {
      if (user.customerType === 'CUSTOMER') {
        // Checking account - everyone has one
        newAccounts.push({
          userId: user._id,
          accountNumber: Account.generateAccountNumber(),
          accountType: 'CHECKING',
          balance: Math.floor(Math.random() * 50000000) + 500000, // 500K - 50M VND
          interestRate: 0.5,
          currency: 'VND'
        });

        // Savings account - 80% have one
        if (Math.random() > 0.2) {
          newAccounts.push({
            userId: user._id,
            accountNumber: Account.generateAccountNumber(),
            accountType: 'SAVING',
            balance: Math.floor(Math.random() * 100000000) + 1000000, // 1M - 100M VND
            interestRate: Math.random() * 3 + 4, // 4-7% interest
            currency: 'VND'
          });
        }

        // Mortgage account - 30% have one
        if (Math.random() > 0.7) {
          newAccounts.push({
            userId: user._id,
            accountNumber: Account.generateAccountNumber(),
            accountType: 'MORTGAGE',
            balance: Math.floor(Math.random() * 2000000000) + 500000000, // 500M - 2B VND
            interestRate: Math.random() * 3 + 7, // 7-10% interest
            currency: 'VND'
          });
        }

        // Some users have USD accounts
        if (Math.random() > 0.6) {
          newAccounts.push({
            userId: user._id,
            accountNumber: Account.generateAccountNumber(),
            accountType: 'SAVING',
            balance: Math.floor(Math.random() * 50000) + 1000, // 1K - 50K USD
            interestRate: Math.random() * 2 + 1, // 1-3% interest
            currency: 'USD'
          });
        }
      }
    }

    const createdAccounts = await Account.create(newAccounts);
    console.log('🏦 Created additional accounts:', createdAccounts.length);

    // Create sample transactions between accounts
    const allAccounts = await Account.find({ isActive: true }).populate('userId');
    const sampleTransactions = [];

    for (let i = 0; i < 15; i++) {
      const fromAccount = allAccounts[Math.floor(Math.random() * allAccounts.length)];
      let toAccount = allAccounts[Math.floor(Math.random() * allAccounts.length)];
      
      // Ensure different accounts and same currency
      while (toAccount._id.toString() === fromAccount._id.toString() || 
             toAccount.currency !== fromAccount.currency) {
        toAccount = allAccounts[Math.floor(Math.random() * allAccounts.length)];
      }

      const amount = Math.floor(Math.random() * 5000000) + 100000; // 100K - 5M VND
      const fee = Transaction.calculateFee(amount);
      
      const statuses = ['COMPLETED', 'COMPLETED', 'COMPLETED', 'FAILED', 'PENDING'];
      const status = statuses[Math.floor(Math.random() * statuses.length)];
      
      const descriptions = [
        'Chuyển tiền gia đình',
        'Thanh toán hóa đơn',
        'Trả nợ bạn bè',
        'Mua sắm online',
        'Đóng học phí',
        'Chi phí y tế',
        'Tiền thuê nhà',
        'Lương thưởng'
      ];

      sampleTransactions.push({
        transactionId: Transaction.generateTransactionId(),
        fromAccountId: fromAccount._id,
        toAccountId: toAccount._id,
        fromAccountNumber: fromAccount.accountNumber,
        toAccountNumber: toAccount.accountNumber,
        amount,
        currency: fromAccount.currency,
        description: descriptions[Math.floor(Math.random() * descriptions.length)],
        transactionType: 'TRANSFER',
        status,
        initiatedBy: fromAccount.userId._id,
        fee,
        totalAmount: amount + fee,
        processedAt: status === 'COMPLETED' ? new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000) : null,
        otpVerified: status === 'COMPLETED',
        failureReason: status === 'FAILED' ? 'Insufficient balance' : null,
        createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000) // Random within last 30 days
      });
    }

    const createdTransactions = await Transaction.create(sampleTransactions);
    console.log('💸 Created sample transactions:', createdTransactions.length);

    // Display comprehensive test data summary
    console.log('\n📊 COMPREHENSIVE TEST DATA CREATED:');
    console.log('='.repeat(60));
    
    const allUsers = await User.find({});
    const allUserAccounts = await Account.find({ isActive: true });
    const allTransactions = await Transaction.find({});

    console.log(`\n👥 Total Users: ${allUsers.length}`);
    console.log(`   - Customers: ${allUsers.filter(u => u.customerType === 'CUSTOMER').length}`);
    console.log(`   - Bank Officers: ${allUsers.filter(u => u.customerType === 'BANK_OFFICER').length}`);

    console.log(`\n🏦 Total Accounts: ${allUserAccounts.length}`);
    console.log(`   - Checking: ${allUserAccounts.filter(a => a.accountType === 'CHECKING').length}`);
    console.log(`   - Savings: ${allUserAccounts.filter(a => a.accountType === 'SAVING').length}`);
    console.log(`   - Mortgage: ${allUserAccounts.filter(a => a.accountType === 'MORTGAGE').length}`);
    console.log(`   - VND Accounts: ${allUserAccounts.filter(a => a.currency === 'VND').length}`);
    console.log(`   - USD Accounts: ${allUserAccounts.filter(a => a.currency === 'USD').length}`);

    console.log(`\n💸 Total Transactions: ${allTransactions.length}`);
    console.log(`   - Completed: ${allTransactions.filter(t => t.status === 'COMPLETED').length}`);
    console.log(`   - Pending: ${allTransactions.filter(t => t.status === 'PENDING').length}`);
    console.log(`   - Failed: ${allTransactions.filter(t => t.status === 'FAILED').length}`);

    // Calculate total money in system
    const totalVND = allUserAccounts
      .filter(a => a.currency === 'VND')
      .reduce((sum, a) => sum + a.balance, 0);
    const totalUSD = allUserAccounts
      .filter(a => a.currency === 'USD')
      .reduce((sum, a) => sum + a.balance, 0);

    console.log(`\n💰 Total Money in System:`);
    console.log(`   - VND: ${totalVND.toLocaleString()} VND`);
    console.log(`   - USD: ${totalUSD.toLocaleString()} USD`);

    console.log('\n🧪 TEST ACCOUNTS FOR ANDROID APP:');
    console.log('='.repeat(40));
    
    const testUsers = await User.find({ customerType: 'CUSTOMER' }).limit(3);
    for (let user of testUsers) {
      const userAccounts = await Account.find({ userId: user._id, isActive: true });
      console.log(`\n👤 ${user.fullName}`);
      console.log(`   📧 Email: ${user.email}`);
      console.log(`   🔑 Password: 123456`);
      console.log(`   📱 Phone: ${user.phone}`);
      userAccounts.forEach(acc => {
        console.log(`   💳 ${acc.accountType}: ${acc.accountNumber} - ${acc.formattedBalance}`);
      });
    }

    console.log('\n🎉 Extensive test data creation completed!');
    
  } catch (error) {
    console.error('❌ Test data creation failed:', error);
  } finally {
    await mongoose.connection.close();
    console.log('📴 MongoDB connection closed');
  }
};

// Run the test data creation
createExtensiveTestData();
