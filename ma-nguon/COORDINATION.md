# Điều phối 4 cửa sổ làm song song

Đồ án IX.4 (Nhận dạng hình ảnh phân tán, Java Socket TCP + DJL SSD-MobileNet) được chia thành 4 plan chạy song song.
Mỗi cửa sổ Claude Code mở tại thư mục này và nhận lệnh: **"đọc PLAN-X.md và thực hiện"**.

| Plan | Phạm vi | Lệnh build/test của plan |
|---|---|---|
| [PLAN-A](PLAN-A.md) | Server mạng & đa luồng | `.\mvnw.cmd -q -pl server test` |
| [PLAN-B](PLAN-B.md) | Nhận dạng AI bằng DJL | `.\mvnw.cmd -q -pl server test "-Dbuild.dir=target-b"` |
| [PLAN-C](PLAN-C.md) | Client mạng & giao diện chính | `.\mvnw.cmd -q -pl client test` |
| [PLAN-D](PLAN-D.md) | Lịch sử, xuất file, tài liệu, triển khai | `.\mvnw.cmd -q -pl client test "-Dbuild.dir=target-d"` |

`-Dbuild.dir` cho B và D build ra thư mục riêng, vì A/B cùng module `server` và C/D cùng module `client`.

## Quy tắc chung (bắt buộc)
1. **Chỉ tạo/sửa file thuộc quyền sở hữu** của plan mình (bảng dưới). Muốn đổi file của plan khác hoặc hợp đồng chung → ghi vào mục "Yêu cầu thay đổi hợp đồng" bên dưới, rồi tiếp tục bằng cách khác; không tự sửa.
2. **`common/`, `pom.xml` gốc, `mvnw*`, `.mvn/` bị đóng băng.** Chữ ký public của các lớp hợp đồng (mục dưới) không được đổi.
3. **Không chạy `git commit`/`git add`** (4 cửa sổ cùng repo sẽ tranh `index.lock`). Cửa sổ điều phối commit ở các mốc.
4. Không dùng `-am`, không chạy `install` hay `clean` ở gốc (sẽ xóa/ghi đè build của cửa sổ khác).
5. Làm theo karpathy-guidelines: tối giản, không thêm tính năng ngoài plan, mỗi bước có `verify`. TDD tại các seam ghi trong plan (red → green).
6. Code theo java-coding-standards: record bất biến, tên rõ, không nuốt exception, hằng số thay số ma thuật. Java 17 (`--release 17`, không dùng pattern matching cho switch).
7. Xong plan: cập nhật dòng của mình ở bảng "Trạng thái".

## Sở hữu file

| Plan | Được tạo/sửa |
|---|---|
| A | `server/src/main/java/com/nhom01/imagerec/server/*.java` (package `server`, không gồm `server/detect/`), `server/src/main/java/.../server/detect/FakeObjectDetector.java`, `server/src/main/resources/server.properties`, `server/src/test/**` trừ file test DJL |
| B | `server/pom.xml`, `server/src/main/java/.../server/detect/DjlObjectDetector.java` (giữ nguyên chữ ký public), file mới trong `server/src/main/java/.../server/detect/djl/**` nếu cần, `server/src/test/java/.../server/detect/DjlObjectDetectorTest.java`, `server/src/test/resources/djl/**`, `docs/adr/0002-*.md`, `scripts/download-model.*` |
| C | `client/pom.xml`, `client/src/main/java/.../client/net/**` (giữ chữ ký public `ServerConnection`, `ServerErrorException`), `client/src/main/java/.../client/ui/**`, `client/src/main/resources/samples/**`, `client/src/test/java/.../client/net/**`, `client/src/test/java/.../client/ui/**` |
| D | `client/src/main/java/.../client/history/**`, `client/src/main/java/.../client/export/**` (giữ chữ ký public), `client/src/test/java/.../client/history/**`, `client/src/test/java/.../client/export/**`, `GLOSSARY.md`, `SPEC.md`, `docs/adr/0001-*.md`, `docs/deploy.md`, `docs/bao-cao/**`, `.scratch/**` |
| Điều phối | mọi file còn lại: `common/**`, `client/.../render/**`, `server/.../detect/ObjectDetector.java`, `DetectionException.java`, pom gốc, file này |

