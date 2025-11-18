const EkycVerification = require('../models/EkycVerification');
const User = require('../models/User');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

class EkycController {
  // Upload face image for eKYC
  static async uploadFace(req, res) {
    try {
      const userId = req.userId;
      
      if (!req.file) {
        return res.status(400).json({
          success: false,
          message: 'Face image is required'
        });
      }

      // Validate file type
      const allowedMimeTypes = ['image/jpeg', 'image/jpg', 'image/png'];
      if (!allowedMimeTypes.includes(req.file.mimetype)) {
        // Delete uploaded file
        if (fs.existsSync(req.file.path)) {
          fs.unlinkSync(req.file.path);
        }
        return res.status(400).json({
          success: false,
          message: 'Invalid file type. Only JPEG, JPG, and PNG images are allowed'
        });
      }

      // Validate file size (max 5MB)
      const maxSize = 5 * 1024 * 1024; // 5MB
      if (req.file.size > maxSize) {
        // Delete uploaded file
        if (fs.existsSync(req.file.path)) {
          fs.unlinkSync(req.file.path);
        }
        return res.status(400).json({
          success: false,
          message: 'File size too large. Maximum size is 5MB'
        });
      }

      // Find or create eKYC verification record
      let ekycVerification = await EkycVerification.findOne({ userId });
      
      if (!ekycVerification) {
        ekycVerification = new EkycVerification({
          userId,
          verificationStatus: 'PENDING'
        });
      }

      // Delete old image if exists
      if (ekycVerification.faceImageUrl && fs.existsSync(ekycVerification.faceImageUrl)) {
        try {
          fs.unlinkSync(ekycVerification.faceImageUrl);
        } catch (err) {
          console.error('Error deleting old image:', err);
        }
      }

      // Store image path
      ekycVerification.faceImageUrl = req.file.path;
      ekycVerification.verificationStatus = 'PENDING';
      
      // Basic image quality check (simplified - in production use proper ML)
      const imageQuality = EkycController.assessImageQuality(req.file);
      ekycVerification.metadata = {
        ...ekycVerification.metadata,
        deviceInfo: req.headers['user-agent'] || '',
        ipAddress: req.ip,
        userAgent: req.headers['user-agent'] || '',
        imageQuality: imageQuality.quality,
        faceDetected: imageQuality.faceDetected,
        verificationScore: imageQuality.score
      };

      await ekycVerification.save();

      // In production, here you would:
      // 1. Send image to face recognition service (AWS Rekognition, Google Cloud Vision, etc.)
      // 2. Compare with ID document photo
      // 3. Perform liveness detection
      // 4. Update verification status based on results

      // For now, simulate verification (auto-approve for demo)
      // In production, this should be async and handled by a background job
      setTimeout(async () => {
        try {
          const verification = await EkycVerification.findById(ekycVerification._id);
          if (verification && verification.verificationStatus === 'PENDING') {
            // Simulate verification process
            const score = Math.floor(Math.random() * 20) + 80; // 80-100 score
            await verification.markAsVerified({
              verificationScore: score
            });
          }
        } catch (err) {
          console.error('Error in async verification:', err);
        }
      }, 2000);

      res.status(200).json({
        success: true,
        message: 'Face image uploaded successfully. Verification in progress.',
        data: {
          verification_id: ekycVerification._id,
          verification_status: ekycVerification.verificationStatus,
          image_quality: imageQuality.quality,
          face_detected: imageQuality.faceDetected,
          message: 'Your face image has been uploaded. Verification will be completed shortly.'
        }
      });

    } catch (error) {
      console.error('Upload face error:', error);
      
      // Clean up uploaded file on error
      if (req.file && req.file.path && fs.existsSync(req.file.path)) {
        try {
          fs.unlinkSync(req.file.path);
        } catch (err) {
          console.error('Error cleaning up file:', err);
        }
      }
      
      res.status(500).json({
        success: false,
        message: error.message || 'Failed to upload face image',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // Verify identity for high-value transaction
  static async verifyIdentity(req, res) {
    try {
      const userId = req.userId;
      const { transactionId, amount, faceImage } = req.body;

      // Validation
      if (!transactionId || !amount) {
        return res.status(400).json({
          success: false,
          message: 'Transaction ID and amount are required'
        });
      }

      const amountNumber = Number(amount);
      const HIGH_VALUE_THRESHOLD = 10000000; // 10 million VND

      // Check if transaction requires biometric verification
      if (amountNumber < HIGH_VALUE_THRESHOLD) {
        return res.status(400).json({
          success: false,
          message: 'Biometric verification is only required for transactions above 10,000,000 VND'
        });
      }

      // Find eKYC verification
      const ekycVerification = await EkycVerification.findOne({ userId });

      if (!ekycVerification) {
        return res.status(404).json({
          success: false,
          message: 'eKYC verification not found. Please upload your face image first.'
        });
      }

      // Check if eKYC is verified
      if (!ekycVerification.isValid()) {
        return res.status(403).json({
          success: false,
          message: 'eKYC verification is not valid. Please complete eKYC verification first.',
          verification_status: ekycVerification.verificationStatus
        });
      }

      // If face image is provided, compare with stored image
      if (faceImage) {
        // In production, use proper face recognition service
        // For now, simulate comparison
        const comparisonResult = await EkycController.compareFaceImages(
          faceImage,
          ekycVerification.faceImageUrl
        );

        if (!comparisonResult.match) {
          return res.status(401).json({
            success: false,
            message: 'Face verification failed. Face does not match registered image.',
            match_score: comparisonResult.score
          });
        }

        // Update last transaction verification
        await ekycVerification.updateLastTransactionVerification(transactionId, amountNumber);
      } else {
        // If no face image provided, just check if eKYC is valid
        // This allows using device biometric (fingerprint/face unlock) instead
        await ekycVerification.updateLastTransactionVerification(transactionId, amountNumber);
      }

      res.status(200).json({
        success: true,
        message: 'Identity verified successfully',
        data: {
          verified: true,
          transaction_id: transactionId,
          verification_timestamp: new Date().toISOString()
        }
      });

    } catch (error) {
      console.error('Verify identity error:', error);
      res.status(500).json({
        success: false,
        message: error.message || 'Failed to verify identity',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // Get verification status
  static async getVerificationStatus(req, res) {
    try {
      const userId = req.userId;

      const ekycVerification = await EkycVerification.findOne({ userId })
        .select('-faceImage -faceImageUrl'); // Don't send image data

      if (!ekycVerification) {
        return res.status(200).json({
          success: true,
          message: 'No eKYC verification found',
          data: {
            verification_status: 'NOT_STARTED',
            has_face_image: false,
            is_valid: false
          }
        });
      }

      res.status(200).json({
        success: true,
        message: 'Verification status retrieved successfully',
        data: {
          verification_id: ekycVerification._id,
          verification_status: ekycVerification.verificationStatus,
          has_face_image: !!ekycVerification.faceImageUrl,
          is_valid: ekycVerification.isValid(),
          verified_at: ekycVerification.verifiedAt,
          expires_at: ekycVerification.expiresAt,
          image_quality: ekycVerification.metadata?.imageQuality,
          verification_score: ekycVerification.metadata?.verificationScore,
          last_transaction_verification: ekycVerification.lastVerifiedForTransaction
        }
      });

    } catch (error) {
      console.error('Get verification status error:', error);
      res.status(500).json({
        success: false,
        message: error.message || 'Failed to get verification status',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }

  // Helper: Assess image quality (simplified)
  static assessImageQuality(file) {
    // In production, use proper image analysis library
    // For now, simulate quality assessment
    const size = file.size;
    const minSize = 100 * 1024; // 100KB minimum
    const maxSize = 5 * 1024 * 1024; // 5MB maximum

    let quality = 'MEDIUM';
    let score = 70;
    let faceDetected = false;

    if (size >= minSize && size <= maxSize) {
      quality = 'HIGH';
      score = 85;
      faceDetected = true; // Simulate face detection
    } else if (size < minSize) {
      quality = 'LOW';
      score = 50;
    }

    return { quality, score, faceDetected };
  }

  // Helper: Compare face images (simplified - in production use ML service)
  static async compareFaceImages(newImageBase64, storedImagePath) {
    // In production, this would:
    // 1. Decode base64 image
    // 2. Send both images to face recognition service
    // 3. Get similarity score
    // 4. Return match result

    // For demo, simulate comparison
    return new Promise((resolve) => {
      setTimeout(() => {
        // Simulate 85-95% match score
        const score = Math.floor(Math.random() * 10) + 85;
        resolve({
          match: score >= 80, // Match if score >= 80%
          score: score
        });
      }, 500);
    });
  }
}

module.exports = EkycController;

