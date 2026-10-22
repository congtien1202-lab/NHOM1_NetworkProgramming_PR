# PLAN-D: Lịch sử, xuất kết quả, tài liệu, triển khai

> Cửa sổ này chỉ làm PLAN-D. Đọc `COORDINATION.md` trước (quy tắc, sở hữu file, hợp đồng). Ba cửa sổ khác đang làm A, B, C cùng lúc trong cùng thư mục.

## Bối cảnh
Đồ án IX.4 "Hệ thống Nhận dạng Hình ảnh Phân tán" (môn Lập trình mạng, Java 17, NetBeans/Maven, nhóm 01: Trần Công Tiến, Hồ Ngọc Linh, Nguyễn Văn An, Vương Thị Mỹ Hoa; 4 tuần). Phiếu đăng ký: `MoTaDoAn.md`. Client (PLAN-C) gửi ảnh tới Server (PLAN-A) qua giao thức TCP tùy chỉnh; Server nhận dạng bằng DJL SSD-MobileNet (PLAN-B) và trả nhãn + độ tin cậy + bounding box.

PLAN-D làm hai phần độc lập với ba plan kia:
1. **Code Client**: `HistoryStore`, `HistoryPanel`, `ResultExporter` (stub đã có, PLAN-C đã/đang gắn vào giao diện).
2. **Tài liệu**: GLOSSARY, SPEC, ADR-0001, tickets, hướng dẫn triển khai, khung báo cáo, kịch bản demo.

## Sở hữu file
- Tạo/sửa: `client/src/main/java/com/nhom01/imagerec/client/history/**`, `client/src/main/java/.../client/export/**` (**giữ chữ ký public hiện có**, được thêm method/lớp mới), `client/src/test/java/.../client/history/**`, `client/src/test/java/.../client/export/**`, `GLOSSARY.md`, `SPEC.md`, `docs/adr/0001-*.md`, `docs/deploy.md`, `docs/bao-cao/**`, `.scratch/**`.
- Chỉ đọc: mọi thứ khác (đặc biệt `common/**`, `client/.../render/BoxRenderer.java`, `client/.../ui/**` của C).
- Build/test: luôn thêm `"-Dbuild.dir=target-d"` (PLAN-C dùng chung module `client`): `.\mvnw.cmd -q -pl client test "-Dbuild.dir=target-d"`. Không `-am`, không `clean` ở gốc, không commit.

## Phần 1: Code (TDD, mỗi bước có verify)

### Định dạng lưu trữ
- CSV kết quả (dùng cho cả xuất file và lịch sử): UTF-8 **có BOM** (Excel đọc đúng tiếng Việt), dòng tiêu đề `label,confidence,x,y,width,height`, mỗi đối tượng một dòng. **Số luôn ghi bằng `Locale.ROOT`** (máy nhóm dùng locale `vi_VN`, dấu thập phân là dấu phẩy sẽ phá CSV). Nhãn chứa dấu phẩy/ngoặc kép thì đặt trong ngoặc kép theo chuẩn CSV.
- Thư mục lịch sử (mặc định `HistoryStore.defaultRoot()` = `~/.image-recognition/history/`), mỗi lần nhận dạng có id `yyyyMMdd-HHmmss-SSS` (thêm hậu tố `-2`, `-3` nếu trùng):
  - `<id>.png`: ảnh đã vẽ box; `<id>.csv`: kết quả;
  - `index.tsv`: mỗi dòng `id<TAB>epochMillis<TAB>processingMillis<TAB>tên file gốc` (tên file đã thay TAB/xuống dòng bằng dấu cách).
- `list()`: đọc `index.tsv`, mới nhất trước; dòng hỏng hoặc thiếu file `.png`/`.csv` thì bỏ qua (không ném). Thư mục chưa có → danh sách rỗng.
- `save` và `list` `synchronized` (C gọi `save` từ luồng nền).

### Bước
1. **`ResultExporter`** (test red → green) `ResultExporterTest` với `@TempDir`:
   - `exportCsv` 2 đối tượng → file bắt đầu bằng BOM, 3 dòng, số dùng dấu chấm (chạy test với `Locale.setDefault(new Locale("vi", "VN"))` để chứng minh);
   - `exportImage` → `ImageIO.read` đọc lại được, đúng kích thước;
   - thêm `public static DetectionResult readCsv(Path, long processingMillis)` (method mới, cho lịch sử dùng lại): `readCsv(exportCsv(x)) == x`.
   → verify: `.\mvnw.cmd -q -pl client test "-Dbuild.dir=target-d"` xanh.
