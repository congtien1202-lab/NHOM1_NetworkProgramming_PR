# Giao thức nhị phân tùy chỉnh trên TCP

Đề tài thuộc môn Lập trình mạng nên Client và Server nói chuyện bằng giao thức tự thiết kế trên `Socket` TCP, không dùng HTTP hay JSON. Mỗi Message có khung `[command: 1 byte][length: 4 byte][body]` (big-endian); ảnh đi nguyên dạng byte, không mã hóa base64.

Lý do: ảnh là dữ liệu nhị phân lớn nên khung nhị phân tránh phình 33% của base64 và việc parse văn bản; khung có độ dài cố định ở đầu cho phép Server từ chối Message quá khổ trước khi cấp phát bộ nhớ; và việc tự thiết kế khung là nội dung chính của môn học, còn HTTP sẽ che mất phần đó.

## Các quy ước đi kèm

- **Giới hạn 10 MB** mỗi ảnh (body tối đa cộng thêm 64 KB cho tên file). Mỗi yêu cầu tốn khoảng 20 MB RAM khi giải mã, nên giới hạn này bảo vệ Server nhỏ (VPS 2 GB). Server còn giới hạn số điểm ảnh để chặn "bom ảnh".
- **Một yêu cầu, một phản hồi**: Client gửi Yêu cầu nhận dạng rồi chờ Kết quả nhận dạng hoặc lỗi trước khi gửi tiếp. Giao thức đơn giản, không cần ghép nối yêu cầu với phản hồi; muốn song song thì mở nhiều Phiên.
- **Heartbeat do Client gửi** mỗi 15 giây; Server đóng Phiên nếu 60 giây không nhận được gì. Cách này dọn được Phiên của Client rút mạng đột ngột (TCP không tự báo), và Client phát hiện Server chết khi không nhận được PONG.

## Consequences

- Muốn thêm Lệnh mới phải sửa cả hai phía; không có công cụ gỡ lỗi sẵn như với HTTP/JSON.
- Mọi Message đều được giải mã hoàn toàn ngay khi đọc, nên lỗi định dạng bị chặn ở một chỗ duy nhất.
