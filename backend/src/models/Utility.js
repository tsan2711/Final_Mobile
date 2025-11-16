const mongoose = require('mongoose');

const utilitySchema = new mongoose.Schema({
  transactionId: {
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
  serviceType: {
    type: String,
    enum: ['ELECTRICITY', 'WATER', 'INTERNET', 'PHONE_TOPUP', 'DATA_PACKAGE', 'SCRATCH_CARD', 'FLIGHT', 'MOVIE', 'HOTEL', 'ECOMMERCE'],
    required: true
  },
  provider: {
    type: String,
    required: true
  },
  serviceNumber: {
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
  fee: {
    type: Number,
    default: 0,
    min: 0
  },
  totalAmount: {
    type: Number,
    required: true
  },
  status: {
    type: String,
    enum: ['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'],
    default: 'PENDING'
  },
  description: {
    type: String,
    trim: true
  },
  referenceNumber: {
    type: String
  },
  processedAt: {
    type: Date
  },
  failureReason: {
    type: String
  },
  metadata: {
    customerName: String,
    period: String,
    phoneNumber: String,
    packageName: String,
    additionalInfo: mongoose.Schema.Types.Mixed
  }
}, {
  timestamps: true
});

// Indexes
utilitySchema.index({ transactionId: 1 }, { unique: true });
utilitySchema.index({ userId: 1, createdAt: -1 });
utilitySchema.index({ accountId: 1, createdAt: -1 });
utilitySchema.index({ serviceType: 1, createdAt: -1 });

// Virtual for formatted amount
utilitySchema.virtual('formattedAmount').get(function() {
  return `${this.amount.toLocaleString()} ${this.currency}`;
});

utilitySchema.virtual('formattedTotalAmount').get(function() {
  return `${this.totalAmount.toLocaleString()} ${this.currency}`;
});

// Methods
utilitySchema.methods.markAsProcessing = function() {
  this.status = 'PROCESSING';
  return this.save();
};

utilitySchema.methods.markAsCompleted = function() {
  this.status = 'COMPLETED';
  this.processedAt = new Date();
  return this.save();
};

utilitySchema.methods.markAsFailed = function(reason) {
  this.status = 'FAILED';
  this.failureReason = reason;
  this.processedAt = new Date();
  return this.save();
};

// Static method to generate transaction ID
utilitySchema.statics.generateTransactionId = function() {
  const timestamp = Date.now().toString();
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  return `UTL${timestamp}${random}`;
};

// Static method to calculate utility fee
utilitySchema.statics.calculateFee = function(amount, serviceType) {
  let fee = 0;
  
  switch(serviceType) {
    case 'ELECTRICITY':
    case 'WATER':
    case 'INTERNET':
      fee = amount * 0.01; // 1% fee
      if (fee > 20000) fee = 20000; // Max 20k VND
      break;
    case 'PHONE_TOPUP':
    case 'DATA_PACKAGE':
    case 'SCRATCH_CARD':
      fee = 0; // No fee for mobile services
      break;
    case 'FLIGHT':
    case 'HOTEL':
    case 'MOVIE':
    case 'ECOMMERCE':
      fee = 5000; // Flat fee 5k VND
      break;
    default:
      fee = 0;
  }
  
  return Math.round(fee);
};

module.exports = mongoose.model('Utility', utilitySchema);

