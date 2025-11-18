require('dotenv').config();
const axios = require('axios');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8000';
const API_URL = `${BASE_URL}/api`;

// Test credentials (you need to login first to get token)
const ADMIN_EMAIL = 'admin@bank.com';
const ADMIN_PASSWORD = '123456';

let authToken = '';

async function login() {
  try {
    console.log('🔐 Logging in as admin...');
    const response = await axios.post(`${API_URL}/auth/login`, {
      email: ADMIN_EMAIL,
      password: ADMIN_PASSWORD
    });
    
    if (response.data.success && response.data.data && response.data.data.token) {
      authToken = response.data.data.token;
      console.log('✅ Login successful!');
      return true;
    } else {
      console.error('❌ Login failed:', response.data);
      return false;
    }
  } catch (error) {
    console.error('❌ Login error:', error.response?.data || error.message);
    return false;
  }
}

async function testRoute(method, endpoint, data = null) {
  try {
    const config = {
      method: method.toLowerCase(),
      url: `${API_URL}${endpoint}`,
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      }
    };
    
    if (data && (method === 'POST' || method === 'PUT')) {
      config.data = data;
    }
    
    const response = await axios(config);
    console.log(`✅ ${method} ${endpoint} - Status: ${response.status}`);
    return { success: true, data: response.data };
  } catch (error) {
    const status = error.response?.status || 'N/A';
    const message = error.response?.data?.message || error.message;
    console.error(`❌ ${method} ${endpoint} - Status: ${status}, Error: ${message}`);
    return { success: false, error: message, status };
  }
}

async function runTests() {
  console.log('🧪 Testing Admin Routes\n');
  
  // Login first
  const loggedIn = await login();
  if (!loggedIn) {
    console.error('❌ Cannot proceed without authentication');
    return;
  }
  
  console.log('\n📋 Testing Customer Management Routes:\n');
  
  // Test GET /api/admin/customers
  await testRoute('GET', '/admin/customers?page=1&limit=5');
  
  // Test GET /api/admin/customers/search
  await testRoute('GET', '/admin/customers/search?query=test');
  
  // Test POST /api/admin/customers
  const newCustomer = {
    email: `test${Date.now()}@example.com`,
    password: '123456',
    fullName: 'Test Customer',
    phone: `090${Date.now().toString().slice(-7)}`,
    address: 'Test Address'
  };
  const createResult = await testRoute('POST', '/admin/customers', newCustomer);
  
  // If customer created, test update
  if (createResult.success && createResult.data?.data?.id) {
    const customerId = createResult.data.data.id;
    console.log(`\n📝 Customer created with ID: ${customerId}\n`);
    
    // Test GET /api/admin/customers/:customerId
    await testRoute('GET', `/admin/customers/${customerId}`);
    
    // Test PUT /api/admin/customers/:customerId
    await testRoute('PUT', `/admin/customers/${customerId}`, {
      fullName: 'Updated Test Customer',
      address: 'Updated Address'
    });
  }
  
  console.log('\n📋 Testing Account Management Routes:\n');
  
  // Test POST /api/admin/accounts/create (need a valid customer ID)
  await testRoute('POST', '/admin/accounts/create', {
    customerId: 'dummy_id',
    accountType: 'CHECKING',
    initialBalance: 100000
  });
  
  console.log('\n📋 Testing Dashboard Route:\n');
  
  // Test GET /api/admin/dashboard
  await testRoute('GET', '/admin/dashboard');
  
  console.log('\n✅ All tests completed!\n');
}

runTests().catch(console.error);

