const mongoose = require('mongoose');

const interestRateHistorySchema = new mongoose.Schema({
  accountId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Account',
    required: true
  },
  accountType: {
    type: String,
    enum: ['CHECKING', 'SAVING', 'MORTGAGE'],
    required: true
  },
  oldRate: {
    type: Number,
    min: 0,
    max: 100
  },
  newRate: {
    type: Number,
    required: true,
    min: 0,
    max: 100
  },
  changedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  reason: {
    type: String,
    trim: true
  },
  effectiveDate: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true
});

// Index for efficient queries
interestRateHistorySchema.index({ accountId: 1, createdAt: -1 });
interestRateHistorySchema.index({ accountType: 1, createdAt: -1 });
interestRateHistorySchema.index({ changedBy: 1 });

module.exports = mongoose.model('InterestRateHistory', interestRateHistorySchema);

