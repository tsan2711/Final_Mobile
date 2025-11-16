#!/bin/bash

# Script to reset database and seed with fresh data
# This will fix "No accounts found" error

echo "🔄 Resetting Database and Seeding Fresh Data..."
echo "================================================"
echo ""

# Check if we're in the backend directory
if [ ! -f "seed.js" ]; then
    echo "❌ Error: seed.js not found!"
    echo "Please run this script from the backend directory:"
    echo "  cd backend"
    echo "  ./reset-and-seed.sh"
    exit 1
fi

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Error: Node.js is not installed!"
    echo "Please install Node.js first."
    exit 1
fi

# Check if MongoDB is running
echo "🔍 Checking if MongoDB is running..."
if lsof -Pi :27017 -sTCP:LISTEN -t >/dev/null ; then
    echo "✅ MongoDB is running"
else
    echo "⚠️  MongoDB is not running. Starting MongoDB..."
    
    # Try to start MongoDB
    if command -v mongod &> /dev/null; then
        mongod --dbpath /opt/homebrew/var/mongodb --logpath /opt/homebrew/var/log/mongodb/mongo.log --fork
        sleep 2
        
        if lsof -Pi :27017 -sTCP:LISTEN -t >/dev/null ; then
            echo "✅ MongoDB started successfully"
        else
            echo "❌ Failed to start MongoDB"
            echo "Please start MongoDB manually:"
            echo "  mongod --dbpath /opt/homebrew/var/mongodb --logpath /opt/homebrew/var/log/mongodb/mongo.log --fork"
            exit 1
        fi
    else
        echo "❌ mongod command not found"
        echo "Please install and start MongoDB first"
        exit 1
    fi
fi

echo ""
echo "🗑️  Clearing old data and creating fresh seed data..."
echo ""

# Run seed script
node seed.js

echo ""
echo "✅ Database reset complete!"
echo ""
echo "📝 You can now login with:"
echo "   Email: customer@example.com"
echo "   Password: 123456"
echo ""
echo "🚀 To start the server:"
echo "   node server.js"
echo ""



