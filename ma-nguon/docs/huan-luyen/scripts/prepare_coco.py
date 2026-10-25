"""Dựng tập con COCO 2017 ở định dạng YOLO.
train: N_TRAIN ảnh lấy ngẫu nhiên từ train2017; val và test: lấy từ val2017 (không trùng nhau, không trùng train).
Nhãn: 80 lớp COCO theo thứ tự id tăng dần (0..79); bỏ box iscrowd=1.
"""
import json, os, random, sys, zipfile, urllib.request
from concurrent.futures import ThreadPoolExecutor

RAW = r"C:\Users\Asus\tools\yolo-data\coco_raw"
OUT = r"C:\Users\Asus\tools\yolo-data\coco_sub"
N_TRAIN, N_VAL, N_TEST = int(os.environ.get("N_TRAIN", 12000)), 1000, 1000
random.seed(20261022)

with zipfile.ZipFile(os.path.join(RAW, "annotations_trainval2017.zip")) as z:
    for n in ("annotations/instances_train2017.json", "annotations/instances_val2017.json"):
        if not os.path.exists(os.path.join(RAW, n)):
            z.extract(n, RAW)


def load(name):
    return json.load(open(os.path.join(RAW, "annotations", "instances_%s2017.json" % name), encoding="utf-8"))


train, val = load("train"), load("val")
cats = sorted(c["id"] for c in train["categories"])
cid = {c: i for i, c in enumerate(cats)}
names = {cid[c["id"]]: c["name"] for c in train["categories"]}


def index(d):
    anns = {}
    for a in d["annotations"]:
        if a.get("iscrowd"):
            continue
        anns.setdefault(a["image_id"], []).append(a)
    imgs = {i["id"]: i for i in d["images"]}
    return imgs, anns


def pick(d, n, exclude=()):
    imgs, anns = index(d)
    ids = sorted(i for i in imgs if i in anns and i not in exclude)
    random.shuffle(ids)
    return [(imgs[i], anns[i]) for i in ids[:n]]


sets = {}
sets["train"] = (pick(train, N_TRAIN), "train2017")
vsel = pick(val, N_VAL + N_TEST)
sets["val"] = (vsel[:N_VAL], "val2017")
sets["test"] = (vsel[N_VAL:], "val2017")


def job(args):
    split, folder, img, anns = args
    d_img = os.path.join(OUT, "images", split)
    d_lab = os.path.join(OUT, "labels", split)
    dst = os.path.join(d_img, img["file_name"])
    if not os.path.exists(dst):
        for k in range(4):
            try:
                urllib.request.urlretrieve("http://images.cocodataset.org/%s/%s" % (folder, img["file_name"]), dst)
                break
            except Exception as e:
                if k == 3:
                    raise
    w, h = img["width"], img["height"]
    lines = []
    for a in anns:
        x, y, bw, bh = a["bbox"]
        if bw <= 1 or bh <= 1:
            continue
        cx, cy = (x + bw / 2) / w, (y + bh / 2) / h
        lines.append("%d %.6f %.6f %.6f %.6f" % (cid[a["category_id"]], cx, cy, bw / w, bh / h))
    open(os.path.join(d_lab, os.path.splitext(img["file_name"])[0] + ".txt"), "w").write("\n".join(lines))
    return 1


for split in sets:
    os.makedirs(os.path.join(OUT, "images", split), exist_ok=True)
    os.makedirs(os.path.join(OUT, "labels", split), exist_ok=True)
tasks = [(sp, folder, img, anns) for sp, (items, folder) in sets.items() for img, anns in items]
print("tổng ảnh cần có:", len(tasks), {k: len(v[0]) for k, v in sets.items()}, flush=True)
done = 0
with ThreadPoolExecutor(24) as ex:
    for _ in ex.map(job, tasks):
        done += 1
        if done % 1000 == 0:
            print("đã xong", done, flush=True)
json.dump({"names": names}, open(os.path.join(OUT, "names.json"), "w", encoding="utf-8"), ensure_ascii=False)
print("xong COCO subset", flush=True)
