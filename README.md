# 📱 AURORA RESTAURANT & LOUNGE · ANDROID POS & KITCHEN DISPLAY (KDS) APP

> Ứng dụng Android chuyên dụng cho Nhân viên phục vụ và Màn hình Bếp (KDS) trong hệ sinh thái nhà hàng thông minh Aurora. Đồng bộ thời gian thực (0 giây) với Web khách đặt món qua Firebase Firestore Native SDK.

[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20Native%20SDK-orange.svg?logo=firebase&logoColor=white)](https://firebase.google.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 🌟 TÍNH NĂNG CHÍNH

1. **Sơ đồ bàn trực quan đa trạng thái (Floor Plan):**
   - Hiển thị toàn bộ các bàn theo lưới thông minh, tối ưu hóa cho Máy tính bảng (Tablet 10 - 12 inch) và Điện thoại di động.
   - Nhận diện trạng thái bàn bằng màu sắc trực quan:
     - ⚪ `Sẵn sàng (Trống)`
     - 🟡 `Chờ món (Khách vừa gọi đơn)`
     - 🔵 `Đang nấu`
     - 🟢 `Đang ăn`
     - 🔴 `Cần hỗ trợ (Khách rung chuông gọi phục vụ)`
     - 🟣 `Chờ thanh toán`
   - Chuẩn hóa mã bàn thông minh (Tự động nhận diện `Bàn A1`, `ban_a1`, `A1` đồng bộ).

2. **Màn hình Bếp thời gian thực (Kitchen Display System - KDS):**
   - Đơn gọi món từ khách quét QR hoặc từ nhân viên nhảy thẳng vào màn hình bếp tức thì trong 0 giây.
   - Báo chuông âm thanh khi có món mới được đặt.
   - Cập nhật trạng thái từng món: *Đang chờ $\rightarrow$ Đang nấu $\rightarrow$ Đã nấu xong*.

3. **Quản lý thực đơn & Khóa món hết hàng:**
   - Dành riêng cho Quản lý / Bếp: Bật hoặc tắt khóa các món hết nguyên liệu chỉ với 1 lần chạm.
   - Khi món bị khóa, Web khách hàng quét QR tại bàn sẽ lập tức hiển thị nhãn "Hết hàng" và không cho đặt.

4. **Bảo mật & Phân quyền nhân sự (RBAC):**
   - Đăng nhập bảo mật bằng Mã PIN bàn phím số.
   - Mã PIN được bảo vệ bằng mã băm một chiều SHA-256 kèm chuỗi muối (Salt) chống giải mã.
   - Phân quyền chặt chẽ:
     - `Quản trị viên (Admin)`: Toàn quyền truy cập.
     - `Quản lý (Manager)`: Quản lý sơ đồ bàn, duyệt bếp KDS, khóa món hết hàng (Không thể sửa giá hoặc xóa thực đơn).
     - `Nhân viên (Staff)`: Xem sơ đồ bàn và phục vụ khách.

---

## 🛠️ CÔNG NGHỆ SỬ DỤNG (TECH STACK)

- **Ngôn ngữ:** Kotlin 2.0+
- **Giao diện (UI Toolkit):** Jetpack Compose, Material Design 3 (Declarative UI)
- **Kiến trúc:** MVVM (Model - View - ViewModel) + Repository Pattern
- **Bất đồng bộ:** Kotlin Coroutines, StateFlow, SharedFlow
- **Dữ liệu đám mây:** Google Firebase Firestore Native Android SDK (Realtime Listeners)
- **Hệ thống Build:** Gradle 8.x với Kotlin DSL (`build.gradle.kts`)

---

## 🚀 HƯỚNG DẪN CÀI ĐẶT & CHẠY DỰ ÁN

### Bước 1: Tải mã nguồn về máy
```bash
git clone https://github.com/your-username/aurora-pos-android.git
cd aurora-pos-android
```

### Bước 2: Cấu hình Firebase Google Services
1. Truy cập [Firebase Console](https://console.firebase.google.com/) vào dự án của bạn.
2. Thêm một ứng dụng Android với Package Name:
   ```text
   com.example.qrorderkitchen
   ```
3. Tải file `google-services.json` về và dán vào thư mục:
   ```text
   aurora-pos-android/app/google-services.json
   ```
   *(Tham khảo cấu trúc tại file mẫu `app/google-services.json.example`)*.

### Bước 3: Mở và Chạy ứng dụng bằng Android Studio
1. Mở **Android Studio** (Hedgehog, Koala hoặc mới hơn).
2. Chọn **Open** $\rightarrow$ Trỏ tới thư mục `aurora-pos-android`.
3. Chờ Android Studio đồng bộ xong Gradle dependencies.
4. Bấm nút **Run** (biểu tượng Play xanh) để chạy trên Thiết bị thật hoặc Máy ảo Android.

### Bước 4: Xuất file APK cài đặt trực tiếp
Chạy lệnh sau trong Terminal của Android Studio để xuất file APK cài đặt lên máy tính bảng / điện thoại:
```bash
./gradlew assembleDebug
```
File APK sẽ được tạo tại:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📁 CẤU TRÚC THƯ MỤC NGUỒN (SOURCE CODE)

```
app/src/main/java/com/example/qrorderkitchen/
├── data/
│   ├── model/          # Định nghĩa cấu trúc dữ liệu: Table, Order, Dish, Staff, Customer
│   └── repository/     # Xử lý kết nối Firestore: TableRepository, OrderRepository, AuthRepository
├── ui/
│   ├── auth/           # Màn hình đăng nhập bàn phím số PIN
│   ├── components/     # Các thành phần giao diện dùng chung (Button, Dialog, Badge, Input)
│   ├── customer/       # Giao diện theo dõi đơn hàng, hóa đơn tạm tính và tài khoản hội viên
│   ├── kitchen/        # Màn hình Bếp KDS (Kitchen Display System)
│   ├── manager/        # Màn hình khóa món thực đơn dành cho Quản lý
│   ├── tables/         # Màn hình sơ đồ bàn Floorplan đa trạng thái
│   └── theme/          # Bảng màu Aurora Gold, Typography, Theme Material 3
├── utils/              # Tiện ích: TableUtils (Chuẩn hóa mã bàn), CurrencyUtils (Định dạng VNĐ)
└── MainActivity.kt     # Điểm khởi chạy ứng dụng
```

---

## 📜 GIẤY PHÉP (LICENSE)

Dự án được phân phối dưới giấy phép **MIT License**. Tự do sử dụng, chỉnh sửa và triển khai phục vụ nhà hàng, quán ăn của bạn.
