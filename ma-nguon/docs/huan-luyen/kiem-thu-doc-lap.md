# Kiểm thử độc lập mô hình YOLOv8s 81 lớp (COCO + monkey)

Người làm: Hồ Ngọc Linh. Ngày 22/10/2026. Mô hình kiểm thử: `server/src/main/resources/models/yolov8s-coco81.onnx`, tương ứng `best.pt` của lần fine-tune thứ 3 (xem `nhat-ky.md`). Mọi số dưới đây lấy từ lần chạy thật; script nằm ở `scripts-kiem-thu/` (sao chép từ thư mục làm việc ngoài repo), dữ liệu và ảnh đầy đủ để ngoài repo. Trong repo chỉ có ảnh ghép `anh-kiem-thu/luat-nguoi-va-khi.jpg` (ảnh COCO/Open Images dùng cho kiểm thử nội bộ); các ảnh ghép khác được nhắc trong tài liệu (`nguoi-thanh-monkey-coco.jpg`, `oi-person-nham-monkey.jpg`, `so-sanh-*.jpg`...) để ngoài repo. Script trong `scripts-kiem-thu/` ghi đường dẫn của máy kiểm thử (thư mục dữ liệu `ROOT`), nên chạy lại ở máy khác phải sửa đường dẫn; nhóm chưa chạy lại toàn bộ quy trình trên máy khác.

Cách đo chung: ngưỡng tin cậy 0,3; NMS IoU 0,45 (như Server); imgsz 640. Chạy Python (Ultralytics 8.4.183, GPU RTX 5060 sau khi `nvidia-smi` cho thấy GPU rảnh: tải 0 %, chỉ có tiến trình giao diện) trừ khi ghi khác.

## Tóm tắt

| Câu hỏi | Kết quả |
|---|---|
| Người có bị nhầm thành `monkey` không? | **Có, hiếm nhưng không bằng 0.** Trên 10 777 box person của COCO val2017 (2693 ảnh): **4 box (0,037 %)** bị dự đoán monkey (IoU ≥ 0,5, conf ≥ 0,3); YOLOv8s gốc: 0. Trên 920 box Person của 321 ảnh Open Images test: **4 box (0,43 %)** (đều là linh vật, tượng, bóng đen). Thêm 1 ảnh vận động viên bơi (0,49) trong 30 ảnh người OI kiểm tra tay. Ca nặng nhất: ảnh cận mặt người (monkey 0,885) |
| Khỉ có được nhận đúng không? | Trên 20 ảnh khỉ OI test: SSD cũ 0/20 ra khỉ (gán dog, cat, bird, person), YOLOv8s gốc 0/20 (gán bear 11, dog 9, ...), **YOLOv8s 81 lớp: 19/20 ảnh ra `monkey`** |
| Có làm hỏng các vật khác không? | Số đối tượng trên 30 ảnh đời thường: SSD 80, YOLO 80 lớp 252, YOLO 81 lớp 259. 41 dự đoán `monkey` sai trên 5000 ảnh COCO val2017 (không có khỉ), phần lớn rơi vào chó, mèo, gấu, chim, gấu bông |
| Pipeline Java có khớp Python không? | Gần khớp, **không giống hệt**: 59/84 ảnh cùng tập nhãn; 475/509 box của Java khớp nhãn và IoU ≥ 0,5 với Python; chênh độ tin cậy trung bình 0,034 |
| Tốc độ CPU | Suy luận ONNX Runtime ≈ 49,7 ms (80 lớp) so với 49,8 ms (81 lớp); Server Java: 62 ms so với 63 ms (trung vị) |

## 1. Người có bị nhầm thành khỉ không

### 1.1 COCO val2017 (ảnh không dùng để train)

Tập: toàn bộ 2693 ảnh val2017 có ít nhất một box person không-crowd, tổng **10 777 box person**. Train chỉ lấy từ `train2017` nên các ảnh này không nằm trong tập train (1000 ảnh trong số đó là `val_coco`/`test_coco` dùng để chọn và đo ở `nhat-ky.md`; số đo dưới đây là một cách đo khác trên toàn bộ ảnh có người).

