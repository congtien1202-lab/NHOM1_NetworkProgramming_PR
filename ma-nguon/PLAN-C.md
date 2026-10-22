# PLAN-C: Client mạng & giao diện chính (Swing)

> Cửa sổ này chỉ làm PLAN-C. Đọc `COORDINATION.md` trước (quy tắc, sở hữu file, hợp đồng). Ba cửa sổ khác đang làm A, B, D cùng lúc trong cùng thư mục.

## Bối cảnh
Đồ án IX.4 "Hệ thống Nhận dạng Hình ảnh Phân tán" (môn Lập trình mạng, Java 17, NetBeans/Maven). Người dùng chọn ảnh JPG/PNG (≤ 10 MB) hoặc "mô phỏng chụp ảnh", gửi lên Server qua TCP, nhận kết quả (nhãn, độ tin cậy, bounding box) và xem box vẽ trên ảnh; xuất kết quả và xem lịch sử. Giao thức đã cài trong `common` (`ProtocolCodec`, `Message`, `Protocol`). Server do PLAN-A làm (chưa chắc đã xong khi bạn bắt đầu).

PLAN-C làm **`ServerConnection`** (mạng phía Client) và **cửa sổ chính**. Lịch sử và xuất file là của PLAN-D: bạn chỉ gắn các stub đã có (`HistoryStore`, `HistoryPanel`, `ResultExporter`). Vẽ box dùng `BoxRenderer` đã có sẵn.

## Sở hữu file
- Tạo/sửa: `client/pom.xml`, `client/src/main/java/com/nhom01/imagerec/client/net/**` (**giữ chữ ký public** của `ServerConnection`, `ServerErrorException`), `client/src/main/java/.../client/ui/**`, `client/src/main/resources/samples/**`, `client/src/test/java/.../client/net/**`, `client/src/test/java/.../client/ui/**`.
- Chỉ đọc: `common/**`, `client/.../render/BoxRenderer.java`, `client/.../history/**`, `client/.../export/**` (PLAN-D).
- Build/test: `.\mvnw.cmd -q -pl client test`. Không `-am`, không `clean` ở gốc, không commit.

## Thiết kế
### `ServerConnection` (deep module, hợp đồng trong Javadoc của file)
- `connect`: `new Socket()` + `connect(addr, 5000 ms)`, `setSoTimeout(30_000)` cho việc chờ phản hồi, gửi `Hello(clientName)`, nhận `HelloOk` → trả `sessionId`; nhận `ErrorReply` → `ServerErrorException`; message khác → `ProtocolException`.
- `detect`: gửi `DetectRequest`, nhận `DetectResult` → trả `result()`; `ErrorReply` → `ServerErrorException(code, message)` (kết nối vẫn dùng được).
- `ping`: gửi `Ping`, chờ `Pong`; mọi lỗi → đánh dấu mất kết nối, trả `false`.
- `isConnected`, `close` (gửi `Bye` nếu còn kết nối, đóng socket, gọi nhiều lần không lỗi).
- Mọi method public `synchronized` (một yêu cầu, một phản hồi). Bất kỳ `IOException` nào → đóng socket, `isConnected()` = false, ném tiếp.

### Giao diện (Swing viết tay, BorderLayout/GridBagLayout; không cần file `.form`)
| Thành phần | Hành vi |
|---|---|
| `ClientMain` | đặt Look&Feel hệ thống, `SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true))` |
| Thanh kết nối (trên) | ô Host (mặc định `localhost`), Port (`Protocol.DEFAULT_PORT`), Tên (mặc định tên máy), nút Kết nối/Ngắt, nhãn trạng thái màu (xám "Chưa kết nối", xanh "Đã kết nối (phiên #N)", đỏ "Mất kết nối") |
| Thanh thao tác | nút Chọn ảnh, Mô phỏng chụp ảnh, Nhận dạng (chỉ bật khi đã kết nối và có ảnh), Xuất ảnh, Xuất CSV (chỉ bật khi có kết quả) |
| Tab "Nhận dạng" | `ImagePanel` (giữa) hiển thị ảnh hoặc ảnh đã vẽ box, co giãn giữ tỉ lệ; JTable kết quả bên phải (STT, Nhãn, Độ tin cậy %) |
| Tab "Lịch sử" | `new HistoryPanel(historyStore)` với `historyStore = new HistoryStore(HistoryStore.defaultRoot())` |
| Thanh trạng thái (dưới) | thông báo cuối: "3 đối tượng, 420 ms", lỗi... |

