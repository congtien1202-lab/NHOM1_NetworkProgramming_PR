"""Gá»™p COCO subset + Open Images Monkey thĂ nh dataset 81 lá»›p (lá»›p 80 = monkey).

áº¢nh khá»‰ cá»§a Open Images chá»‰ cĂ³ nhĂ£n Monkey; cĂ¡c váº­t khĂ¡c trong áº£nh (ngÆ°á»i, xe...) khĂ´ng Ä‘Æ°á»£c gĂ¡n nhĂ£n. Náº¿u Ä‘á»ƒ nguyĂªn,
mĂ´ hĂ¬nh há»c "áº£nh khá»‰ thĂ¬ khĂ´ng cĂ³ ngÆ°á»i" vĂ  quĂªn cáº£ lá»›p person. VĂ¬ váº­y gĂ¡n nhĂ£n giáº£ cho 80 lá»›p COCO báº±ng YOLOv8s gá»‘c
(conf >= 0.5) vĂ  Bá» má»i dá»± Ä‘oĂ¡n chá»“ng lĂªn box khá»‰ (IoU > 0.1 hoáº·c tĂ¢m náº±m trong box khá»‰), Ä‘á»ƒ khá»‰ khĂ´ng bá»‹ gĂ¡n person.
"""
import glob, json, os, shutil, sys
from ultralytics import YOLO

ROOT = r"C:\Users\Asus\tools\yolo-data"
COCO = os.path.join(ROOT, "coco_sub")
OI = os.path.join(ROOT, "oi_monkey_raw")
OUT = os.path.join(ROOT, os.environ.get("OUT_DIR", "yolo81"))
PSEUDO_CONF = 0.5
MONKEY = 80
names = json.load(open(os.path.join(COCO, "names.json"), encoding="utf-8"))["names"]
names = {int(k): v for k, v in names.items()}
names[MONKEY] = "monkey"


def iou(a, b):
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    iw, ih = max(0, min(ax2, bx2) - max(ax1, bx1)), max(0, min(ay2, by2) - max(ay1, by1))
    inter = iw * ih
    u = (ax2 - ax1) * (ay2 - ay1) + (bx2 - bx1) * (by2 - by1) - inter
    return inter / u if u > 0 else 0


def xyxy(cx, cy, w, h):
    return (cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)


for sp in ("train", "val", "test"):
    for k in ("images", "labels"):
        os.makedirs(os.path.join(OUT, k, sp), exist_ok=True)

lists = {k: [] for k in ("train", "val", "test", "val_coco", "val_monkey", "test_coco", "test_monkey")}
# 1) COCO subset: chĂ©p sang
for sp in ("train", "val", "test"):
    for img in glob.glob(os.path.join(COCO, "images", sp, "*.jpg")):
        base = os.path.splitext(os.path.basename(img))[0]
        dst = os.path.join(OUT, "images", sp, base + ".jpg")
        if not os.path.exists(dst):
            shutil.copy2(img, dst)
        shutil.copy2(os.path.join(COCO, "labels", sp, base + ".txt"), os.path.join(OUT, "labels", sp, base + ".txt"))
        lists[sp].append(dst)
        if sp != "train":
            lists[sp + "_coco"].append(dst)
print("COCO:", {k: len(v) for k, v in lists.items() if v}, flush=True)

# 2) Open Images Monkey + nhĂ£n giáº£
model = YOLO(os.path.join(ROOT, "yolov8s.pt"))
stats = {"images": {}, "monkey_boxes": {}, "pseudo_boxes": {}, "dropped_overlap": {}}
for oisp, sp in (("train", "train"), ("validation", "val"), ("test", "test")):
    imgs = sorted(glob.glob(os.path.join(OI, oisp, "images", "*", "*.jpg")) + glob.glob(os.path.join(OI, oisp, "images", "*.jpg")))
    n_m = n_p = n_d = 0
    for img in imgs:
        base = os.path.splitext(os.path.basename(img))[0]
        lab = img.replace(os.sep + "images" + os.sep, os.sep + "labels" + os.sep)
        lab = os.path.splitext(lab)[0] + ".txt"
        monkeys = []
        if os.path.exists(lab):
            for ln in open(lab).read().split("\n"):
                p = ln.split()
                if len(p) == 5:
                    monkeys.append(tuple(map(float, p[1:])))
        if not monkeys:
            continue
        res = model.predict(img, conf=PSEUDO_CONF, imgsz=640, verbose=False, device=0)[0]
        lines = ["%d %.6f %.6f %.6f %.6f" % ((MONKEY,) + m) for m in monkeys]
        n_m += len(monkeys)
        mb = [xyxy(*m) for m in monkeys]
        for box, cls in zip(res.boxes.xywhn.tolist(), res.boxes.cls.tolist()):
            b = xyxy(*box)
            cxy = (box[0], box[1])
            hit = any(iou(b, m) > 0.1 or (m[0] <= cxy[0] <= m[2] and m[1] <= cxy[1] <= m[3]) for m in mb)
            if hit:
                n_d += 1
                continue
            lines.append("%d %.6f %.6f %.6f %.6f" % ((int(cls),) + tuple(box)))
            n_p += 1
        dst = os.path.join(OUT, "images", sp, "oi_" + base + ".jpg")
        shutil.copy2(img, dst)
        open(os.path.join(OUT, "labels", sp, "oi_" + base + ".txt"), "w").write("\n".join(lines))
        lists[sp].append(dst)
        if sp != "train":
            lists[sp + "_monkey"].append(dst)
    stats["images"][sp] = len([1 for _ in imgs]); stats["monkey_boxes"][sp] = n_m
    stats["pseudo_boxes"][sp] = n_p; stats["dropped_overlap"][sp] = n_d
    print(oisp, "áº£nh", len(imgs), "box khá»‰", n_m, "nhĂ£n giáº£", n_p, "bá» vĂ¬ chá»“ng khá»‰", n_d, flush=True)

for k, v in lists.items():
    open(os.path.join(OUT, k + ".txt"), "w").write("\n".join(v))
yaml = "path: %s\ntrain: train.txt\nval: val.txt\ntest: test.txt\nnames:\n" % OUT.replace("\\", "/")
for i in range(81):
    yaml += "  %d: %s\n" % (i, names[i])
open(os.path.join(OUT, "data81.yaml"), "w", encoding="utf-8").write(yaml)
# yaml chá»‰ COCO val (80 lá»›p) vĂ  cĂ¡c táº­p riĂªng
for k in ("val_coco", "val_monkey", "test_coco", "test_monkey"):
    y = "path: %s\ntrain: train.txt\nval: %s.txt\nnames:\n" % (OUT.replace("\\", "/"), k)
    for i in range(81):
        y += "  %d: %s\n" % (i, names[i])
    open(os.path.join(OUT, "eval_%s.yaml" % k), "w", encoding="utf-8").write(y)
json.dump({"counts": {k: len(v) for k, v in lists.items()}, "oi": stats}, open(os.path.join(OUT, "stats.json"), "w"), indent=1)
print("xong dataset:", {k: len(v) for k, v in lists.items()}, flush=True)
