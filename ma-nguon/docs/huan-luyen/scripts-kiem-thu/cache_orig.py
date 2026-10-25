"""Luu du doan (sau NMS cua Ultralytics, IoU 0.45) cua ban 81 lop de thu luat hau xu ly offline.
 - COCO val2017 (5000 anh), OI Person (400 anh da co trong fiftyone), 30 anh nguoi OI: conf>=0.3
 - OI val_monkey, test_monkey: conf>=0.001 (de tinh AP)
Moi du doan: [x1,y1,x2,y2 (px), ten lop, conf]. Luu pickle/JSON o indep/preds_*.json"""
import glob, json, os
import fiftyone as fo
from ultralytics import YOLO

ROOT = r"C:\Users\Asus\tools\yolo-data"
IND = os.path.join(ROOT, "indep")
m = YOLO(os.path.join(ROOT, "yolov8s.pt"))


def run(paths, conf):
    out = {}
    for k in range(0, len(paths), 32):
        rs = m.predict(paths[k:k + 32], conf=conf, iou=0.45, imgsz=640, verbose=False, device=0, max_det=300)
        for p, r in zip(paths[k:k + 32], rs):
            H, W = r.orig_shape
            out[os.path.basename(p)] = {"size": [int(W), int(H)], "dets": [[round(v, 1) for v in b] + [m.names[int(c)], round(float(s), 4)]
                                                                      for b, c, s in zip(r.boxes.xyxy.tolist(), r.boxes.cls.tolist(), r.boxes.conf.tolist())]}
    return out


def save(name, d):
    json.dump(d, open(os.path.join(IND, "preds_orig_%s.json" % name), "w"), ensure_ascii=False)
    print(name, len(d), flush=True)


def main():
    for sp in ("val", "test"):
        lst = [l for l in open(os.path.join(ROOT, "yolo81b", sp + "_monkey.txt")).read().split(chr(10)) if l]
        save(sp + "_monkey", run(lst, 0.05))
    save("coco", run(sorted(glob.glob(os.path.join(ROOT, "coco_raw", "val2017_all", "*.jpg"))), 0.05))
    save("manual_person", run(sorted(glob.glob(os.path.join(ROOT, "manual_person", "*.jpg"))), 0.05))
    for sp in ("val", "test"):
        lst = [l for l in open(os.path.join(ROOT, "yolo81b", sp + "_monkey.txt")).read().split("\n") if l]
        save(sp + "_monkey", run(lst, 0.001))
    # OI Person (400 anh, seed 21) trong dataset fiftyone da tao
    import fiftyone.zoo as foz
    fo.config.dataset_zoo_dir = os.path.join(ROOT, "fiftyone")
    ds = foz.load_zoo_dataset("open-images-v7", split="test", label_types=["detections"], classes=["Person"], max_samples=400, seed=21, shuffle=True, dataset_name="oi_person_big2", drop_existing_dataset=True)
    items = {}
    for s in ds:
        gts = []
        for d in (s.ground_truth.detections if s.ground_truth else []):
            if d.label == "Person" and not d.get_attribute_value("IsGroupOf", False):
                x, y, w, h = d.bounding_box
                gts.append([x, y, x + w, y + h])
        if gts:
            items[s.filepath] = gts
    preds = run(sorted(items), 0.05)
    for p, g in items.items():
        preds[os.path.basename(p)]["gt_person_norm"] = g
    save("oi_person", preds)


if __name__ == "__main__":
    main()
