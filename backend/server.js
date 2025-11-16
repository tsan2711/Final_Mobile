require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const errorHandler = require('./src/middleware/errorHandler');

const app = express();
const PORT = process.env.PORT || 8000;
const HOST = process.env.HOST || '0.0.0.0';

// Middleware
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
}));

// CRITICAL: Override res.send globally to prevent HTML responses
// This must be before all other middleware
app.use((req, res, next) => {
  const originalSend = res.send;
  const originalStatus = res.status.bind(res);
  
  // Override res.send to catch HTML and convert to JSON
  res.send = function(data) {
    // Force JSON for API routes
    if (req.path.startsWith('/api/') || req.path.startsWith('/health') || req.path.startsWith('/api/test')) {
      res.setHeader('Content-Type', 'application/json');
      
      // If HTML response, convert to JSON error
      if (typeof data === 'string') {
        const trimmed = data.trim();
        if (trimmed.startsWith('<!') || trimmed.startsWith('<html') || trimmed.startsWith('<!DOCTYPE')) {
          return res.json({
            success: false,
            message: 'Route not found',
            path: req.path,
            method: req.method,
            error: 'Server returned HTML instead of JSON'
          });
        }
      }
    }
    
    return originalSend.call(this, data);
  };
  
  // Also override res.status to ensure JSON
  res.status = function(code) {
    if (req.path.startsWith('/api/') || req.path.startsWith('/health') || req.path.startsWith('/api/test')) {
      res.setHeader('Content-Type', 'application/json');
    }
    return originalStatus(code);
  };
  
  next();
});

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Helper function to clean data before JSON response
const cleanDataForJSON = (data) => {
  if (data === null || data === undefined) {
    return null;
  }
  
  if (typeof data !== 'object') {
    return data;
  }
  
  if (Array.isArray(data)) {
    return data.map(cleanDataForJSON);
  }
  
  const cleaned = {};
  for (const key in data) {
    if (data.hasOwnProperty(key)) {
      const value = data[key];
      if (value !== undefined) {
        if (typeof value === 'number' && (isNaN(value) || !isFinite(value))) {
          cleaned[key] = 0;
        } else if (typeof value === 'object' && value !== null) {
          // Skip circular references
          try {
            cleaned[key] = cleanDataForJSON(value);
          } catch (e) {
            cleaned[key] = null;
          }
        } else {
          cleaned[key] = value;
        }
      }
    }
  }
  return cleaned;
};

// Request logging middleware for debugging
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
  if (req.headers.authorization) {
    console.log('  Auth header present');
  }
  next();
});

// Custom JSON response middleware to ensure valid JSON
app.use((req, res, next) => {
  const originalJson = res.json;
  res.json = function(data) {
    try {
      // Clean data before sending
      const cleanedData = cleanDataForJSON(data);
      // Verify it's valid JSON
      JSON.stringify(cleanedData);
      // Ensure Content-Type is set to JSON
      res.setHeader('Content-Type', 'application/json');
      return originalJson.call(this, cleanedData);
    } catch (error) {
      console.error('JSON serialization error:', error);
      res.setHeader('Content-Type', 'application/json');
      return originalJson.call(this, {
        success: false,
        message: 'Error serializing response data'
      });
    }
  };
  next();
});

// MongoDB Connection
const connectDB = async () => {
  try {
    if (!process.env.MONGODB_URI) {
      console.error('❌ MONGODB_URI is not defined in .env file');
      process.exit(1);
    }

    const conn = await mongoose.connect(process.env.MONGODB_URI);

    console.log(`✅ MongoDB Connected: ${conn.connection.host}`);
    console.log(`📊 Database: ${conn.connection.name}`);
    
    // Handle connection events
    mongoose.connection.on('error', (err) => {
      console.error('❌ MongoDB connection error:', err);
    });

    mongoose.connection.on('disconnected', () => {
      console.log('⚠️  MongoDB disconnected');
    });

    mongoose.connection.on('reconnected', () => {
      console.log('🔄 MongoDB reconnected');
    });

  } catch (error) {
    console.error('❌ Database connection failed:', error.message);
    console.error('Please check:');
    console.error('1. MongoDB is running (mongod)');
    console.error('2. MONGODB_URI in .env file is correct');
    console.error('3. Network connection is available');
    process.exit(1);
  }
};

connectDB();

// Import routes
const authRoutes = require('./src/routes/auth');
const accountRoutes = require('./src/routes/accounts');
const transactionRoutes = require('./src/routes/transactions');
const utilityRoutes = require('./src/routes/utilities');
const adminRoutes = require('./src/routes/admin');

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
app.use('/api/utilities', utilityRoutes);
app.use('/api/admin', adminRoutes);

// Final catch-all 404 handler - must be after all routes
// This will catch any route that doesn't match above
app.use((req, res) => {
  // Ensure we always return JSON for API routes
  res.status(404).json({
    success: false,
    message: 'Route not found',
    path: req.path,
    method: req.method
  });
});

// Error handler middleware (must be last)
app.use(errorHandler);

// Start server
app.listen(PORT, HOST, () => {
  console.log(`🚀 Server running on http://${HOST}:${PORT}`);
  console.log(`📱 Android app can connect to: http://YOUR_IP:${PORT}/api/`);
  console.log(`🔗 Health check: http://${HOST}:${PORT}/health`);
});
