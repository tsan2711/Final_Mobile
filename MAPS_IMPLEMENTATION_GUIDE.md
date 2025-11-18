# 🗺️ Maps & Navigation Feature - Implementation Guide

## ✅ Đã Hoàn Thành

Tính năng Navigation & Maps đã được triển khai đầy đủ với các chức năng:

1. ✅ **Locate user location** - Xác định vị trí người dùng
2. ✅ **Hiển thị vị trí các chi nhánh ngân hàng** - Hiển thị tất cả chi nhánh trên bản đồ
3. ✅ **Recommendation đường đi ngắn nhất** - Tìm và đề xuất chi nhánh gần nhất

## 📋 Các Thành Phần Đã Triển Khai

### Backend (Node.js)

1. **API Endpoints:**
   - `GET /api/utilities/branches` - Lấy danh sách tất cả chi nhánh
   - `GET /api/utilities/branches/nearest?latitude=X&longitude=Y` - Tìm chi nhánh gần nhất

2. **Files Modified:**
   - `backend/src/controllers/UtilityController.js` - Thêm `getBranches()` và `getNearestBranch()`
   - `backend/src/routes/utilities.js` - Thêm routes cho branches

### Android

1. **Models:**
   - `app/src/main/java/com/example/final_mobile/models/Branch.java` - Model cho chi nhánh

2. **Services:**
   - `app/src/main/java/com/example/final_mobile/services/BranchService.java` - Service để gọi API branches

3. **UI:**
   - `app/src/main/java/com/example/final_mobile/MapsFragment.java` - Fragment hiển thị bản đồ
   - `app/src/main/res/layout/fragment_maps.xml` - Layout cho MapsFragment
   - `app/src/main/res/layout/fragment_utilities.xml` - Thêm card "Chi nhánh"

4. **Dependencies:**
   - Google Maps SDK: `com.google.android.gms:play-services-maps:18.2.0`
   - Location Services: `com.google.android.gms:play-services-location:21.0.1`

5. **Permissions:**
   - `ACCESS_FINE_LOCATION`
   - `ACCESS_COARSE_LOCATION`

## 🔧 Cấu Hình Cần Thiết

### 1. Google Maps API Key

**QUAN TRỌNG:** Bạn cần lấy Google Maps API Key và thêm vào `AndroidManifest.xml`:

1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Tạo project mới hoặc chọn project hiện có
3. Enable **Maps SDK for Android** API
4. Tạo API Key trong **Credentials**
5. Thay thế `YOUR_GOOGLE_MAPS_API_KEY` trong `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_ACTUAL_API_KEY_HERE" />
```

**File:** `app/src/main/AndroidManifest.xml` (dòng 25-27)

### 2. Backend API

Backend đã được cấu hình sẵn với mock data cho 5 chi nhánh:
- Chi nhánh Hồ Chí Minh - Trung tâm
- Chi nhánh Hà Nội - Hoàn Kiếm
- Chi nhánh Đà Nẵng
- Chi nhánh Hồ Chí Minh - Quận 7
- Chi nhánh Hà Nội - Cầu Giấy

Trong production, bạn nên lưu branches vào database thay vì dùng mock data.

## 🚀 Cách Sử Dụng

1. **Truy cập Maps:**
   - Vào tab **Tiện ích** (Utilities)
   - Click vào card **"Chi nhánh"**
   - MapsFragment sẽ mở ra

2. **Tính năng:**
   - Tự động lấy vị trí hiện tại của bạn (nếu có quyền)
   - Hiển thị tất cả chi nhánh trên bản đồ với markers
   - Chi nhánh gần nhất được đánh dấu màu xanh lá
   - Card ở dưới hiển thị thông tin chi nhánh gần nhất
   - Click "Chỉ đường" để mở Google Maps navigation
   - Click FAB (nút tròn) để quay về vị trí của bạn

## 📱 Testing

1. **Test trên Emulator:**
   - Emulator có thể không có GPS thật
   - Bạn có thể set location manually trong Extended Controls
   - Hoặc test với real device

2. **Test trên Real Device:**
   - Cần cấp quyền location khi app yêu cầu
   - Đảm bảo GPS được bật

3. **Test API:**
   ```bash
   # Test get branches
   curl http://localhost:8000/api/utilities/branches \
     -H "Authorization: Bearer YOUR_TOKEN"
   
   # Test nearest branch
   curl "http://localhost:8000/api/utilities/branches/nearest?latitude=10.7769&longitude=106.7009" \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

## 🐛 Troubleshooting

### Maps không hiển thị
- Kiểm tra Google Maps API Key đã được thêm chưa
- Kiểm tra API Key có enable Maps SDK for Android chưa
- Xem logcat để tìm lỗi cụ thể

### Không lấy được vị trí
- Kiểm tra quyền location đã được cấp chưa
- Kiểm tra GPS có bật không (trên real device)
- Trên emulator, set location manually

### API không trả về data
- Kiểm tra backend đang chạy
- Kiểm tra authentication token
- Xem log backend để debug

## 📝 Notes

- Route hiện tại là đường thẳng (straight line). Để có route thực tế, cần tích hợp Google Directions API
- Mock data có thể được thay thế bằng database trong production
- Có thể thêm tính năng filter branches theo khoảng cách, dịch vụ, etc.

## 🎯 Next Steps (Optional)

1. Tích hợp Google Directions API cho route thực tế
2. Thêm search/filter branches
3. Thêm thông tin chi tiết hơn cho mỗi chi nhánh (hình ảnh, giờ mở cửa, etc.)
4. Cache branches data để tải nhanh hơn
5. Thêm clustering markers khi zoom out

