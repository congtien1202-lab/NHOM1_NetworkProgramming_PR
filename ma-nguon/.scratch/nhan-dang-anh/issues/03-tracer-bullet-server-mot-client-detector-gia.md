# 03: Tracer bullet server 1 client với detector giả

**What to build:** Một Client kết nối tới Server thật qua TCP, chào, gửi một ảnh và nhận Kết quả nhận dạng từ bộ nhận dạng giả; các tình huống lỗi cơ bản trả đúng mã lỗi.

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] Trình tự chào, nhận dạng, tạm biệt chạy đúng trên socket thật (test tự động)
- [ ] Ảnh hỏng trả lỗi ảnh hỏng và Phiên vẫn dùng tiếp được
- [ ] Ảnh quá nhiều điểm ảnh trả lỗi ảnh quá lớn mà không giải mã toàn bộ
- [ ] Gửi yêu cầu khi chưa chào bị báo lỗi rồi bị đóng kết nối
- [ ] Bộ nhận dạng ném lỗi thì Client nhận lỗi nhận dạng thất bại
