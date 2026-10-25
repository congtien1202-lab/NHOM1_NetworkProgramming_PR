"""Ghep anh OI (val+test) co nguoi that + khi: ve box khi con lai sau luat cuoi (xanh la), box khi bi luat bo (do), box person YOLO goc conf>=0.5 (xanh duong)."""
import os
from PIL import Image, ImageDraw
import rules as R
import rules2 as R2

FINAL = R2.FINAL if hasattr(R2, "FINAL") else (0.5, None, None, None, 0.2, 0.5, None)
NO = (0.3, None, None, None, None, None, None)
tiles = []
for sp, items in (("val", R.val_m), ("test", R.test_m)):
    lst = [l for l in open(os.path.join(R.ROOT, "yolo81b", sp + "_monkey.txt")).read().split("\n") if l]
    paths = {os.path.basename(p): p for p in lst}
    for f, dets, gts in items:
        d0 = R2.O[sp + "_monkey"][f]["dets"]
        people = [d for d in d0 if d[4] == "person" and d[5] >= 0.5 and all(R.iou(d, g) < 0.3 and R.cover(g, d) < 0.8 for g in gts)]
        if not (people and gts):
            continue
        k0 = R2.keep(dets, d0, NO); k1 = R2.keep(dets, d0, FINAL)
        im = Image.open(paths[f]).convert("RGB"); dr = ImageDraw.Draw(im); W = im.size[0]; lw = max(3, W // 200)
        for p in people:
            dr.rectangle(p[:4], outline=(0, 90, 220), width=lw)
        for m in k0:
            kept = any(m is x for x in k1)
            dr.rectangle(m[:4], outline=(0, 170, 60) if kept else (230, 0, 0), width=lw + 1)
        im.thumbnail((320, 240)); t = Image.new("RGB", (320, 244), "white"); t.paste(im, (0, 0)); tiles.append(t)
        if len(tiles) >= 10:
            break
    if len(tiles) >= 10:
        break
S = Image.new("RGB", (1600, 488), "white")
for i, t in enumerate(tiles):
    S.paste(t, ((i % 5) * 320, (i // 5) * 244))
S.save(os.path.join(R.IND, "nguoi_va_khi.jpg"), quality=85)
print("so anh", len(tiles), os.path.getsize(os.path.join(R.IND, "nguoi_va_khi.jpg")))
