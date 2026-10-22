# PLAN-A: Server mạng & đa luồng

> Cửa sổ này chỉ làm PLAN-A. Đọc `COORDINATION.md` trước (quy tắc, sở hữu file, hợp đồng). Ba cửa sổ khác đang làm B, C, D cùng lúc trong cùng thư mục.

## Bối cảnh
Đồ án IX.4 "Hệ thống Nhận dạng Hình ảnh Phân tán" (môn Lập trình mạng, Java, NetBeans). Client gửi ảnh JPG/PNG (≤ 10 MB) qua TCP Socket, Server đa luồng nhận dạng đối tượng (DJL SSD-MobileNet, do PLAN-B làm) và trả kết quả (nhãn, độ tin cậy, bounding box). Giao thức tùy chỉnh `[command 1B][length 4B][body]` đã cài xong trong module `common` (`ProtocolCodec`, `Message`...).

PLAN-A xây **phần mạng của Server**: lắng nghe, đa luồng, bắt tay, xử lý yêu cầu, lỗi, heartbeat, quản lý phiên, ghi log, cửa sổ Server. Nhận dạng đi qua interface `ObjectDetector`; trong lúc B chưa xong, dùng `FakeObjectDetector`.

## Sở hữu file
- Tạo/sửa: `server/src/main/java/com/nhom01/imagerec/server/*.java` (package `com.nhom01.imagerec.server`), `server/src/main/java/.../server/detect/FakeObjectDetector.java`, `server/src/main/resources/server.properties`, `server/src/test/**` (trừ test DJL của B).
- Chỉ đọc: `common/**`, `ObjectDetector`, `DetectionException`, `DjlObjectDetector` (B), `server/pom.xml` (B).
- Build/test: `.\mvnw.cmd -q -pl server test` (không `-am`, không `clean` ở gốc, không commit).

## Thiết kế
| Lớp | Vai trò |
|---|---|
| `ServerConfig` (record) | `port`, `detector` (`fake`/`djl`), `gui` (true/false), `maxClients` (mặc định 20). Đọc `server.properties` trong classpath, ghi đè bằng tham số `--port=5000 --detector=djl --gui=false --maxClients=20`. |
| `ImageRecognitionServer` | **Seam chính để test.** `ImageRecognitionServer(int port, ObjectDetector detector, SessionRegistry registry, int readTimeoutMs, int maxClients)`, `start()` (mở `ServerSocket`, chạy vòng accept trên luồng riêng), `port()` (port thật, hỗ trợ port 0), `stop()`. Mỗi socket giao cho `ExecutorService` **giới hạn** `maxClients` luồng chạy `ClientHandler`. Khi đã đủ `maxClients` phiên: gửi ngay `ErrorReply(SERVER_BUSY, ...)` rồi đóng socket mới (không xếp hàng). Lý do: mỗi yêu cầu chiếm ~20 MB RAM, VPS 2 GB không chịu được số luồng không giới hạn. |
| `ClientHandler` (Runnable) | Một luồng / một client. `socket.setSoTimeout(readTimeoutMs)`; vòng `ProtocolCodec.read` → xử lý → `ProtocolCodec.write`. |
| `SessionRegistry` | `ConcurrentHashMap<Integer, SessionInfo>`, id tăng dần (`AtomicInteger`), `register`, `recordImage(id)`, `remove(id)`, `snapshot()`, `addListener(Consumer<List<SessionInfo>>)`. |
| `SessionInfo` (record) | `id, clientName, remoteAddress, connectedAt, imagesProcessed`. Bất biến; registry thay bản mới khi tăng số ảnh. |
| `ServerLogging` | Cấu hình `java.util.logging`: console + `logs/server.log` (tạo thư mục nếu thiếu), định dạng 1 dòng `HH:mm:ss LEVEL message`. |
| `ServerWindow` (JFrame) | JTextArea log (một `Handler` đẩy log vào) + JTable client (ID, Tên, IP, Kết nối lúc, Số ảnh) cập nhật qua listener của registry. Listener và Handler được gọi từ luồng `ClientHandler`, nên **mọi cập nhật Swing (log và bảng) phải bọc trong `SwingUtilities.invokeLater`**. Viết Swing bằng tay (BorderLayout/JSplitPane), không cần file `.form`. |
| `ServerMain` | Đọc config → tạo detector (`new FakeObjectDetector()` hoặc `new DjlObjectDetector()`) → tạo registry → nếu `gui=true` mở `ServerWindow` → `start()` → shutdown hook gọi `stop()` và `detector.close()`. |

