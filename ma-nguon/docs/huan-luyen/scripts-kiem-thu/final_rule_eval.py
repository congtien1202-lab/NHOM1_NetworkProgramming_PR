"""Danh gia luat cuoi: Tm=0.5; veto boi YOLOv8s goc: person conf>=0.2 va IoU>=0.5 -> bo monkey. In bang truoc/sau va cac ca con sot / ca khi that bi mat."""
import json, os
import rules as R
import rules2 as R2

NO = (0.3, None, None, None, None, None, None)
FINAL = (0.5, None, None, None, 0.2, 0.5, None)
TMONLY = (0.5, None, None, None, None, None, None)
out = {}
for name, r in (("khong luat", NO), ("chi Tm=0.5", TMONLY), ("luat cuoi (Tm=0.5 + veto YOLO goc P=0.2,U=0.5)", FINAL)):
    x = R2.eval_rule(r)
    # tong box person va monkey sai tren ca 2 nua COCO
    A = R2.coco_m("A", R.half["A"], r); B = R2.coco_m("B", R.half["B"], r)
    x["A_person_box"] = A[2]
    out[name] = x
    print(name, json.dumps({k: v for k, v in x.items() if k != "rule"}, ensure_ascii=False), flush=True)

# ca con sot tren COCO (ca 4 nua A+B) va OI Person sau luat cuoi
rem = []
for i, f, dets, gts in R.coco_items:
    mk = R2.keep(dets, R2.O["coco"][f]["dets"], FINAL)
    for g in gts:
        for m in mk:
            if R.iou(g, m) >= 0.5:
                rem.append(("COCO", f, "nua " + ("A" if i % 2 == 0 else "B"), m[5]))
for i, f, dets, gts in R.oi_items:
    mk = R2.keep(dets, R2.O["oi_person"][f]["dets"], FINAL)
    for g in gts:
        for m in mk:
            if R.iou(g, m) >= 0.5:
                rem.append(("OI Person", f, "", m[5]))
print("CA CON SOT:", rem)
out["ca_con_sot"] = rem

# khi that bi mat do luat: tren val+test monkey, tach theo anh co nguoi that (YOLO goc person>=0.5 khong trung box khi GT IoU<0.3)
lost = []; same_img = {"anh": 0, "box_khi": 0, "mat_truoc_luat": 0, "mat_sau_luat": 0}
for sp, items in (("val", R.val_m), ("test", R.test_m)):
    for f, dets, gts in items:
        d0 = R2.O[sp + "_monkey"][f]["dets"]
        k0 = R2.keep(dets, d0, NO); k1 = R2.keep(dets, d0, FINAL)
        people = [d for d in d0 if d[4] == "person" and d[5] >= 0.5 and all(R.iou(d, g) < 0.3 and R.cover(g, d) < 0.8 for g in gts)]
        for g in gts:
            h0 = any(R.iou(g, m) >= 0.5 for m in k0); h1 = any(R.iou(g, m) >= 0.5 for m in k1)
            if h0 and not h1:
                lost.append((sp, f))
        if people and gts:
            same_img["anh"] += 1; same_img["box_khi"] += len(gts)
            same_img["mat_truoc_luat"] += sum(1 for g in gts if not any(R.iou(g, m) >= 0.5 for m in k0))
            same_img["mat_sau_luat"] += sum(1 for g in gts if not any(R.iou(g, m) >= 0.5 for m in k1))
print("khi that bi mat boi luat (tren val+test):", len(lost))
print("anh co nguoi that + khi:", same_img)
out["khi_that_bi_mat"] = len(lost); out["anh_nguoi_va_khi"] = same_img
json.dump(out, open(os.path.join(R.IND, "final_rule.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1, default=str)
