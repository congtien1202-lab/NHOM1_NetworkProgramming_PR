# Hướng dẫn sử dụng – Hệ thống Nhận dạng Hình ảnh Phân tán (Nhóm 01)

## 1. Giới thiệu

Hệ thống gồm **Client** (ứng dụng có giao diện trên máy bạn) và **Server** (máy chủ có mô hình AI). Bạn chọn một ảnh, Client gửi ảnh lên Server, Server tìm các vật thể trong ảnh và trả về ảnh có vẽ khung cùng bảng nhãn và độ tin cậy.

Server của nhóm **đang chạy sẵn tại `ai.devlinh.com`, cổng `5000`**. Chỉ cần mở Client và bấm Kết nối, không phải cài gì thêm.

Nội dung gửi kèm:

```
NhanDangAnh-Nhom01/
  HUONG-DAN-SU-DUNG.md                  (file này)
  chuong-trinh/
    NhanDangAnh/NhanDangAnh.exe         Client, không cần cài Java
    client-1.0-SNAPSHOT-all.jar         Client cho máy đã có Java 17+
    server-1.0-SNAPSHOT-all.jar         Server
    chay-server.cmd                     Chạy Server trên máy mình
  ma-nguon/                             Mã nguồn, mở bằng NetBeans
```

## 2. Dùng Client

1. **Giải nén** file zip ra một thư mục bất kỳ.
2. Mở `chuong-trinh\NhanDangAnh\NhanDangAnh.exe`.
   - Nếu Windows hiện màn hình xanh "Windows protected your PC" (SmartScreen): bấm **More info**, rồi **Run anyway**. Đây là cảnh báo bình thường với chương trình chưa có chữ ký số.
3. Trên thanh trên cùng có sẵn `Host: ai.devlinh.com`, `Port: 5000` và ô **Tên** (mặc định là tên máy bạn; Server dùng tên này để hiển thị bạn trong danh sách). Bấm **Kết nối**.
   - Khi thành công, bên cạnh hiện chữ xanh **"Đã kết nối (phiên #…)"** và nút đổi thành **Ngắt**.
4. **Đưa ảnh vào** bằng một trong ba cách:
   - **Chọn ảnh**: mở hộp thoại chọn file JPG/PNG.
   - **Kéo thả** file ảnh từ Explorer vào khung ảnh.
   - **Mô phỏng chụp ảnh**: lấy ngẫu nhiên một ảnh mẫu có sẵn (dùng khi không có ảnh trong tay).
5. Bấm **Nhận dạng** (nút chỉ sáng khi đã kết nối và đã có ảnh). Giao diện không bị đơ trong lúc chờ; thanh dưới cùng ghi "Đang nhận dạng...".
6. **Đọc kết quả**:
   - Khung ảnh hiện ảnh có vẽ khung quanh từng vật thể, kèm nhãn và độ tin cậy.
   - Bảng bên phải có 3 cột: **STT**, **Nhãn**, **Độ tin cậy (%)**.
   - Thanh dưới cùng ghi số đối tượng và thời gian Server xử lý (ví dụ "3 đối tượng, 150 ms").
7. **Lưu kết quả**:
   - **Xuất ảnh**: lưu ảnh đã vẽ khung ra file PNG.
   - **Xuất CSV**: lưu bảng kết quả ra file CSV, mở được bằng Excel (tiếng Việt hiển thị đúng).
8. Tab **Lịch sử**: mỗi lần nhận dạng thành công được lưu tự động, mới nhất ở trên đầu, kèm giờ, tên file và số đối tượng. Bấm vào một mục để xem lại ảnh và bảng kết quả. Lịch sử vẫn còn sau khi đóng và mở lại chương trình. Dữ liệu nằm ở thư mục `%USERPROFILE%\.image-recognition\history` (dán đường dẫn này vào thanh địa chỉ Explorer để mở).

### Giới hạn cần biết

- Chỉ nhận ảnh **JPG hoặc PNG**, dung lượng **tối đa 10 MB**. Ảnh sai định dạng hoặc quá lớn bị Client chặn ngay với thông báo "Ảnh phải là JPG/PNG và không quá 10 MB".
- Mô hình chỉ nhận dạng được **20 lớp** (bộ dữ liệu PASCAL VOC). Vật thể ngoài danh sách sẽ không được khung lại. Nhãn hiển thị bằng tiếng Anh:

| Nhóm | Nhãn (tiếng Việt) |
|---|---|
| Người | `person` (người) |
| Động vật | `bird` (chim), `cat` (mèo), `cow` (bò), `dog` (chó), `horse` (ngựa), `sheep` (cừu) |
| Phương tiện | `aeroplane` (máy bay), `bicycle` (xe đạp), `boat` (thuyền), `bus` (xe buýt), `car` (ô tô), `motorbike` (xe máy), `train` (tàu hỏa) |
| Đồ vật | `bottle` (chai), `chair` (ghế), `diningtable` (bàn ăn), `pottedplant` (chậu cây), `sofa` (ghế sofa), `tvmonitor` (tivi/màn hình) |

- Trạng thái kết nối:
  - **"Mất kết nối"** (chữ đỏ): Server tắt hoặc mạng đứt. Bấm **Kết nối** lại, không cần mở lại chương trình.
  - **"Server đang bận, thử lại sau"**: Server đã đủ số người dùng cùng lúc. Chờ một lát rồi bấm **Kết nối** lại.

## 3. Chạy Server trên máy mình (demo trong mạng LAN)

Chỉ cần khi muốn demo không dùng Server trên Internet.

