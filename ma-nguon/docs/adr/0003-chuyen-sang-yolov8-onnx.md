# Chuyển nhận dạng sang YOLOv8s (COCO 80 lớp, ONNX) chạy bằng DJL OnnxRuntime

Thay thế quyết định trong [ADR-0002](0002-chon-engine-djl.md) (SSD-MobileNet VOC trên MXNet). Giao diện `ObjectDetector` và giao thức không đổi nên Client không phải sửa.

## Lý do đổi

Người dùng thử thấy SSD-MobileNet VOC bỏ sót nhiều vật thông dụng (điện thoại, laptop, ly... không thuộc 20 lớp VOC), nhận nhầm nhãn, có khung thừa và khung trùng. Số đo so sánh ở mục "So sánh" bên dưới.

## Quyết định

- Mô hình: **YOLOv8s** (Ultralytics) huấn luyện sẵn trên COCO, 80 lớp, xuất ONNX (`imgsz=640`, opset 12, đầu ra `[1, 84, 8400]`).
- Chạy bằng **DJL 0.38.0 + engine OnnxRuntime** (CPU). Không dùng GPU vì VPS không có.
- `synset.txt` (80 nhãn, text nhỏ) nằm trong git tại `server/src/main/resources/models/`. File `yolov8s.onnx` (~43 MB) **không đưa vào git** (`.gitignore` chặn `*.onnx`, theo quy định về file lớn); tải từ GitHub Releases (tag `model-yolov8s-coco80-v1`) bằng `scripts/download-model.cmd` / `.sh` vào cùng thư mục đó **trước khi build**, Maven sẽ đóng vào jar nên VPS không phải tải gì lúc chạy.
- Tham số `--model=<file.onnx>` (hoặc `model=` trong `server.properties`) cho phép dùng mô hình khác; `synset.txt` phải nằm cạnh file đó. Mặc định dùng mô hình trong jar.
- Tham số `--ortThreads=N` (hoặc `ortThreads=` trong `server.properties`) giới hạn số luồng intra-op của ONNX Runtime cho mỗi lần suy luận; để trống thì dùng mặc định. Hữu ích khi VPS ít nhân và nhiều Client gọi cùng lúc.
- Lớp `DjlObjectDetector` giữ tên và CLI `--detector=djl`; phần tiền/hậu xử lý nằm trong `YoloTranslator`.
- Ngưỡng tin cậy 0,3 (giữ như trước), NMS IoU 0,45 theo từng lớp, tối đa 100 đối tượng.
- Gỡ MXNet khỏi `server/pom.xml`. Shade chỉ giữ native ONNX Runtime cho Windows x64 và Linux x64.

### Tọa độ và letterbox

YOLO nhận ảnh 640x640. Ảnh được thu nhỏ giữ tỉ lệ rồi chèn viền xám (114) cho đủ vuông (letterbox). Khung dự đoán tính theo ảnh đã letterbox nên phải bỏ phần viền và chia cho hệ số thu: `x_gốc = (x - padX) / scale`, rồi chia cho kích thước ảnh gốc để ra tỉ lệ 0..1 và kẹp trong [0, 1]. Phần này có test riêng (`YoloTranslatorTest`).

### Thread-safe

Giống ADR-0002: `ZooModel` dùng chung, mỗi lần `detect` tạo một `Predictor`. `YoloTranslator` không giữ trạng thái giữa các lần gọi (thông tin letterbox đi qua `TranslatorContext`).

## Phương án đã xét

- **Giữ SSD-MobileNet VOC**: nhẹ nhưng chỉ 20 lớp, đúng các lỗi người dùng nêu.
- **YOLOv8n**: nhanh hơn, kém chính xác hơn; bản `s` vẫn đủ nhanh trên CPU (xem số đo).
- **TensorFlow / PyTorch engine của DJL**: nặng hơn, không cần khi đã có ONNX.
- **`YoloV8Translator` có sẵn của DJL**: resize thẳng về 640x640 không letterbox, làm méo ảnh; tự viết translator để kiểm soát letterbox và NMS.

## So sánh trên 4 ảnh mẫu của Client (CPU máy dev, ngày 22/10/2026)

Thời gian là trung vị của 10 lần gọi `detect` sau 2 lần khởi động, chỉ phần suy luận trong Server.

| Ảnh | SSD-MobileNet VOC (cũ) | YOLOv8s COCO (mới) |
|---|---|---|
| cat.jpg | 1 đối tượng: cat; 91 ms | 2: cat, bed; 64 ms |
| dogs.jpg | 12: dog x8, cat x3, sheep, cow (có cả nhãn sai); 92 ms | 10: dog x8, bear, potted plant; 62 ms |
| dog_bike_car.jpg | 4: car, dog x2, bicycle (0,34); 82 ms | 3: dog, bicycle (0,89), truck; 66 ms |
| taxi.jpg | 8: car x2, person x6; 88 ms | 12: car x2, person x9, stop sign, traffic light; 67 ms |

