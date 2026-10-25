"""Danh gia mot trong so tren mot yaml eval: mAP50-95, mAP50 va AP cua lop monkey (neu co). Ghi JSON.
Dung: evaluate.py <weights> <yaml> <ten_ket_qua> [imgsz]
(workers=0: tren Windows workers>0 bi treo.)
"""
import json, os, sys
from ultralytics import YOLO

w, y, tag = sys.argv[1], sys.argv[2], sys.argv[3]
imgsz = int(sys.argv[4]) if len(sys.argv) > 4 else 640
m = YOLO(w)
r = m.val(data=y, imgsz=imgsz, batch=32, conf=0.001, iou=0.7, device=0, plots=False, verbose=False, workers=0)
out = {"weights": w, "yaml": y, "map50_95": float(r.box.map), "map50": float(r.box.map50), "n_classes_with_gt": len(r.box.ap_class_index)}
per = {}
for i, c in enumerate(r.box.ap_class_index):
    per[m.names[int(c)] if int(c) in m.names else int(c)] = {"ap50_95": float(r.box.all_ap[i].mean()), "ap50": float(r.box.all_ap[i][0])}
out["per_class"] = per
if "monkey" in per:
    out["monkey_ap50"] = per["monkey"]["ap50"]
json.dump(out, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "eval_%s.json" % tag), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("KET QUA", tag, "mAP50-95=%.4f mAP50=%.4f lop_co_GT=%d" % (out["map50_95"], out["map50"], out["n_classes_with_gt"]), "monkey_AP50=%s" % out.get("monkey_ap50"))
