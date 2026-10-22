# Đề xuất sửa phiếu đăng ký đề tài

**Vấn đề:** Mục 5 của phiếu ghi "Nhận dạng hình ảnh: Viết ra các chỉ số đã đọc được trong hình ảnh". Câu này mô tả việc đọc số liệu từ ảnh (giống OCR), không đúng với hệ thống nhóm làm là phát hiện đối tượng trong ảnh.

**Đề xuất thay bằng:**

> Nhận dạng hình ảnh: phát hiện các đối tượng trong ảnh bằng mô hình SSD-MobileNet, trả về tên đối tượng, độ tin cậy và vị trí (bounding box).

**Lý do:** Câu mới khớp với đầu ra thật của hệ thống (nhãn, độ tin cậy, khung bao), tránh bị hiểu nhầm yêu cầu đọc chữ hoặc số trong ảnh khi chấm.

Nhóm cần hỏi giảng viên có đồng ý sửa phiếu hay không trước buổi bảo vệ.
