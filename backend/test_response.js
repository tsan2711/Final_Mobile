// Quick test to check if accounts_by_type is in response
const axios = require('axios');

async function test() {
  try {
    // You'll need to replace with actual admin token
    const response = await axios.get('http://localhost:8000/api/admin/customers?page=1&limit=1', {
      headers: {
        'Authorization': 'Bearer YOUR_TOKEN_HERE'
      }
    });
    
    if (response.data.success && response.data.data.length > 0) {
      const customer = response.data.data[0];
      console.log('Customer:', customer.full_name || customer.email);
      console.log('Has accounts_by_type:', !!customer.accounts_by_type);
      if (customer.accounts_by_type) {
        console.log('Checking:', customer.accounts_by_type.checking?.length || 0);
        console.log('Saving:', customer.accounts_by_type.saving?.length || 0);
        console.log('Mortgage:', customer.accounts_by_type.mortgage?.length || 0);
      } else {
        console.log('❌ accounts_by_type is missing!');
      }
    }
  } catch (error) {
    console.error('Error:', error.message);
  }
}

test();
