# 02: Spike DJL, chọn engine, ADR-0002

**What to build:** Xác định được engine DJL và mô hình SSD-MobileNet nào thực sự chạy được trên Windows x64 với Java 17 trở lên, nhận dạng được một ảnh mẫu, và ghi lại lựa chọn cùng phương án bị loại.

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] Một chương trình thử nạp mô hình và in ra các đối tượng của ảnh mẫu
- [ ] ADR-0002 ghi engine, mô hình, phương án bị loại, lý do và hệ quả (dung lượng tải lần đầu, số lớp)
- [ ] Ghi cách tải sẵn mô hình để Server trên VPS không tải lúc khởi động
