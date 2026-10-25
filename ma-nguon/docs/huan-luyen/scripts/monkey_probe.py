"""Tren tap anh khi (OI val/test): mo hinh gan nhan gi cho vung co khi?
Dung: monkey_probe.py <weights> <list.txt> <ten> [conf]
Voi moi box khi (GT, lop 80) lay du doan co IoU>=0.3 voi box do va diem cao nhat; dem theo ten lop.
Dem them: so anh co it nhat 1 'person' (conf>=conf) va so box khi bi 'person' bao trum (IoU>=0.3).
"""
import json, os, sys
from collections import Counter
from ultralytics import YOLO

w, lst, tag = sys.argv[1], sys.argv[2], sys.argv[3]
conf = float(sys.argv[4]) if len(sys.argv) > 4 else 0.3
m = YOLO(w)
imgs = [l for l in open(lst).read().split("\n") if l]


def iou(a, b):
    iw, ih = max(0, min(a[2], b[2]) - max(a[0], b[0])), max(0, min(a[3], b[3]) - max(a[1], b[1]))
    i = iw * ih
    u = (a[2] - a[0]) * (a[3] - a[1]) + (b[2] - b[0]) * (b[3] - b[1]) - i
    return i / u if u > 0 else 0


top = Counter(); n_box = 0; missed = 0; imgs_person = 0; boxes_person = 0
for p in imgs:
    lab = p.replace("\\images\\", "\\labels\\").replace("/images/", "/labels/")
    lab = os.path.splitext(lab)[0] + ".txt"
    gts = []
    for ln in open(lab).read().split("\n"):
        q = ln.split()
        if len(q) == 5 and int(q[0]) == 80:
            cx, cy, bw, bh = map(float, q[1:])
            gts.append((cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2))
    r = m.predict(p, conf=conf, imgsz=640, verbose=False, device=0)[0]
    preds = [(b, int(c), float(s)) for b, c, s in zip(r.boxes.xyxyn.tolist(), r.boxes.cls.tolist(), r.boxes.conf.tolist())]
    names = m.names
    if any(names[c] == "person" for _, c, _ in preds):
        imgs_person += 1
    for g in gts:
        n_box += 1
        match = [(s, c) for b, c, s in preds if iou(b, g) >= 0.3]
        if not match:
            missed += 1
            top["(khong co du doan)"] += 1
            continue
        s, c = max(match)
        top[names[c]] += 1
        if any(names[c2] == "person" for _, c2 in match):
            boxes_person += 1
res = {"tag": tag, "weights": w, "conf": conf, "images": len(imgs), "monkey_boxes": n_box,
       "images_with_person_pred": imgs_person, "monkey_boxes_covered_by_person": boxes_person,
       "top_label_per_monkey_box": dict(top.most_common())}
json.dump(res, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "probe_%s.json" % tag), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(json.dumps(res, ensure_ascii=False))