### Quy tắc xử lý trong `ClientHandler`
| Tình huống | Phản hồi | Kết nối |
|---|---|---|
| Message đầu tiên là `Hello` | `HelloOk(sessionId)`, đăng ký phiên, log `Client #1 (tên, IP) connected` | giữ |
| Message đầu tiên không phải `Hello` | `ErrorReply(NOT_GREETED, ...)` | đóng |
| `DetectRequest`, ảnh đọc được (`ImageIO.read` ≠ null) | `DetectResult(DetectionResult(ms, detections))`, tăng số ảnh, log `Client #1 detect cat.jpg: 2 objects in 3 ms` | giữ |
| `DetectRequest`, không phải ảnh | `ErrorReply(BAD_IMAGE, ...)` | giữ |
| `DetectRequest`, ảnh rộng × cao > `Protocol.MAX_IMAGE_PIXELS` | `ErrorReply(IMAGE_TOO_LARGE, ...)`. Đọc kích thước bằng `ImageIO.getImageReaders` + `reader.getWidth(0)/getHeight(0)` **trước** khi giải mã (chống "bom ảnh") | giữ |
| Kết nối mới khi đã đủ `maxClients` | `ErrorReply(SERVER_BUSY, ...)` | đóng ngay |
| `detector.detect` ném `DetectionException` hoặc RuntimeException | `ErrorReply(DETECTION_FAILED, ...)`, log WARNING kèm stack trace | giữ |
| `Ping` | `Pong` | giữ |
| `Bye` | không trả lời, log `Client #1 disconnected` | đóng |
| `Hello` lần hai, hoặc message chỉ dành cho S→C | `ErrorReply(BAD_MESSAGE, ...)` | giữ |
| `ProtocolException` khi đọc | cố gửi `ErrorReply(BAD_MESSAGE, ...)` | đóng |
| `SocketTimeoutException` | log `Client #1 timed out` | đóng |
| `EOFException` / `IOException` | log `Client #1 connection lost` | đóng |

Mọi đường đóng kết nối đều đi qua một `finally`: `registry.remove(id)`, đóng socket. `processingMillis` đo quanh lời gọi `detect`.

## Các bước (TDD, mỗi bước có verify)
Test nằm ở `server/src/test/java/com/nhom01/imagerec/server/ImageRecognitionServerTest.java`: khởi động `ImageRecognitionServer` trên port 0 với `FakeObjectDetector`, client là `Socket` thô + `ProtocolCodec`. Ảnh test tạo bằng `ImageIO.write(new BufferedImage(...), "png", bytes)`.

