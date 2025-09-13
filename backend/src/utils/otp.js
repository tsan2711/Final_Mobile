const crypto = require('crypto');

class OTPUtils {
  static generateOTP(length = 6) {
    const digits = '0123456789';
    let otp = '';
    
    for (let i = 0; i < length; i++) {
      otp += digits[Math.floor(Math.random() * digits.length)];
    }
    
    return otp;
  }

  static generateSecureOTP(length = 6) {
    const min = Math.pow(10, length - 1);
    const max = Math.pow(10, length) - 1;
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  static hashOTP(otp) {
    return crypto.createHash('sha256').update(otp.toString()).digest('hex');
  }

  static verifyOTP(inputOTP, hashedOTP) {
    const inputHash = this.hashOTP(inputOTP);
    return inputHash === hashedOTP;
  }

  static generateExpiryTime(minutes = 5) {
    return new Date(Date.now() + minutes * 60 * 1000);
  }

  static isExpired(expiryTime) {
    return new Date() > new Date(expiryTime);
  }
}

module.exports = OTPUtils;
