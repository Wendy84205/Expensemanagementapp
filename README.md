# Ứng Dụng Quản Lý Tài Chính Cá Nhân

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.5.0-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](https://developer.android.com/about/versions/android-7.0)
[![Firebase](https://img.shields.io/badge/Firebase-Đã%20kích%20hoạt-red.svg)](https://firebase.google.com)

Ứng dụng quản lý tài chính cá nhân hiện đại, giúp bạn theo dõi thu chi, lập ngân sách và quản lý tài chính thông minh.

## Mục Lục
- [Tính Năng](#tính-năng)
- [Kiến Trúc](#kiến-trúc)
- [Cài Đặt](#cài-đặt)
- [Cấu Hình](#cấu-hình)
- [Đóng Góp](#đóng-góp)
- [Giấy Phép](#giấy-phép)
- [Liên Hệ](#liên-hệ)

## Tính Năng Chính

### Quản Lý Cơ Bản
- **Theo dõi thu chi**: Thêm, sửa, xóa giao dịch dễ dàng
- **Phân loại danh mục**: Tùy chỉnh danh mục thu chi
- **Chi tiêu định kỳ**: Tự động ghi nhận chi tiêu hàng tháng

### Phân Tích & Báo Cáo
- **Dashboard trực quan**: Biểu đồ và thống kê sinh động
- **Phân tích danh mục**: Xem chi tiêu theo từng hạng mục
- **Xu hướng chi tiêu**: Theo dõi thói quen chi tiêu theo thời gian
- **Báo cáo chi tiết**: Báo cáo tuần, tháng, năm

### Tính Năng Thông Minh
- **Gợi ý từ AI**: Đề xuất tối ưu ngân sách thông minh
- **Dự báo tài chính**: Dự đoán chi tiêu dựa trên lịch sử
- **Cảnh báo chi tiêu**: Thông báo khi chi tiêu bất thường

### Trải Nghiệm Người Dùng
- **Giao diện hiện đại**: Thiết kế Material 3 đẹp mắt
- **Hoạt động offline**: Sử dụng không cần internet
- **Đa ngôn ngữ**: Tiếng Việt và Tiếng Anh

### Bảo Mật & Đồng Bộ
- **Mã hóa dữ liệu**: Bảo vệ thông tin tài chính
- **Sao lưu đám mây**: Đồng bộ với Firebase
- **Lưu trữ cục bộ**: Tùy chọn lưu dữ liệu local
- **Bảo mật riêng tư**: Không thu thập dữ liệu cá nhân

## 🏗️ Kiến Trúc Ứng Dụng

### Công Nghệ Sử Dụng
- **Ngôn ngữ**: Kotlin 1.9.0
- **Giao diện**: Jetpack Compose 1.5.0
- **Kiến trúc**: Clean Architecture với MVVM
- **Dependency Injection**: Dagger Hilt
- **Database local**: Room
- **Database cloud**: Firebase Firestore
- **Xác thực**: Firebase Auth
- **Xử lý ảnh**: ML Kit
- **Thông báo**: WorkManager + AlarmManager

## Hướng Dẫn Cài Đặt

### Yêu Cầu Hệ Thống
- Android Studio 2022.2.1 trở lên
- JDK 17 trở lên
- Android SDK 33 (API Level 33)
- Kotlin 1.9.0

### Bước 1: Clone Dự Án
```bash
git clone https://github.com/Wendy84205/Expensemanagementapp.git
cd Expensemanagementapp
Bước 2: Mở Trong Android Studio

Mở Android Studio
Chọn "Open an Existing Project"
Chọn thư mục vừa clone
Nhấn "Open"
Bước 3: Cấu Hình Firebase

Truy cập Firebase Console
Tạo project mới hoặc chọn project có sẵn
Nhấn "Add app" và chọn Android
Đăng ký app với package name: com.example.financeapp
Tải file google-services.json
Đặt file vào thư mục app/ của dự án
Bước 4: Cấu Hình API Keys

Tạo file secrets.properties trong thư mục gốc:

properties
# OpenAI API (cho tính năng AI)
OPENAI_API_KEY=your_openai_api_key_here

# Currency Exchange API (tùy chọn)
CURRENCY_API_KEY=your_exchange_api_key_here

# OCR API (tùy chọn cho quét hóa đơn)
OCR_API_KEY=your_ocr_api_key_here
🏗️ Build Ứng Dụng

Build Debug

bash
./gradlew assembleDebug
Build Release

bash
./gradlew assembleRelease
Chạy Test

bash
./gradlew test
Tạo APK

bash
./gradlew assembleRelease
Cấu Hình Ứng Dụng

Biến Môi Trường

Biến	                Mô tả	              Bắt buộc	      Mặc định
ENABLE_CLOUD_SYNC	Bật đồng bộ Firebase	  Không	            true
ENABLE_AI_FEATURES	Bật tính năng AI	    Không	            true
ENABLE_OCR	        Bật quét hóa đơn	    Không	            true
ENABLE_BIOMETRICS	Bật đăng nhập sinh trắc	Không	            true
DEFAULT_CURRENCY	Tiền tệ mặc định	      Không	            VND
Các Biến Thể Build

debug: Build phát triển, có debug
release: Build sản phẩm, tối ưu hóa
staging: Build thử nghiệm trước sản phẩm
Các Loại Build

Unit Test

bash
./gradlew testDebugUnitTest
Instrumentation Test

bash
./gradlew connectedDebugAndroidTest
UI Test

Dự án có đầy đủ UI test sử dụng Espresso và Compose testing.

Đóng Góp Cho Dự Án

Chúng tôi hoan nghênh mọi đóng góp từ cộng đồng!

Báo Cáo Lỗi

Kiểm tra lỗi đã có trong Issues
Tạo issue mới với tiêu đề và mô tả rõ ràng
Bao gồm các bước tái hiện lỗi
Thêm ảnh chụp màn hình nếu có
Gửi Thay Đổi

Fork repository
Tạo branch mới: git checkout -b feature/ten-tinh-nang
Thực hiện thay đổi
Chạy test: ./gradlew test
Commit: git commit -m 'Thêm tính năng mới'
Push: git push origin feature/ten-tinh-nang
Tạo Pull Request
Quy Ước Code

Tuân thủ quy ước Kotlin
Đặt tên biến và hàm có ý nghĩa
Thêm comment cho logic phức tạp
Viết test cho tính năng mới
Cập nhật tài liệu
Checklist Pull Request

Code tuân thủ quy ước
Tất cả test pass
Không có warning mới
Cập nhật tài liệu
Thêm ảnh chụp cho thay đổi UI
📈 Lộ Trình Phát Triển

Phiên Bản 1.0 (Hiện Tại)

Quản lý giao dịch cơ bản
Theo dõi ngân sách
Chi tiêu định kỳ
Đa ngôn ngữ
Phiên Bản 1.1 (Đang Phát Triển)

Tích hợp tài khoản ngân hàng
Theo dõi đầu tư
Phân tích nâng cao
Ngân sách gia đình
Lưu trữ hóa đơn
Chế độ sáng/tối
Phiên Bản 1.2 (Kế Hoạch)

Tính thuế tự động
Công cụ lập kế hoạch tài chính
Xuất sang phần mềm kế toán
Dự báo AI nâng cao
Web dashboard
Phiên Bản 2.0 (Tương Lai)

Đa nền tảng (iOS, Web)
Tính năng bảo mật nâng cao
API cho developer
Hệ thống plugin
Tính năng cộng đồng
Giấy Phép

MIT License

Dự án được phân phối dưới giấy phép MIT License, một trong những giấy phép nguồn mở tự do nhất.
Đọc toàn văn giấy phép tại: LICENSE
MIT License
Copyright (c) 2024 Wendy

Tác giả: Wendy
Email: wendy84205@gmail.com
GitHub: Wendy84205
Repository: Expensemanagementapp
Issues: GitHub Issues
Hỗ Trợ Dự Án

Nếu bạn thấy dự án hữu ích, hãy:

-Star trên GitHub - Để theo dõi và hỗ trợ
-Báo cáo lỗi - Giúp cải thiện chất lượng
-Đề xuất tính năng - Chia sẻ ý tưởng mới
-Chia sẻ - Giới thiệu với bạn bè và cộng đồng
-Đóng góp code - Tham gia phát triển dự án
<div align="center">
Thống kê

https://img.shields.io/github/stars/Wendy84205/Expensemanagementapp?style=social
https://img.shields.io/github/forks/Wendy84205/Expensemanagementapp?style=social
https://img.shields.io/github/issues/Wendy84205/Expensemanagementapp
https://img.shields.io/github/last-commit/Wendy84205/Expensemanagementapp

Cảm ơn bạn đã quan tâm đến dự án! 
