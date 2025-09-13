const mongoose = require('mongoose');

const accountSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  accountNumber: {
    type: String,
    required: true,
    unique: true,
    length: 16
  },
  accountType: {
    type: String,
    enum: ['CHECKING', 'SAVING', 'MORTGAGE'],
    required: true
  },
  balance: {
    type: Number,
    default: 0,
    min: 0
  },
  interestRate: {
    type: Number,
    min: 0,
    max: 100
  },
  currency: {
    type: String,
    default: 'VND',
    enum: ['VND', 'USD', 'EUR']
  },
  isActive: {
    type: Boolean,
    default: true
  }
}, {
  timestamps: true
});

// Virtual for masked account number
accountSchema.virtual('maskedAccountNumber').get(function() {
  if (!this.accountNumber || this.accountNumber.length < 4) {
    return this.accountNumber;
  }
  const lastFour = this.accountNumber.slice(-4);
  return `**** **** **** ${lastFour}`;
});

// Virtual for formatted balance
accountSchema.virtual('formattedBalance').get(function() {
  return `${this.balance.toLocaleString()} ${this.currency}`;
});

// Methods
accountSchema.methods.canDebit = function(amount) {
  return this.isActive && this.balance >= amount;
};

accountSchema.methods.debit = function(amount) {
  if (!this.canDebit(amount)) {
    throw new Error('Insufficient balance or inactive account');
  }
  this.balance -= amount;
  return this.save();
};

accountSchema.methods.credit = function(amount) {
  if (!this.isActive) {
    throw new Error('Cannot credit to inactive account');
  }
  this.balance += amount;
  return this.save();
};

// Static method to generate account number
accountSchema.statics.generateAccountNumber = function() {
  const timestamp = Date.now().toString();
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  return (timestamp + random).slice(-16);
};

module.exports = mongoose.model('Account', accountSchema);
