#!/bin/bash

# Banking Backend Startup Script
# This script starts MongoDB and Node.js server

cd "/Users/tsangcuteso1/Documents/GitHub/CKSOA/My project/Final_Mobile/backend"

echo "🚀 Starting Banking Backend..."
echo "================================"

# Check if MongoDB is running
if ! lsof -i :27017 > /dev/null 2>&1; then
    echo "🔄 Starting MongoDB..."
    
    # Try different MongoDB data paths
    if [ -d "/opt/homebrew/var/mongodb" ]; then
        mongod --dbpath /opt/homebrew/var/mongodb --logpath /opt/homebrew/var/log/mongodb/mongo.log --fork 2>/dev/null
    elif [ -d "/usr/local/var/mongodb" ]; then
        mongod --dbpath /usr/local/var/mongodb --logpath /usr/local/var/log/mongodb/mongo.log --fork 2>/dev/null
    else
        echo "⚠️  MongoDB data directory not found. Trying default..."
        mongod --fork --logpath /tmp/mongodb.log 2>/dev/null
    fi
    
    sleep 2
    
    if lsof -i :27017 > /dev/null 2>&1; then
        echo "✅ MongoDB started successfully"
    else
        echo "❌ Failed to start MongoDB. Please check MongoDB installation."
        exit 1
    fi
else
    echo "✅ MongoDB is already running"
fi

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "⚠️  .env file not found. Creating default .env..."
    cat > .env << 'EOF'
# Server Configuration
NODE_ENV=development
PORT=8000
HOST=0.0.0.0

# Database Configuration  
MONGODB_URI=mongodb://localhost:27017/banking_app

# JWT Configuration
JWT_SECRET=banking-app-super-secret-jwt-key-2024-secure-make-it-long-and-random
JWT_EXPIRE=24h
JWT_REFRESH_EXPIRE=7d

# CORS Configuration
CORS_ORIGIN=*

# Rate Limiting
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100

# OTP Configuration
OTP_EXPIRE_MINUTES=5
OTP_LENGTH=6
EOF
    echo "✅ Created .env file"
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Check if port 8000 is already in use
if lsof -i :8000 > /dev/null 2>&1; then
    echo "⚠️  Port 8000 is already in use. Trying to kill existing process..."
    lsof -ti :8000 | xargs kill -9 2>/dev/null
    sleep 1
fi

# Start Node.js server
echo ""
echo "🚀 Starting Node.js server..."
echo "================================"
node server.js

