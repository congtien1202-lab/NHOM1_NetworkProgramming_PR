# Đồ Án Lập Trình Mạng: Hệ Thống Nhận Dạng Hình Ảnh Phân Tán
> **Nhóm thực hiện:** Nhóm 01  
> **Lớp:** 23DTHE4, 23DTHA5  
> **Đề tài:** IX.4 - Hệ thống Nhận dạng Hình ảnh Phân tán (Mô hình Client - Server qua giao thức TCP)

---

## 1. Giới thiệu tổng quan
Dự án xây dựng hệ thống nhận dạng hình ảnh phân tán hoạt động theo mô hình Client - Server:
- **Server:** Xử lý đa luồng (Multi-threading), tiếp nhận kết nối TCP từ nhiều Client đồng thời, nhận ảnh JPG/PNG, chạy mô hình nhận dạng vật thể và trả về danh sách đối tượng (nhãn, độ tin cậy, toạ độ 0..1 theo ảnh gốc).
- **Client:** Giao diện đồ họa (Java Swing), cho phép người dùng chọn hình ảnh, gửi yêu cầu qua Socket TCP đến Server và nhận danh sách đối tượng từ Server, tự vẽ bounding box lên ảnh và hiển thị bảng kết quả chi tiết (Server không trả ảnh đã vẽ).
- **Giao thức truyền thông:** Thiết kế giao thức nhị phân tùy chỉnh (Custom Application Protocol) trên nền tầng vận chuyển TCP.
- **Công nghệ:** Java 17, Maven đa module, Swing; nhận dạng bằng DJL + ONNX Runtime với YOLOv8s (COCO 80 lớp + lớp `monkey`, tổng 81 lớp) và lớp bảo vệ MonkeyGuard giảm lỗi nhận người thành khỉ.

---

## 2. Bảng ma trận thống kê đóng góp của thành viên
*(Bảng theo dõi tiến độ và đối chiếu đánh giá quá trình theo quy định thực hiện đồ án)*

| MSSV | Họ và Tên | Công việc đã thực hiện | Merge Requests / PR |
| :--- | :--- | :--- | :--- |
| **2380614894** | **Trần Công Tiến** | Khởi tạo khung Maven (`common`); Xây dựng Client Swing kết nối Socket TCP, chọn ảnh và gửi nhận dạng | *#1, #6, #7* |
| **2380601216** | **Hồ Ngọc Linh** | Xây dựng module Server nhận dạng AI và tích hợp mô hình nhận dạng vật thể; triển khai Server lên VPS và tên miền ai.devlinh.com (systemd, ufw); nâng cấp YOLOv8s ONNX, fine-tune lớp monkey, đóng gói Windows, MonkeyGuard | *#2, #5, #10, #12, #13, #14, #15* |
| **2380600020** | **Nguyễn Văn An** | Xây dựng Server TCP đa luồng, quản lý phiên kết nối và ghi nhật ký (logging) | *#3, #4* |
| **2380600688** | **Vương Thị Mỹ Hoa** | Xây dựng chức năng quản lý lịch sử nhận dạng và xuất kết quả (PNG, CSV) | *#8, #9* |

---

## 3. Cấu trúc dự án
```text
ma-nguon/
├── common/         # Module dùng chung: Giao thức mạng, Message, mã lỗi và ProtocolCodec
├── server/         # Module Server: Xử lý Socket TCP đa luồng và dịch vụ nhận dạng
├── client/         # Client giao diện Swing, quản lý lịch sử và xuất kết quả
├── docs/           # Tài liệu: adr/ (quyết định kiến trúc), deploy.md (triển khai VPS), huan-luyen/ (fine-tune và kiểm thử mô hình)
├── scripts/        # download-model.cmd/.sh (tải mô hình), package-client/server/release.ps1 (đóng gói Windows)
├── pom.xml         # File cấu hình Maven tổng (Multi-module)
└── mvnw / mvnw.cmd # Maven Wrapper
```

---

## 4. Hướng dẫn biên dịch và kiểm thử
Yêu cầu môi trường: **Java Development Kit (JDK) 17 trở lên**. Mọi lệnh Maven chạy tại thư mục `ma-nguon` (từ gốc repo chạy `.\ma-nguon\mvnw.cmd` sẽ lỗi `MissingProjectException`).

### Tải mô hình trước khi build
Hai file mô hình ONNX không nằm trong git mà ở GitHub Releases (AGPL-3.0, của Ultralytics), phải tải **trước** khi build/test vì chúng được đóng vào jar của Server:
- `yolov8s-coco81.onnx` (~43 MB, YOLOv8s tinh chỉnh thêm lớp `monkey`, 81 lớp): [release `model-yolov8s-coco81-v1`](https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR/releases/tag/model-yolov8s-coco81-v1).
- `guard/yolov8s-coco80.onnx` (~43 MB, YOLOv8s gốc 80 lớp cho lớp bảo vệ MonkeyGuard): [release `model-yolov8s-coco80-guard-v1`](https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR/releases/tag/model-yolov8s-coco80-guard-v1). Thiếu file này, Server mô hình 81 lớp không khởi động được.

