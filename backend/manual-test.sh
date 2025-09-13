#!/bin/bash

echo "🧪 MANUAL API TESTING WITH CURL"
echo "================================"

BASE_URL="http://localhost:8000/api"

# Test 1: Health Check
echo ""
echo "🔍 Testing Health Check..."
curl -s http://localhost:8000/health | python3 -m json.tool
echo ""

# Test 2: API Test Endpoint
echo "🔍 Testing API Endpoint..."
curl -s $BASE_URL/test | python3 -m json.tool
echo ""

# Test 3: Login
echo "🔐 Testing Login..."
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"123456"}')

echo $LOGIN_RESPONSE | python3 -m json.tool

# Extract userId and OTP from response
USER_ID=$(echo $LOGIN_RESPONSE | python3 -c "
import sys, json
data = json.load(sys.stdin)
if 'data' in data and 'userId' in data['data']:
    print(data['data']['userId'])
else:
    print('ERROR')
")

OTP=$(echo $LOGIN_RESPONSE | python3 -c "
import sys, json
data = json.load(sys.stdin)
if 'data' in data and 'developmentOTP' in data['data']:
    print(data['data']['developmentOTP'])
else:
    print('ERROR')
")

echo ""
echo "📋 Extracted Data:"
echo "User ID: $USER_ID"
echo "OTP: $OTP"

if [ "$USER_ID" != "ERROR" ] && [ "$OTP" != "ERROR" ]; then
    echo ""
    echo "🔐 Testing OTP Verification..."
    OTP_RESPONSE=$(curl -s -X POST $BASE_URL/auth/verify-otp \
      -H "Content-Type: application/json" \
      -d "{\"userId\":\"$USER_ID\",\"otpCode\":\"$OTP\"}")
    
    echo $OTP_RESPONSE | python3 -m json.tool
    
    # Extract access token
    ACCESS_TOKEN=$(echo $OTP_RESPONSE | python3 -c "
import sys, json
data = json.load(sys.stdin)
if 'data' in data and 'accessToken' in data['data']:
    print(data['data']['accessToken'])
else:
    print('ERROR')
")
    
    if [ "$ACCESS_TOKEN" != "ERROR" ]; then
        echo ""
        echo "🏦 Testing Get Accounts..."
        curl -s -X GET $BASE_URL/accounts \
          -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool
        
        echo ""
        echo "📊 Testing Account Summary..."
        curl -s -X GET $BASE_URL/accounts/summary \
          -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool
        
        echo ""
        echo "📜 Testing Transaction History..."
        curl -s -X GET $BASE_URL/transactions/history \
          -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool
    else
        echo "❌ Failed to get access token"
    fi
else
    echo "❌ Failed to get user ID or OTP"
fi

echo ""
echo "✅ Manual testing completed!"
