# Nhật ký fine-tune YOLOv8s (thêm lớp monkey)

Người làm: Hồ Ngọc Linh. Mọi số dưới đây lấy từ lần chạy thật. Dữ liệu và trọng số để ngoài repo: `C:\Users\Asus\tools\yolo-data` và `C:\Users\Asus\tools\yolo` (venv).

## 1. Môi trường (22/10/2026)

- `nvidia-smi` lúc 18:06: GPU NVIDIA GeForce RTX 5060 Laptop, 8151 MiB VRAM, đang dùng 1264 MiB, tải 1 %; các tiến trình trên GPU chỉ là giao diện Windows (Chrome, Windows Terminal, explorer...), không có tiến trình tính toán nặng. Driver 617.42, CUDA UMD 13.4.
- venv: `C:\Users\Asus\tools\yolo` (Python 3.12.10).
- PyTorch 2.11.0+cu128 (cài từ `https://download.pytorch.org/whl/cu128`), torchvision 0.26.0+cu128.
- Ultralytics 8.4.183, FiftyOne 1.22.1, onnx 1.23.2, onnxruntime 1.31.0.
- Kiểm tra: `torch.cuda.is_available()` = True; `get_device_capability` = (12, 0) (Blackwell, sm_120); nhân ma trận 1000×1000 trên GPU chạy được.

## 2. Dữ liệu (22/10/2026, tải 18:13–18:28, pseudo-label thêm ~2 phút)

