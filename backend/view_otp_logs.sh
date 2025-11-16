#!/bin/bash
# Script to view OTP logs in real-time
cd "$(dirname "$0")"
echo "📱 Watching for OTP logs... (Press Ctrl+C to stop)"
echo "=========================================="
tail -f server.log | grep --line-buffered -i "🔐\|OTP\|otp"

