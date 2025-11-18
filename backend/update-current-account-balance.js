/**
 * Update balance của account đang được app sử dụng
 * Account: 7633962511599891 (user2@example.com)
 */

require('dotenv').config();
const mongoose = require('mongoose');
const Account = require('./src/models/Account');

async function updateBalance() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('✅ Connected to MongoDB\n');

    const accountNumber = '7633962511599891';
    const newBalance = 80000000; // 80,000,000 VND

    const account = await Account.findOne({ accountNumber, isActive: true });
    
    if (!account) {
      console.log(`❌ Account ${accountNumber} not found!`);
      process.exit(1);
    }

    console.log('📊 Current Account Info:');
    console.log(`   Account Number: ${account.accountNumber}`);
    console.log(`   Current Balance: ${account.balance.toLocaleString()} VND`);
    console.log(`   Account Type: ${account.accountType}`);
    console.log(`   User ID: ${account.userId}\n`);

    console.log(`🔄 Updating balance to: ${newBalance.toLocaleString()} VND...`);
    
    account.balance = newBalance;
    await account.save();

    console.log(`✅ Balance updated successfully!\n`);

    // Verify
    const updated = await Account.findOne({ accountNumber });
    console.log(`✅ Verified: ${updated.balance.toLocaleString()} VND\n`);

    console.log('📝 Next steps:');
    console.log('   1. Restart backend server (if running)');
    console.log('   2. In Android app, click Refresh button (🔄)');
    console.log('   3. Balance should now show: 80,000,000 VND');

    process.exit(0);
  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

updateBalance();

