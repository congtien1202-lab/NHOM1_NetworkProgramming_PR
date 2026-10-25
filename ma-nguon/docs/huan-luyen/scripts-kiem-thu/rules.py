"""Thu luat hau xu ly loai box monkey sau NMS (chi bo box monkey nen mAP 80 lop COCO khong doi).
Luat: bo box monkey m neu
  (a) conf(m) < Tm, hoac
  (b) ton tai box person p (conf >= P) voi IoU(m,p) >= U, hoac
  (c) ton tai box person p (conf >= P) voi inter(m,p)/area(m) >= C.
Chon tham so tren COCO val2017 nua A (image_id chan) + OI val_monkey; bao cao tren nua B (le), OI Person, OI test_monkey."""
import itertools, json, os
import numpy as np

ROOT = r"C:\Users\Asus\tools\yolo-data"
IND = os.path.join(ROOT, "indep")


def L(n):
    return json.load(open(os.path.join(IND, "preds_%s.json" % n), encoding="utf-8"))


def iou(a, b):
    iw = max(0, min(a[2], b[2]) - max(a[0], b[0])); ih = max(0, min(a[3], b[3]) - max(a[1], b[1]))
    i = iw * ih; u = (a[2] - a[0]) * (a[3] - a[1]) + (b[2] - b[0]) * (b[3] - b[1]) - i
    return i / u if u > 0 else 0


def cover(m, p):  # inter / area(m)
    iw = max(0, min(m[2], p[2]) - max(m[0], p[0])); ih = max(0, min(m[3], p[3]) - max(m[1], p[1]))
    a = (m[2] - m[0]) * (m[3] - m[1])
    return iw * ih / a if a > 0 else 0


def keep_monkey(dets, rule):
    """tra ve danh sach box monkey con lai (dets: [x1,y1,x2,y2,ten,conf])."""
    Tm, P, U, C = rule
    persons = [d for d in dets if d[4] == "person" and d[5] >= (P if P is not None else 2)]
    out = []
    for d in dets:
        if d[4] != "monkey" or d[5] < Tm:
            continue
        if U is not None and any(iou(d, p) >= U for p in persons):
            continue
        if C is not None and any(cover(d, p) >= C for p in persons):
            continue
        out.append(d)
    return out


# ---------- COCO
coco = L("coco")
ann = json.load(open(os.path.join(ROOT, "coco_raw", "annotations", "instances_val2017.json"), encoding="utf-8"))
cat = {c["id"]: c["name"] for c in ann["categories"]}
imgid = {i["file_name"]: i["id"] for i in ann["images"]}
gt_person = {}
for a in ann["annotations"]:
    if not a.get("iscrowd") and cat[a["category_id"]] == "person":
        x, y, w, h = a["bbox"]
        gt_person.setdefault(a["image_id"], []).append([x, y, x + w, y + h])
coco_items = []
for f, e in coco.items():
    i = imgid[f]
    coco_items.append((i, f, e["dets"], gt_person.get(i, [])))
half = {"A": [x for x in coco_items if x[0] % 2 == 0], "B": [x for x in coco_items if x[0] % 2 == 1]}


def coco_metrics(items, rule):
    pm = 0; tot_m = 0; person_boxes = 0
    for i, f, dets, gts in items:
        mk = keep_monkey(dets, rule)
        tot_m += len(mk)
        for g in gts:
            person_boxes += 1
            if any(iou(g, m) >= 0.5 for m in mk):
                pm += 1
    return {"person_box": person_boxes, "person_nham_monkey": pm, "du_doan_monkey_con_lai": tot_m}


# ---------- OI person
oip = L("oi_person")
oi_items = []
for f, e in oip.items():
    W, H = e["size"]
    gts = [[g[0] * W, g[1] * H, g[2] * W, g[3] * H] for g in e["gt_person_norm"]]
    oi_items.append((0, f, e["dets"], gts))

# ---------- OI monkey (AP)
def load_monkey(sp):
    d = L(sp + "_monkey")
    lst = [l for l in open(os.path.join(ROOT, "yolo81b", sp + "_monkey.txt")).read().split("\n") if l]
    items = []
    for p in lst:
        f = os.path.basename(p)
        e = d[f]; W, H = e["size"]
        lab = os.path.splitext(p.replace("\\images\\", "\\labels\\"))[0] + ".txt"
        gts = []
        for ln in open(lab).read().split("\n"):
            q = ln.split()
            if len(q) == 5 and int(q[0]) == 80:
                cx, cy, w, h = [float(v) for v in q[1:]]
                gts.append([(cx - w / 2) * W, (cy - h / 2) * H, (cx + w / 2) * W, (cy + h / 2) * H])
        items.append((f, e["dets"], gts))
    return items