| | YOLOv8s gốc (80 lớp) | YOLOv8s 81 lớp |
|---|---|---|
| Box person được dự đoán `person` (IoU ≥ 0,5) | 7676 (71,2 %) | 7730 (71,7 %) |
| **Box person bị dự đoán `monkey` (IoU ≥ 0,5, conf ≥ 0,3)** | 0 (không có lớp) | **4 (0,037 %)** |
| theo cỡ box: nhỏ (< 32²) | 0 / 3206 | 0 / 3206 |
| vừa (32²–96²) | 0 / 3722 | 1 / 3722 |
| lớn (≥ 96²) | 0 / 3849 | 3 / 3849 |

Bốn ca (ảnh ghép `anh-kiem-thu/nguoi-thanh-monkey-coco.jpg`, theo thứ tự trên-trái, trên-phải, dưới-trái, dưới-phải):

| Ảnh COCO | monkey conf | Mô tả (xem ảnh) |
|---|---|---|
| 000000500716 | **0,885** | Ảnh cận mặt một người (kẹp quần áo trên kính); box gần phủ cả ảnh |
| 000000149770 | 0,869 | Bóng đen một người lướt sóng |
| 000000565962 | 0,791 | Tượng Phật nhỏ cạnh con mèo (COCO gán nhãn person cho tượng) |
| 000000117719 | 0,450 | Một người chơi đàn trong quán, bị cắt một phần, ảnh tối |

### 1.2 Toàn bộ 5000 ảnh val2017: mọi dự đoán `monkey` (đều sai vì COCO không có khỉ)

Có **41** dự đoán `monkey` (conf ≥ 0,3). Vật có nhãn trùng với box (IoU ≥ 0,3): dog 9, bear 6, cat 6, bird 4, **person 4**, teddy bear 3, elephant 1, cow 1, không trùng vật nào có nhãn 7. Ảnh của từng ca được lưu ngoài repo (`indep/fp/`).

### 1.3 Open Images Person (phân phối khác COCO)

Lấy 400 ảnh có nhãn Person từ tập **test** của Open Images V7 (FiftyOne, seed 21); sau khi bỏ box `IsGroupOf` còn 321 ảnh, **920 box Person**. Không ảnh nào nằm trong tập train khỉ (tập khỉ lấy lớp Monkey).

| | YOLOv8s gốc | YOLOv8s 81 lớp |
|---|---|---|
| Box được dự đoán `person` (IoU ≥ 0,5) | 575 | 574 |
| **Box bị dự đoán `monkey`** | 0 | **4 (0,43 %), trong 3 ảnh** |

Bốn ca (`anh-kiem-thu/oi-person-nham-monkey.jpg`): linh vật gấu bông cỡ người (0,48), tượng thần lùn trong vườn (0,50), hai bóng người đen trên chữ "YOGA" (0,83 và 0,47). Đây là các vật mà Open Images vẫn gán nhãn Person; không phải ảnh chụp người thật.

Trong 30 ảnh có người (OI test, FiftyOne seed 7) dùng để kiểm tra tay, 1 ảnh có `monkey` 0,49 (kèm `person` 0,31) trên người đang bơi (`anh-kiem-thu/nguoi-boi-thanh-monkey-oi.jpg`); 29 ảnh còn lại không có `monkey`. Python và Java cho cùng kết quả ở ảnh này.

### 1.4 Nhận xét

- Tỉ lệ người thật bị nhầm là thấp (COCO: 4/10 777; riêng người chụp thật khoảng 3 ca) nhưng **lỗi có thật và có thể độ tin cậy cao (0,885)**: ảnh cận mặt, bóng đen, tư thế lạ, ảnh tối.
- Nếu nâng ngưỡng lên 0,5, vẫn còn 3 trong 4 ca COCO (0,885; 0,869; 0,791) và 2 trong 4 ca OI (0,83; 0,50). Muốn giảm cần giảm ảnh khỉ trong train hay thêm ảnh người cận mặt/bóng đen vào train, nhóm chưa làm.
- Mẫu đo còn nhỏ cho một lỗi hiếm (vài ca); không có khoảng tin cậy.

## 2. So sánh SSD cũ, YOLOv8s 80 lớp, YOLOv8s 81 lớp

Bộ ảnh (54 ảnh, `indep/set/` ngoài repo): 4 ảnh mẫu của Client; 30 ảnh đời thường từ COCO val2017 (6 nhóm × 5 ảnh, không thuộc `val_coco`/`test_coco`: đường phố, trong nhà có điện thoại/laptop/ly, thú cưng, vườn thú có gấu/voi/ngựa vằn/hươu, đám đông, thể thao/đồ ăn); 20 ảnh khỉ từ Open Images test (ngẫu nhiên seed 11). SSD cũ chạy bằng Java trong jar cũ (`NhanDangAnh-Nhom01.zip`, bản 17/10/2026), ngưỡng 0,3 của chính nó; YOLO chạy bằng Python.

