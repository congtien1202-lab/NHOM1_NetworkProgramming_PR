# Nhận dạng Hình ảnh Phân tán

Hệ thống Client-Server qua TCP: người dùng gửi ảnh từ Client, Server nhận dạng các đối tượng trong ảnh và trả kết quả để Client hiển thị.

## Language

### Các bên tham gia

**Client**:
Chương trình của người dùng, chọn ảnh, gửi đi và hiển thị kết quả.
_Avoid_: Máy khách, ứng dụng khách

**Server**:
Chương trình trung tâm nhận ảnh từ nhiều Client cùng lúc và nhận dạng đối tượng trong ảnh.
_Avoid_: Máy chủ, backend

**Phiên (Session)**:
Quan hệ làm việc của một Client với Server, bắt đầu khi Server chấp nhận lời chào của Client và kết thúc khi một bên đóng kết nối.
_Avoid_: Kết nối, connection

### Giao tiếp

**Message**:
Một đơn vị thông tin trọn vẹn trao đổi giữa Client và Server, luôn mang một Lệnh.
_Avoid_: Gói tin, packet

**Lệnh (Command)**:
Loại của một Message, cho biết Message đó có ý nghĩa gì (chào, yêu cầu nhận dạng, kết quả, lỗi, ping, pong, tạm biệt).
_Avoid_: Mã lệnh, opcode

**Yêu cầu nhận dạng**:
Message Client gửi để nhờ Server nhận dạng một ảnh; mang tên file và nội dung ảnh.
_Avoid_: Request ảnh, upload

**Kết quả nhận dạng**:
Message Server trả lời một Yêu cầu nhận dạng; gồm các Đối tượng phát hiện và thời gian xử lý.
_Avoid_: Response, phản hồi

**Heartbeat (PING/PONG)**:
Cặp Message Client gửi định kỳ để Server biết Phiên còn sống và Client biết Server còn sống.
_Avoid_: Keep-alive

### Kết quả nhận dạng

**Đối tượng phát hiện (Detection)**:
Một vật thể Server tìm thấy trong ảnh, gồm nhãn, Độ tin cậy và Bounding box.
_Avoid_: Kết quả đơn, object

**Bounding box**:
Hình chữ nhật bao quanh một Đối tượng phát hiện, tính theo tỉ lệ 0..1 so với kích thước ảnh gốc nên không phụ thuộc kích thước hiển thị.
_Avoid_: Khung, hộp, tọa độ điểm ảnh

**Độ tin cậy**:
Mức chắc chắn của Server rằng nhãn của một Đối tượng phát hiện là đúng, từ 0 đến 1 (hiển thị dạng phần trăm).
_Avoid_: Xác suất, điểm số, score

**Ảnh đã chú thích (annotated)**:
Ảnh gốc đã được vẽ Bounding box và nhãn của các Đối tượng phát hiện.
_Avoid_: Ảnh kết quả, ảnh output

### Lịch sử

**Mục lịch sử**:
Bản lưu một lần nhận dạng thành công trên máy của Client: Ảnh đã chú thích, kết quả, tên file gốc và thời điểm.
_Avoid_: Bản ghi, log
