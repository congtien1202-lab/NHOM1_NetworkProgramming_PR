# SPEC: Hệ thống Nhận dạng Hình ảnh Phân tán

## Problem Statement

Người dùng muốn biết trong một bức ảnh có những vật gì, nhưng máy của họ không có mô hình nhận dạng hoặc không đủ mạnh để chạy nó. Nhóm cần một hệ thống Client-Server trong đó nhiều người dùng cùng gửi ảnh tới một Server tập trung, Server nhận dạng và trả kết quả, đồng thời hệ thống phải chạy ổn định khi nhiều Client đồng thời và truy cập được qua Internet.

## Solution

Client là ứng dụng giao diện đồ họa: người dùng kết nối tới Server, chọn một ảnh JPG/PNG (hoặc ảnh mẫu), bấm nhận dạng và thấy Ảnh đã chú thích cùng bảng nhãn và Độ tin cậy. Server nhận nhiều Phiên đồng thời, dùng mô hình SSD-MobileNet để phát hiện đối tượng, ghi log mọi hoạt động và hiển thị danh sách Client đang kết nối. Client lưu mỗi lần nhận dạng thành công vào Lịch sử và cho phép xuất ảnh PNG hoặc kết quả CSV.

## User Stories

1. Là người dùng Client, tôi muốn nhập địa chỉ và cổng của Server rồi bấm kết nối, để bắt đầu gửi ảnh.
2. Là người dùng Client, tôi muốn thấy rõ trạng thái "Đã kết nối / Chưa kết nối", để biết có thể nhận dạng hay không.
3. Là người dùng Client, tôi muốn nhập tên hiển thị khi kết nối, để người vận hành Server biết tôi là ai trong danh sách.
4. Là người dùng Client, tôi muốn chọn một ảnh JPG hoặc PNG từ máy, để gửi đi nhận dạng.
5. Là người dùng Client, tôi muốn chọn nhanh một ảnh mẫu có sẵn, để thử hệ thống khi không có ảnh trong tay.
6. Là người dùng Client, tôi muốn bị chặn ngay trên Client khi chọn file quá 10 MB, để không mất thời gian gửi rồi mới bị từ chối.
7. Là người dùng Client, tôi muốn bị chặn ngay khi chọn file không phải JPG/PNG, để không gửi nhầm dữ liệu.
8. Là người dùng Client, tôi muốn thấy ảnh có vẽ khung quanh từng đối tượng và nhãn kèm Độ tin cậy, để hiểu Server tìm thấy gì và ở đâu.
9. Là người dùng Client, tôi muốn khung vẽ đúng vị trí dù tôi thay đổi kích thước cửa sổ, để kết quả luôn chính xác.
10. Là người dùng Client, tôi muốn có bảng liệt kê nhãn và Độ tin cậy phần trăm, để đọc số liệu chính xác.
11. Là người dùng Client, tôi muốn thấy thời gian Server xử lý, để đánh giá tốc độ.
12. Là người dùng Client, tôi muốn giao diện không bị đơ khi đang chờ Server, để vẫn thao tác được các thành phần khác.
13. Là người dùng Client, tôi muốn thấy thông báo rõ ràng khi Server từ chối ảnh (ảnh hỏng, quá lớn, Server bận), để biết phải làm gì.
14. Là người dùng Client, tôi muốn được báo "Mất kết nối" khi Server tắt hoặc mạng đứt, để không chờ vô ích.
15. Là người dùng Client, tôi muốn kết nối lại mà không phải khởi động lại chương trình, để tiếp tục làm việc.
16. Là người dùng Client, tôi muốn xuất Ảnh đã chú thích ra file PNG, để chèn vào báo cáo hoặc chia sẻ.
17. Là người dùng Client, tôi muốn xuất kết quả ra CSV mở được trong Excel với đúng tiếng Việt và số thập phân, để phân tích thêm.
18. Là người dùng Client, tôi muốn mỗi lần nhận dạng thành công tự được lưu vào Lịch sử, để không phải xuất tay.
19. Là người dùng Client, tôi muốn mở tab Lịch sử và thấy các lần nhận dạng, mới nhất ở trên đầu, kèm giờ, tên file và số đối tượng, để tìm lại nhanh.
20. Là người dùng Client, tôi muốn bấm vào một Mục lịch sử để xem lại ảnh có khung và bảng kết quả, để không phải gửi lại.
21. Là người dùng Client, tôi muốn Lịch sử vẫn còn sau khi đóng và mở lại chương trình, để tham khảo lâu dài.
22. Là người dùng Client, tôi muốn một dòng Lịch sử bị hỏng không làm mất cả danh sách, để dữ liệu không bị mất vì một lỗi nhỏ.
23. Là người dùng Client, tôi muốn chỉ cần file jar và Java 17 trở lên để chạy Client, để cài đặt đơn giản.
24. Là người vận hành Server, tôi muốn khởi động Server với một dòng lệnh chọn cổng, bộ nhận dạng và số Client tối đa, để cấu hình theo máy.
25. Là người vận hành Server, tôi muốn chạy không có giao diện trên máy chủ không màn hình, để triển khai lên VPS.
26. Là người vận hành Server, tôi muốn có cửa sổ liệt kê Client đang kết nối cùng IP và tên, để biết ai đang dùng.
27. Là người vận hành Server, tôi muốn log ghi mỗi lần kết nối, yêu cầu, lỗi và ngắt kết nối vào console và file, để điều tra sự cố.
28. Là người vận hành Server, tôi muốn Server phục vụ nhiều Client cùng lúc mà kết quả không bị lẫn giữa các Client, để hệ thống dùng được trong thực tế.
29. Là người vận hành Server, tôi muốn Server từ chối Client mới khi đã đủ số Phiên tối đa bằng thông báo "bận", để Server nhỏ không bị quá tải.
30. Là người vận hành Server, tôi muốn Server tự dọn Phiên của Client rút mạng sau 60 giây im lặng, để không rò rỉ tài nguyên.
31. Là người vận hành Server, tôi muốn Server chặn ảnh quá lớn kể cả "bom ảnh" nhỏ khi nén, để bộ nhớ không bị cạn.
32. Là người vận hành Server, tôi muốn một ảnh lỗi hoặc một Client hành xử sai chỉ ảnh hưởng chính Phiên đó, để Server không sập.
33. Là người vận hành Server, tôi muốn có hướng dẫn từng bước dựng Server chạy liên tục trên VPS và mở cổng cho Internet, để người mới cũng làm được.
34. Là người vận hành Server, tôi muốn có phương án dự phòng chạy trên máy cá nhân qua dịch vụ đường hầm, để demo khi chưa có VPS.
35. Là giảng viên chấm demo, tôi muốn thấy 3 Client trên 2 máy gửi ảnh gần như cùng lúc và mỗi Client nhận đúng kết quả của mình, để kiểm chứng tính đa luồng.
36. Là giảng viên chấm demo, tôi muốn thấy cửa sổ Server liệt kê đúng 3 Client với IP đúng và log ghi 3 yêu cầu, để kiểm chứng quản lý Phiên.
37. Là giảng viên chấm demo, tôi muốn thấy khung quanh đối tượng thật (người, xe, vật), để kiểm chứng đây là nhận dạng thật chứ không phải kết quả giả.
38. Là giảng viên chấm demo, tôi muốn thấy Server dọn Phiên khi rút mạng một Client và Client báo mất kết nối khi tắt Server, để kiểm chứng xử lý lỗi mạng.
39. Là giảng viên chấm demo, tôi muốn thấy một Client ở mạng khác (4G) kết nối bằng tên miền và nhận dạng thành công, để kiểm chứng việc triển khai qua Internet.
40. Là giảng viên chấm demo, tôi muốn đọc tài liệu giao thức và lý do thiết kế, để đánh giá phần lập trình mạng.