Tổng số đối tượng (chỉ để so độ phủ, không phải độ đúng, vì SSD chỉ có 20 lớp VOC và bộ ảnh này không có nhãn chuẩn của từng khung):

| Bộ ảnh | SSD cũ | YOLOv8s 80 lớp | YOLOv8s 81 lớp |
|---|---|---|---|
| 4 ảnh mẫu | 25 | 26 | 28 |
| 30 ảnh đời thường | 80 | 252 | 259 |

Ảnh khỉ (20 ảnh): nhãn của mọi dự đoán.

| Mô hình | Số ảnh ra `monkey` | Nhãn xuất hiện |
|---|---|---|
| SSD cũ | 0 / 20 | dog 10, cat 4, person 2, bird 2 |
| YOLOv8s 80 lớp | 0 / 20 | bear 11, dog 9, person 3, bird 2, cat 1, horse 1 |
| YOLOv8s 81 lớp | **19 / 20** | monkey 20, person 2, bear 2 |

Về nhãn `person` trong ảnh khỉ: ở SSD, 1 ảnh (`oi_1c9ac82ca48c66c7`, con đười ươi) có `person` 0,65 chồng lên vùng của con vật (IoU 0,51 với khung khác); ở cả hai bản YOLO, nhãn `person` trên ảnh này rơi vào những người đứng phía sau (IoU ≈ 0 với khỉ). Ảnh ghép: `anh-kiem-thu/so-sanh-*.jpg` (mỗi hàng một ảnh, ba cột SSD cũ | YOLOv8s 80 | YOLOv8s 81). Chỉ có số liệu, chưa chấm đúng/sai thủ công từng khung ngoài những ảnh nêu trên; trên Open Images test, AP50 của lớp monkey (có nhãn chuẩn) là 0,948 (xem `nhat-ky.md`).

Bảng nhãn từng ảnh (số trong ngoặc là số đối tượng):