2. **`HistoryStore`** `HistoryStoreTest` với `@TempDir`:
   - thư mục rỗng → `list()` rỗng;
   - lưu 3 mục → `list()` trả 3 mục, mới nhất trước, đúng tên file, đúng số đối tượng, file `annotatedImage` tồn tại;
   - tạo `HistoryStore` mới trên cùng thư mục (giả lập mở lại app) → vẫn đọc được 3 mục;
   - một dòng rác trong `index.tsv` → bị bỏ qua, các mục khác vẫn đọc được;
   - 2 lần lưu trong cùng mili giây → 2 mục khác id.
   → verify: xanh.
3. **`HistoryPanel`** (không unit test GUI): trái là `JList<HistoryEntry>` hiển thị `HH:mm:ss dd/MM/yyyy - cat.jpg (3 đối tượng)`; phải là ảnh (`ImageIcon` co giãn) + `JTable` (Nhãn, Độ tin cậy %) của mục đang chọn; nút "Làm mới". `refresh()` (gọi trên EDT) nạp lại danh sách, giữ lựa chọn nếu còn. Lỗi đọc → hiện thông báo trong panel, không ném.
   → verify thủ công: viết `HistoryPanelDemo` (có `main`) trong `client/src/test/java/.../client/history/`, tạo vài mục trong thư mục tạm rồi mở panel trong một JFrame; chạy bằng NetBeans (Run File); xem danh sách, chọn mục thấy ảnh và bảng.

## Phần 2: Tài liệu (mỗi file có verify: tồn tại, đúng template, không mâu thuẫn với code/hợp đồng)
Đọc các file skill trước khi viết:
`Support/skills-main/skills/engineering/domain-modeling/SKILL.md`, `GLOSSARY-FORMAT.md`, `ADR-FORMAT.md`; `Support/skills-main/skills/engineering/to-spec/SKILL.md`; `Support/skills-main/skills/engineering/to-tickets/SKILL.md`.

4. **`GLOSSARY.md`** (theo GLOSSARY-FORMAT, không chứa chi tiết cài đặt): Client, Server, Phiên (Session), Message, Lệnh (Command), Yêu cầu nhận dạng, Kết quả nhận dạng, Đối tượng phát hiện (Detection), Bounding box (tọa độ tỉ lệ 0..1), Độ tin cậy, Heartbeat (PING/PONG), Mục lịch sử, Ảnh đã chú thích (annotated).
5. **`docs/adr/0001-giao-thuc-nhi-phan-tuy-chinh-tren-tcp.md`**: vì sao khung `[command][length][body]` nhị phân (không JSON, không HTTP), giới hạn 10 MB, một yêu cầu một phản hồi, heartbeat do client gửi + timeout 60 s ở server. (ADR-0002 về engine DJL là của PLAN-B.)
6. **`SPEC.md`** theo template to-spec: Problem Statement, Solution, **User Stories dài** (người dùng Client, người vận hành Server, giảng viên chấm demo), Implementation Decisions (module, giao thức, đa luồng, seam `ObjectDetector`), Testing Decisions (các seam đang được test), Out of Scope, Further Notes. Không ghi đường dẫn file hay code.
7. **Tickets** `.scratch/nhan-dang-anh/issues/01-…md` → `11-…md` theo template local của to-tickets, nội dung khớp bảng:

   | # | Ticket | Blocked by | Plan |
   |---|---|---|---|
   | 01 | Khung Maven + giao thức + stub hợp đồng | — | Phase 0 (Done) |
   | 02 | Spike DJL, chọn engine, ADR-0002 | 01 | B |
   | 03 | Tracer bullet server 1 client với detector giả | 01 | A |
   | 04 | Đa luồng, quản lý phiên, log, cửa sổ server, chế độ không GUI | 03 | A |
   | 05 | DjlObjectDetector thread-safe | 02 | B |
   | 06 | Client: kết nối, chọn/mô phỏng ảnh, giới hạn 10 MB, nhận dạng, vẽ box | 01 | C |
   | 07 | Heartbeat, timeout, hiển thị lỗi server | 04, 06 | A + C |
   | 08 | Xuất kết quả PNG + CSV | 01 | D |
   | 09 | Lịch sử nhận dạng | 08 | D |
   | 10 | Triển khai Internet | 05, 07 | D (+ B tải mô hình) |
   | 11 | Kiểm thử nhiều client, báo cáo, demo | tất cả | cả nhóm |
