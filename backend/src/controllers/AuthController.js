const User = require('../models/User');
const Account = require('../models/Account');
const OtpCode = require('../models/OtpCode');
const JWTUtils = require('../utils/jwt');
const OTPUtils = require('../utils/otp');
const { formatUser } = require('../utils/responseFormatter');

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

      // Auto-create default accounts for new user
      try {
        // Create checking account with initial balance
        const checkingAccount = new Account({
          userId: user._id,
          accountNumber: Account.generateAccountNumber(),
          accountType: 'CHECKING',
          balance: 100000, // Starting balance: 100,000 VND
          interestRate: 0.5,
          currency: 'VND'
        });
        await checkingAccount.save();

        // Create savings account
        const savingsAccount = new Account({
          userId: user._id,
          accountNumber: Account.generateAccountNumber(),
          accountType: 'SAVING',
          balance: 0,
          interestRate: 5.5,
          currency: 'VND'
        });
        await savingsAccount.save();

        console.log(`✅ Auto-created accounts for new user: ${user.email}`);
        console.log(`   - Checking: ${checkingAccount.accountNumber}`);
        console.log(`   - Savings: ${savingsAccount.accountNumber}`);
      } catch (accountError) {
        console.error('Failed to create default accounts:', accountError);
        // Continue with registration even if account creation fails
      }

      // Generate tokens
      const { accessToken, refreshToken } = JWTUtils.generateTokenPair(user);

      // Save refresh token
      user.refreshTokens.push({ token: refreshToken });
      await user.save();

      res.status(201).json({
        success: true,
        message: 'User registered successfully',
        data: {
          user: formatUser(user),
          accessToken,
          refreshToken,
          token: accessToken,
          access_token: accessToken,
          refresh_token: refreshToken
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

      // Generate a temporary token for OTP verification session
      // This token will be replaced with a real token after OTP verification
      const tempToken = JWTUtils.generateTokenPair(user).accessToken;

      // In real app, send OTP via SMS/Email
      console.log(`🔐 LOGIN OTP for ${user.email}: ${otp}`);

      res.status(200).json({
        success: true,
        message: 'OTP sent to your registered email/phone',
        data: {
          user_id: user._id.toString(),
          email: user.email,
          otp_required: true,
          token: tempToken,  // Temporary token for Android compatibility
          user: formatUser(user),
          // FOR DEVELOPMENT ONLY - Remove in production
          developmentOTP: otp,
          development_otp: otp
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
      // Support both userId and otp_code (Android sends otp_code)
      const { userId, otpCode, otp_code, token } = req.body;
      const actualOtpCode = otpCode || otp_code;
      let actualUserId = userId;

      // If token is provided instead of userId, extract userId from token
      if (!actualUserId && token) {
        try {
          const decoded = JWTUtils.verifyToken(token);
          actualUserId = decoded.userId;
        } catch (e) {
          // Token might be invalid, continue with userId lookup
        }
      }

      // Validation
      if (!actualUserId || !actualOtpCode) {
        return res.status(400).json({
          success: false,
          message: 'User ID and OTP are required'
        });
      }

      // Find user
      const user = await User.findById(actualUserId);
      if (!user || !user.isActive) {
        return res.status(401).json({
          success: false,
          message: 'User not found or inactive'
        });
      }

      // Find valid OTP
      const validOTP = await OtpCode.findValidOTP(actualUserId, 'LOGIN');
      
      if (!validOTP) {
        return res.status(401).json({
          success: false,
          message: 'Invalid or expired OTP'
        });
      }

      // Verify OTP
      const isValidOTP = OTPUtils.verifyOTP(actualOtpCode, validOTP.otpHash);
      
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
          user: formatUser(user),
          accessToken,
          refreshToken,
          refresh_token: refreshToken,  // Also provide as 'refresh_token' for Android compatibility
          token: accessToken,  // Also provide as 'token' for Android compatibility
          access_token: accessToken  // Also provide as 'access_token' for Android compatibility
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
          refreshToken: newRefreshToken,
          access_token: accessToken,
          refresh_token: newRefreshToken,
          token: accessToken
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
        data: formatUser(user)
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
