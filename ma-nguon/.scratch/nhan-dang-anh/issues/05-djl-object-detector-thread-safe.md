# 05: DjlObjectDetector thread-safe

**What to build:** Bộ nhận dạng thật dùng mô hình đã chọn ở ADR-0002, trả đối tượng với nhãn, Độ tin cậy và Bounding box tỉ lệ 0..1, an toàn khi nhiều luồng gọi cùng lúc, cắm vào Server bằng cấu hình `detector=djl`.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Ảnh mẫu cho ra đúng các đối tượng chính với khung hợp lý (test chậm)
- [ ] Nhiều luồng gọi đồng thời không lỗi và không lẫn kết quả
- [ ] Server chạy với `detector=djl` mà không phải sửa code Server
- [ ] Log thời gian nạp mô hình khi khởi động