8. **`docs/deploy.md`**:
   - Build: `.\mvnw.cmd -q package` (hoặc `./mvnw -q package` trên Linux) tạo 2 jar chạy độc lập (maven-shade đã cấu hình): `server/target/server-1.0-SNAPSHOT-all.jar` và `client/target/client-1.0-SNAPSHOT-all.jar`. Chạy server: `java -jar server-1.0-SNAPSHOT-all.jar --gui=false --detector=djl --port=5000 --maxClients=20`. Phát cho người dùng: chỉ cần file jar client + JDK/JRE 17+.
   - **Demo trong LAN / máy Windows**: lần đầu chạy server, Windows Defender Firewall hỏi quyền cho `java.exe`: chọn cho phép mạng Private (hoặc thêm rule inbound TCP 5000); nếu không, client ở máy khác không kết nối được dù cùng mạng. Client dùng IP LAN của máy server (`ipconfig`).
   - **Phương án khuyến nghị: VPS** (≥ 2 GB RAM, Ubuntu, Java 17+): cài JDK, mở port 5000 (ufw + firewall nhà cung cấp), tạo bản ghi DNS A `<tên miền>` → IP tĩnh của VPS, chạy bằng `systemd` (unit file mẫu, `Restart=always`), tải sẵn mô hình (theo ADR-0002 của B), xem log `logs/server.log`.
   - **Phương án dự phòng**: chạy trên máy cá nhân + ngrok TCP / playit.gg (địa chỉ + port do dịch vụ cấp, có thể đổi mỗi lần).
   - Kiểm tra: từ mạng khác (4G) `Test-NetConnection <tên miền> -Port 5000`, rồi client kết nối bằng tên miền.
   - Nhóm chốt phương án cuối tuần 3.
9. **`docs/bao-cao/`**: `dan-y-bao-cao.md` (dàn ý báo cáo: giới thiệu, cơ sở lý thuyết TCP/Socket/đa luồng/giao thức tùy chỉnh/SSD-MobileNet, thiết kế, cài đặt, kiểm thử, triển khai, kết luận); `kich-ban-demo.md` (kịch bản bên dưới); `de-xuat-sua-phieu.md` (đề xuất thay câu mục 5 "Nhận dạng hình ảnh: Viết ra các chỉ số đã đọc được trong hình ảnh" bằng câu đúng với object detection, ví dụ: "Nhận dạng hình ảnh: phát hiện các đối tượng trong ảnh bằng mô hình SSD-MobileNet, trả về tên đối tượng, độ tin cậy và vị trí (bounding box)").

### Kịch bản demo đầu-cuối (đưa vào `kich-ban-demo.md`)
1. Khởi động server `--detector=djl` → cửa sổ Server hiện, log mô hình đã nạp, đang lắng nghe port 5000.
2. Mở 3 client trên 2 máy, kết nối → cửa sổ Server hiện 3 dòng client với IP đúng.
3. Ba client gửi ảnh gần như cùng lúc → mỗi client nhận đúng kết quả ảnh của mình, có box quanh người/xe/vật; log ghi 3 yêu cầu.
4. Chọn file > 10 MB hoặc không phải JPG/PNG → bị chặn ở client.
5. Xuất PNG và CSV, mở bằng trình xem ảnh và Excel; tab Lịch sử còn đủ mục sau khi khởi động lại client.
6. Rút mạng một client → ≤ 60 s server xóa phiên; tắt server → client báo "Mất kết nối" trong ≤ 15 s.
7. (Sau triển khai) client trên mạng 4G kết nối bằng tên miền + port và nhận dạng thành công.

## Ngoài phạm vi
Không sửa giao diện chính của C (chỉ cung cấp panel/lớp được gắn vào), không sửa server, không tự triển khai lên VPS thật (chỉ viết hướng dẫn; việc thuê VPS/tên miền là quyết định của nhóm).

## Kết quả mong đợi khi chạy đúng
- `.\mvnw.cmd -q -pl client test "-Dbuild.dir=target-d"` xanh, gồm test history/export.
- Trong client (sau khi C gắn): mỗi lần nhận dạng thành công, tab Lịch sử có thêm một dòng ở đầu; đóng và mở lại client vẫn còn; bấm vào dòng hiện lại đúng ảnh có box và bảng kết quả.
- Xuất ảnh → PNG có box, mở bằng trình xem ảnh; Xuất CSV → mở bằng Excel đúng 6 cột, số dạng `0.92`, nhãn không lỗi font.
- Thư mục `~/.image-recognition/history/` chứa các cặp `<id>.png` / `<id>.csv` và `index.tsv`.
- Một người mới chỉ đọc `docs/deploy.md` là dựng được server chạy liên tục trên VPS và kết nối được từ Internet.
- `GLOSSARY.md`, `SPEC.md`, ADR-0001, 11 ticket nhất quán với `COORDINATION.md` và code hiện có.

## Khi xong
Cập nhật dòng D trong bảng "Trạng thái" của `COORDINATION.md` (Done + danh sách tài liệu đã viết + quyết định triển khai còn chờ nhóm chốt).
