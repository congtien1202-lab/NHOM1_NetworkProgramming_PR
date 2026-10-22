# 04: Đa luồng, quản lý phiên, log, cửa sổ server, chế độ không GUI

**What to build:** Server phục vụ nhiều Client cùng lúc, từ chối Client dư bằng lỗi "bận", ghi log ra console và file, có cửa sổ liệt kê Client đang kết nối, và chạy được không giao diện.

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] Nhiều Client đồng thời đều nhận đúng kết quả ảnh của mình (test tự động)
- [ ] Vượt số Phiên tối đa thì Client mới nhận lỗi Server bận rồi bị đóng
- [ ] Cửa sổ Server hiện đúng danh sách Client với IP và tên, cập nhật khi vào/ra
- [ ] Log kết nối, yêu cầu, lỗi, ngắt kết nối ghi ra console và file
- [ ] Cổng, bộ nhận dạng, GUI, số Client tối đa cấu hình được bằng tham số dòng lệnh; giá trị sai báo lỗi rõ
- [ ] Chạy được bằng file jar với chế độ không giao diện