**Yêu cầu:** Windows, **Java 17 trở lên** đã cài, và có **Internet ở lần chạy đầu** (Server tải mô hình nhận dạng, vài chục MB, về thư mục `%USERPROFILE%\.djl.ai`; các lần sau không cần mạng).

1. Mở `chuong-trinh\chay-server.cmd` (bấm đúp). Lệnh bên trong là `java -jar server-1.0-SNAPSHOT-all.jar --detector=djl`.
2. Lần đầu, Windows Firewall hiện hộp thoại hỏi quyền cho `java.exe`: chọn **Allow access** ở mạng **Private**. Nếu bỏ qua, các máy khác trong mạng sẽ không kết nối được.
3. Đợi cửa sổ Server hiện và log báo đã nạp mô hình và đang lắng nghe cổng 5000. Cửa sổ Server liệt kê các Client đang kết nối (IP và tên).
4. Trên **máy chạy Server**: mở Client, đổi ô **Host** thành `localhost`.
   Trên **máy khác cùng mạng**: đổi **Host** thành IP LAN của máy Server (mở Command Prompt trên máy đó, gõ `ipconfig`, xem dòng *IPv4 Address*, ví dụ `192.168.1.10`). Giữ Port `5000`.
5. Bấm **Kết nối** như bình thường.

**Thử nhanh không cần mô hình:** chạy `java -jar server-1.0-SNAPSHOT-all.jar --detector=fake`. Chế độ này trả kết quả giả, không cần Internet, chỉ dùng để kiểm tra kết nối mạng; kết quả khung không phải nhận dạng thật.

Tùy chọn khác: `--port=5000` (đổi cổng), `--maxClients=20` (số Client tối đa cùng lúc).

## 4. Mở mã nguồn

Mã nguồn nằm trong thư mục `ma-nguon`. Dự án dùng Maven đa module, cần **JDK 17+**.

**Bằng NetBeans:** *File → Open Project…* → chọn thư mục `ma-nguon` (NetBeans nhận ra dự án Maven và nạp luôn 3 module).

**Bằng dòng lệnh** (PowerShell, đứng trong thư mục `ma-nguon`):

```powershell
.\mvnw.cmd -q test       # chạy kiểm thử
.\mvnw.cmd -q package    # build ra các file jar trong server\target và client\target
```

**Cấu trúc module:**

| Module | Vai trò |
|---|---|
| `common` | Giao thức mạng và kiểu dữ liệu dùng chung |
| `server` | Server đa luồng, bộ nhận dạng (DJL + SSD-MobileNet), cửa sổ danh sách Client, log |
| `client` | Giao diện Swing, kết nối mạng, xuất file, lịch sử |

Tài liệu liên quan trong mã nguồn:

- `SPEC.md`: đặc tả hệ thống và các yêu cầu.
- `docs/deploy.md`: build, triển khai lên VPS, chạy demo LAN.
- `docs/adr/`: các quyết định thiết kế (giao thức nhị phân trên TCP, lựa chọn engine DJL/MXNet).
- `docs/bao-cao/kich-ban-demo.md`: kịch bản demo từng bước.

## 5. Lỗi thường gặp

| Hiện tượng | Nguyên nhân thường gặp và cách xử lý |
|---|---|
| Hộp thoại "Không kết nối được tới …" | Kiểm tra máy có Internet. Kiểm tra ô Host/Port đúng (`ai.devlinh.com`, `5000`). Mạng công ty/trường đôi khi chặn cổng 5000: thử đổi sang mạng khác hoặc điểm phát 4G. Nếu vẫn lỗi, báo nhóm để kiểm tra Server. |
| Kết nối Server trên máy khác trong LAN không được | Chưa cho phép `java.exe` qua Windows Firewall (xem mục 3, bước 2); hoặc nhập sai IP (dùng `ipconfig` để lấy lại); hai máy không cùng mạng. |
| "Server đang bận, thử lại sau" | Server đã đủ người dùng. Chờ vài giây rồi kết nối lại. |
| Chuyển sang "Mất kết nối" giữa chừng | Server bị tắt hoặc mạng đứt. Bấm **Kết nối** lại. |
| Windows SmartScreen chặn `NhanDangAnh.exe` | Bấm **More info → Run anyway**. |
| "Ảnh phải là JPG/PNG và không quá 10 MB" | Đổi sang ảnh JPG/PNG nhỏ hơn 10 MB (mở ảnh bằng Paint rồi lưu lại, hoặc giảm kích thước). Ảnh hỏng, không mở được cũng bị từ chối với thông báo này. |
| Nút **Nhận dạng** bị mờ | Chưa kết nối Server hoặc chưa chọn ảnh. |
| Nhận dạng xong nhưng bảng trống, không có khung | Trong ảnh không có vật thể thuộc 20 lớp ở mục 2, hoặc vật quá nhỏ/mờ (độ tin cậy dưới 30% bị bỏ). Thử ảnh khác, rõ nét hơn, vật chiếm phần lớn khung hình. |
| `chay-server.cmd` báo `'java' is not recognized` | Máy chưa cài Java 17+. Cài JDK/JRE 17 rồi mở lại. |
| Server báo lỗi khi chạy lần đầu | Cần Internet để tải mô hình. Hoặc thử `--detector=fake` để kiểm tra riêng phần mạng. |
| Mở file CSV trong Excel bị lỗi font | Mở bằng *Data → From Text/CSV* và chọn mã hóa **UTF-8**; thông thường mở trực tiếp là đúng. |