## Hợp đồng chung (đã có trong code, đóng băng)
- `common`: `Message` (sealed interface: `Hello`, `HelloOk`, `DetectRequest`, `DetectResult`, `ErrorReply`, `Ping`, `Pong`, `Bye`), `ProtocolCodec.read/write`, `Command`, `ErrorCode` (`BAD_IMAGE`, `IMAGE_TOO_LARGE`, `NOT_GREETED`, `DETECTION_FAILED`, `BAD_MESSAGE`, `SERVER_BUSY`), `Detection`, `DetectionResult`, `ProtocolException`, `Protocol` (hằng số: `DEFAULT_PORT`, `MAX_IMAGE_BYTES`, `MAX_BODY_BYTES`, `MAX_IMAGE_PIXELS`, `PING_INTERVAL_MS`, `READ_TIMEOUT_MS`). 20 test xanh.
- Đóng gói: pom gốc cấu hình `maven-shade-plugin`; `server/pom.xml` và `client/pom.xml` khai báo `main.class` + plugin shade. `package` tạo thêm `*-all.jar` chạy bằng `java -jar`. Ai sửa hai pom này phải giữ nguyên phần đó.
- `server.detect.ObjectDetector`: `List<Detection> detect(BufferedImage) throws DetectionException`, `close()`. **Implementation phải thread-safe.** `FakeObjectDetector` (2 đối tượng cố định) đã có.
- `client.net.ServerConnection`: `connect(host, port, name) → int`, `detect(fileName, bytes) → DetectionResult`, `ping() → boolean`, `isConnected()`, `close()`. `ServerErrorException(ErrorCode, message)`.
- `client.render.BoxRenderer.render(BufferedImage, DetectionResult) → BufferedImage` (đã cài, có test).
- `client.history`: `HistoryEntry` record, `HistoryStore(Path)` với `save(...)`, `list()`, `defaultRoot()`; `HistoryPanel(HistoryStore)` với `refresh()`.
- `client.export.ResultExporter.exportImage(BufferedImage, File)`, `exportCsv(DetectionResult, File)`.

Stub ném `UnsupportedOperationException("PLAN-X")` cho tới khi plan sở hữu cài đặt (riêng `HistoryPanel` stub hiển thị nhãn "chưa cài đặt").

## Trạng thái