| Ảnh | SSD cũ (số đối tượng) | YOLOv8s 80 lớp | YOLOv8s 81 lớp |
|---|---|---|---|
| sample/cat.jpg | cat (1) | cat, bed (2) | bed, cat (2) |
| sample/dog_bike_car.jpg | dog×2, car, bicycle (4) | dog, bicycle, truck (3) | dog, bicycle, truck, car (4) |
| sample/dogs.jpg | dog×7, cat×3, sheep, cow (12) | dog×8, bear (9) | dog×8, bear (9) |
| sample/taxi.jpg | person×6, car×2 (8) | person×8, car×2, stop sign, traffic light (12) | person×8, car×2, traffic light, truck, stop sign (13) |
| daily/dam_dong_000000012670.jpg | person×15 (15) | person×12, handbag×2 (14) | person×10, handbag×3 (13) |
| daily/dam_dong_000000203931.jpg | person×3 (3) | person×7, bench×2, baseball bat (10) | person×7, baseball bat, bench (9) |
| daily/dam_dong_000000243034.jpg | (không có) (0) | person×3, clock (4) | person×3, clock (4) |
| daily/dam_dong_000000370486.jpg | person×8 (8) | person×18, handbag×2, umbrella, sports ball (22) | person×18, handbag×2, umbrella (21) |
| daily/dam_dong_000000551820.jpg | person×2 (2) | person×12 (12) | person×14 (14) |
| daily/duong_pho_000000054967.jpg | car×2, bus (3) | car×13, person×2, traffic light×2, truck (18) | car×12, traffic light×3, person×2, truck (18) |
| daily/duong_pho_000000138639.jpg | person×3, bicycle, car (5) | person×10, car×3, potted plant×3, bicycle, traffic light (18) | person×11, potted plant×4, car×3, bicycle, backpack (20) |
| daily/duong_pho_000000183246.jpg | (không có) (0) | traffic light×8, car×2, truck (11) | traffic light×10, car×3, truck×2 (15) |
| daily/duong_pho_000000215723.jpg | person×2 (2) | person×2, traffic light, car, umbrella (5) | person×2, traffic light, car, umbrella (5) |
| daily/duong_pho_000000254814.jpg | car×4 (4) | motorcycle×12, person×10, car×5, truck, bicycle (29) | motorcycle×13, person×10, car×6, bicycle×2, truck (32) |
| daily/so_thu_000000083113.jpg | cow, horse (2) | elephant×2 (2) | elephant×2 (2) |
| daily/so_thu_000000134689.jpg | bird (1) | giraffe (1) | giraffe (1) |
| daily/so_thu_000000344059.jpg | horse (1) | giraffe×2 (2) | giraffe×2 (2) |
| daily/so_thu_000000464872.jpg | (không có) (0) | zebra×2 (2) | zebra×2 (2) |
| daily/so_thu_000000480842.jpg | bird, cat (2) | giraffe×3 (3) | giraffe×4 (4) |
| daily/the_thao_do_an_000000060102.jpg | person×2 (2) | person×2, sports ball (3) | person×2, sports ball (3) |
| daily/the_thao_do_an_000000252776.jpg | person×2 (2) | person×4, chair (5) | person×4, chair (5) |
| daily/the_thao_do_an_000000345466.jpg | person×2 (2) | person×6, baseball glove, sports ball (8) | person×6, baseball glove, sports ball (8) |
| daily/the_thao_do_an_000000377368.jpg | diningtable, sheep (2) | cup×2, cake×2, person, dining table (6) | cup×2, cake×2, dining table, person (6) |
| daily/the_thao_do_an_000000562581.jpg | person (1) | person, tennis racket, sports ball (3) | person, sports ball, tennis racket (3) |
| daily/thu_cung_000000015497.jpg | cat (1) | mouse, cat (2) | mouse, cat, bed (3) |
| daily/thu_cung_000000077595.jpg | cat (1) | laptop, cat, bed (3) | laptop, cat, bed (3) |
| daily/thu_cung_000000108244.jpg | diningtable (1) | cat, cup (2) | cat (1) |
| daily/thu_cung_000000387383.jpg | cat×2, sofa (3) | cat×3, bed (4) | cat×3, bed (4) |
| daily/thu_cung_000000446522.jpg | dog, sofa (2) | dog, couch, refrigerator, chair (4) | dog, couch, refrigerator (3) |
| daily/trong_nha_000000181796.jpg | diningtable, bottle (2) | cup×4, fork×2, wine glass, dining table, knife, spoon, bowl (11) | cup×4, fork×2, spoon×2, knife×2, wine glass, dining table, bowl (13) |
| daily/trong_nha_000000226147.jpg | person×2, diningtable (3) | sandwich×3, knife×3, person×2, fork, cup, vase, dining table, potted plant (13) | sandwich×3, person×2, fork×2, knife×2, cup, dining table, vase, potted plant (13) |
| daily/trong_nha_000000244833.jpg | person×2, bicycle (3) | person×4, motorcycle×2, cup, scissors (8) | person×4, motorcycle×2, cup (7) |
| daily/trong_nha_000000306139.jpg | person×5, chair (6) | person×9, laptop×6, chair×3, bottle, car, backpack, potted plant, handbag, dining table (24) | person×9, laptop×4, chair×3, bottle, handbag, car, backpack, dining table, potted plant (22) |
| daily/trong_nha_000000445792.jpg | person (1) | person×2, remote (3) | person×2, remote (3) |
| monkey/oi_1c5923cf93d432fe.jpg | dog (1) | dog (1) | monkey (1) |
| monkey/oi_1c9ac82ca48c66c7.jpg | person, dog (2) | person×3, dog (4) | person×2, monkey (3) |
| monkey/oi_276e07e1b4f277e5.jpg | dog (1) | bird (1) | monkey (1) |
| monkey/oi_343cdfe3517037d8.jpg | dog (1) | dog (1) | monkey (1) |
| monkey/oi_34c5750f3825d5ea.jpg | (không có) (0) | dog (1) | monkey (1) |
| monkey/oi_35cb03cec4153727.jpg | bird (1) | bear (1) | monkey (1) |
| monkey/oi_5928342d7e76b468.jpg | dog (1) | cat (1) | monkey (1) |
| monkey/oi_7f808fd34aab3c19.jpg | cat (1) | bear, bird (2) | monkey (1) |
| monkey/oi_82fd609fd5eed8fc.jpg | dog×2 (2) | dog×2 (2) | monkey×2 (2) |
| monkey/oi_891b561fed6ed07e.jpg | (không có) (0) | horse, dog (2) | monkey (1) |
| monkey/oi_8a0137c4b0591733.jpg | (không có) (0) | (không có) (0) | monkey (1) |
| monkey/oi_9444440de9a9b647.jpg | cat (1) | bear (1) | monkey (1) |
| monkey/oi_94ca12f0d0aa4748.jpg | dog (1) | dog (1) | monkey (1) |
| monkey/oi_a51cff6f9d647598.jpg | person (1) | bear (1) | monkey (1) |
| monkey/oi_afc19a4f621f7834.jpg | dog (1) | bear×3 (3) | monkey, bear (2) |
| monkey/oi_b5f4736bfb42a787.jpg | dog (1) | dog (1) | monkey (1) |
| monkey/oi_bbd2d048af251756.jpg | cat (1) | bear (1) | monkey (1) |
| monkey/oi_ee3d39033a42907d.jpg | (không có) (0) | bear (1) | bear (1) |
| monkey/oi_f1ddc40e2b21cfa1.jpg | cat (1) | bear (1) | monkey (1) |
| monkey/oi_f41edd61a694da06.jpg | bird (1) | bear (1) | monkey (1) |


