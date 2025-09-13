const mongoose = require('mongoose');

const transactionSchema = new mongoose.Schema({
  transactionId: {
    type: String,
    required: true
  },
  fromAccountId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Account',
    required: true
  },
  toAccountId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Account',
    required: true
  },
  fromAccountNumber: {
    type: String,
    required: true
  },
  toAccountNumber: {
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
  description: {
    type: String,
    trim: true,
    maxlength: 200
  },
  transactionType: {
    type: String,
    enum: ['TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT'],
    default: 'TRANSFER'
  },
  status: {
    type: String,
    enum: ['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'],
    default: 'PENDING'
  },
  initiatedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  fee: {
    type: Number,
    default: 0,
    min: 0
  },
  totalAmount: {
    type: Number,
    required: true
  },
  processedAt: {
    type: Date
  },
  failureReason: {
    type: String
  },
  otpVerified: {
    type: Boolean,
    default: false
  },
  metadata: {
    ipAddress: String,
    userAgent: String,
    deviceInfo: String
  }
}, {
  timestamps: true
});

// Indexes for better performance
transactionSchema.index({ transactionId: 1 }, { unique: true });
transactionSchema.index({ fromAccountId: 1, createdAt: -1 });
transactionSchema.index({ toAccountId: 1, createdAt: -1 });
transactionSchema.index({ initiatedBy: 1, createdAt: -1 });
transactionSchema.index({ status: 1, createdAt: -1 });

// Virtual for formatted amount
transactionSchema.virtual('formattedAmount').get(function() {
  return `${this.amount.toLocaleString()} ${this.currency}`;
});

transactionSchema.virtual('formattedTotalAmount').get(function() {
  return `${this.totalAmount.toLocaleString()} ${this.currency}`;
});

// Methods
transactionSchema.methods.markAsProcessing = function() {
  this.status = 'PROCESSING';
  return this.save();
};

transactionSchema.methods.markAsCompleted = function() {
  this.status = 'COMPLETED';
  this.processedAt = new Date();
  return this.save();
};

transactionSchema.methods.markAsFailed = function(reason) {
  this.status = 'FAILED';
  this.failureReason = reason;
  this.processedAt = new Date();
  return this.save();
};

transactionSchema.methods.markOtpVerified = function() {
  this.otpVerified = true;
  return this.save();
};

// Static method to generate transaction ID
transactionSchema.statics.generateTransactionId = function() {
  const timestamp = Date.now().toString();
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  return `TXN${timestamp}${random}`;
};

// Static method to calculate transaction fee
transactionSchema.statics.calculateFee = function(amount, transactionType = 'TRANSFER') {
  // Simple fee calculation - in real app this would be more complex
  let fee = 0;
  
  if (transactionType === 'TRANSFER') {
    if (amount <= 100000) { // <= 100K VND
      fee = 0;
    } else if (amount <= 1000000) { // <= 1M VND
      fee = 5000;
    } else if (amount <= 10000000) { // <= 10M VND
      fee = 10000;
    } else {
      fee = 20000;
    }
  }
  
  return fee;
};

module.exports = mongoose.model('Transaction', transactionSchema);