| Plan | Trạng thái | Ghi chú |
|---|---|---|
| Phase 0 | Done | Khung Maven, `common` + test, stub hợp đồng, `BoxRenderer`, Maven Wrapper |
| A | Done | 17 test xanh (`ImageRecognitionServerTest` 10, `ServerConfigTest` 4, `SessionRegistryTest` 3, ~2,4 s). Chạy: `.\mvnw.cmd -q -pl server exec:java` (GUI, port 5000) hoặc `"-Dexec.args=--gui=false --port=5000 --detector=fake"`; jar: `.\mvnw.cmd -q -pl server package` rồi `java -jar server\target\server-1.0-SNAPSHOT-all.jar --gui=false`. Tham số: `--port --detector=fake\|djl --gui --maxClients`. Log ở `logs/server.log` (nên thêm `logs/` vào .gitignore). Đã thử jar headless với fake và djl: lắng nghe + nhận kết nối OK; cửa sổ GUI mở không lỗi nhưng chưa kiểm bằng mắt. |
| B | Done | MXNet engine 0.38.0 + `ssd_512_mobilenet1.0_voc` (20 lớp VOC, ngưỡng 0.3). Test slow: `.\mvnw.cmd -q -pl server test "-Dbuild.dir=target-b" "-Dgroups=slow" "-DexcludedGroups=none"`. Jar gộp `-all.jar` chạy được; xem ADR-0002 |
| C | Done | `ServerConnection` (9 test stub-server) + `ImageFiles` (6 test) xanh; `MainFrame`/`ImagePanel`/`SampleImages`/`ClientMain`; 4 ảnh mẫu + `SOURCES.txt`. Chạy: `.\mvnw.cmd -q -pl client exec:java` hoặc `java -jar client\target\client-1.0-SNAPSHOT-all.jar` (jar mở được, không crash). Bước 7 (server thật) CHƯA kiểm vì A chưa Done; GUI chưa thử thủ công với server fake. Lịch sử/xuất: bắt `UnsupportedOperationException` nên chạy tiếp được khi D xong. |
| D | Done | Code: `ResultExporter` (+`readCsv`), `HistoryStore`, `HistoryPanel`, `HistoryPanelDemo` (test: `ResultExporterTest` 5, `HistoryStoreTest` 6, xanh). Tài liệu: `GLOSSARY.md`, `SPEC.md`, `docs/adr/0001-*`, `docs/deploy.md`, `docs/bao-cao/{dan-y-bao-cao,kich-ban-demo,de-xuat-sua-phieu}.md`, `.scratch/nhan-dang-anh/issues/01-11`. Chờ nhóm chốt: VPS hay ngrok/playit (cuối tuần 3), thuê VPS + tên miền, có sửa phiếu đăng ký hay không. `deploy.md` tham chiếu ADR-0002 của B để tải sẵn mô hình. C cần gọi `HistoryStore.save` sau mỗi lần nhận dạng rồi `HistoryPanel.refresh()` trên EDT. |

## Yêu cầu thay đổi hợp đồng
(Plan nào cần đổi file không thuộc quyền mình thì ghi ở đây: plan, file, lý do, đề xuất.)

- (trống)

## Hộp thư giữa các cửa sổ
Các cửa sổ không nhìn thấy nhau, nên trao đổi bằng cách **thêm một dòng vào cuối bảng này** (chỉ thêm, không sửa/xóa dòng của người khác). Mỗi cửa sổ đọc lại bảng này **trước mỗi bước lớn** và ngay trước khi báo xong.

Dòng mẫu: `| 2026-10-11 14:05 | B | A | Detector cần ảnh RGB, ClientHandler đừng truyền ảnh TYPE_CUSTOM | mở |`
Loại tin: thông báo "xong, dùng được rồi" (ví dụ A báo port và lệnh chạy server cho C thử), hỏi đáp về hợp đồng, báo lỗi phát hiện ở phần của người khác. Người nhận xử lý xong thì đổi `mở` thành `xong`.

| Giờ | Từ | Đến | Nội dung | Trạng thái |
|---|---|---|---|---|
| 2026-10-11 13:40 | B | A | DjlObjectDetector xong: `--detector=djl` dùng được, không cần sửa code; lần đầu tải mô hình, nạp lần sau <1 s. Ảnh đầu vào bất kỳ, tọa độ đã chuẩn hóa 0..1 | xong |
| 2026-10-11 13:50 | A | C | Server xong, C có thể thử với server thật: `java -jar server\target\server-1.0-SNAPSHOT-all.jar --gui=false --port=5000` (hoặc `.\mvnw.cmd -q -pl server exec:java`). Mặc định detector=fake; đủ `maxClients` thì nhận ERROR SERVER_BUSY rồi bị đóng | mở |

## Mốc hội tụ (cửa sổ điều phối)
1. A + C xong → server `detector=fake` + 2 client GUI trên cùng máy. Commit.
2. B xong → `detector=djl`, kiểm tra bounding box thật. Commit.
3. D xong → Xuất / Lịch sử hoạt động trong GUI. Commit.
4. Kiểm thử tổng theo kịch bản demo trong `PLAN-D.md` và triển khai theo `docs/deploy.md`.
