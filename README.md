# Đồ Án Lập Trình Mạng: Hệ Thống Nhận Dạng Hình Ảnh Phân Tán
> **Nhóm thực hiện:** Nhóm 01  
> **Lớp:** 23DTHE4, 23DTHA5  
> **Đề tài:** IX.4 - Hệ thống Nhận dạng Hình ảnh Phân tán (Mô hình Client - Server qua giao thức TCP)

---

## 1. Giới thiệu tổng quan
Dự án xây dựng hệ thống nhận dạng hình ảnh phân tán hoạt động theo mô hình Client - Server:
- **Server:** Xử lý đa luồng (Multi-threading), tiếp nhận kết nối TCP từ nhiều Client đồng thời, xử lý và phân tích hình ảnh nhận dạng vật thể.
- **Client:** Giao diện đồ họa (Java Swing), cho phép người dùng chọn hình ảnh, gửi yêu cầu qua Socket TCP đến Server và nhận lại ảnh đã được vẽ bounding box cùng bảng kết quả chi tiết.
- **Giao thức truyền thông:** Thiết kế giao thức nhị phân tùy chỉnh (Custom Application Protocol) trên nền tầng vận chuyển TCP.

---

## 2. Bảng ma trận thống kê đóng góp của thành viên
*(Bảng theo dõi tiến độ và đối chiếu đánh giá quá trình theo quy định thực hiện đồ án)*

| MSSV | Họ và Tên | Công việc đã thực hiện | Merge Requests / PR |
| :--- | :--- | :--- | :--- |
| **2380614894** | **Trần Công Tiến** | Khởi tạo khung Maven (`common`); Xây dựng Client Swing kết nối Socket TCP, chọn ảnh và gửi nhận dạng | *#1, #6, #7* |
| **2380601216** | **Hồ Ngọc Linh** | Xây dựng module Server nhận dạng AI và tích hợp mô hình nhận dạng vật thể; triển khai Server lên VPS và tên miền ai.devlinh.com (systemd, ufw) | *#2, #5, #10* |
| **2380600020** | **Nguyễn Văn An** | Xây dựng Server TCP đa luồng, quản lý phiên kết nối và ghi nhật ký (logging) | *#3, #4* |
| **2380600688** | **Vương Thị Mỹ Hoa** | Xây dựng chức năng quản lý lịch sử nhận dạng và xuất kết quả (PNG, CSV) | *#8, #9* |

---

## 3. Cấu trúc dự án
```text
ma-nguon/
├── common/         # Module dùng chung: Giao thức mạng, Message, mã lỗi và ProtocolCodec
├── server/         # Module Server: Xử lý Socket TCP đa luồng và dịch vụ nhận dạng
├── client/         # Client giao diện Swing, quản lý lịch sử và xuất kết quả
├── pom.xml         # File cấu hình Maven tổng (Multi-module)
└── mvnw / mvnw.cmd # Maven Wrapper
```

---

## 4. Hướng dẫn biên dịch và kiểm thử
Yêu cầu môi trường: **Java Development Kit (JDK) 17 trở lên**.

Mở terminal tại thư mục gốc của dự án:

```powershell
# Chạy toàn bộ Unit Tests của hệ thống
.\ma-nguon\mvnw.cmd test

# Biên dịch và đóng gói ứng dụng
.\ma-nguon\mvnw.cmd package
```

### Tải mô hình trước khi build
File mô hình `yolov8s.onnx` (~43 MB, YOLOv8s COCO 80 lớp của Ultralytics, giấy phép AGPL-3.0) không nằm trong git mà ở [GitHub Releases](https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR/releases/tag/model-yolov8s-coco80-v1). Tải một lần, tại thư mục `ma-nguon`, trước khi build/test:

```powershell
cd ma-nguon
.\scripts\download-model.cmd   # Linux: ./scripts/download-model.sh
```

Chi tiết xem `ma-nguon/docs/adr/0003-chuyen-sang-yolov8-onnx.md`.

