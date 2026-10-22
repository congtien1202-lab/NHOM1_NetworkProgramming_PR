# 01: Khung Maven + giao thức + stub hợp đồng

**What to build:** Dự án nhiều module có thể build ra hai file jar chạy độc lập, kèm giao thức Message nhị phân dùng chung (ADR-0001) và các lớp hợp đồng để các phần còn lại làm song song.

**Blocked by:** None (can start immediately)

**Status:** done (Phase 0)

- [x] Build một lệnh ra jar Server và jar Client
- [x] Mọi loại Message ghi rồi đọc lại đúng; dữ liệu sai bị từ chối bằng lỗi giao thức
- [x] Các lớp hợp đồng (bộ nhận dạng, kết nối Client, vẽ khung, lịch sử, xuất file) có sẵn dạng stub
