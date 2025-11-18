package com.example.final_mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.final_mobile.services.EkycService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FaceCaptureActivity extends AppCompatActivity {
    private static final String TAG = "FaceCaptureActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private FrameLayout cameraPreview;
    private Button captureButton;
    private Button retakeButton;
    private Button uploadButton;
    private Bitmap capturedBitmap;
    private File capturedImageFile;
    private EkycService ekycService;

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                if (data == null || data.length == 0) {
                    Log.e(TAG, "Picture data is null or empty");
                    runOnUiThread(() -> {
                        Toast.makeText(FaceCaptureActivity.this, "Lỗi: Không có dữ liệu ảnh", Toast.LENGTH_SHORT).show();
                        camera.startPreview(); // Restart preview
                    });
                    return;
                }
                
                // Convert byte array to bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                
                if (bitmap == null) {
                    Log.e(TAG, "Failed to decode bitmap");
                    runOnUiThread(() -> {
                        Toast.makeText(FaceCaptureActivity.this, "Lỗi: Không thể xử lý ảnh", Toast.LENGTH_SHORT).show();
                        camera.startPreview(); // Restart preview
                    });
                    return;
                }
                
                // Rotate bitmap if needed (camera orientation)
                Matrix matrix = new Matrix();
                matrix.postRotate(90); // Most Android cameras need 90 degree rotation
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                
                // Recycle original bitmap to free memory
                if (bitmap != rotatedBitmap) {
                    bitmap.recycle();
                }
                
                capturedBitmap = rotatedBitmap;
                
                // Save to file
                capturedImageFile = saveBitmapToFile(rotatedBitmap);
                
                if (capturedImageFile == null) {
                    Log.e(TAG, "Failed to save image file");
                    runOnUiThread(() -> {
                        Toast.makeText(FaceCaptureActivity.this, "Lỗi: Không thể lưu ảnh", Toast.LENGTH_SHORT).show();
                        camera.startPreview(); // Restart preview
                    });
                    return;
                }
                
                // Show preview and buttons
                runOnUiThread(() -> {
                    captureButton.setVisibility(View.GONE);
                    retakeButton.setVisibility(View.VISIBLE);
                    uploadButton.setVisibility(View.VISIBLE);
                    Toast.makeText(FaceCaptureActivity.this, "Ảnh đã được chụp. Vui lòng kiểm tra và tải lên.", Toast.LENGTH_SHORT).show();
                });
                
                // Restart preview
                camera.startPreview();
                
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory processing image", e);
                runOnUiThread(() -> {
                    Toast.makeText(FaceCaptureActivity.this, "Lỗi: Không đủ bộ nhớ để xử lý ảnh", Toast.LENGTH_LONG).show();
                    if (camera != null) {
                        camera.startPreview();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing captured image", e);
                runOnUiThread(() -> {
                    Toast.makeText(FaceCaptureActivity.this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (camera != null) {
                        camera.startPreview();
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_capture);
        
        // Initialize eKYC service
        ekycService = new EkycService(this);
        
        // Initialize views
        cameraPreview = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.btn_capture);
        retakeButton = findViewById(R.id.btn_retake);
        uploadButton = findViewById(R.id.btn_upload);
        
        // Initially hide retake and upload buttons
        retakeButton.setVisibility(View.GONE);
        uploadButton.setVisibility(View.GONE);
        
        // Setup surface view for camera preview
        surfaceView = new SurfaceView(this);
        cameraPreview.addView(surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceCallback);
        // SURFACE_TYPE_PUSH_BUFFERS is deprecated but still needed for Camera API
        try {
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        } catch (Exception e) {
            Log.w(TAG, "setType deprecated, but continuing", e);
        }
        
        // Capture button click
        captureButton.setOnClickListener(v -> {
            if (camera == null) {
                Toast.makeText(this, "Camera chưa sẵn sàng. Vui lòng đợi...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                // Disable button during capture
                captureButton.setEnabled(false);
                captureButton.setText("Đang chụp...");
                
                // Take picture with shutter callback for better UX
                camera.takePicture(
                    new Camera.ShutterCallback() {
                        @Override
                        public void onShutter() {
                            // Shutter callback - camera sound
                            runOnUiThread(() -> {
                                // Optional: play camera sound
                            });
                        }
                    },
                    null, // Raw callback (not used)
                    pictureCallback
                );
            } catch (Exception e) {
                Log.e(TAG, "Error taking picture", e);
                runOnUiThread(() -> {
                    captureButton.setEnabled(true);
                    captureButton.setText("Chụp ảnh");
                    Toast.makeText(this, "Lỗi chụp ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // Retake button click
        retakeButton.setOnClickListener(v -> {
            capturedBitmap = null;
            if (capturedImageFile != null && capturedImageFile.exists()) {
                capturedImageFile.delete();
            }
            capturedImageFile = null;
            
            captureButton.setVisibility(View.VISIBLE);
            captureButton.setEnabled(true);
            captureButton.setText("Chụp ảnh");
            retakeButton.setVisibility(View.GONE);
            uploadButton.setVisibility(View.GONE);
            
            // Restart camera preview
            if (camera != null) {
                try {
                    camera.startPreview();
                } catch (Exception e) {
                    Log.e(TAG, "Error restarting preview", e);
                }
            }
            
            Toast.makeText(this, "Vui lòng chụp lại ảnh.", Toast.LENGTH_SHORT).show();
        });
        
        // Upload button click
        uploadButton.setOnClickListener(v -> {
            if (capturedImageFile != null && capturedImageFile.exists()) {
                uploadFaceImage();
            } else {
                Toast.makeText(this, "Không tìm thấy ảnh. Vui lòng chụp lại.", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            initializeCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this, "Cần quyền camera để chụp ảnh khuôn mặt.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeCamera() {
        try {
            // Try to open camera (0 = back camera)
            camera = Camera.open(0);
            if (camera == null) {
                Toast.makeText(this, "Không thể mở camera. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Camera.Parameters parameters = camera.getParameters();
            
            // Set focus mode
            if (parameters.getSupportedFocusModes() != null) {
                if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                } else if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
            }
            
            // Set preview size
            Camera.Size bestSize = getBestPreviewSize(parameters);
            if (bestSize != null) {
                parameters.setPreviewSize(bestSize.width, bestSize.height);
            }
            
            // Set picture size
            Camera.Size bestPictureSize = getBestPictureSize(parameters);
            if (bestPictureSize != null) {
                parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);
            }
            
            // Set JPEG quality
            parameters.setJpegQuality(85);
            
            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing camera", e);
            Toast.makeText(this, "Lỗi khởi tạo camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (camera != null) {
                camera.release();
                camera = null;
            }
        }
    }
    
    private Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (bestSize == null) {
                bestSize = size;
            } else if (size.width * size.height > bestSize.width * bestSize.height) {
                bestSize = size;
            }
        }
        return bestSize;
    }
    
    private Camera.Size getBestPictureSize(Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (bestSize == null) {
                bestSize = size;
            } else if (size.width * size.height > bestSize.width * bestSize.height) {
                bestSize = size;
            }
        }
        return bestSize;
    }

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (camera != null) {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } else {
                    // Camera not initialized yet, initialize it
                    if (ContextCompat.checkSelfPermission(FaceCaptureActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        initializeCamera();
                        if (camera != null) {
                            camera.setPreviewDisplay(holder);
                            camera.startPreview();
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error setting preview display", e);
                runOnUiThread(() -> {
                    Toast.makeText(FaceCaptureActivity.this, "Lỗi hiển thị camera preview", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in surfaceCreated", e);
                runOnUiThread(() -> {
                    Toast.makeText(FaceCaptureActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (camera != null) {
                try {
                    camera.stopPreview();
                    
                    // Set preview display before starting preview
                    camera.setPreviewDisplay(holder);
                    
                    // Start preview
                    camera.startPreview();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting camera preview", e);
                    runOnUiThread(() -> {
                        Toast.makeText(FaceCaptureActivity.this, "Lỗi hiển thị camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCamera();
        }
    };

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private File saveBitmapToFile(Bitmap bitmap) {
        try {
            File cacheDir = getCacheDir();
            File imageFile = new File(cacheDir, "face_capture_" + System.currentTimeMillis() + ".jpg");
            
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            
            return imageFile;
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file", e);
            return null;
        }
    }

    private void uploadFaceImage() {
        if (capturedImageFile == null || !capturedImageFile.exists()) {
            Toast.makeText(this, "Không tìm thấy ảnh. Vui lòng chụp lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadButton.setEnabled(false);
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        ekycService.uploadFaceImage(capturedImageFile, new EkycService.EkycCallback() {
            @Override
            public void onSuccess(org.json.JSONObject data) {
                runOnUiThread(() -> {
                    uploadButton.setEnabled(true);
                    try {
                        String status = data.optString("verification_status", "PENDING");
                        String message = "Tải ảnh thành công! Trạng thái xác thực: " + status;
                        Toast.makeText(FaceCaptureActivity.this, message, Toast.LENGTH_LONG).show();
                        
                        // Return result
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("verification_status", status);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing upload success", e);
                        Toast.makeText(FaceCaptureActivity.this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    uploadButton.setEnabled(true);
                    Toast.makeText(FaceCaptureActivity.this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera == null && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initializeCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }
}

