# Dùng DJL + engine MXNet với SSD-MobileNet1.0 (VOC) cho nhận dạng

Phiếu đăng ký cam kết DJL + SSD-MobileNet pre-trained. DJL 0.38.0 vẫn có `ai.djl.mxnet:ssd` (`backbone=mobilenet1.0`, `dataset=voc`) trong MXNet model zoo, chạy được trên Windows x64 + Java 17/22 (đã kiểm với Java 22), nên chọn model `ssd_512_mobilenet1.0_voc`.

## Considered Options

- **TensorFlow engine + SSD MobileNet v2 (COCO 80 lớp)**: nhiều lớp hơn nhưng không cần vì MXNet đã đáp ứng cam kết; không thử.
- **ONNX Runtime**: dự phòng, không cần dùng.

## Consequences

- Chỉ nhận **20 lớp VOC** (person, dog, car, bicycle, ...), không phải 80 lớp COCO.
- Native MXNet 1.9.1 (win-x86_64, linux-x86_64) nằm trong jar gộp (~90 MB); Windows cần VC++ Redistributable.
- Lần đầu chạy tải mô hình (~vài chục MB) vào `~/.djl.ai` (đổi bằng `DJL_CACHE_DIR`) và giải nén native; lần sau nạp < 1 s, suy luận ~0.2 s/ảnh trên CPU.
- Tọa độ SSD trả về theo ảnh resize 512x512 nên `DjlObjectDetector` chia cho 512 để ra tỉ lệ 0..1.
- Ngưỡng tin cậy 0.3 (không phải 0.5): "bicycle" trong ảnh mẫu chỉ đạt ~0.34.
- Triển khai VPS: chạy `scripts/download-model.sh` một lần để tải sẵn mô hình, server không tải lúc khởi động.
