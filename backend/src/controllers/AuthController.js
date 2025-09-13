const User = require('../models/User');
const OtpCode = require('../models/OtpCode');
const JWTUtils = require('../utils/jwt');
const OTPUtils = require('../utils/otp');

class AuthController {
  // Register new user
  static async register(req, res) {
    try {
      const { email, password, fullName, phone, address } = req.body;

      // Validation
      if (!email || !password || !fullName || !phone) {
        return res.status(400).json({
          success: false,
          message: 'Email, password, full name, and phone are required'
        });
      }

      // Check if user already exists
      const existingUser = await User.findOne({
        $or: [{ email }, { phone }]
      });

      if (existingUser) {
        return res.status(400).json({
          success: false,
          message: 'User already exists with this email or phone'
        });
      }

      // Create user
      const user = new User({
        email: email.toLowerCase(),
        password,
        fullName,
        phone,
        address: address || ''
      });

      await user.save();

      // Generate tokens
      const { accessToken, refreshToken } = JWTUtils.generateTokenPair(user);

      // Save refresh token
      user.refreshTokens.push({ token: refreshToken });
      await user.save();

      res.status(201).json({
        success: true,
        message: 'User registered successfully',
        data: {
          user: user.toJSON(),
          accessToken,
          refreshToken
        }
      });

    } catch (error) {
      console.error('Register error:', error);
      
      if (error.code === 11000) {
        return res.status(400).json({
          success: false,
          message: 'Email or phone already exists'
        });
      }

      res.status(500).json({
        success: false,
        message: 'Registration failed'
      });
    }
  }

  // Login user
  static async login(req, res) {
    try {
      const { email, password } = req.body;

      // Validation
      if (!email || !password) {
        return res.status(400).json({
          success: false,
          message: 'Email and password are required'
        });
      }

      // Find user
      const user = await User.findOne({ 
        email: email.toLowerCase(),
        isActive: true 
      });

      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'Invalid email or password'
        });
      }

      // Check password
      const isValidPassword = await user.comparePassword(password);
      
      if (!isValidPassword) {
        return res.status(401).json({
          success: false,
          message: 'Invalid email or password'
        });
      }

      // Generate and send OTP
      const otp = OTPUtils.generateOTP(6);
      const otpHash = OTPUtils.hashOTP(otp);

      // Save OTP to database
      const otpCode = new OtpCode({
        userId: user._id,
        email: user.email,
        otpHash,
        otpType: 'LOGIN',
        expiresAt: OTPUtils.generateExpiryTime(5)
      });

      await otpCode.save();

      // In real app, send OTP via SMS/Email
      console.log(`🔐 LOGIN OTP for ${user.email}: ${otp}`);

      res.status(200).json({
        success: true,
        message: 'OTP sent to your registered email/phone',
        data: {
          userId: user._id,
          email: user.email,
          requiresOTP: true,
          // FOR DEVELOPMENT ONLY - Remove in production
          developmentOTP: otp
        }
      });

    } catch (error) {
      console.error('Login error:', error);
      res.status(500).json({
        success: false,
        message: 'Login failed'
      });
    }
  }

  // Verify OTP and complete login
  static async verifyLoginOTP(req, res) {
    try {
      const { userId, otpCode } = req.body;

      // Validation
      if (!userId || !otpCode) {
        return res.status(400).json({
          success: false,
          message: 'User ID and OTP are required'
        });
      }

      // Find user
      const user = await User.findById(userId);
      if (!user || !user.isActive) {
        return res.status(401).json({
          success: false,
          message: 'User not found or inactive'
        });
      }

      // Find valid OTP
      const validOTP = await OtpCode.findValidOTP(userId, 'LOGIN');
      
      if (!validOTP) {
        return res.status(401).json({
          success: false,
          message: 'Invalid or expired OTP'
        });
      }

      // Verify OTP
      const isValidOTP = OTPUtils.verifyOTP(otpCode, validOTP.otpHash);
      
      if (!isValidOTP) {
        await validOTP.incrementAttempt();
        
        return res.status(401).json({
          success: false,
          message: 'Invalid OTP',
          attemptsLeft: 3 - validOTP.attempts - 1
        });
      }

      // Mark OTP as used
      await validOTP.markAsUsed();

      // Update last login
      user.lastLogin = new Date();
      await user.save();

      // Generate tokens
      const { accessToken, refreshToken } = JWTUtils.generateTokenPair(user);

      // Save refresh token
      user.refreshTokens.push({ token: refreshToken });
      await user.save();

      res.status(200).json({
        success: true,
        message: 'Login successful',
        data: {
          user: user.toJSON(),
          accessToken,
          refreshToken
        }
      });

    } catch (error) {
      console.error('Verify OTP error:', error);
      res.status(500).json({
        success: false,
        message: 'OTP verification failed'
      });
    }
  }

  // Refresh access token
  static async refreshToken(req, res) {
    try {
      const { refreshToken } = req.body;

      if (!refreshToken) {
        return res.status(400).json({
          success: false,
          message: 'Refresh token is required'
        });
      }

      // Verify refresh token
      const decoded = JWTUtils.verifyToken(refreshToken);
      
      // Find user with this refresh token
      const user = await User.findOne({
        _id: decoded.userId,
        'refreshTokens.token': refreshToken,
        isActive: true
      });

      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'Invalid refresh token'
        });
      }

      // Generate new tokens
      const { accessToken, refreshToken: newRefreshToken } = JWTUtils.generateTokenPair(user);

      // Remove old refresh token and add new one
      user.refreshTokens = user.refreshTokens.filter(rt => rt.token !== refreshToken);
      user.refreshTokens.push({ token: newRefreshToken });
      await user.save();

      res.status(200).json({
        success: true,
        message: 'Token refreshed successfully',
        data: {
          accessToken,
          refreshToken: newRefreshToken
        }
      });

    } catch (error) {
      console.error('Refresh token error:', error);
      res.status(401).json({
        success: false,
        message: 'Token refresh failed'
      });
    }
  }

  // Logout user
  static async logout(req, res) {
    try {
      const { refreshToken } = req.body;
      const user = req.user;

      if (refreshToken && user) {
        // Remove refresh token
        user.refreshTokens = user.refreshTokens.filter(rt => rt.token !== refreshToken);
        await user.save();
      }

      res.status(200).json({
        success: true,
        message: 'Logout successful'
      });

    } catch (error) {
      console.error('Logout error:', error);
      res.status(500).json({
        success: false,
        message: 'Logout failed'
      });
    }
  }

  // Get current user
  static async getMe(req, res) {
    try {
      const user = req.user;

      res.status(200).json({
        success: true,
        data: {
          user: user.toJSON()
        }
      });

    } catch (error) {
      console.error('Get me error:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to get user data'
      });
    }
  }
}

module.exports = AuthController;
