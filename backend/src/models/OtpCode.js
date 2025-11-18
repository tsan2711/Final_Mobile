const mongoose = require('mongoose');

const otpCodeSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  email: {
    type: String,
    required: true
  },
  otpHash: {
    type: String,
    required: true
  },
  otpType: {
    type: String,
    enum: ['LOGIN', 'TRANSACTION', 'PASSWORD_RESET', 'PHONE_VERIFICATION', 'UTILITY'],
    required: true
  },
  transactionId: {
    type: String, // For transaction OTPs
    default: null
  },
  expiresAt: {
    type: Date,
    required: true,
    default: () => new Date(Date.now() + 5 * 60 * 1000) // 5 minutes
  },
  isUsed: {
    type: Boolean,
    default: false
  },
  attempts: {
    type: Number,
    default: 0,
    max: 3
  }
}, {
  timestamps: true
});

// Index for automatic cleanup
otpCodeSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

// Index for faster queries
otpCodeSchema.index({ userId: 1, otpType: 1, isUsed: 1 });
otpCodeSchema.index({ email: 1, otpType: 1, isUsed: 1 });

// Methods
otpCodeSchema.methods.incrementAttempt = function() {
  this.attempts += 1;
  return this.save();
};

otpCodeSchema.methods.markAsUsed = function() {
  this.isUsed = true;
  return this.save();
};

otpCodeSchema.methods.isExpired = function() {
  return new Date() > this.expiresAt;
};

otpCodeSchema.methods.canAttempt = function() {
  return this.attempts < 3 && !this.isUsed && !this.isExpired();
};

// Static methods
otpCodeSchema.statics.findValidOTP = function(userId, otpType, transactionId = null) {
  const query = {
    userId,
    otpType,
    isUsed: false,
    expiresAt: { $gt: new Date() },
    attempts: { $lt: 3 }
  };

  if (transactionId) {
    query.transactionId = transactionId;
  }

  return this.findOne(query).sort({ createdAt: -1 });
};

otpCodeSchema.statics.cleanupExpired = function() {
  return this.deleteMany({
    $or: [
      { expiresAt: { $lt: new Date() } },
      { isUsed: true },
      { attempts: { $gte: 3 } }
    ]
  });
};

module.exports = mongoose.model('OtpCode', otpCodeSchema);