1. **Tracer bullet**: test "Hello → HelloOk; DetectRequest PNG → DetectResult chứa đúng `FakeObjectDetector.FIXED_DETECTIONS`" → cài `ImageRecognitionServer`, `ClientHandler`, `SessionRegistry`, `SessionInfo` tối thiểu → verify: test xanh.
2. **Lỗi**: test bytes không phải ảnh → `BAD_IMAGE` và kết nối vẫn detect tiếp được; PNG 8000×6000 (48 MP; tạo bằng `BufferedImage.TYPE_BYTE_BINARY` để test chỉ tốn ~6 MB heap, file nén rất nhỏ) → `IMAGE_TOO_LARGE`; với `maxClients = 2`, client thứ 3 nhận `SERVER_BUSY`; message đầu là `Ping` → `NOT_GREETED` rồi server đóng (đọc tiếp gặp EOF); detector ném lỗi (detector giả trong test) → `DETECTION_FAILED` → verify: xanh.
3. **Heartbeat & timeout**: test `Ping` → `Pong`; với `readTimeoutMs = 300`, client im lặng → server đóng kết nối và registry rỗng (chờ có hạn, poll tối đa 3 s, không `sleep` cố định dài) → verify: xanh.
4. **Đa luồng & phiên**: test 5 client song song (mỗi client một luồng, `CountDownLatch` để gửi cùng lúc), ai cũng nhận đúng kết quả; registry có 5 phiên với id khác nhau; mỗi client gửi `Bye` → registry rỗng; `recordImage` làm tăng `imagesProcessed` → verify: xanh.
5. **Cấu hình**: test `ServerConfig.parse(Properties, String[] args)`: mặc định port 5000/fake/gui=true; tham số ghi đè properties; giá trị sai (`--port=abc`, `--detector=xyz`) → `IllegalArgumentException` có thông báo rõ → verify: xanh.
6. **Log + ServerWindow + ServerMain** (không unit test GUI) → verify thủ công: `.\mvnw.cmd -q -pl server exec:java` (cần `common` đã install, đã có) mở cửa sổ, log `Server listening on port 5000`; mở PowerShell thứ hai chạy `Test-NetConnection localhost -Port 5000` → log có client kết nối rồi mất kết nối, bảng thêm rồi xóa dòng.
7. **Chế độ không GUI**: `.\mvnw.cmd -q -pl server exec:java "-Dexec.args=--gui=false"` → không có cửa sổ, log ra console và `logs/server.log`.
   Jar chạy độc lập: `.\mvnw.cmd -q -pl server package` tạo `server/target/server-1.0-SNAPSHOT-all.jar` (maven-shade đã cấu hình sẵn, Main-Class = `ServerMain`) → verify: `java -jar server\target\server-1.0-SNAPSHOT-all.jar --gui=false` chạy được.
8. **detector=djl**: `--detector=djl` khi B chưa xong → server dừng với thông báo rõ ràng (`PLAN-B: DjlObjectDetector chưa được cài đặt`), không treo. Khi B xong thì không phải sửa gì.

## Ngoài phạm vi
Không làm nhận dạng thật (PLAN-B), không làm Client (PLAN-C), không mã hóa TLS, không xác thực người dùng, không lưu ảnh trên server.

## Kết quả mong đợi khi chạy đúng
- `.\mvnw.cmd -q -pl server test` xanh, tổng thời gian test < 15 s.
- Chạy server (`detector=fake`): cửa sổ "Image Recognition Server" hiện, log `Server listening on port 5000`.
- Client kết nối → bảng thêm 1 dòng (ID, tên, IP, giờ, 0 ảnh), log `Client #1 (Linh-PC, /192.168.1.5) connected`.
- Client gửi ảnh → log `Client #1 detect cat.jpg: 2 objects in 3 ms`, cột "Số ảnh" tăng.
- Client gửi file không phải ảnh → client nhận lỗi BAD_IMAGE, phiên vẫn còn.
- Client tắt đột ngột → tối đa 60 s sau dòng biến mất, log `Client #1 timed out`. Client bấm Ngắt (gửi BYE) → biến mất ngay.
- Nhiều client gửi cùng lúc → mỗi client nhận đúng kết quả của mình, không client nào phải chờ client khác kết nối xong.
- `--gui=false` chạy được trên máy không màn hình (VPS).

## Khi xong
Cập nhật dòng A trong bảng "Trạng thái" của `COORDINATION.md` (Done + ghi chú lệnh chạy server). Nếu cần đổi hợp đồng, ghi vào mục "Yêu cầu thay đổi hợp đồng".