## Implementation Decisions

- **Ba module**: một module dùng chung (giao thức và kiểu dữ liệu), một Server, một Client. Client và Server chỉ biết nhau qua module dùng chung.
- **Giao thức**: nhị phân tùy chỉnh trên TCP, khung `[lệnh][độ dài][body]`; xem ADR-0001. Các Lệnh: chào, chào-xác-nhận, yêu cầu nhận dạng, kết quả nhận dạng, lỗi, ping, pong, tạm biệt. Mã lỗi: ảnh hỏng, ảnh quá lớn, chưa chào, nhận dạng thất bại, Message sai, Server bận.
- **Trình tự Phiên**: Client chào bằng tên, Server trả mã Phiên; sau đó Client gửi lần lượt từng Yêu cầu nhận dạng (một yêu cầu, một phản hồi); Client gửi Ping mỗi 15 giây; Server đóng Phiên sau 60 giây không nhận được gì.
- **Đa luồng**: mỗi Phiên chạy trên một luồng riêng lấy từ một nhóm luồng có giới hạn; vượt giới hạn thì Client mới nhận lỗi "Server bận" rồi bị đóng. Sổ đăng ký Phiên dùng chung giữa các luồng và phục vụ cửa sổ Server.
- **Nhận dạng**: Server dùng mô hình SSD-MobileNet qua thư viện DJL; engine cụ thể ghi ở ADR-0002. Bộ nhận dạng là một seam (`ObjectDetector`) có cài đặt giả để test và chạy thử mạng không cần mô hình; mọi cài đặt phải an toàn khi nhiều luồng gọi cùng lúc.
- **Bounding box**: tọa độ tỉ lệ 0..1 để Client vẽ đúng ở mọi kích thước hiển thị.
- **Giới hạn**: ảnh tối đa 10 MB; Server kiểm tra kích thước (số điểm ảnh) trước khi giải mã đầy đủ.
- **Lịch sử**: lưu trên máy Client, mỗi lần nhận dạng gồm ảnh đã chú thích, kết quả dạng CSV và một dòng chỉ mục; đọc lại được sau khi mở lại chương trình, bỏ qua mục hỏng.
- **Xuất file**: PNG cho ảnh đã chú thích; CSV UTF-8 có BOM, sáu cột, số luôn dùng dấu chấm bất kể ngôn ngữ máy.
- **Đóng gói và triển khai**: mỗi chương trình đóng thành một file jar chạy độc lập; Server chạy không giao diện trên VPS dưới trình quản lý dịch vụ, với tên miền trỏ về IP tĩnh.

