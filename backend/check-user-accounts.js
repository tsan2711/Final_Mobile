/**
 * Script để kiểm tra user và accounts
 */

require('dotenv').config();
const mongoose = require('mongoose');
const Account = require('./src/models/Account');
const User = require('./src/models/User');

async function checkUserAccounts() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('✅ Connected to MongoDB\n');

    // Find both users
    const user1 = await User.findById('691b4a99df94e180e26eb14e');
    const user2 = await User.findById('691b4a99df94e180e26eb14d');

    console.log('👤 USER 1 (691b4a99df94e180e26eb14e):');
    if (user1) {
      console.log(`   Email: ${user1.email}`);
      console.log(`   Name: ${user1.fullName}`);
      console.log(`   Phone: ${user1.phone}`);
    } else {
      console.log('   ❌ User not found');
    }

    const accounts1 = await Account.find({ userId: '691b4a99df94e180e26eb14e', isActive: true });
    console.log(`   Accounts: ${accounts1.length}`);
    accounts1.forEach(acc => {
      console.log(`     - ${acc.accountNumber}: ${acc.balance.toLocaleString()} VND (${acc.accountType})`);
    });

    console.log('\n👤 USER 2 (691b4a99df94e180e26eb14d):');
    if (user2) {
      console.log(`   Email: ${user2.email}`);
      console.log(`   Name: ${user2.fullName}`);
      console.log(`   Phone: ${user2.phone}`);
    } else {
      console.log('   ❌ User not found');
    }

    const accounts2 = await Account.find({ userId: '691b4a99df94e180e26eb14d', isActive: true });
    console.log(`   Accounts: ${accounts2.length}`);
    accounts2.forEach(acc => {
      console.log(`     - ${acc.accountNumber}: ${acc.balance.toLocaleString()} VND (${acc.accountType})`);
    });

    console.log('\n📝 SOLUTION:');
    console.log('   Option 1: Login với user có account 7633962511581091');
    if (user2) {
      console.log(`      Email: ${user2.email}`);
      console.log(`      Password: (check seed.js or create new password)`);
    }
    console.log('\n   Option 2: Update balance của account đang dùng (7633962511599891)');
    console.log('      db.accounts.updateOne(');
    console.log('        {accountNumber: "7633962511599891"},');
    console.log('        {$set: {balance: 80000000}}');
    console.log('      )');

    process.exit(0);
  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

checkUserAccounts();

