const mongoose = require('mongoose');

const paymentSchema = new mongoose.Schema({
  paymentId: {
    type: String,
    required: true,
    unique: true
  },
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  accountId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Account',
    required: true
  },
  accountNumber: {
    type: String,
    required: true
  },
  amount: {
    type: Number,
    required: true,
    min: 0
  },
  currency: {
    type: String,
    default: 'VND',
    enum: ['VND', 'USD', 'EUR']
  },
  paymentMethod: {
    type: String,
    enum: ['VNPAY', 'STRIPE', 'BANK_TRANSFER'],
    required: true
  },
  paymentType: {
    type: String,
    enum: ['DEPOSIT', 'EXTERNAL_TRANSFER', 'PAYMENT'],
    required: true
  },
  status: {
    type: String,
    enum: ['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'],
    default: 'PENDING'
  },
  description: {
    type: String,
    trim: true,
    maxlength: 500
  },
  // VNPay specific fields
  vnpay: {
    transactionId: String,
    orderId: String,
    paymentUrl: String,
    secureHash: String,
    responseCode: String,
    transactionStatus: String
  },
  // Stripe specific fields
  stripe: {
    paymentIntentId: String,
    clientSecret: String,
    chargeId: String,
    customerId: String
  },
  // Bank transfer specific fields
  bankTransfer: {
    bankName: String,
    bankCode: String,
    recipientAccountNumber: String,
    recipientName: String,
    transferReference: String
  },
  // Transaction reference if payment creates a transaction
  transactionId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Transaction'
  },
  failureReason: {
    type: String
  },
  completedAt: {
    type: Date
  },
  metadata: {
    ipAddress: String,
    userAgent: String,
    deviceInfo: String,
    returnUrl: String,
    cancelUrl: String
  }
}, {
  timestamps: true
});

// Indexes
paymentSchema.index({ paymentId: 1 }, { unique: true });
paymentSchema.index({ userId: 1, createdAt: -1 });
paymentSchema.index({ accountId: 1, createdAt: -1 });
paymentSchema.index({ status: 1, createdAt: -1 });
paymentSchema.index({ paymentMethod: 1, status: 1 });

// Static method to generate payment ID
paymentSchema.statics.generatePaymentId = function() {
  const timestamp = Date.now().toString();
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  return `PAY${timestamp}${random}`;
};

// Methods
paymentSchema.methods.markAsProcessing = function() {
  this.status = 'PROCESSING';
  return this.save();
};

paymentSchema.methods.markAsCompleted = function() {
  this.status = 'COMPLETED';
  this.completedAt = new Date();
  return this.save();
};

paymentSchema.methods.markAsFailed = function(reason) {
  this.status = 'FAILED';
  this.failureReason = reason;
  this.completedAt = new Date();
  return this.save();
};

module.exports = mongoose.model('Payment', paymentSchema);