## 3. Chạy qua đúng pipeline Java

Cách làm: build `common`, `server`, `client` từ repo (`mvnw -pl common,server,client package -DskipTests -Dbuild.dir=target-kiemthu`, đã xóa thư mục build sau khi dùng); chạy Server `--detector=djl --gui=false` bằng runtime Java đóng gói sẵn của dự án (`dist/NhanDangAnh-Server/runtime`, đã thay các DLL Visual C++ như ADR-0003); gửi ảnh bằng `ServerConnection` của Client (cùng giao thức, qua TCP localhost). 84 ảnh: 4 mẫu, 30 đời thường, 20 khỉ, 30 ảnh người OI.

| Chỉ số | Kết quả |
|---|---|
| Ảnh có cùng tập nhãn (đa tập) Java và Python | 59 / 84 |
| Số đối tượng | Java 509, Python 501 |
| Box Java khớp một box Python cùng nhãn (IoU ≥ 0,5) | 475 / 509 |
| Chênh độ tin cậy giữa các box khớp | trung bình 0,034, lớn nhất 0,40 |
| Ảnh khỉ ra `monkey` (Java) | 19 / 20 (21 box monkey, person 2, bear 2); Python 19 / 20 (20 box monkey) |
| 30 ảnh người OI: có `person` (Java) | 28 / 30; 1 ảnh có thêm `monkey` 0,49 (người bơi) |
| Thời gian Server xử lý (trung vị, 84 ảnh) | 61 ms; khứ hồi qua giao thức 84,5 ms |

Kết luận: kết quả Java gần với Python nhưng không trùng hoàn toàn: 25/84 ảnh khác tập nhãn (thêm/bớt vài khung ở ngưỡng 0,3, ví dụ `dogs.jpg`: Python có `bear` 0,52, Java có `potted plant` 0,31; `taxi.jpg`: Python có `truck` 0,37). Nguyên nhân khả dĩ là khác biệt tiền xử lý (cách thu nhỏ và chèn viền letterbox giữa `YoloTranslator` và Ultralytics) và giữa PyTorch với ONNX Runtime; mình chưa tách riêng từng nguyên nhân. Nhãn `monkey` và `person` ở ảnh khỉ/người vẫn khớp ở mức ảnh.

## 4. Tốc độ CPU

Máy: Intel Core i7-14650HX (24 luồng logic), Windows 11. Đầu vào [1,3,640,640], 5 lần khởi động rồi 60 lần đo.

| Phép đo | 80 lớp | 81 lớp |
|---|---|---|
| ONNX Runtime thuần (Python), luồng mặc định: trung vị / p90 | 49,7 / 51,8 ms | 49,8 / 53,1 ms |
| ONNX Runtime thuần, giới hạn 4 luồng (giống VPS 4 vCPU): trung vị / p90 | 105,5 / 121,8 ms | 109,5 / 133,6 ms |
| Server Java đầy đủ (tiền xử lý + suy luận + NMS), 54 ảnh, vòng thứ 2: Server xử lý trung vị / p90 | 62 / 66 ms | 63 / 71 ms |
| Khứ hồi qua giao thức (localhost), trung vị | 80,5 ms | 86,5 ms |
| SSD-MobileNet cũ (MXNet, Java, 54 ảnh): trung vị / p90 | 69,8 / 79,9 ms | |