- `ImageFiles.validate(File) → Optional<String>` (lớp tiện ích thuần, có test): đuôi `.jpg/.jpeg/.png` (không phân biệt hoa thường), dung lượng ≤ `Protocol.MAX_IMAGE_BYTES`, `ImageIO.read` ≠ null; trả thông báo lỗi tiếng Việt nếu sai.
- Mô phỏng chụp ảnh: chọn ngẫu nhiên một ảnh trong `client/src/main/resources/samples/` (đọc qua classpath, danh sách tên ghi trong `samples/index.txt`).
- Mọi lời gọi mạng chạy trong `SwingWorker`; GUI không bao giờ đơ. Trong lúc nhận dạng, khóa nút Nhận dạng và hiện "Đang nhận dạng...".
- `javax.swing.Timer` mỗi `Protocol.PING_INTERVAL_MS`: gọi `ping()` trong SwingWorker; `false` → trạng thái đỏ "Mất kết nối", khóa Nhận dạng, dừng timer.
- Sau khi nhận dạng thành công: `annotated = BoxRenderer.render(ảnh, kết quả)` → hiển thị → gọi `historyStore.save(tên, annotated, kết quả)` (ngoài EDT) rồi `historyPanel.refresh()` (trên EDT).
- Xuất ảnh / CSV: `JFileChooser` lưu file, gọi `ResultExporter.exportImage` / `exportCsv`.
- **Khi PLAN-D chưa xong**, các method stub ném `UnsupportedOperationException`: bắt riêng ngoại lệ này và hiện ở thanh trạng thái "Chức năng lịch sử/xuất chưa sẵn sàng", không crash, không hộp thoại lỗi.
- `ServerErrorException` → hộp thoại cảnh báo với thông báo của server (ví dụ "Không đọc được ảnh", "Ảnh quá lớn"); `IOException` → trạng thái đỏ "Mất kết nối".
- Khi `connect` nhận `ServerErrorException` với `ErrorCode.SERVER_BUSY` (server đã đủ số client tối đa): trạng thái "Server đang bận, thử lại sau", về trạng thái chưa kết nối.
- Jar chạy độc lập cho người dùng: `.\mvnw.cmd -q -pl client package` tạo `client/target/client-1.0-SNAPSHOT-all.jar` (maven-shade đã cấu hình trong `client/pom.xml`, giữ nguyên `main.class` và plugin shade khi sửa pom).

