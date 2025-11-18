# 🗺️ Migration to OpenStreetMap (OSMDroid) - Complete Guide

## ✅ Đã Hoàn Thành Migration

App đã được chuyển từ Google Maps sang **OpenStreetMap (OSMDroid)** - **HOÀN TOÀN MIỄN PHÍ, KHÔNG CẦN API KEY!**

## 📋 Những Thay Đổi Đã Thực Hiện

### 1. Dependencies
**Trước (Google Maps):**
```kotlin
implementation("com.google.android.gms:play-services-maps:18.2.0")
implementation("com.google.android.gms:play-services-location:21.0.1")
```

**Sau (OSMDroid):**
```kotlin
implementation("org.osmdroid:osmdroid-android:6.1.17")
implementation("com.google.android.gms:play-services-location:21.0.1") // Vẫn cần cho location
```

### 2. AndroidManifest.xml
**Đã xóa:**
- Google Maps API Key requirement

**Đã thêm:**
- Storage permissions cho OSMDroid cache (chỉ cần cho Android < 29)

### 3. MapsFragment.java
**Thay đổi chính:**
- `GoogleMap` → `MapView` (OSMDroid)
- `LatLng` → `GeoPoint`
- `SupportMapFragment` → `MapView` trong layout
- `OnMapReadyCallback` → Direct initialization
- Marker API khác một chút

### 4. Layout (fragment_maps.xml)
**Trước:**
```xml
<fragment
    android:id="@+id/map"
    android:name="com.google.android.gms.maps.SupportMapFragment"
    ... />
```

**Sau:**
```xml
<org.osmdroid.views.MapView
    android:id="@+id/map"
    ... />
```

## 🎯 Ưu Điểm của OSMDroid

✅ **Hoàn toàn miễn phí** - Không cần API Key
✅ **Không cần billing** - Không cần thẻ tín dụng
✅ **Open source** - Tự do sử dụng
✅ **Offline support** - Có thể cache maps
✅ **Lightweight** - Nhẹ hơn Google Maps

## ⚠️ Lưu Ý

1. **Tile Source:** Hiện đang dùng `MAPNIK` (OpenStreetMap default)
   - Có thể đổi sang các tile source khác nếu muốn
   - Một số tile source có thể có giới hạn request

2. **Marker Icons:** 
   - Hiện đang dùng default Android icons
   - Có thể customize bằng cách tạo custom drawable

3. **Navigation:**
   - Nút "Chỉ đường" vẫn mở Google Maps navigation (nếu có)
   - Fallback sang OpenStreetMap web nếu không có Google Maps

## 🚀 Cách Sử Dụng

1. **Sync Gradle:**
   ```bash
   ./gradlew clean build
   ```

2. **Run App:**
   - Vào tab **Tiện ích**
   - Click card **"Chi nhánh"**
   - Maps sẽ load với OpenStreetMap

3. **Features hoạt động:**
   - ✅ Hiển thị vị trí người dùng
   - ✅ Hiển thị tất cả chi nhánh
   - ✅ Tìm chi nhánh gần nhất
   - ✅ Vẽ đường đi
   - ✅ Navigation

## 🔧 Customization (Optional)

### Thay đổi Tile Source
Trong `MapsFragment.java`:
```java
// Thay vì MAPNIK, có thể dùng:
mapView.setTileSource(TileSourceFactory.USGS_SAT);
mapView.setTileSource(TileSourceFactory.USGS_TOPO);
// Hoặc custom tile source
```

### Custom Marker Icons
```java
// Tạo custom drawable
Drawable customIcon = ContextCompat.getDrawable(getContext(), R.drawable.custom_marker);
marker.setIcon(customIcon);
```

### Thêm Clustering (nếu có nhiều markers)
Có thể thêm thư viện `osmdroid-clustering` để group markers khi zoom out.

## 📱 Testing

1. **Test trên Emulator:**
   - Set location manually trong Extended Controls
   - Maps sẽ load từ OpenStreetMap servers

2. **Test trên Real Device:**
   - Cấp quyền location
   - Maps sẽ load và hiển thị vị trí thật

3. **Test Offline:**
   - OSMDroid có thể cache tiles
   - Cần enable cache trong Configuration

## 🐛 Troubleshooting

### Maps không hiển thị
- Kiểm tra internet connection
- Kiểm tra OSMDroid configuration trong onCreate()
- Xem logcat để tìm lỗi cụ thể

### Markers không hiển thị
- Đảm bảo đã add marker vào overlays
- Gọi `mapView.invalidate()` sau khi thêm markers

### Location không hoạt động
- Kiểm tra permissions
- Kiểm tra GPS có bật không (real device)

## 📚 Tài Liệu Tham Khảo

- [OSMDroid GitHub](https://github.com/osmdroid/osmdroid)
- [OSMDroid Wiki](https://github.com/osmdroid/osmdroid/wiki)
- [OpenStreetMap](https://www.openstreetmap.org/)

## ✨ Kết Luận

App hiện đã sử dụng **OpenStreetMap hoàn toàn miễn phí**, không cần API Key hay billing. Tất cả tính năng maps đều hoạt động bình thường!