Mô hình 81 lớp chậm hơn bản 80 lớp không đáng kể (thêm một lớp đầu ra, đầu ra [1,85,8400] so với [1,84,8400]). Các số này đo trên máy phát triển; số trên VPS (4 vCPU dùng chung) sẽ khác, xem `docs/deploy.md`.

## Hạn chế của lần kiểm thử này

- Chưa chấm đúng/sai thủ công từng khung trên 54 ảnh so sánh (không có nhãn chuẩn cho bộ này, trừ ảnh khỉ có nhãn Open Images ở phần AP).
- Mẫu nhầm người thành khỉ rất ít (4 ca COCO, 4 ca OI, 1 ca kiểm tra tay), nên tỉ lệ chỉ mang tính tham khảo.
- Chênh lệch Java so với Python chưa được tách nguyên nhân.
- Không đo trên ảnh người ở điều kiện thực tế của Client (webcam, ảnh tự chụp).

## Lớp bảo vệ: giảm lỗi người bị nhận thành khỉ (thiết kế bằng Python, cài trong Java ở `MonkeyGuard`)

Mục tiêu theo yêu cầu: luật hậu xử lý sau NMS chỉ được **bỏ box `monkey`**, không đụng các lớp khác (nên mAP50-95 của 80 lớp COCO không đổi theo cấu tạo; không đo lại riêng). Cách chọn tham số để tránh chọn theo chính tập báo cáo: COCO val2017 (2693 ảnh có người) chia theo `image_id` chẵn (nửa A: 5460 box person) và lẻ (nửa B: 5317 box person); tham số chọn trên nửa A và trên OI `val_monkey`; báo cáo trên nửa B, OI Person (321 ảnh, 920 box), OI `test_monkey`. Số đo AP50 và recall của khỉ ở mục này do script riêng (`scripts-kiem-thu/rules.py`: nội suy 101 điểm, IoU 0,5, NMS 0,45, conf ≥ 0,001) nên khác nhẹ với 0,948 trong `nhat-ky.md` (Ultralytics, NMS 0,7); chỉ so sánh trước/sau trong cùng script.

### Phân tích 8 ca lỗi (4 COCO, 3 Open Images, 1 người bơi): luật nào chặn được

| Ca | conf monkey | Box `person` của chính mô hình 81 lớp ở đó (conf ≥ 0,05) | Box `person` của YOLOv8s gốc ở đó | Luật chặn |
|---|---|---|---|---|
| COCO 500716 (cận mặt người) | **0,885** | không có | không có | **không luật nào** (chỉ chặn được nếu T_m > 0,885) |
| COCO 149770 (bóng đen lướt sóng) | 0,869 | không có | có, 0,92, IoU 0,99 | chỉ veto bằng YOLO gốc |
| COCO 565962 (tượng Phật) | 0,791 | không có | có, 0,28, IoU 0,98 | chỉ veto bằng YOLO gốc (cần ngưỡng person ≤ 0,28) |
| COCO 117719 (người chơi đàn, tối) | 0,450 | có nhưng ở chỗ khác (IoU 0,04) | có, 0,47, IoU 0,98 | T_m ≥ 0,5 hoặc veto YOLO gốc |
| OI linh vật gấu bông | 0,481 | có nhưng ở chỗ khác (IoU 0,18) | không trùng | T_m ≥ 0,5 |
| OI tượng thần lùn | 0,496 | không có | không có | T_m ≥ 0,5 |
| OI chữ YOGA (6 box monkey trên bóng người, 0,93 / 0,83 / 0,72 / 0,57 / 0,54 / 0,46) | 0,46–0,93 | không có | không có | T_m ≥ 0,5 chỉ bỏ box 0,46; các box còn lại vẫn sót |
| Người bơi (OI, kiểm tra tay) | 0,494 | có, 0,31, IoU 0,96 | có, 0,49, IoU 0,95 | T_m ≥ 0,5, luật (b) với P ≤ 0,3, veto YOLO gốc |

Trả lời câu hỏi "ảnh cận mặt 0,885 có box person đi kèm không": **không**, cả mô hình 81 lớp lẫn YOLOv8s gốc đều không dự đoán person nào chồng lên box đó (kể cả conf 0,05), nên luật dựa trên box person không chặn được ca này. Luật (b) dùng box person của **chính mô hình 81 lớp** (thử P = 0,05; 0,1; 0,3; 0,5 và U = 0,3; 0,5; 0,7, có hoặc không có điều kiện "phần lớn nằm trong box person" C = 0,7 hay 0,9) không chặn thêm ca nào so với T_m = 0,5 riêng; điều kiện C còn làm mất thêm vài khỉ thật (val AP50 0,892 → 0,889). Lý do: mô hình 81 lớp, khi đã gán `monkey` cho vùng đó, thường không còn dự đoán `person` ở cùng chỗ.

