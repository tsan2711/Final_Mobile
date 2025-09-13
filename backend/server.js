require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 8000;
const HOST = process.env.HOST || '0.0.0.0';

// Middleware
app.use(cors());
app.use(express.json());

// MongoDB Connection
mongoose.connect(process.env.MONGODB_URI)
.then(() => console.log('✅ MongoDB Connected'))
.catch(err => console.error('❌ MongoDB connection error:', err));

// Import routes
const authRoutes = require('./src/routes/auth');
const accountRoutes = require('./src/routes/accounts');
const transactionRoutes = require('./src/routes/transactions');

// Routes
app.get('/health', (req, res) => {
  res.json({ 
    status: 'OK', 
    timestamp: new Date().toISOString(),
    message: 'Banking API is running!',
    database: 'MongoDB connected'
  });
});

app.get('/api/test', (req, res) => {
  res.json({
    success: true,
    message: 'API connection successful!',
    android_app: 'Ready to connect'
  });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/accounts', accountRoutes);
app.use('/api/transactions', transactionRoutes);

// Start server
app.listen(PORT, HOST, () => {
  console.log(`🚀 Server running on http://${HOST}:${PORT}`);
  console.log(`📱 Android app can connect to: http://YOUR_IP:${PORT}/api/`);
  console.log(`🔗 Health check: http://${HOST}:${PORT}/health`);
});
