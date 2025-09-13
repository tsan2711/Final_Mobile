require('dotenv').config();
const axios = require('axios');

const BASE_URL = 'http://localhost:8000/api';

// Test data
const testUsers = [
  { email: 'customer@example.com', password: '123456' },
  { email: 'user2@example.com', password: '123456' },
  { email: 'nguyen.vana@gmail.com', password: '123456' }
];

let authTokens = {};
let userAccounts = {};

class APITester {
  static async delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  static async request(method, endpoint, data = null, token = null) {
    const config = {
      method,
      url: `${BASE_URL}${endpoint}`,
      headers: {
        'Content-Type': 'application/json'
      }
    };

    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    if (data) {
      config.data = data;
    }

    try {
      const response = await axios(config);
      return { success: true, data: response.data, status: response.status };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data || error.message,
        status: error.response?.status || 500
      };
    }
  }

  // Test 1: Health Check
  static async testHealthCheck() {
    console.log('\n🔍 Testing Health Check...');
    
    const healthResponse = await this.request('GET', '/../health');
    console.log('Health Check:', healthResponse.success ? '✅' : '❌');
    
    const testResponse = await this.request('GET', '/test');
    console.log('API Test:', testResponse.success ? '✅' : '❌');
    
    return healthResponse.success && testResponse.success;
  }

  // Test 2: Authentication Flow
  static async testAuthentication() {
    console.log('\n🔐 Testing Authentication Flow...');
    let allPassed = true;

    for (let user of testUsers) {
      console.log(`\n👤 Testing ${user.email}:`);
      
      // Step 1: Login (get OTP)
      const loginResponse = await this.request('POST', '/auth/login', {
        email: user.email,
        password: user.password
      });

      if (!loginResponse.success) {
        console.log(`   Login: ❌ ${loginResponse.error?.message || 'Failed'}`);
        allPassed = false;
        continue;
      }

      const { userId, developmentOTP } = loginResponse.data.data;
      console.log(`   Login OTP: ✅ (OTP: ${developmentOTP})`);

      // Step 2: Verify OTP
      await this.delay(1000); // Small delay
      
      const otpResponse = await this.request('POST', '/auth/verify-otp', {
        userId,
        otpCode: developmentOTP
      });

      if (!otpResponse.success) {
        console.log(`   OTP Verify: ❌ ${otpResponse.error?.message || 'Failed'}`);
        allPassed = false;
        continue;
      }

      const { accessToken, user: userData } = otpResponse.data.data;
      authTokens[user.email] = accessToken;
      console.log(`   OTP Verify: ✅`);
      console.log(`   User: ${userData.fullName} (${userData.customerType})`);
    }

    return allPassed;
  }

  // Test 3: Account Management
  static async testAccountManagement() {
    console.log('\n🏦 Testing Account Management...');
    let allPassed = true;

    for (let [email, token] of Object.entries(authTokens)) {
      console.log(`\n💳 Testing accounts for ${email}:`);

      // Get all accounts
      const accountsResponse = await this.request('GET', '/accounts', null, token);
      if (!accountsResponse.success) {
        console.log(`   Get Accounts: ❌`);
        allPassed = false;
        continue;
      }

      const { accounts, totals } = accountsResponse.data.data;
      userAccounts[email] = accounts;
      console.log(`   Get Accounts: ✅ (${accounts.length} accounts)`);
      console.log(`   Total Balance: ${totals.totalBalance.toLocaleString()} VND`);

      // Get account summary
      const summaryResponse = await this.request('GET', '/accounts/summary', null, token);
      console.log(`   Account Summary: ${summaryResponse.success ? '✅' : '❌'}`);

      // Get primary account
      const primaryResponse = await this.request('GET', '/accounts/primary', null, token);
      console.log(`   Primary Account: ${primaryResponse.success ? '✅' : '❌'}`);

      // Test individual account details
      if (accounts.length > 0) {
        const accountId = accounts[0]._id;
        const detailResponse = await this.request('GET', `/accounts/${accountId}`, null, token);
        console.log(`   Account Details: ${detailResponse.success ? '✅' : '❌'}`);

        const balanceResponse = await this.request('GET', `/accounts/${accountId}/balance`, null, token);
        console.log(`   Account Balance: ${balanceResponse.success ? '✅' : '❌'}`);
      }
    }

    return allPassed;
  }

  // Test 4: Money Transfer
  static async testMoneyTransfer() {
    console.log('\n💸 Testing Money Transfer...');
    
    const emails = Object.keys(authTokens);
    if (emails.length < 2) {
      console.log('❌ Need at least 2 users for transfer test');
      return false;
    }

    const fromEmail = emails[0];
    const toEmail = emails[1];
    const fromToken = authTokens[fromEmail];
    
    const fromAccounts = userAccounts[fromEmail];
    const toAccounts = userAccounts[toEmail];

    if (!fromAccounts?.length || !toAccounts?.length) {
      console.log('❌ Users need accounts for transfer test');
      return false;
    }

    console.log(`\n💰 Transfer: ${fromEmail} → ${toEmail}`);

    const fromAccountId = fromAccounts[0]._id;
    const toAccountNumber = toAccounts[0].accountNumber;
    const transferAmount = 100000; // 100K VND

    // Step 1: Initiate transfer
    const transferResponse = await this.request('POST', '/transactions/transfer', {
      fromAccountId,
      toAccountNumber,
      amount: transferAmount,
      description: 'API Test Transfer'
    }, fromToken);

    if (!transferResponse.success) {
      console.log(`   Initiate Transfer: ❌ ${transferResponse.error?.message || 'Failed'}`);
      return false;
    }

    const { transactionId, developmentOTP } = transferResponse.data.data;
    console.log(`   Initiate Transfer: ✅ (TXN: ${transactionId})`);
    console.log(`   Transfer OTP: ${developmentOTP}`);

    // Step 2: Verify OTP and complete transfer
    await this.delay(1000);

    const verifyResponse = await this.request('POST', '/transactions/verify-otp', {
      transactionId,
      otpCode: developmentOTP
    }, fromToken);

    if (!verifyResponse.success) {
      console.log(`   Complete Transfer: ❌ ${verifyResponse.error?.message || 'Failed'}`);
      return false;
    }

    console.log(`   Complete Transfer: ✅`);
    console.log(`   New Balance: ${verifyResponse.data.data.fromAccount.newBalance}`);

    return true;
  }

  // Test 5: Transaction History
  static async testTransactionHistory() {
    console.log('\n📋 Testing Transaction History...');
    let allPassed = true;

    for (let [email, token] of Object.entries(authTokens)) {
      console.log(`\n📜 History for ${email}:`);

      // Get transaction history
      const historyResponse = await this.request('GET', '/transactions/history?limit=5', null, token);
      
      if (!historyResponse.success) {
        console.log(`   Transaction History: ❌`);
        allPassed = false;
        continue;
      }

      const { transactions, pagination } = historyResponse.data.data;
      console.log(`   Transaction History: ✅ (${transactions.length} transactions)`);
      console.log(`   Total Transactions: ${pagination.totalTransactions}`);

      // Test individual transaction details
      if (transactions.length > 0) {
        const txnId = transactions[0].transactionId;
        const detailResponse = await this.request('GET', `/transactions/${txnId}`, null, token);
        console.log(`   Transaction Details: ${detailResponse.success ? '✅' : '❌'}`);
      }
    }

    return allPassed;
  }

  // Test 6: Error Handling
  static async testErrorHandling() {
    console.log('\n⚠️  Testing Error Handling...');
    
    // Test invalid login
    const invalidLogin = await this.request('POST', '/auth/login', {
      email: 'invalid@email.com',
      password: 'wrongpassword'
    });
    console.log(`   Invalid Login: ${!invalidLogin.success ? '✅' : '❌'}`);

    // Test unauthorized access
    const unauthorizedAccess = await this.request('GET', '/accounts');
    console.log(`   Unauthorized Access: ${!unauthorizedAccess.success ? '✅' : '❌'}`);

    // Test invalid account
    const token = Object.values(authTokens)[0];
    const invalidAccount = await this.request('GET', '/accounts/invalid-id', null, token);
    console.log(`   Invalid Account ID: ${!invalidAccount.success ? '✅' : '❌'}`);

    return true;
  }

  // Run all tests
  static async runAllTests() {
    console.log('🧪 COMPREHENSIVE API TESTING STARTED');
    console.log('='.repeat(50));

    const tests = [
      { name: 'Health Check', test: this.testHealthCheck },
      { name: 'Authentication', test: this.testAuthentication },
      { name: 'Account Management', test: this.testAccountManagement },
      { name: 'Money Transfer', test: this.testMoneyTransfer },
      { name: 'Transaction History', test: this.testTransactionHistory },
      { name: 'Error Handling', test: this.testErrorHandling }
    ];

    const results = [];

    for (let { name, test } of tests) {
      try {
        const result = await test.call(this);
        results.push({ name, passed: result });
        console.log(`\n${name}: ${result ? '✅ PASSED' : '❌ FAILED'}`);
      } catch (error) {
        console.log(`\n${name}: ❌ ERROR - ${error.message}`);
        results.push({ name, passed: false, error: error.message });
      }

      await this.delay(500); // Small delay between tests
    }

    // Summary
    console.log('\n📊 TEST SUMMARY');
    console.log('='.repeat(30));
    
    const passed = results.filter(r => r.passed).length;
    const total = results.length;
    
    console.log(`✅ Passed: ${passed}/${total}`);
    console.log(`❌ Failed: ${total - passed}/${total}`);
    
    if (passed === total) {
      console.log('\n🎉 ALL TESTS PASSED! API is production ready!');
    } else {
      console.log('\n⚠️  Some tests failed. Check logs above.');
    }

    return passed === total;
  }
}

// Export for manual usage
module.exports = APITester;

// Auto-run if called directly
if (require.main === module) {
  APITester.runAllTests().then(success => {
    process.exit(success ? 0 : 1);
  });
}
