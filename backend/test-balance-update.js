/**
 * Script để test balance update
 * 
 * Usage:
 * 1. Update balance trong MongoDB
 * 2. Chạy script này để test API
 * 3. Xem logs để debug
 */

require('dotenv').config();
const mongoose = require('mongoose');
const Account = require('./src/models/Account');
const User = require('./src/models/User');

async function testBalanceUpdate() {
  try {
    // Connect to database
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('✅ Connected to MongoDB');

    // Find a test user
    const user = await User.findOne({ email: { $exists: true } });
    if (!user) {
      console.log('❌ No user found. Please create a user first.');
      process.exit(1);
    }

    console.log(`\n👤 Testing with user: ${user.email} (${user._id})`);

    // Find user's checking account
    const account = await Account.findOne({ 
      userId: user._id, 
      accountType: 'CHECKING',
      isActive: true 
    });

    if (!account) {
      console.log('❌ No checking account found for user');
      process.exit(1);
    }

    console.log(`\n📊 Current Account Info:`);
    console.log(`   Account ID: ${account._id}`);
    console.log(`   Account Number: ${account.accountNumber}`);
    console.log(`   Current Balance: ${account.balance} ${account.currency}`);
    console.log(`   Balance Type: ${typeof account.balance}`);

    // Test: Update balance
    const newBalance = 99999999; // 99,999,999 VND
    console.log(`\n🔄 Updating balance to: ${newBalance} VND`);
    
    account.balance = newBalance;
    await account.save();

    console.log(`✅ Balance updated in database`);

    // Verify update
    const updatedAccount = await Account.findById(account._id);
    console.log(`\n✅ Verified Balance: ${updatedAccount.balance} ${updatedAccount.currency}`);
    console.log(`   Balance Type: ${typeof updatedAccount.balance}`);

    // Test API response format
    const { formatAccount } = require('./src/utils/responseFormatter');
    const formatted = formatAccount(updatedAccount);
    
    console.log(`\n📤 Formatted for API:`);
    console.log(`   Balance: ${formatted.balance} (type: ${typeof formatted.balance})`);
    console.log(`   Formatted Balance: ${formatted.formatted_balance}`);

    console.log(`\n✅ Test completed!`);
    console.log(`\n📝 Next steps:`);
    console.log(`   1. Restart backend server (if running)`);
    console.log(`   2. Open Android app`);
    console.log(`   3. Click refresh button (🔄) on Home screen`);
    console.log(`   4. Check logs in Android Studio Logcat`);
    console.log(`   5. Balance should show: ${formatted.formatted_balance}`);

    process.exit(0);

  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

testBalanceUpdate();