## Các bước (TDD ở seam `ServerConnection` và `ImageFiles`, mỗi bước có verify)
Test `ServerConnection` dùng **stub server trong test** (`ServerSocket(0)` + một luồng đọc/ghi bằng `ProtocolCodec` theo kịch bản), nên không phải chờ PLAN-A.
1. Test (red) → cài (green): `connect` gửi `Hello` đúng tên và trả session id từ `HelloOk` → verify: `.\mvnw.cmd -q -pl client test` xanh.
2. `detect` trả đúng `DetectionResult`; `ErrorReply(BAD_IMAGE)` → `ServerErrorException` với đúng `code()`, và `detect` tiếp theo trên cùng kết nối vẫn thành công → verify: xanh.
3. Stub server trả `ErrorReply(SERVER_BUSY)` thay cho `HelloOk` → `connect` ném `ServerErrorException` với `code() == SERVER_BUSY`, `isConnected()` false. Stub server đóng socket → `ping()` trả `false`, `isConnected()` false; `close()` gửi `Bye` (stub nhận được) và gọi hai lần không lỗi; kết nối tới port không ai nghe → `IOException` → verify: xanh.
4. `ImageFiles.validate`: file `.gif` → lỗi; file > 10 MB (tạo file thưa bằng `RandomAccessFile.setLength`) → lỗi; PNG hợp lệ → rỗng; file `.jpg` nhưng nội dung rác → lỗi → verify: xanh.
5. Ảnh mẫu: tải 3–5 ảnh có người/xe/động vật, giấy phép tự do (ví dụ `https://resources.djl.ai/images/dog_bike_car.jpg` và ảnh Public Domain/CC0 từ Wikimedia Commons), mỗi ảnh < 1 MB, vào `samples/` + `index.txt`; ghi nguồn ảnh trong `samples/SOURCES.txt` → verify: các file mở được.
6. `MainFrame`, `ImagePanel`, `ClientMain` → verify thủ công: `.\mvnw.cmd -q -pl client exec:java` mở cửa sổ; Chọn ảnh hiện ảnh; chọn file `.gif` hoặc > 10 MB bị từ chối bằng hộp thoại; Kết nối tới port không có server → trạng thái đỏ + thông báo, không đơ.
7. Khi PLAN-A đã xong (xem bảng Trạng thái trong `COORDINATION.md`; nếu chưa thì bỏ qua bước này và ghi chú): chạy server fake (`.\mvnw.cmd -q -pl server exec:java` ở một terminal khác), kết nối, nhận dạng → thấy 2 box giả (person, dog); tắt server → ≤ 15 s trạng thái đỏ.

## Ngoài phạm vi
Không làm `HistoryStore`, `HistoryPanel`, `ResultExporter` (PLAN-D), không sửa `BoxRenderer`, không webcam thật (chỉ "mô phỏng chụp ảnh" bằng ảnh mẫu), không tự động kết nối lại.

## Kết quả mong đợi khi chạy đúng
- `.\mvnw.cmd -q -pl client test` xanh.
- Mở client: cửa sổ "Nhận dạng hình ảnh - Client" với thanh kết nối, các nút, tab Nhận dạng / Lịch sử.
- Nhập host/port, bấm Kết nối → nhãn xanh "Đã kết nối (phiên #1)", nút đổi thành Ngắt.
- Chọn ảnh → ảnh hiện trong khung, co giãn theo cửa sổ. Chọn file 12 MB hoặc `.gif` → hộp thoại "Ảnh phải là JPG/PNG và không quá 10 MB", không gửi.
- Mô phỏng chụp ảnh → một ảnh mẫu ngẫu nhiên hiện ra.
- Bấm Nhận dạng → GUI vẫn kéo/di chuyển được trong lúc chờ; sau đó ảnh có khung màu quanh từng đối tượng kèm `nhãn xx%`, bảng liệt kê đúng các đối tượng, thanh trạng thái "3 đối tượng, 420 ms".
- Chọn file đổi đuôi `.jpg` nhưng không phải ảnh → bị chặn ngay ở client. (Lỗi BAD_IMAGE từ server vẫn được xử lý: hộp thoại lỗi, kết nối dùng tiếp; trường hợp này được kiểm bằng test ở bước 2.)
- Tắt server → trong ≤ 15 s trạng thái đỏ "Mất kết nối", nút Nhận dạng bị khóa. Bấm Ngắt → server xóa phiên ngay.
- `java -jar client\target\client-1.0-SNAPSHOT-all.jar` mở được client trên máy chỉ có JDK/JRE 17+, không cần mã nguồn.
- Trước khi PLAN-D xong: bấm Xuất / tab Lịch sử không làm crash, chỉ báo "chưa sẵn sàng". Sau khi D xong: hoạt động mà không phải sửa code C.

## Khi xong
Cập nhật dòng C trong bảng "Trạng thái" của `COORDINATION.md` (Done + lệnh chạy client + bước 7 đã/ chưa kiểm với server thật).
