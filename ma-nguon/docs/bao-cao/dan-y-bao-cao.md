# Dàn ý báo cáo

Đồ án IX.4: Hệ thống Nhận dạng Hình ảnh Phân tán. Nhóm 01: Trần Công Tiến, Hồ Ngọc Linh, Nguyễn Văn An, Vương Thị Mỹ Hoa.

## 1. Giới thiệu
- Bài toán và động lực: nhiều người dùng cần nhận dạng ảnh mà không tự chạy mô hình.
- Mục tiêu, phạm vi, phân công nhóm.
- Tóm tắt kết quả đạt được.

## 2. Cơ sở lý thuyết
- **TCP và Socket**: kết nối tin cậy, `ServerSocket`/`Socket` trong Java, luồng byte, vấn đề ranh giới message.
- **Đa luồng**: một luồng cho mỗi Phiên, nhóm luồng giới hạn, dữ liệu dùng chung và đồng bộ.
- **Giao thức tùy chỉnh**: khung `[lệnh][độ dài][body]`, so sánh với HTTP/JSON (ADR-0001).
- **Nhận dạng đối tượng**: khái niệm object detection, mạng SSD-MobileNet, nhãn, Độ tin cậy, bounding box, thư viện DJL.
- Kiến thức hỗ trợ: heartbeat và timeout, NAT/firewall, DNS.

## 3. Thiết kế
- Kiến trúc tổng thể và ba module; sơ đồ triển khai.
- Đặc tả giao thức: các Lệnh, mã lỗi, sơ đồ trình tự một Phiên.
- Thiết kế Server: vòng accept, nhóm luồng, sổ Phiên, bộ nhận dạng (seam).
- Thiết kế Client: giao diện, luồng nền, vẽ khung, Lịch sử, xuất file.
- Các ràng buộc: giới hạn 10 MB, chống bom ảnh, giới hạn số Phiên.

## 4. Cài đặt
- Công nghệ: Java 17, Maven, Swing, DJL.
- Điểm đáng chú ý từng module (đọc/ghi Message, xử lý lỗi, thread-safe của bộ nhận dạng).
- Lựa chọn engine và mô hình (ADR-0002).

## 5. Kiểm thử
- Kiểm thử tự động: giao thức, Server với socket thật, xuất file, Lịch sử.
- Kiểm thử đầu-cuối theo kịch bản demo (`kich-ban-demo.md`).
- Kiểm thử nhiều Client đồng thời; đo thời gian xử lý; kiểm thử lỗi mạng.

## 6. Triển khai
- Đóng gói jar, firewall, VPS, tên miền, systemd (`docs/deploy.md`).
- Phương án dự phòng và kết quả kiểm tra từ mạng 4G.

## 7. Kết luận
- Kết quả so với mục tiêu, hạn chế (20 lớp VOC, không mã hóa đường truyền).
- Hướng phát triển: TLS, xác thực, nhiều ảnh trên một Phiên, mô hình nhiều lớp hơn.

## Phụ lục
- Bảng Lệnh và mã lỗi, ảnh chụp màn hình, danh sách ADR, hướng dẫn chạy.
