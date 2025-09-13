const jwt = require('jsonwebtoken');

class JWTUtils {
  static generateAccessToken(payload) {
    return jwt.sign(
      payload,
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRE || '24h' }
    );
  }

  static generateRefreshToken(payload) {
    return jwt.sign(
      payload,
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_REFRESH_EXPIRE || '7d' }
    );
  }

  static verifyToken(token) {
    return jwt.verify(token, process.env.JWT_SECRET);
  }

  static decodeToken(token) {
    return jwt.decode(token);
  }

  static generateTokenPair(user) {
    const payload = {
      userId: user._id,
      email: user.email,
      customerType: user.customerType
    };

    const accessToken = this.generateAccessToken(payload);
    const refreshToken = this.generateRefreshToken(payload);

    return { accessToken, refreshToken };
  }
}

module.exports = JWTUtils;
