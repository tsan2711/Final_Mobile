const User = require('../models/User');
const { formatUser } = require('../utils/responseFormatter');

class UserController {
  // Update user profile
  static async updateProfile(req, res) {
    try {
      const { full_name, fullName, phone, address } = req.body;
      const userId = req.userId;

      // Support both full_name and fullName for compatibility
      const actualFullName = full_name || fullName;

      // Check if phone is already used by another user
      if (phone) {
        const existingUser = await User.findOne({ 
          phone, 
          _id: { $ne: userId } 
        });
        
        if (existingUser) {
          return res.status(400).json({
            success: false,
            message: 'Số điện thoại đã được sử dụng'
          });
        }
      }

      // Build update object
      const updateData = {};
      if (actualFullName) updateData.fullName = actualFullName;
      if (phone) updateData.phone = phone;
      if (address !== undefined) updateData.address = address;
      updateData.updatedAt = new Date();

      const user = await User.findByIdAndUpdate(
        userId,
        updateData,
        { new: true, runValidators: true }
      ).select('-password -refreshTokens');

      if (!user) {
        return res.status(404).json({
          success: false,
          message: 'User not found'
        });
      }

      res.json({
        success: true,
        message: 'Cập nhật thông tin thành công',
        data: formatUser(user)
      });
    } catch (error) {
      console.error('Update profile error:', error);
      
      if (error.code === 11000) {
        return res.status(400).json({
          success: false,
          message: 'Số điện thoại đã được sử dụng'
        });
      }

      res.status(500).json({
        success: false,
        message: 'Lỗi server: ' + error.message
      });
    }
  }

  // Change password
  static async changePassword(req, res) {
    try {
      const { current_password, currentPassword, new_password, newPassword, new_password_confirmation } = req.body;
      const userId = req.userId;

      // Support both naming conventions
      const actualCurrentPassword = current_password || currentPassword;
      const actualNewPassword = new_password || newPassword;

      if (!actualCurrentPassword || !actualNewPassword) {
        return res.status(400).json({
          success: false,
          message: 'Mật khẩu hiện tại và mật khẩu mới là bắt buộc'
        });
      }

      // Validate new password length
      if (actualNewPassword.length < 6) {
        return res.status(400).json({
          success: false,
          message: 'Mật khẩu mới phải có ít nhất 6 ký tự'
        });
      }

      // Check password confirmation if provided
      if (new_password_confirmation && actualNewPassword !== new_password_confirmation) {
        return res.status(400).json({
          success: false,
          message: 'Mật khẩu xác nhận không khớp'
        });
      }

      // Get user with password (password is included by default in User model)
      const user = await User.findById(userId);
      
      if (!user) {
        return res.status(404).json({
          success: false,
          message: 'User not found'
        });
      }

      // Verify current password
      const isValidPassword = await user.comparePassword(actualCurrentPassword);
      if (!isValidPassword) {
        return res.status(400).json({
          success: false,
          message: 'Mật khẩu hiện tại không đúng'
        });
      }

      // Check if new password is same as current
      const isSamePassword = await user.comparePassword(actualNewPassword);
      if (isSamePassword) {
        return res.status(400).json({
          success: false,
          message: 'Mật khẩu mới phải khác mật khẩu hiện tại'
        });
      }

      // Update password
      user.password = actualNewPassword;
      user.refreshTokens = []; // Clear all refresh tokens for security
      await user.save();

      res.json({
        success: true,
        message: 'Đổi mật khẩu thành công. Vui lòng đăng nhập lại.'
      });
    } catch (error) {
      console.error('Change password error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi server: ' + error.message
      });
    }
  }
}

module.exports = UserController;