```powershell
cd ma-nguon
.\scripts\download-model.cmd   # Linux: ./scripts/download-model.sh (kiểm SHA-256 từng file)
```

### Build và kiểm thử
```powershell
cd ma-nguon

# Unit test nhanh của cả ba module
.\mvnw.cmd test

# Test chậm (nạp mô hình ONNX thật, gồm ảnh khỉ)
.\mvnw.cmd -pl server -am test "-Dgroups=slow" "-DexcludedGroups=none"

# Biên dịch và đóng gói (tạo hai jar chạy độc lập trong server/target và client/target)
.\mvnw.cmd package
```

JDK 22 của Oracle trên Windows kèm các DLL Visual C++ bản cũ nên ONNX Runtime báo `UnsatisfiedLinkError` khi khởi tạo; test chậm chạy được bằng JDK khác: thêm `"-Djvm=<đường dẫn java.exe của JDK khác>"` (xem `ma-nguon/docs/adr/0003-chuyen-sang-yolov8-onnx.md`). Linux/VPS không bị lỗi này.

Chi tiết mô hình: `ma-nguon/docs/adr/0003-chuyen-sang-yolov8-onnx.md`.

---

## 5. Chạy chương trình
Sau khi `.\mvnw.cmd package` (tại `ma-nguon`):

```powershell
# Server (mặc định mở cửa sổ giao diện; --gui=false cho máy không màn hình)
java -jar server\target\server-1.0-SNAPSHOT-all.jar --gui=false --detector=djl --port=5000 --maxClients=20 --ortThreads=2

# Client (mặc định kết nối ai.devlinh.com:5000; đổi host trên giao diện hoặc bằng -Dimagerec.host)
java -jar client\target\client-1.0-SNAPSHOT-all.jar
java -Dimagerec.host=localhost -jar client\target\client-1.0-SNAPSHOT-all.jar
```

| Tham số Server | Ý nghĩa | Mặc định |
| :--- | :--- | :--- |
| `--port` | Cổng lắng nghe | 5000 |
| `--detector` | `djl` (mô hình YOLOv8s thật) hoặc `fake` (kết quả giả để thử mạng) | `fake` |
| `--gui` | `false` để chạy không cửa sổ (VPS) | `true` |
| `--maxClients` | Số phiên tối đa cùng lúc; vượt thì Client mới nhận "Server bận" | 20 |
| `--ortThreads` | Số luồng ONNX Runtime cho mỗi lần suy luận (VPS 4 vCPU dùng 2) | để trống = mặc định |
| `--model` | File ONNX khác thay mô hình trong jar (cần `synset.txt` cạnh nó) | để trống = mô hình trong jar |

Các giá trị cũng đọc được từ `server.properties`; tham số dòng lệnh ghi đè.

Server công khai của nhóm: `ai.devlinh.com:5000` (VPS Ubuntu, `systemd`). Cách triển khai, số đo và cách quay lại bản cũ: `ma-nguon/docs/deploy.md`. Đóng gói cho Windows (cần JDK 21 trở lên có `jpackage`, chạy tại `ma-nguon`): `scripts\package-client.ps1` (Client `.exe` kèm Java thu gọn), `scripts\package-server.ps1` (Server `.exe`, thay DLL VC++ để ONNX Runtime chạy), `scripts\package-release.ps1` (zip gồm chương trình, mã nguồn, hướng dẫn, tài liệu).

---

## 6. Giao thức
Giao thức nhị phân trên TCP, mỗi message là một khung `[lệnh: 1 byte][độ dài body: 4 byte][body]` (big-endian), cài đặt trong `common` (`Command`, `Message`, `ProtocolCodec`, `Protocol`).

| Mã | Lệnh | Hướng | Nội dung body |
| :--- | :--- | :--- | :--- |
| 1 | `HELLO` | Client → Server | tên hiển thị của Client |
| 2 | `HELLO_OK` | Server → Client | mã phiên |
| 3 | `DETECT_REQUEST` | Client → Server | tên file, ảnh JPG/PNG |
| 4 | `DETECT_RESULT` | Server → Client | thời gian xử lý, danh sách đối tượng (nhãn, độ tin cậy, toạ độ x, y, rộng, cao theo tỉ lệ 0..1 của ảnh gốc) |
| 5 | `ERROR` | Server → Client | mã lỗi, thông báo |
| 6 | `PING` | Client → Server | rỗng |
| 7 | `PONG` | Server → Client | rỗng |
| 8 | `BYE` | Client → Server | rỗng |

Giới hạn: ảnh tối đa 10 MB (và tối đa 40 triệu điểm ảnh khi giải mã); Client gửi `PING` mỗi 15 giây; Server đóng kết nối nếu 60 giây không nhận được gì.
