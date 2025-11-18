const mongoose = require('mongoose');

const ekycVerificationSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    unique: true
  },
  faceImage: {
    type: String, // Base64 encoded image or file path
    required: false
  },
  faceImageUrl: {
    type: String, // URL to stored image file
    required: false
  },
  verificationStatus: {
    type: String,
    enum: ['PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED'],
    default: 'PENDING'
  },
  verifiedAt: {
    type: Date
  },
  expiresAt: {
    type: Date,
    default: function() {
      // Verification expires after 1 year
      const expiry = new Date();
      expiry.setFullYear(expiry.getFullYear() + 1);
      return expiry;
    }
  },
  rejectionReason: {
    type: String
  },
  metadata: {
    deviceInfo: String,
    ipAddress: String,
    userAgent: String,
    imageQuality: String, // 'HIGH', 'MEDIUM', 'LOW'
    faceDetected: Boolean,
    verificationScore: Number // 0-100, confidence score
  },
  lastVerifiedForTransaction: {
    transactionId: String,
    verifiedAt: Date,
    amount: Number
  }
}, {
  timestamps: true
});

// Indexes
ekycVerificationSchema.index({ userId: 1 }, { unique: true });
ekycVerificationSchema.index({ verificationStatus: 1 });
ekycVerificationSchema.index({ expiresAt: 1 });

// Check if verification is valid
ekycVerificationSchema.methods.isValid = function() {
  if (this.verificationStatus !== 'VERIFIED') {
    return false;
  }
  
  if (this.expiresAt && this.expiresAt < new Date()) {
    return false;
  }
  
  return true;
};

// Mark as verified
ekycVerificationSchema.methods.markAsVerified = function(metadata = {}) {
  this.verificationStatus = 'VERIFIED';
  this.verifiedAt = new Date();
  if (metadata.verificationScore) {
    this.metadata.verificationScore = metadata.verificationScore;
  }
  return this.save();
};

// Mark as rejected
ekycVerificationSchema.methods.markAsRejected = function(reason) {
  this.verificationStatus = 'REJECTED';
  this.rejectionReason = reason;
  return this.save();
};

// Update last verification for transaction
ekycVerificationSchema.methods.updateLastTransactionVerification = function(transactionId, amount) {
  this.lastVerifiedForTransaction = {
    transactionId,
    verifiedAt: new Date(),
    amount
  };
  return this.save();
};

module.exports = mongoose.model('EkycVerification', ekycVerificationSchema);

