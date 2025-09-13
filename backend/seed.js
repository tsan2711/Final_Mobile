require('dotenv').config();
const mongoose = require('mongoose');
const User = require('./src/models/User');
const Account = require('./src/models/Account');

const seedData = async () => {
  try {
    // Connect to MongoDB
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });
    
    console.log('✅ MongoDB Connected for seeding');

    // Clear existing data
    await User.deleteMany({});
    await Account.deleteMany({});
    console.log('🗑️  Cleared existing data');

    // Create test users
    const users = [
      {
        email: 'admin@bank.com',
        password: '123456',
        fullName: 'Bank Administrator',
        phone: '0987654321',
        address: 'Ha Noi, Vietnam',
        customerType: 'BANK_OFFICER',
        emailVerified: true,
        phoneVerified: true
      },
      {
        email: 'customer@example.com',
        password: '123456',
        fullName: 'Nguyen Van A',
        phone: '0123456789',
        address: 'Ho Chi Minh City, Vietnam',
        customerType: 'CUSTOMER',
        emailVerified: true,
        phoneVerified: true
      },
      {
        email: 'user2@example.com',
        password: '123456',
        fullName: 'Tran Thi B',
        phone: '0987123456',
        address: 'Da Nang, Vietnam',
        customerType: 'CUSTOMER',
        emailVerified: true,
        phoneVerified: true
      }
    ];

    const createdUsers = await User.create(users);
    console.log('👥 Created users:', createdUsers.length);

    // Create accounts for each user
    const accounts = [];
    
    for (let user of createdUsers) {
      if (user.customerType === 'CUSTOMER') {
        // Checking account
        accounts.push({
          userId: user._id,
          accountNumber: Account.generateAccountNumber(),
          accountType: 'CHECKING',
          balance: Math.floor(Math.random() * 10000000) + 1000000, // 1M - 10M VND
          interestRate: 0.5,
          currency: 'VND'
        });

        // Savings account
        accounts.push({
          userId: user._id,
          accountNumber: Account.generateAccountNumber(),
          accountType: 'SAVING',
          balance: Math.floor(Math.random() * 50000000) + 5000000, // 5M - 50M VND
          interestRate: 5.5,
          currency: 'VND'
        });

        // Mortgage account (some users)
        if (Math.random() > 0.5) {
          accounts.push({
            userId: user._id,
            accountNumber: Account.generateAccountNumber(),
            accountType: 'MORTGAGE',
            balance: Math.floor(Math.random() * 500000000) + 100000000, // 100M - 500M VND
            interestRate: 8.5,
            currency: 'VND'
          });
        }
      }
    }

    const createdAccounts = await Account.create(accounts);
    console.log('🏦 Created accounts:', createdAccounts.length);

    // Display sample data
    console.log('\n📊 Sample Data Created:');
    console.log('='.repeat(50));
    
    for (let user of createdUsers) {
      console.log(`\n👤 ${user.fullName} (${user.customerType})`);
      console.log(`   📧 Email: ${user.email}`);
      console.log(`   📱 Phone: ${user.phone}`);
      console.log(`   🔑 Password: 123456`);
      
      if (user.customerType === 'CUSTOMER') {
        const userAccounts = createdAccounts.filter(acc => 
          acc.userId.toString() === user._id.toString()
        );
        
        userAccounts.forEach(acc => {
          console.log(`   💳 ${acc.accountType}: ${acc.accountNumber} - ${acc.formattedBalance}`);
        });
      }
    }

    console.log('\n🎉 Seeding completed successfully!');
    console.log('\n🔧 Test with these credentials:');
    console.log('📧 Email: customer@example.com');
    console.log('🔑 Password: 123456');
    
  } catch (error) {
    console.error('❌ Seeding failed:', error);
  } finally {
    await mongoose.connection.close();
    console.log('📴 MongoDB connection closed');
  }
};

// Run seeding
seedData();