Script (ngoài repo): `C:\Users\Asus\tools\yolo-data\scripts\` (`prepare_coco.py`, `oi_monkey.py`, `build_dataset.py`, `evaluate.py`, `monkey_probe.py`); bản sao các script và cấu hình nằm ở `docs/huan-luyen/`.

| Nguồn | Cách lấy | Thời gian |
|---|---|---|
| COCO 2017 (tập con) | `annotations_trainval2017.zip` (243 MB), lấy ngẫu nhiên (seed 20261022) 12000 ảnh train2017 để train; 1000 ảnh val và 1000 ảnh test từ val2017 (không trùng nhau, không trùng train); bỏ box `iscrowd`; tải 24 luồng | khoảng 14 phút |
| Open Images V7, lớp Monkey có box | FiftyOne zoo (`classes=["Monkey"]`, `label_types=["detections"]`), ba split riêng | khoảng 6 phút (gồm tải file nhãn bbox của train) |

Open Images Monkey: train 2143 ảnh / 5143 box; validation 155 ảnh / 350 box; test 428 ảnh / 1018 box (số box theo FiftyOne, trước khi lọc). Sau khi chuyển sang YOLO còn 3026 / 198 / 551 box khỉ (train / val / test): số này chỉ tính box khỉ của đúng các ảnh được giữ.

**Nhãn giả cho ảnh khỉ.** Ảnh Open Images chỉ gán nhãn Monkey, các vật khác (người, xe...) không có nhãn. Để mô hình không học "ảnh khỉ thì không có người" (sẽ làm hỏng lớp person), ảnh khỉ được gán thêm nhãn giả 80 lớp COCO bằng YOLOv8s gốc (conf ≥ 0,5), và bỏ mọi dự đoán chồng lên box khỉ (IoU > 0,1 hoặc tâm nằm trong box khỉ). Kết quả (train / val / test): giữ 402 / 6 / 25 box nhãn giả, bỏ 2083 / 156 / 427 dự đoán vì chồng lên khỉ (phần lớn là YOLOv8s gốc gọi khỉ bằng nhãn khác, xem mục 3).

Dataset 81 lớp (lớp 80 = `monkey`): train 14143 ảnh (12000 COCO + 2143 khỉ); val 1155 (1000 + 155); test 1428 (1000 + 428). Các tập đánh giá riêng: `val_coco` 1000, `val_monkey` 155, `test_coco` 1000, `test_monkey` 428.

## 3. Baseline: YOLOv8s gốc (`yolov8s.pt`, bản v8.4.0)

Trên cùng ảnh, imgsz 640, conf 0,001, IoU NMS 0,7, workers=0 (trên Windows `workers>0` bị treo, đã gặp thật):

| Tập | Kết quả |
|---|---|
| COCO val 1000 ảnh, 80 lớp | **mAP50-95 = 0,4575; mAP50 = 0,6236** |

Khỉ (conf 0,3, imgsz 640; với mỗi box khỉ lấy dự đoán IoU ≥ 0,3 có điểm cao nhất):

| Tập | Ảnh | Box khỉ | Nhãn của dự đoán cao nhất | Ảnh có dự đoán `person` | Box khỉ bị `person` bao trùm |
|---|---|---|---|---|---|
| OI test | 428 | 551 | bear 250, dog 147, bird 43, cat 29, elephant 9, person 8, horse 3, sheep/teddy bear/cow 1 mỗi loại; **không có dự đoán nào: 59** | 18 | 12 |
| OI val | 155 | 198 | bear 88, dog 50, cat 14, bird 12, elephant/horse/sheep 2, person 2, teddy bear 1; không có dự đoán: 25 | 8 | 5 |

Nhận xét: với YOLOv8s gốc, khỉ hầu như không bị gán `person` (8/551), mà bị gán chủ yếu là `bear` (45%) và `dog` (27%); AP50 của lớp monkey bằng 0 vì mô hình không có lớp này.

## 4. Các lần fine-tune

Khởi tạo chung: `make_init81.py` mở rộng đầu phân loại 80→81 lớp từ `yolov8s.pt` (chép 349 tensor, mở rộng 6 tensor `cv3.*.2` giữ nguyên 80 hàng cũ). Lệnh chung: Ultralytics `train(data=..., imgsz=640, batch=16, device=0, optimizer="SGD", cos_lr=True, warmup_epochs=1, close_mosaic=5, amp=True)`. Tiêu chí (mục 2.3 của tài liệu yêu cầu nội bộ, không nằm trong repo này; nội dung tiêu chí ghi ngay sau đây): mAP50-95 COCO (`val_coco`, 1000 ảnh) giảm ≤ 2 điểm so với baseline 0,4575 và AP50 monkey ≥ 0,6.

| Lần | Cấu hình | Kết quả | Kết luận |
|---|---|---|---|
| 1 | Không đóng băng, lr0 0,003, 12000 COCO + 2143 khỉ, 30 epoch dự định, 18:43; dừng ở epoch 5 (mỗi epoch 183 s epoch đầu, ≈142 s các epoch sau) | Sau 5 epoch: val_coco mAP50-95 **0,3295** (giảm 12,8), mAP50 0,4743; val_monkey AP50 **0,894** | KHÔNG ĐẠT: quên COCO. Dừng. Nguyên nhân khả dĩ: lr cao, chỉ 12000 ảnh COCO (chưa kiểm chứng riêng từng nguyên nhân) |
| 2 | `freeze=10` (đóng băng backbone), lr0 0,001, lrf 0,1, 12000 COCO + 2143 khỉ, 25 epoch dự định, 18:58; mỗi epoch ≈ 95–100 s; dừng ở epoch 15 | Epoch 5: val_coco **0,4210** (giảm 3,65), val_monkey AP50 0,923. Epoch 10: val_coco **0,4231** (giảm 3,44), mAP50 0,5908, val_monkey AP50 0,912. Val gộp (COCO+khỉ) đứng yên 0,423–0,427 từ epoch 2 đến 15 | KHÔNG ĐẠT: bão hòa, vẫn giảm > 2 điểm. Dừng ở epoch 15 |
| 3 | **40000 COCO** + 2143 khỉ = 42143 ảnh train (`yolo81b`), `freeze=10`, lr0 0,001, lrf 0,1, 15 epoch, batch 16, workers 6; bắt đầu 19:26, kết thúc 20:37; **tổng 4217 s (70,3 phút)**, mỗi epoch ≈ 4,5 phút (epoch 1: 321 s gồm val). Chỉ đổi so với lần 2: số ảnh COCO 12000 → 40000 | Epoch 5: val_coco 0,4349 (giảm 2,26), val_monkey AP50 0,913. Epoch 10: val_coco 0,4378 (giảm 1,97), AP50 0,908. Epoch 15 (cuối, `best.pt`): xem mục 5 | **ĐẠT** (mục 5) |

### 4.1 Số đo từng epoch trên val gộp (1155 ảnh: 1000 COCO + 155 khỉ)

Do Ultralytics ghi trong `results.csv`; đây là val gộp có cả ảnh khỉ nên KHÔNG so trực tiếp với baseline 0,4575 (baseline chỉ đo trên COCO val). Số so sánh chính thức là cột val_coco ở bảng mục 4.

**Lần 1 (lr0 0,003, không đóng băng, 12000 COCO + 2143 khỉ)** — 5 epoch:

| Epoch | Thời gian tích lũy (s) | mAP50 | mAP50-95 | lr |
|---|---|---|---|---|
| 1 | 183 | 0.531 | 0.379 | 0.00300 |
| 2 | 325 | 0.482 | 0.334 | 0.00299 |
| 3 | 464 | 0.478 | 0.333 | 0.00297 |
| 4 | 604 | 0.489 | 0.337 | 0.00293 |
| 5 | 746 | 0.478 | 0.332 | 0.00288 |

**Lần 2 (freeze 10, lr0 0,001, 12000 COCO + 2143 khỉ)** — 14 epoch:

| Epoch | Thời gian tích lũy (s) | mAP50 | mAP50-95 | lr |
|---|---|---|---|---|
| 1 | 149 | 0.607 | 0.441 | 0.00100 |
| 2 | 242 | 0.594 | 0.428 | 0.00100 |
| 3 | 336 | 0.594 | 0.424 | 0.00099 |
| 4 | 429 | 0.595 | 0.426 | 0.00097 |
| 5 | 523 | 0.594 | 0.423 | 0.00094 |
| 6 | 625 | 0.593 | 0.425 | 0.00091 |
| 7 | 720 | 0.592 | 0.425 | 0.00088 |
| 8 | 817 | 0.592 | 0.424 | 0.00084 |
| 9 | 913 | 0.595 | 0.427 | 0.00079 |
| 10 | 1007 | 0.593 | 0.425 | 0.00074 |
| 11 | 1109 | 0.588 | 0.423 | 0.00069 |
| 12 | 1204 | 0.592 | 0.423 | 0.00063 |
| 13 | 1303 | 0.591 | 0.425 | 0.00058 |
| 14 | 1402 | 0.596 | 0.427 | 0.00052 |


Lệnh lần 3 (từ `C:\Users\Asus\tools\yolo-data`): `$env:LR0='0.001'; $env:FREEZE='10'; $env:DATA_DIR='yolo81b'; $env:WORKERS='6'; python scripts\train81.py 15 16 ft81c`. Cấu hình huấn luyện thực tế do Ultralytics lưu ở `runs\ft81c\args.yaml` (ngoài repo). Đường cong: `ket-qua-lan3-duong-cong.png`, số theo epoch: `ket-qua-lan3-results.csv`.

Ghi chú vận hành: (1) trên Windows `workers>0` bị treo nếu script không có `if __name__ == "__main__":`; (2) trong lúc vừa tải dữ liệu vừa train, hệ thống cảnh báo thiếu RAM (còn khoảng 3,6 GB / 31,6 GB) và dừng vài lệnh chờ nền; tiến trình train không bị ảnh hưởng; (3) lần 2 bị dừng chủ động ở epoch 15/25 vì đã bão hòa.

**Lần 3 (freeze 10, lr0 0,001, 40000 COCO + 2143 khỉ)** — 15 epoch (val gộp, không so trực tiếp với baseline 0,4575):

| Epoch | Thời gian tích lũy (s) | mAP50 | mAP50-95 | lr |
|---|---|---|---|---|
| 1 | 321 | 0.594 | 0.430 | 0.00100 |
| 2 | 591 | 0.596 | 0.429 | 0.00099 |
| 3 | 861 | 0.597 | 0.431 | 0.00096 |
| 4 | 1130 | 0.594 | 0.431 | 0.00091 |
| 5 | 1396 | 0.602 | 0.437 | 0.00085 |
| 6 | 1669 | 0.594 | 0.432 | 0.00077 |
| 7 | 1936 | 0.600 | 0.437 | 0.00069 |
| 8 | 2202 | 0.597 | 0.434 | 0.00060 |
| 9 | 2468 | 0.601 | 0.438 | 0.00050 |
| 10 | 2733 | 0.600 | 0.439 | 0.00041 |
| 11 | 3036 | 0.602 | 0.439 | 0.00032 |
| 12 | 3301 | 0.606 | 0.442 | 0.00025 |
| 13 | 3566 | 0.605 | 0.441 | 0.00019 |
| 14 | 3831 | 0.605 | 0.441 | 0.00014 |
| 15 | 4096 | 0.607 | 0.442 | 0.00011 |

## 5. Kết quả cuối (lần 3, `best.pt` = epoch 15)

Đánh giá cùng cách đo cho cả hai mô hình: imgsz 640, conf 0,001, IoU NMS 0,7, `val(data=...)` của Ultralytics 8.4.183, GPU RTX 5060. val_coco và test_coco là 1000 ảnh COCO val2017 mỗi tập (không nằm trong train, không trùng nhau); test_monkey là 428 ảnh Open Images test (551 box); val_monkey 155 ảnh Open Images validation (198 box).

| Tập | YOLOv8s gốc (80 lớp) mAP50-95 | Fine-tune lần 3 mAP50-95 | Chênh |
|---|---|---|---|
| val_coco (80 lớp COCO) | 0,4575 | 0,4406 | **−1,69 điểm** |
| **test_coco (80 lớp COCO)** | 0,4605 | 0,4444 | **−1,61 điểm** |

mAP50 trên COCO: val 0,6236 → 0,6051; test 0,6346 → 0,6175.

| Tập | AP50 lớp monkey (fine-tune) | Ghi chú |
|---|---|---|
| val_monkey (155 ảnh) | **0,921** | gốc: 0 (không có lớp) |
| **test_monkey (428 ảnh)** | **0,948** | gốc: 0 |

Kết luận theo tiêu chí mục 2.3 (đánh giá trên TEST): mAP50-95 COCO giảm 1,61 điểm (≤ 2) và monkey AP50 = 0,948 (≥ 0,6): **đạt**. Biên độ còn gần ngưỡng ở val (−1,69) và cả val lẫn test chỉ 1000 ảnh, chưa tính khoảng tin cậy.

### 5.1 Ma trận nhầm lẫn của khỉ trên OI test (551 box khỉ, conf 0,3, imgsz 640)

Nhãn của dự đoán có điểm cao nhất có IoU ≥ 0,3 với từng box khỉ:

| Nhãn dự đoán | YOLOv8s gốc | Fine-tune |
|---|---|---|
| monkey | (chưa có lớp) | **509 (92,4 %)** |
| bear | 250 (45,4 %) | 8 |
| dog | 147 (26,7 %) | 6 |
| bird | 43 | 0 |
| cat | 29 | 0 |
| elephant | 9 | 1 |
| person | 8 (1,5 %) | 4 (0,7 %) |
| horse / sheep / teddy bear / cow | 3 / 1 / 1 / 1 | 0 |
| không có dự đoán | 59 (10,7 %) | 23 (4,2 %) |

Số ảnh có ít nhất một dự đoán `person`: gốc 18/428, fine-tune 9/428. Trên val_monkey: fine-tune gán monkey 180/198 box (gốc: bear 88, dog 50, ...).

### 5.2 Kiểm tra tay (ảnh khỉ không ra bear/dog/person; ảnh người ra person)

Mẫu: 10 ảnh khỉ lấy ngẫu nhiên (seed 3) từ OI test (không có trong train) và 10 ảnh có người từ OI test (FiftyOne, seed 7, 10 ảnh đầu theo tên file), conf 0,3. Ảnh ghép có khung dự đoán: `anh-kiem-tra/kiem-tra-tay-ft.jpg` (fine-tune) và `kiem-tra-tay-base.jpg` (gốc); chi tiết nhãn: `manual_ft.json`, `manual_base.json`.

| | YOLOv8s gốc | Fine-tune |
|---|---|---|
| Ảnh khỉ: có `monkey` và không có person/bear/dog | 0/10 (gán cat, bear, dog, sheep, bird) | **8/10**; 1 ảnh không có dự đoán nào, 1 ảnh có `monkey` 0,76 kèm `bear` 0,52 |
| Ảnh người: có `person` | 10/10 | **10/10** |

Ảnh người vẫn có thêm các lớp khác đúng theo COCO (chair, car, motorcycle...). Ảnh được lấy từ Open Images (giấy phép CC BY); ảnh ghép chỉ để kiểm tra nội bộ.

## 6. Xuất ONNX

- `model.export(format="onnx", opset=12, imgsz=640, simplify=True, dynamic=False)` → `C:\Users\Asus\tools\yolo-data\final\yolov8s-coco81.onnx` (44 871 540 byte). Kiểm tra (`verify_onnx.py`): opset 12; input `images` [1,3,640,640]; output `output0` [1,85,8400]; chạy được bằng onnxruntime CPU, nhãn cao nhất trùng PyTorch trên một ảnh khỉ và một ảnh người.
- `C:\Users\Asus\tools\yolo-data\final\synset.txt`: 81 dòng, dòng đầu `person`, dòng cuối `monkey`, kết thúc bằng một ký tự xuống dòng.
- Trọng số PyTorch: `C:\Users\Asus\tools\yolo-data\final\best.pt` (ngoài repo, AGPL-3.0 theo Ultralytics).
