# 07: Heartbeat, timeout, hiển thị lỗi server

**What to build:** Client gửi Ping định kỳ, Server dọn Phiên của Client im lặng, và Client báo "Mất kết nối" nhanh khi Server tắt hoặc mạng đứt, rồi cho phép kết nối lại.

**Blocked by:** 04, 06

**Status:** ready-for-agent

- [ ] Client gửi Ping mỗi 15 giây và Server trả Pong
- [ ] Server đóng Phiên sau 60 giây không nhận được gì và xóa khỏi danh sách
- [ ] Tắt Server thì Client báo mất kết nối trong tối đa 15 giây
- [ ] Sau khi mất kết nối, người dùng kết nối lại được mà không khởi động lại Client