### Luật được chọn (đặc tả cho cài đặt Java)

Áp dụng sau đường xử lý hiện tại (conf ≥ 0,3, NMS 0,45 theo từng lớp, tối đa 100 box), theo thứ tự:

1. **Ngưỡng riêng cho monkey:** bỏ mọi box `monkey` có conf < **0,5**.
2. **Veto bằng YOLOv8s gốc (80 lớp):** nếu còn box `monkey` sau bước 1, chạy thêm mô hình YOLOv8s gốc (cùng `yolov8s-coco80.onnx`, cùng tiền xử lý, ngưỡng conf **0,2**, NMS IoU 0,45 theo từng lớp) trên cùng ảnh; với mỗi box `monkey` còn lại, nếu tồn tại một box `person` của mô hình gốc (conf ≥ **0,2**) có **IoU ≥ 0,5** với nó (cùng hệ tọa độ, tọa độ chuẩn hóa theo ảnh gốc) thì bỏ box `monkey` đó.
3. Các box lớp khác giữ nguyên. Không có điều kiện "nằm trong box person" (C) và không dùng box `person` của mô hình 81 lớp.

Mô hình gốc được nạp một lần lúc Server khởi động (trong constructor của `DjlObjectDetector`, dùng chung `--ortThreads`); bước 2 chỉ được CHẠY khi còn ít nhất một box `monkey` sau bước 1, nên phần lớn ảnh (không có khỉ) không tốn thêm một lần suy luận. Khi có khỉ, thêm một lần suy luận ≈ 50 ms (ONNX Runtime thuần trên máy phát triển, luồng mặc định; ≈ 105 ms nếu giới hạn 4 luồng) và thêm khoảng 45 MB bộ nhớ cho file ONNX thứ hai. Bản rút gọn không cần mô hình thứ hai là chỉ dùng bước 1 (T_m = 0,5). Các ngưỡng được chọn bằng Python (Ultralytics); khi cài trong Java nên kiểm tra lại qua Server Java (script `E2E.java` dùng cho việc này không nằm trong repo) vì conf giữa Java và Python lệch nhẹ (mục 3).

### Bảng trước/sau

"Nham" = số box person GT bị dự đoán `monkey` (IoU ≥ 0,5). "Monkey còn" = tổng dự đoán `monkey` còn lại trên cả nửa ảnh COCO (đều sai vì không có khỉ). Recall khỉ = số box khỉ GT được dự đoán đúng (IoU ≥ 0,5) với box còn lại ở conf ≥ T_m.

| Luật | Nửa A: nhầm / monkey còn (5460 box person) | **Nửa B: nhầm / monkey còn (5317 box person)** | **OI Person (920 box)** | Người bơi (30 ảnh kiểm tra tay) | OI val_monkey: AP50 / recall | **OI test_monkey: AP50 / recall** |
|---|---|---|---|---|---|---|
| Không luật (hiện tại) | 3 / 27 | 1 / 14 | 4 | 1 ảnh | 0,918 / 175 trên 198 | 0,932 / 491 trên 551 |
| Chỉ T_m = 0,5 | 3 / 19 | 0 / 6 | 1 | 0 | 0,892 / 163 | 0,912 / 466 |
| Chỉ veto YOLO gốc (P = 0,2, U = 0,5) | 1 / 24 | 0 / 12 | 3 | 0 | 0,913 / 173 | 0,927 / 486 |
| **Luật chọn (T_m = 0,5 + veto)** | **1 / 16** | **0 / 6** | **1** | **0** | **0,889 / 162** | **0,907 / 461** |

Đối chiếu tiêu chí: số box người bị gán khỉ giảm rõ (nửa A 3 → 1, nửa B 1 → 0, OI Person 4 → 1) nhưng **chưa về 0 trên toàn bộ**: còn ảnh cận mặt (0,885) và một box bóng đen YOGA (0,83) mà không luật nào dựa trên box person bắt được. AP50 của khỉ trên test_monkey = 0,907 (≥ 0,85, đạt) nhưng recall khỉ giảm từ 491 xuống 461 trên 551 box (89,1 % → 83,7 %): luật làm mất 30 khỉ thật ở test (13 ở val), chủ yếu do ngưỡng T_m = 0,5 (khỉ thật có độ tin cậy dưới 0,5); riêng veto làm mất 5 khỉ thật ở test (491 → 486), vì YOLO gốc đôi khi gọi khỉ là người (đo ở `nhat-ky.md`: 8 trên 551 box). mAP50-95 80 lớp COCO không đổi theo cấu tạo.

