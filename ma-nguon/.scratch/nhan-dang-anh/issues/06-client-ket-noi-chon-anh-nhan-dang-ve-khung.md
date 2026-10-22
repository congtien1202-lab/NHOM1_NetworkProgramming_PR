# 06: Client: kết nối, chọn/mô phỏng ảnh, giới hạn 10 MB, nhận dạng, vẽ box

**What to build:** Người dùng mở Client, nhập địa chỉ Server, kết nối, chọn ảnh JPG/PNG (hoặc ảnh mẫu), bấm nhận dạng và thấy ảnh có khung cùng bảng nhãn và Độ tin cậy mà giao diện không bị đơ.

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] Kết nối thành công/thất bại hiện trạng thái và thông báo rõ
- [ ] File lớn hơn 10 MB hoặc không phải JPG/PNG bị chặn ngay ở Client
- [ ] Nhận dạng chạy nền, hiển thị ảnh có khung đúng vị trí khi đổi kích thước cửa sổ
- [ ] Bảng kết quả hiện nhãn, Độ tin cậy phần trăm và thời gian xử lý
- [ ] Lỗi do Server trả về (ảnh hỏng, quá lớn, bận) hiển thị bằng thông báo dễ hiểu
- [ ] Có ảnh mẫu để thử khi không có ảnh sẵn
