"""Kiem tra tay: 10 anh khi (OI test, ngau nhien seed 3) + 10 anh nguoi (OI test). In nhan du doan (conf>=0.3) cho tung anh,
ve khung len anh, ghep thanh luoi 5x4 luu thanh PNG. Dung: manual_check.py <weights> <ten> [thu_muc_ra]"""
import glob, json, os, random, sys
from PIL import Image, ImageDraw
from ultralytics import YOLO

ROOT = r"C:\Users\Asus\tools\yolo-data"
w, tag = sys.argv[1], sys.argv[2]
outdir = sys.argv[3] if len(sys.argv) > 3 else os.path.join(ROOT, "manual_out")
os.makedirs(outdir, exist_ok=True)
random.seed(3)
monk = [l for l in open(os.path.join(ROOT, "yolo81b", "test_monkey.txt")).read().split("\n") if l]
monk = random.sample(monk, 10)
pers = sorted(glob.glob(os.path.join(ROOT, "manual_person", "*.jpg")))[:10]
m = YOLO(w)
res = {"weights": w, "monkey": [], "person": []}
tiles = []
for kind, files in (("monkey", monk), ("person", pers)):
    for p in files:
        r = m.predict(p, conf=0.3, imgsz=640, verbose=False, device=0)[0]
        labs = sorted([(m.names[int(c)], round(float(s), 2)) for c, s in zip(r.boxes.cls.tolist(), r.boxes.conf.tolist())], key=lambda x: -x[1])
        res[kind].append({"file": os.path.basename(p), "preds": labs})
        im = Image.fromarray(r.plot()[..., ::-1]).convert("RGB")
        im.thumbnail((320, 240))
        t = Image.new("RGB", (320, 262), "white")
        t.paste(im, ((320 - im.width) // 2, 0))
        ImageDraw.Draw(t).text((4, 244), "%s %d" % (kind, len(res[kind])), fill=(0, 0, 0))
        tiles.append(t)
grid = Image.new("RGB", (320 * 5, 262 * 4), "white")
for i, t in enumerate(tiles):
    grid.paste(t, ((i % 5) * 320, (i // 5) * 262))
grid.save(os.path.join(outdir, "kiem-tra-tay-%s.jpg" % tag), quality=85)
ok_m = sum(1 for d in res["monkey"] if any(n == "monkey" for n, _ in d["preds"]) and not any(n in ("person", "bear", "dog") for n, _ in d["preds"]))
ok_p = sum(1 for d in res["person"] if any(n == "person" for n, _ in d["preds"]))
res["monkey_ok_(co monkey, khong co person/bear/dog)"] = ok_m
res["person_ok_(co person)"] = ok_p
json.dump(res, open(os.path.join(outdir, "manual_%s.json" % tag), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
for k in ("monkey", "person"):
    for d in res[k]:
        print(k, d["file"], d["preds"][:5])
print("monkey_ok", ok_m, "/10 ; person_ok", ok_p, "/10")