Ghi chú quan trọng về độ tin cậy của kết luận: nửa B chỉ có **1** ca nhầm ở trạng thái gốc (conf 0,45), nửa A có 3, OI Person có 4, nên "0/5317" ở nửa B không chứng minh được nhiều; con số đáng tin hơn là tập hợp ca đã phân tích ở bảng trên (luật chọn chặn hẳn 6 trong 8 ca: bóng lướt sóng, tượng Phật, người chơi đàn, linh vật, tượng thần lùn, người bơi; còn lại ảnh cận mặt và chữ YOGA).

### Ca "người và khỉ cùng ảnh": luật có xóa nhầm khỉ thật không

Tìm trong OI val+test_monkey các ảnh có người thật (YOLO gốc dự đoán person conf ≥ 0,5, không trùng box khỉ GT): 10 ảnh, 15 box khỉ GT (người đưa tay cho khỉ ăn, du khách đứng sau lồng, người dắt khỉ...). Số khỉ GT không được nhận đúng: trước luật 4, sau luật 4; tức luật **không làm mất thêm khỉ nào trong các ảnh có người thật**. Ảnh ghép: `anh-kiem-thu/luat-nguoi-va-khi.jpg` (box khỉ giữ lại: xanh lá; box person của YOLO gốc: xanh dương; không có box đỏ nghĩa là không box khỉ nào bị luật bỏ). Giới hạn: tập này chỉ 10 ảnh và không có ảnh "người bế khỉ" sát nhau thật sự (box khỉ chồng lên box người với IoU cao); luật chỉ veto khi box `person` của YOLO gốc **gần trùng** box khỉ (IoU ≥ 0,5), nên một con khỉ nhỏ được người bế (box khỉ nằm gọn trong box người, IoU thấp) sẽ không bị bỏ, nhưng điều này chưa được đo trên ảnh thật.

### Khuyến nghị

- Nếu chỉ muốn giảm lỗi ở mức thấp nhất và không thêm mô hình thứ hai: chỉ dùng T_m = 0,5 (nhầm ở OI Person 4 → 1, nửa B 1 → 0; nhưng nửa A vẫn 3 và recall khỉ giảm 491 → 466).
- Luật đầy đủ chặn được thêm 2 ca COCO có độ tin cậy cao (bóng đen lướt sóng 0,87, tượng 0,79) với chi phí một mô hình thứ hai chỉ chạy khi có khỉ.
- Không luật nào dựa trên NMS hiện có chặn được ảnh cận mặt (0,885) và bóng đen YOGA (0,83). Chặn được chúng đòi hỏi thêm dữ liệu huấn luyện (ảnh người cận mặt, bóng đen, tượng/linh vật gán nhãn không phải khỉ), chưa làm.

## Kiểm tra sau khi cài `MonkeyGuard` trong Java

Cài đặt: `MonkeyGuard` (bước 1 và bước 2 ở trên), `DjlObjectDetector` gọi nó sau NMS khi mô hình chính có nhãn `monkey`; test `MonkeyGuardTest` (7 test) kiểm ngưỡng, veto và việc không gọi mô hình gốc khi không còn khỉ. Chạy qua Server Java (jar mới, 84 ảnh như mục 3): 19/20 ảnh khỉ Open Images vẫn ra `monkey` (một box 0,398 bị bỏ đúng luật); 0/30 ảnh người ra `monkey` (ảnh người bơi 0,49 hết nhầm); 30 ảnh đời thường và 4 ảnh mẫu không đổi; 8/10 ảnh có cả người và khỉ còn `monkey` (chưa có bản Java không có lớp bảo vệ để đối chiếu; Python: mất 4 khỉ trước và sau luật). Đo trên VPS: xem `docs/deploy.md` mục 7.

Ca còn sót sau khi cài: ảnh cận mặt người (`monkey` 0,885) và khung bóng đen chữ YOGA (0,83) không luật nào dựa trên box `person` bắt được; recall khỉ trên `test_monkey` giảm 491 → 461 trên 551 khung (chủ yếu do ngưỡng 0,5).
