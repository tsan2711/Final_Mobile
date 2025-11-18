/**
 * Script để kiểm tra balance của account cụ thể
 * 
 * Usage: node check-account-balance.js
 */

require('dotenv').config();
const mongoose = require('mongoose');
const Account = require('./src/models/Account');
const User = require('./src/models/User');
const { formatAccount } = require('./src/utils/responseFormatter');

async function checkAccountBalance() {
  try {
    // Connect to database
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('✅ Connected to MongoDB\n');

    const accountNumber = '7633962511581091';
    const expectedBalance = 80000000;

    console.log(`🔍 Checking account: ${accountNumber}`);
    console.log(`💰 Expected balance: ${expectedBalance.toLocaleString()} VND\n`);

    // Find account by account number
    const account = await Account.findOne({ 
      accountNumber: accountNumber,
      isActive: true 
    }).populate('userId', 'email fullName');

    if (!account) {
      console.log('❌ Account not found!');
      console.log(`   Account Number: ${accountNumber}`);
      console.log(`   Make sure account exists and is active.`);
      process.exit(1);
    }

    console.log('✅ Account found!\n');
    console.log('📊 Account Details:');
    console.log(`   Account ID: ${account._id}`);
    console.log(`   Account Number: ${account.accountNumber}`);
    console.log(`   Account Type: ${account.accountType}`);
    console.log(`   User ID: ${account.userId}`);
    
    if (account.userId && account.userId.email) {
      console.log(`   User Email: ${account.userId.email}`);
      console.log(`   User Name: ${account.userId.fullName}`);
    }
    
    console.log(`\n💰 Balance Information:`);
    console.log(`   Raw Balance (from DB): ${account.balance}`);
    console.log(`   Balance Type: ${typeof account.balance}`);
    console.log(`   Currency: ${account.currency}`);
    
    // Check if balance matches expected
    const balanceNumber = typeof account.balance === 'string' 
      ? parseFloat(account.balance) 
      : account.balance;
    
    if (Math.abs(balanceNumber - expectedBalance) < 0.01) {
      console.log(`   ✅ Balance matches expected value!`);
    } else {
      console.log(`   ⚠️  Balance does NOT match expected value!`);
      console.log(`      Expected: ${expectedBalance}`);
      console.log(`      Actual: ${balanceNumber}`);
      console.log(`      Difference: ${Math.abs(balanceNumber - expectedBalance)}`);
    }

    // Test formatted response
    console.log(`\n📤 API Response Format:`);
    const formatted = formatAccount(account);
    console.log(`   Formatted Balance: ${formatted.balance} (type: ${typeof formatted.balance})`);
    console.log(`   Formatted Balance String: ${formatted.formatted_balance}`);
    
    // Test what Android app would receive
    console.log(`\n📱 What Android App Would Receive:`);
    const mockResponse = {
      success: true,
      data: [formatted]
    };
    console.log(JSON.stringify(mockResponse, null, 2));

    // Check all accounts for this user
    if (account.userId) {
      const allAccounts = await Account.find({ 
        userId: account.userId._id,
        isActive: true 
      });
      
      console.log(`\n📋 All Accounts for User:`);
      allAccounts.forEach(acc => {
        console.log(`   - ${acc.accountNumber}: ${acc.balance} ${acc.currency} (${acc.accountType})`);
      });
    }

    console.log(`\n✅ Check completed!\n`);
    console.log(`📝 Next steps:`);
    console.log(`   1. If balance is correct in DB but wrong in app:`);
    console.log(`      - Restart backend server`);
    console.log(`      - Refresh app (click 🔄 button)`);
    console.log(`      - Check Android logs for [DEBUG] messages`);
    console.log(`   2. If balance is wrong in DB:`);
    console.log(`      - Update it using MongoDB`);
    console.log(`      - Then refresh app`);

    process.exit(0);

  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

checkAccountBalance();