def ap50(items, rule, conf_floor=0.001):
    recs = []; npos = 0
    for f, dets, gts in items:
        npos += len(gts)
        mk = keep_monkey([d for d in dets if d[5] >= conf_floor], (conf_floor if rule[0] < conf_floor else rule[0], rule[1], rule[2], rule[3]))
        mk.sort(key=lambda d: -d[5])
        used = [False] * len(gts)
        for d in mk:
            best, bj = 0, -1
            for j, g in enumerate(gts):
                v = iou(d, g)
                if v > best and not used[j]:
                    best, bj = v, j
            if best >= 0.5:
                used[bj] = True; recs.append((d[5], 1))
            else:
                recs.append((d[5], 0))
    recs.sort(key=lambda r: -r[0])
    tp = np.cumsum([r[1] for r in recs]); fp = np.cumsum([1 - r[1] for r in recs])
    if npos == 0 or len(recs) == 0:
        return 0.0, 0, 0
    rec = tp / npos; prec = tp / np.maximum(tp + fp, 1e-9)
    mrec = np.concatenate(([0.0], rec, [1.0])); mpre = np.concatenate(([1.0], prec, [0.0]))
    mpre = np.flip(np.maximum.accumulate(np.flip(mpre)))
    x = np.linspace(0, 1, 101)
    ap = float(np.trapezoid(np.interp(x, mrec, mpre), x))
    return ap, int(tp[-1]), npos


def recall_at(items, rule):
    """So box khi GT duoc nhan dung monkey (IoU>=0.5) voi cac du doan con lai (conf>=Tm)."""
    hit = 0; n = 0
    for f, dets, gts in items:
        mk = keep_monkey([d for d in dets if d[5] >= rule[0]], rule)
        for g in gts:
            n += 1
            hit += any(iou(g, m) >= 0.5 for m in mk)
    return hit, n


val_m = load_monkey("val"); test_m = load_monkey("test")
NO = (0.3, None, None, None)

if __name__ == "__main__":
    out = {"baseline_khong_luat": {}}
    # phan tich 8 ca
    cases = {}
    for f, e in coco.items():
        mk = [d for d in e["dets"] if d[4] == "monkey" and d[5] >= 0.3]
        gts = gt_person.get(imgid[f], [])
        for m_ in mk:
            if any(iou(g, m_) >= 0.5 for g in gts):
                pers = [d for d in e["dets"] if d[4] == "person"]
                cases["COCO " + f] = {"monkey": m_[5], "person_preds": [(round(iou(m_, p), 2), round(cover(m_, p), 2), p[5]) for p in pers]}
    for f, e in oip.items():
        mk = [d for d in e["dets"] if d[4] == "monkey" and d[5] >= 0.3]
        W, H = e["size"]
        gts = [[g[0] * W, g[1] * H, g[2] * W, g[3] * H] for g in e["gt_person_norm"]]
        for m_ in mk:
            if any(iou(g, m_) >= 0.5 for g in gts):
                pers = [d for d in e["dets"] if d[4] == "person"]
                cases["OI " + f] = cases.get("OI " + f, []) if False else {"monkey": m_[5], "person_preds": [(round(iou(m_, p), 2), round(cover(m_, p), 2), p[5]) for p in pers]}
    for f, e in L("manual_person").items():
        mk = [d for d in e["dets"] if d[4] == "monkey" and d[5] >= 0.3]
        for m_ in mk:
            pers = [d for d in e["dets"] if d[4] == "person"]
            cases["OI-manual " + f] = {"monkey": m_[5], "person_preds": [(round(iou(m_, p), 2), round(cover(m_, p), 2), p[5]) for p in pers]}
    out["cases"] = cases
    for k, v in cases.items():
        print(k, v)
    # luoi tham so
    Tms = [0.3, 0.4, 0.5, 0.6, 0.7, 0.8]
    Ps = [0.05, 0.1, 0.15, 0.2, 0.3, 0.5]
    Us = [None, 0.3, 0.5, 0.7]
    Cs = [None, 0.7, 0.9]
    rows = []
    for Tm, P, U, C in itertools.product(Tms, Ps, Us, Cs):
        if U is None and C is None and P != Ps[0]:
            continue
        r = (Tm, P, U, C)
        A = coco_metrics(half["A"], r); B = coco_metrics(half["B"], r)
        apv, tpv, npv = ap50(val_m, r)
        rows.append({"rule": r, "A": A, "B": B, "val_ap50": round(apv, 4), "val_recall": [tpv, npv]})
    out["grid"] = rows
    base_val = ap50(val_m, NO)[0]; base_test = ap50(test_m, NO)
    print("AP50 khong luat: val %.4f test %.4f (%d/%d)" % (base_val, base_test[0], base_test[1], base_test[2]))
    out["baseline_ap"] = {"val": base_val, "test": base_test[0]}
    json.dump(out, open(os.path.join(IND, "rules_grid.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1, default=str)
    print("so to hop:", len(rows))
