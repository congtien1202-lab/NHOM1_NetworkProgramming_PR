# Đồ Án Cuối Kỳ: Hệ Thống Nhận Dạng Hình Ảnh Phân Tán (Nhóm 01)
> **Môn học:** Lập trình mạng máy tính  
> **Lớp:** 23DTHE4, 23DTHA5  
> **Đề tài:** IX.4 - Hệ thống Nhận dạng Hình ảnh Phân tán (Server-Client TCP + AI Object Detection)

---

## 1. Giới thiệu dự án
Hệ thống gồm:
- **Server:** Xây dựng bằng Java Socket đa luồng (multi-threaded), tích hợp mô hình AI SSD-MobileNet thông qua thư viện Deep Java Library (DJL) để phát hiện và nhận dạng vật thể.
- **Client:** Xây dựng bằng Java Swing, cho phép kết nối tới Server, chọn ảnh/mô phỏng chụp ảnh, gửi ảnh qua TCP Socket, hiển thị bounding box nhận dạng, lưu lịch sử và xuất kết quả (PNG, CSV).

---

## 2. Bảng ma trận thống kê đóng góp của thành viên
*(Bắt buộc duy trì để đối chiếu khi chấm điểm bảo vệ cuối kỳ)*

| MSSV | Họ và Tên | Công việc đã thực hiện | Merge Requests / PR |
| :--- | :--- | :--- | :--- |
| **2380614894** | **Trần Công Tiến** | Khởi tạo khung Maven đa module, giao thức mạng TCP nhị phân, bộ mã hóa/giải mã ProtocolCodec và các stub hợp đồng | **#1** |
| **2380601216** | **Hồ Ngọc Linh** | Xây dựng bộ nhận dạng AI bằng Deep Java Library (DJL) với mô hình SSD-MobileNet | |
| **2380600020** | **Nguyễn Văn An** | Xây dựng giao diện Client Swing, kết nối mạng và hiển thị kết quả khung nhận dạng | |
| **2380600688** | **Vương Thị Mỹ Hoa** | Xây dựng quản lý lịch sử nhận dạng, xuất file báo cáo (PNG, CSV) và tài liệu triển khai | |

---

## 3. Cấu trúc thư mục mã nguồn
```text
ma-nguon/
├── common/         # Giao thức truyền thông nhị phân và kiểu dữ liệu dùng chung
├── server/         # Server đa luồng TCP và module nhận dạng DJL
├── client/         # Client giao diện Swing, quản lý lịch sử và xuất kết quả
├── docs/           # Tài liệu kỹ thuật, kiến trúc và hướng dẫn triển khai
├── pom.xml         # File cấu hình Maven tổng (Multi-module)
└── mvnw / mvnw.cmd # Maven Wrapper
```

---

## 4. Hướng dẫn build và chạy thử
Yêu cầu: **JDK 17 trở lên**.

```powershell
# Chạy kiểm thử toàn bộ dự án
.\ma-nguon\mvnw.cmd test

# Đóng gói ra file jar
.\ma-nguon\mvnw.cmd package
```
Chi tiết cách sử dụng và cấu hình xem tại [HUONG-DAN-SU-DUNG.md](file:///c:/Users/ADMIN/Downloads/NhanDangAnh-Nhom01/HUONG-DAN-SU-DUNG.md).
