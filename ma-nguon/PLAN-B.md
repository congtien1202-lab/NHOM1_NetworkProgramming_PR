# PLAN-B: Nhận dạng đối tượng bằng DJL + SSD-MobileNet

> Cửa sổ này chỉ làm PLAN-B. Đọc `COORDINATION.md` trước (quy tắc, sở hữu file, hợp đồng). Ba cửa sổ khác đang làm A, C, D cùng lúc trong cùng thư mục.

## Bối cảnh
Đồ án IX.4 "Hệ thống Nhận dạng Hình ảnh Phân tán" (môn Lập trình mạng, Java 17, NetBeans/Maven). Server đa luồng (PLAN-A) nhận ảnh từ Client qua TCP và gọi `ObjectDetector.detect(BufferedImage)`. Phiếu đăng ký đề tài cam kết: **Deep Java Library (DJL) + mô hình pre-trained SSD-MobileNet, object detection, không training**.

PLAN-B cài `DjlObjectDetector` thỏa hợp đồng `ObjectDetector`, không đụng tới phần mạng.

## Sở hữu file
- Tạo/sửa: `server/pom.xml` (thêm dependency DJL; **giữ nguyên** thuộc tính `main.class`, plugin `exec-maven-plugin` và `maven-shade-plugin` đã có: shade đóng gói jar chạy độc lập và giữ file đăng ký engine của DJL), `server/src/main/java/com/nhom01/imagerec/server/detect/DjlObjectDetector.java` (**giữ nguyên chữ ký public**: constructor không tham số, `detect`, `close`; được thêm `main`), lớp phụ mới trong `server/.../server/detect/djl/**` nếu cần, `server/src/test/java/com/nhom01/imagerec/server/detect/DjlObjectDetectorTest.java`, `server/src/test/resources/djl/**`, `docs/adr/0002-chon-engine-djl.md`, `scripts/download-model.*` (tùy chọn).
- Chỉ đọc: `common/**`, `ObjectDetector.java`, `DetectionException.java`, mọi file của A.
- Build/test: luôn thêm `"-Dbuild.dir=target-b"` (PLAN-A dùng chung module `server`):
  - test nhanh: `.\mvnw.cmd -q -pl server test "-Dbuild.dir=target-b"`
  - test DJL: `.\mvnw.cmd -q -pl server test "-Dbuild.dir=target-b" "-Dgroups=slow" "-DexcludedGroups=none"`
- Không `-am`, không `clean` ở gốc, không commit.

## Hợp đồng phải thỏa (`ObjectDetector`)
- `detect` **thread-safe**: nhiều luồng `ClientHandler` gọi đồng thời.
- Trả `List<Detection>` với `x, y, width, height` là **tỉ lệ 0..1** so với ảnh đầu vào, `confidence` 0..1, `label` là tên lớp (ví dụ `person`).
- Lỗi khi suy luận → `DetectionException` (A chuyển thành ERROR `DETECTION_FAILED`).
- Constructor nạp mô hình **một lần**; lỗi nạp mô hình → `IllegalStateException` với thông báo rõ (không đổi chữ ký để thêm checked exception). `close()` giải phóng mô hình.

## Các bước (mỗi bước có verify)
1. **Tìm hiểu (spike, ~nửa ngày)**: dùng nguồn chính thống (docs.djl.ai, github.com/deepjavalibrary/djl, Maven Central) xác định:
   - phiên bản DJL mới nhất ổn định;
   - engine nào còn cung cấp SSD-MobileNet trong model zoo và chạy được trên Windows x64 + Java 17/22. Ứng viên theo thứ tự ưu tiên:
     a. MXNet model zoo: artifact `ssd`, filter `backbone=mobilenet1.0`, `dataset=voc` (20 lớp VOC) nếu phiên bản DJL còn hỗ trợ MXNet (cần VC++ Redistributable);
     b. TensorFlow engine + SSD MobileNet v2 (80 lớp COCO) theo ví dụ object detection TensorFlow của DJL;
     c. engine khác có SSD-MobileNet (ví dụ ONNX Runtime) nếu a, b không chạy.
   - Viết một chương trình thử ngắn trong `DjlObjectDetector.main(String[] args)` (đọc ảnh từ đường dẫn, in nhãn + độ tin cậy + box) → verify: chạy `.\mvnw.cmd -q -pl server exec:java "-Dbuild.dir=target-b" "-Dexec.mainClass=com.nhom01.imagerec.server.detect.DjlObjectDetector" "-Dexec.args=server/src/test/resources/djl/dog_bike_car.jpg"` in ra đối tượng hợp lý.
   - Nếu **không có** SSD-MobileNet nào chạy được: dừng, ghi vào `COORDINATION.md` mục "Yêu cầu thay đổi hợp đồng" và hỏi người dùng (đề tài cam kết SSD-MobileNet), không tự đổi sang mô hình khác.
