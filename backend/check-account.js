// Script to check if an account number exists in database
require('dotenv').config();
const mongoose = require('mongoose');
const Account = require('./src/models/Account');

const accountNumberToCheck = '7631868158854390';

async function checkAccount() {
  try {
    // Connect to MongoDB
    if (!process.env.MONGODB_URI) {
      console.error('❌ MONGODB_URI is not defined in .env file');
      console.log('Please set MONGODB_URI in .env file or run: export MONGODB_URI="mongodb://localhost:27017/banking_app"');
      return;
    }
    
    await mongoose.connect(process.env.MONGODB_URI);

    console.log('🔍 Checking account number:', accountNumberToCheck);
    console.log('='.repeat(60));

    // Normalize the account number
    const normalized = accountNumberToCheck.replace(/\s/g, '').replace(/\D/g, '');
    console.log('Normalized:', normalized);

    // 1. Try exact match
    const exactMatch = await Account.findOne({ accountNumber: normalized });
    console.log('\n1. Exact match:', exactMatch ? '✅ FOUND' : '❌ NOT FOUND');
    if (exactMatch) {
      console.log('   Account:', {
        id: exactMatch._id,
        accountNumber: exactMatch.accountNumber,
        isActive: exactMatch.isActive,
        accountType: exactMatch.accountType
      });
    }

    // 2. Try with spaces removed from DB
    const allAccounts = await Account.find().select('accountNumber isActive accountType userId');
    console.log(`\n2. Checking ${allAccounts.length} accounts in database...`);
    
    const foundAccount = allAccounts.find(acc => {
      if (!acc.accountNumber) return false;
      const cleanDbNumber = acc.accountNumber.toString().trim().replace(/\s/g, '').replace(/\D/g, '');
      return cleanDbNumber === normalized;
    });

    console.log('   Normalized search:', foundAccount ? '✅ FOUND' : '❌ NOT FOUND');
    if (foundAccount) {
      console.log('   Account:', {
        id: foundAccount._id,
        accountNumber: foundAccount.accountNumber,
        normalized: foundAccount.accountNumber.toString().trim().replace(/\s/g, '').replace(/\D/g, ''),
        isActive: foundAccount.isActive,
        accountType: foundAccount.accountType
      });
    }

    // 3. List all account numbers
    console.log('\n3. All account numbers in database:');
    allAccounts.forEach((acc, index) => {
      const clean = acc.accountNumber ? acc.accountNumber.toString().trim().replace(/\s/g, '').replace(/\D/g, '') : '';
      console.log(`   ${index + 1}. "${acc.accountNumber}" (normalized: "${clean}") - Active: ${acc.isActive}`);
    });

    // 4. Find similar account numbers
    console.log('\n4. Similar account numbers (at least 12 matching digits):');
    const similarAccounts = allAccounts.filter(acc => {
      if (!acc.accountNumber) return false;
      const cleanDbNumber = acc.accountNumber.toString().trim().replace(/\s/g, '').replace(/\D/g, '');
      let matchingDigits = 0;
      for (let i = 0; i < Math.min(cleanDbNumber.length, normalized.length); i++) {
        if (cleanDbNumber[i] === normalized[i]) matchingDigits++;
      }
      return matchingDigits >= 12;
    });

    if (similarAccounts.length > 0) {
      similarAccounts.forEach(acc => {
        const clean = acc.accountNumber.toString().trim().replace(/\s/g, '').replace(/\D/g, '');
        let matchingDigits = 0;
        for (let i = 0; i < Math.min(clean.length, normalized.length); i++) {
          if (clean[i] === normalized[i]) matchingDigits++;
        }
        console.log(`   - "${acc.accountNumber}" (${matchingDigits}/16 digits match)`);
      });
    } else {
      console.log('   No similar accounts found');
    }

  } catch (error) {
    console.error('❌ Error:', error);
  } finally {
    await mongoose.connection.close();
    console.log('\n✅ Connection closed');
  }
}

checkAccount();

