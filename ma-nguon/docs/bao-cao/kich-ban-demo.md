# Kịch bản demo đầu-cuối

Chuẩn bị: file jar Server và Client, 3 ảnh thử (có người, xe, vật), một file lớn hơn 10 MB, một file không phải JPG/PNG, hai máy cùng mạng (hoặc một máy có kết nối Internet cho bước 7).

| # | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Khởi động Server với `--detector=djl` | Cửa sổ Server hiện; log báo mô hình đã nạp và đang lắng nghe cổng 5000 |
| 2 | Mở 3 Client trên 2 máy, kết nối tới Server | Cửa sổ Server hiện 3 dòng Client với IP đúng |
| 3 | Ba Client gửi ảnh gần như cùng lúc | Mỗi Client nhận đúng kết quả ảnh của mình, có khung quanh người/xe/vật; log ghi 3 yêu cầu |
| 4 | Chọn file lớn hơn 10 MB, rồi file không phải JPG/PNG | Client chặn cả hai, hiển thị thông báo, không gửi gì |
| 5 | Xuất PNG và CSV; mở PNG bằng trình xem ảnh, CSV bằng Excel; đóng và mở lại Client | PNG có khung; CSV đúng 6 cột, số dạng `0.92`, tiếng Việt không lỗi font; tab Lịch sử còn đủ mục, bấm vào mục thấy lại ảnh và bảng |
| 6a | Rút mạng một Client | Trong tối đa 60 giây Server xóa Phiên đó khỏi danh sách |
| 6b | Tắt Server | Các Client báo "Mất kết nối" trong tối đa 15 giây |
| 7 | (Sau triển khai) Client trên mạng 4G kết nối bằng tên miền và cổng, nhận dạng một ảnh | Kết nối và nhận dạng thành công |

Ghi lại kết quả thực tế (đạt/không đạt, ghi chú, ảnh chụp màn hình) cho từng bước vào báo cáo.
