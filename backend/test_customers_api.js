// Test script to check if accounts_by_type is returned correctly
const axios = require('axios');

const BASE_URL = process.env.API_URL || 'http://localhost:8000/api';

async function testCustomersAPI() {
  try {
    // You need to provide a valid admin token here
    const token = process.env.ADMIN_TOKEN || 'YOUR_ADMIN_TOKEN_HERE';
    
    console.log('Testing GET /api/admin/customers...\n');
    
    const response = await axios.get(`${BASE_URL}/admin/customers?page=1&limit=5`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (response.data.success) {
      console.log('✅ API Response successful\n');
      const customers = response.data.data;
      
      console.log(`Found ${customers.length} customers:\n`);
      
      customers.forEach((customer, index) => {
        console.log(`Customer ${index + 1}: ${customer.full_name || customer.email}`);
        console.log(`  - Account count: ${customer.account_count}`);
        
        if (customer.accounts_by_type) {
          console.log(`  - Checking accounts: ${customer.accounts_by_type.checking?.length || 0}`);
          console.log(`  - Saving accounts: ${customer.accounts_by_type.saving?.length || 0}`);
          console.log(`  - Mortgage accounts: ${customer.accounts_by_type.mortgage?.length || 0}`);
          
          // Show sample account data
          if (customer.accounts_by_type.checking?.length > 0) {
            console.log(`  - Sample CHECKING account:`, JSON.stringify(customer.accounts_by_type.checking[0], null, 2));
          }
        } else {
          console.log(`  ⚠️  NO accounts_by_type field found!`);
        }
        console.log('');
      });
    } else {
      console.log('❌ API returned success=false');
      console.log('Message:', response.data.message);
    }
  } catch (error) {
    console.error('❌ Error testing API:', error.message);
    if (error.response) {
      console.error('Response status:', error.response.status);
      console.error('Response data:', error.response.data);
    }
  }
}

// Run test
testCustomersAPI();



