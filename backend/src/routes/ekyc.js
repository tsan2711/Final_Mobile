const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const EkycController = require('../controllers/EkycController');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();

// Create uploads directory if it doesn't exist
const uploadsDir = path.join(__dirname, '../../uploads/ekyc');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Configure multer for file uploads
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, uploadsDir);
  },
  filename: function (req, file, cb) {
    // Generate unique filename: userId_timestamp_random.extension
    const userId = req.userId || 'unknown';
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 15);
    const ext = path.extname(file.originalname);
    cb(null, `${userId}_${timestamp}_${random}${ext}`);
  }
});

// File filter
const fileFilter = (req, file, cb) => {
  const allowedMimeTypes = ['image/jpeg', 'image/jpg', 'image/png'];
  if (allowedMimeTypes.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error('Invalid file type. Only JPEG, JPG, and PNG images are allowed'), false);
  }
};

// Configure multer
const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB max file size
  }
});

// All eKYC routes require authentication
router.use(authMiddleware);

// POST /api/ekyc/upload-face - Upload face image
router.post('/upload-face', upload.single('faceImage'), EkycController.uploadFace);

// POST /api/ekyc/verify-identity - Verify face for high-value transaction
router.post('/verify-identity', EkycController.verifyIdentity);

// GET /api/ekyc/verification-status - Get verification status
router.get('/verification-status', EkycController.getVerificationStatus);

module.exports = router;

