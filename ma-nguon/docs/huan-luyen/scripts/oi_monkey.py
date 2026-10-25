"""Tải Open Images V7 lớp Monkey (có bounding box) bằng FiftyOne zoo, mỗi split riêng."""
import sys
import fiftyone as fo
import fiftyone.zoo as foz

fo.config.dataset_zoo_dir = r"C:\Users\Asus\tools\yolo-data\fiftyone"
out = {}
for split in ("train", "validation", "test"):
    ds = foz.load_zoo_dataset(
        "open-images-v7", split=split, label_types=["detections"], classes=["Monkey"],
        dataset_name="oi_monkey_" + split, drop_existing_dataset=True,
    )
    n_img = len(ds)
    n_box = sum(len(s.ground_truth.detections) for s in ds if s.ground_truth)
    print(split, "ảnh", n_img, "box", n_box, flush=True)
    ds.export(export_dir=r"C:\Users\Asus\tools\yolo-data\oi_monkey_raw\\" + split, dataset_type=fo.types.YOLOv5Dataset,
              label_field="ground_truth", classes=["Monkey"], split=split)
print("xong")