## Testing Decisions

- Một test tốt chỉ kiểm tra hành vi quan sát được qua giao diện công khai của module, không kiểm tra chi tiết cài đặt.
- **Seam giao thức**: ghi rồi đọc lại từng loại Message (đã có test), và các trường hợp dữ liệu sai.
- **Seam Server**: khởi động Server thật trên cổng tự chọn với bộ nhận dạng giả, rồi dùng socket thật kiểm tra trình tự chào, nhận dạng, các mã lỗi, giới hạn số Phiên, nhiều Client đồng thời.
- **Seam bộ nhận dạng thật**: test chậm riêng (chỉ chạy khi yêu cầu) nạp mô hình thật và nhận dạng một ảnh mẫu.
- **Seam Client mạng**: kết nối tới Server thật trong test, kiểm tra nhận dạng, lỗi từ Server, ping và mất kết nối.
- **Seam xuất file và Lịch sử**: dùng thư mục tạm; kiểm tra BOM, dấu thập phân khi ngôn ngữ máy là Việt Nam, vòng ghi-đọc CSV, lưu nhiều mục theo thứ tự, mở lại, dòng chỉ mục hỏng, trùng thời điểm.
- Giao diện đồ họa không có unit test; kiểm bằng kịch bản demo đầu-cuối.
- Prior art: test của module dùng chung và test vẽ khung của Client.

## Out of Scope

- Đăng nhập, phân quyền, mã hóa đường truyền (TLS).
- Huấn luyện hay tinh chỉnh mô hình; nhận dạng video; nhận dạng chữ.
- Gửi nhiều ảnh cùng lúc trên một Phiên (xử lý theo kiểu pipeline).
- Lịch sử phía Server; đồng bộ Lịch sử giữa các máy.
- Việc thuê VPS và tên miền (quyết định của nhóm; tài liệu chỉ hướng dẫn).

## Further Notes

- Mô hình VOC chỉ nhận 20 lớp; ADR-0002 ghi rõ lựa chọn mô hình và hệ quả.
- Phiếu đăng ký đề tài có một câu mục 5 chưa đúng bản chất object detection; đề xuất sửa nằm trong thư mục báo cáo.
- Từ vựng thống nhất xem GLOSSARY; kịch bản demo xem thư mục báo cáo.