2. **ADR-0002** `docs/adr/0002-chon-engine-djl.md` (định dạng: đọc `Support/skills-main/skills/engineering/domain-modeling/ADR-FORMAT.md`): engine + model đã chọn, phương án bị loại, lý do, hệ quả (dung lượng tải lần đầu, số lớp nhận dạng) → verify: file tồn tại, ngắn gọn.
3. **Ảnh mẫu test**: tải `https://resources.djl.ai/images/dog_bike_car.jpg` vào `server/src/test/resources/djl/` (ảnh ví dụ chính thức của DJL: chó, xe đạp, ô tô) → verify: mở được bằng `ImageIO.read`.
4. **Test (red)** `DjlObjectDetectorTest`, gắn `@Tag("slow")` ở mức class (mặc định bị loại khỏi `mvn test` nhờ `excludedGroups=slow` ở pom gốc):
   - ảnh mẫu → có nhãn `dog` và `bicycle`; mọi tọa độ trong 0..1; confidence ≥ ngưỡng;
   - 4 luồng × 5 lần gọi `detect` đồng thời trên cùng instance → không lỗi, kết quả mỗi lần cùng tập nhãn;
   - `detect` sau `close()` → ném exception (không treo).
5. **Cài `DjlObjectDetector` (green)**:
   - Constructor: build `Criteria<Image, DetectedObjects>` (application `CV.OBJECT_DETECTION`, engine/filter theo bước 1, `optProgress(new ProgressBar())`) → `ZooModel` lưu trong field `final`. Log thời gian nạp: `Model <tên> loaded in X s`.
   - `detect`: `ImageFactory.getInstance().fromImage(bufferedImage)`; tạo **Predictor mới cho mỗi lời gọi** trong try-with-resources (đơn giản và thread-safe; chỉ tối ưu bằng pool nếu đo thấy chậm); lọc `probability ≥ 0.5` (hằng số `MIN_CONFIDENCE`); đổi `BoundingBox.getBounds()` sang `Detection`. Kiểm tra bounds DJL trả về đã là tỉ lệ 0..1 chưa; nếu là pixel thì chia cho kích thước ảnh. Kẹp tọa độ về [0, 1].
   - `TranslateException` → `DetectionException`.
   → verify: lệnh test DJL ở trên xanh; lệnh test nhanh (không slow) vẫn xanh.
6. **Chuẩn bị triển khai**: ghi trong ADR (hoặc `scripts/download-model.cmd` + `.sh`) cách tải sẵn mô hình và native library vào cache (chạy `main` một lần; thư mục cache `~/.djl.ai`, đổi bằng biến môi trường `DJL_CACHE_DIR`) để server trên VPS không tải lúc khởi động → verify: chạy lần hai không tải lại (log không có thanh tiến trình tải).

7. **Jar chạy độc lập**: `.\mvnw.cmd -q -pl server package "-Dbuild.dir=target-b"` rồi chạy `main` từ jar gộp: `java -cp server\target-b\server-1.0-SNAPSHOT-all.jar com.nhom01.imagerec.server.detect.DjlObjectDetector server\src\test\resources\djl\dog_bike_car.jpg` → verify: in ra đối tượng giống bước 1 (chứng minh engine DJL vẫn được tìm thấy sau khi gộp jar).

## Ngoài phạm vi
Không sửa phần mạng của server (PLAN-A), không training/fine-tune, không GPU, không đổi `ObjectDetector`.

## Kết quả mong đợi khi chạy đúng
- Lần đầu chạy tải mô hình + native library (vài chục tới vài trăm MB, có thanh tiến trình), các lần sau dùng cache, nạp mô hình vài giây.
- `main` trên `dog_bike_car.jpg` in khoảng: `dog 0.9x [x, y, w, h]`, `bicycle 0.8x [...]`, `car 0.7x [...]`.
- Thời gian một ảnh cỡ 1–2 MP < 1 s trên laptop (CPU).
- Test slow xanh, 4 luồng đồng thời không lỗi.
- Khi PLAN-A chạy server với `--detector=djl`: log `Model ... loaded in X s`, client nhận bounding box thật quanh người/xe/vật, **không cần A sửa code**.

## Khi xong
Cập nhật dòng B trong bảng "Trạng thái" của `COORDINATION.md` (Done + engine/model đã chọn + lệnh test slow).
