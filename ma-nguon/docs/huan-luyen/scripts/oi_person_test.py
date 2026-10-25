"""Lay 30 anh co Person tu tap TEST cua Open Images V7 (de kiem tra tay: anh nguoi van ra person)."""
import os, shutil
import fiftyone as fo
import fiftyone.zoo as foz

fo.config.dataset_zoo_dir = r"C:\Users\Asus\tools\yolo-data\fiftyone"
ds = foz.load_zoo_dataset("open-images-v7", split="test", label_types=["detections"], classes=["Person"],
                          max_samples=30, seed=7, shuffle=True, dataset_name="oi_person_test", drop_existing_dataset=True)
out = r"C:\Users\Asus\tools\yolo-data\manual_person"
os.makedirs(out, exist_ok=True)
for s in ds:
    shutil.copy2(s.filepath, os.path.join(out, os.path.basename(s.filepath)))
print("xong", len(ds))