Nhận xét (chỉ theo nhãn, chưa đối chiếu nhãn đúng từng khung): YOLO chắc chắn hơn ở "bicycle" (0,89 so với 0,34) và nhanh hơn trên máy này. Vẫn còn nhận nhầm: `dogs.jpg` ra thêm "bear" 0,64 và "potted plant" 0,34; `cat.jpg` ra thêm "bed" 0,71 (có thể đúng nếu ảnh có giường). Cần người xem lại ảnh để kết luận.

Qua giao thức, đo bằng chương trình thử gửi thẳng DETECT_REQUEST tới Server cục bộ (cùng máy, 10 lần/ảnh): khứ hồi trung vị 91–100 ms, Server xử lý khoảng 63–66 ms. RAM tiến trình Server (working set sau khi chạy 44 yêu cầu, `-Xms512m -Xmx2g`): khoảng 493 MB.

## Giấy phép

Ultralytics và trọng số YOLOv8 dùng **AGPL-3.0**. Với đồ án học tập, không phân phối dịch vụ thương mại thì chấp nhận được; nếu sau này triển khai thương mại thì phải mua giấy phép Enterprise của Ultralytics hoặc mở mã nguồn toàn bộ dịch vụ theo AGPL. ONNX Runtime (MIT) và DJL (Apache 2.0) không có ràng buộc này.

## Hệ quả

- Phải tải mô hình trước khi build (xem mục "Tải mô hình trước khi build" trong README).
- Mô hình có 80 lớp COCO. COCO **không có lớp khỉ**; muốn khỉ không bị gán `person` phải fine-tune thêm lớp (ngoài phạm vi issue này).
- Jar gộp của Server giảm từ ~90 MB (MXNet) xuống ~56 MB sau khi lọc native (ảnh gồm cả mô hình ~43 MB).
- **Windows: JDK của Oracle kèm `msvcp140.dll`/`vcruntime140*.dll` bản cũ (14.36)**, trong khi `onnxruntime.dll` cần bản mới hơn. Tiến trình `java.exe` nạp bản cũ trước nên `UnsatisfiedLinkError: A dynamic link library (DLL) initialization routine failed` khi khởi tạo engine. Trên máy dev (JDK 22, Windows 11) đã gặp đúng lỗi này; chạy bình thường khi dùng bản JDK có ba file DLL này thay bằng bản mới từ System32. JDK phân phối không bundle các DLL này (ví dụ Temurin) hoặc Linux (VPS) không bị. Khi chạy Server hoặc test chậm cục bộ trên Windows, dùng JDK không bundle ba DLL này, hoặc chỉ định `-Djvm=<đường dẫn java.exe của JDK đó>` cho test.

## Cập nhật: mô hình 81 lớp (COCO + monkey)

- Mô hình mặc định trong jar đổi thành `yolov8s-coco81.onnx` (YOLOv8s tinh chỉnh thêm lớp `monkey`, đầu ra `[1, 85, 8400]`), `synset.txt` có 81 nhãn (`monkey` ở cuối). Bản 80 lớp (`yolov8s.onnx`, release `model-yolov8s-coco80-v1`) vẫn tải được nhưng không còn là mặc định.
- Mô hình 81 lớp tải từ release `model-yolov8s-coco81-v1` bằng `scripts/download-model.*`. `YoloTranslator` không đổi: số lớp lấy từ `synset.txt`.
- Quy trình huấn luyện, số đo và hạn chế nằm ở `docs/huan-luyen/nhat-ky.md`.

## Cập nhật: lớp bảo vệ MonkeyGuard

- Kiểm thử độc lập (`docs/huan-luyen/kiem-thu-doc-lap.md`) thấy khung người bị gán `monkey` hiếm nhưng có thật (4/10 777 khung COCO val2017, 4/920 khung Open Images; có ca 0,885). Thêm hậu xử lý `MonkeyGuard` sau NMS, chỉ được bỏ box `monkey`: bỏ box dưới 0,5; nếu còn khỉ thì chạy YOLOv8s gốc 80 lớp và bỏ box `monkey` trùng box `person` của mô hình gốc (độ tin cậy từ 0,2, IoU từ 0,5).
- Mô hình gốc (`/models/guard/yolov8s-coco80.onnx`) được nạp một lần lúc Server khởi động, dùng chung `--ortThreads`, và chỉ được chạy khi còn box `monkey`. File này không nằm trong git; `scripts/download-model.*` tải từ release `model-yolov8s-coco80-guard-v1` và kiểm SHA-256. Đây là một bản export khác (cùng kích thước, khác nội dung) với `yolov8s.onnx` của release `model-yolov8s-coco80-v1`, vì số đo của luật được thực hiện với file này.
- Đổi lại: jar nặng thêm khoảng 45 MB (~94 MB), ảnh có khỉ chậm hơn (trên VPS khoảng +250-350 ms), recall khỉ trên `test_monkey` giảm 491 → 461 trên 551 khung (chủ yếu do ngưỡng 0,5), RSS của Java tăng. Còn sót: ảnh cận mặt người (0,885) và bóng đen chữ YOGA (0,83) không luật nào dựa trên box `person` bắt được.
