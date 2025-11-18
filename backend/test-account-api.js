/**
 * Test API endpoint cho account cụ thể
 * 
 * Usage: 
 * 1. Lấy token từ app (check Android logs hoặc login)
 * 2. Chạy: node test-account-api.js YOUR_TOKEN
 */

require('dotenv').config();
const axios = require('axios');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8000';
const accountNumber = '7633962511581091';
const expectedBalance = 80000000;

async function testAccountAPI(token) {
  if (!token) {
    console.log('❌ Token is required!');
    console.log('Usage: node test-account-api.js YOUR_JWT_TOKEN');
    console.log('\nTo get token:');
    console.log('1. Login in Android app');
    console.log('2. Check SessionManager logs for token');
    console.log('3. Or check backend logs when login');
    process.exit(1);
  }

  try {
    console.log('🔍 Testing API for account:', accountNumber);
    console.log('💰 Expected balance:', expectedBalance.toLocaleString(), 'VND\n');

    // Test GET /api/accounts
    console.log('📡 Testing GET /api/accounts...');
    const accountsResponse = await axios.get(`${BASE_URL}/api/accounts`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (accountsResponse.data.success) {
      const accounts = accountsResponse.data.data;
      console.log(`✅ Found ${accounts.length} accounts\n`);

      const targetAccount = accounts.find(acc => acc.account_number === accountNumber);
      
      if (targetAccount) {
        console.log('✅ Target account found!\n');
        console.log('📊 Account Details:');
        console.log(`   Account Number: ${targetAccount.account_number}`);
        console.log(`   Account Type: ${targetAccount.account_type}`);
        console.log(`   Balance: ${targetAccount.balance} (type: ${typeof targetAccount.balance})`);
        console.log(`   Formatted Balance: ${targetAccount.formatted_balance}`);
        console.log(`   Currency: ${targetAccount.currency}`);

        // Check balance
        const apiBalance = typeof targetAccount.balance === 'string' 
          ? parseFloat(targetAccount.balance) 
          : targetAccount.balance;

        if (Math.abs(apiBalance - expectedBalance) < 0.01) {
          console.log(`\n✅ Balance matches! API returns correct balance.`);
        } else {
          console.log(`\n⚠️  Balance mismatch!`);
          console.log(`   Expected: ${expectedBalance}`);
          console.log(`   API returned: ${apiBalance}`);
        }

        // Show full response
        console.log('\n📤 Full API Response:');
        console.log(JSON.stringify(targetAccount, null, 2));

      } else {
        console.log(`❌ Account ${accountNumber} not found in API response`);
        console.log('\nAvailable accounts:');
        accounts.forEach(acc => {
          console.log(`   - ${acc.account_number}: ${acc.balance} ${acc.currency}`);
        });
      }
    } else {
      console.log('❌ API returned error:', accountsResponse.data.message);
    }

  } catch (error) {
    if (error.response) {
      console.log('❌ API Error:', error.response.status);
      console.log('Response:', error.response.data);
    } else if (error.request) {
      console.log('❌ No response from server');
      console.log('Make sure backend is running on', BASE_URL);
    } else {
      console.log('❌ Error:', error.message);
    }
    process.exit(1);
  }
}

// Get token from command line
const token = process.argv[2];
testAccountAPI(token);

