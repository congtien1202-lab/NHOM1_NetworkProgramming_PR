"""Thu luat hau xu ly co them 'veto' bang mo hinh YOLOv8s goc (80 lop). Chon tham so tren COCO nua A + OI val_monkey, bao cao nua B, OI Person, OI test_monkey.
Luat (ap dung sau NMS, chi bo box monkey):
  Tm: bo monkey neu conf < Tm
  (b) bo monkey neu co box person CUA MO HINH 81 LOP voi conf >= P va (IoU >= U hoac inter/area(monkey) >= C)
  (v) bo monkey neu co box person CUA YOLOv8s GOC voi conf >= Pv va (IoU >= Uv hoac inter/area(monkey) >= Cv)"""
import itertools, json, os
import numpy as np
import rules as R

IND = R.IND
O = {k: R.L("orig_" + k) for k in ("coco", "oi_person", "manual_person", "val_monkey", "test_monkey")}
S81 = {"coco": R.coco, "oi_person": R.oip, "manual_person": R.L("manual_person"), "val_monkey": R.L("val_monkey"), "test_monkey": R.L("test_monkey")}


def veto(m, persons, thr_iou, thr_cov):
    for p in persons:
        if (thr_iou is not None and R.iou(m, p) >= thr_iou) or (thr_cov is not None and R.cover(m, p) >= thr_cov):
            return True
    return False


def keep(d81, dorig, rule):
    Tm, P, U, C, Pv, Uv, Cv = rule
    pers = [d for d in d81 if d[4] == "person" and d[5] >= (P if P is not None else 9)]
    pers_o = [d for d in dorig if d[4] == "person" and d[5] >= (Pv if Pv is not None else 9)]
    out = []
    for d in d81:
        if d[4] != "monkey" or d[5] < Tm:
            continue
        if veto(d, pers, U, C) or veto(d, pers_o, Uv, Cv):
            continue
        out.append(d)
    return out


def coco_m(key, items, rule):
    pm = tot = nb = 0
    for i, f, dets, gts in items:
        mk = keep(dets, O["coco"][f]["dets"], rule)
        tot += len(mk)
        for g in gts:
            nb += 1
            pm += any(R.iou(g, m) >= 0.5 for m in mk)
    return pm, tot, nb


def oi_person_m(rule):
    pm = nb = 0
    for i, f, dets, gts in R.oi_items:
        mk = keep(dets, O["oi_person"][f]["dets"], rule)
        for g in gts:
            nb += 1
            pm += any(R.iou(g, m) >= 0.5 for m in mk)
    return pm, nb


def manual_m(rule):
    n = 0
    for f, e in S81["manual_person"].items():
        n += len(keep(e["dets"], O["manual_person"][f]["dets"], rule))
    return n


def ap_m(items, key, rule):
    recs = []; npos = 0; hit = 0
    for f, dets, gts in items:
        npos += len(gts)
        mk = [d for d in dets if d[4] == "monkey"]
        # ap: dung moi box monkey conf>=0.001 nhung ap dung luat (Tm chi tren nguong >=0.3 cho van hanh thuc te)
        kept = keep(dets, O[key][f]["dets"], (0.001,) + rule[1:])
        kept = [d for d in kept if d[5] >= rule[0] or rule[0] <= 0.3 and False]
        kept.sort(key=lambda d: -d[5])
        used = [False] * len(gts)
        for d in kept:
            best, bj = 0, -1
            for j, g in enumerate(gts):
                v = R.iou(d, g)
                if v > best and not used[j]:
                    best, bj = v, j
            if best >= 0.5:
                used[bj] = True; recs.append((d[5], 1))
            else:
                recs.append((d[5], 0))
        ope = keep(dets, O[key][f]["dets"], rule)
        for g in gts:
            hit += any(R.iou(g, m) >= 0.5 for m in ope)
    recs.sort(key=lambda r: -r[0])
    if not recs:
        return 0.0, hit, npos
    tp = np.cumsum([r[1] for r in recs]); fp = np.cumsum([1 - r[1] for r in recs])
    rec = tp / npos; prec = tp / np.maximum(tp + fp, 1e-9)
    mrec = np.concatenate(([0.0], rec, [1.0])); mpre = np.concatenate(([1.0], prec, [0.0]))
    mpre = np.flip(np.maximum.accumulate(np.flip(mpre)))
    x = np.linspace(0, 1, 101)
    return float(np.trapezoid(np.interp(x, mrec, mpre), x)), hit, npos


def monkey_items(sp):
    return R.val_m if sp == "val" else R.test_m


def eval_rule(rule):
    A = coco_m("A", R.half["A"], rule); B = coco_m("B", R.half["B"], rule)
    op = oi_person_m(rule)
    vap, vh, vn = ap_m(R.val_m, "val_monkey", rule)
    tap, th, tn = ap_m(R.test_m, "test_monkey", rule)
    return {"rule": rule, "A_nham": A[0], "A_monkey_con": A[1], "B_nham": B[0], "B_monkey_con": B[1], "B_person": B[2],
            "oi_person_nham": op[0], "oi_person_box": op[1], "manual_monkey": manual_m(rule),
            "val_ap50": round(vap, 4), "val_recall": [vh, vn], "test_ap50": round(tap, 4), "test_recall": [th, tn]}


if __name__ == "__main__":
    # luat ung vien (Tm, P, U, C, Pv, Uv, Cv)
    cands = {"0 khong luat": (0.3, None, None, None, None, None, None)}
    for Tm in (0.4, 0.5, 0.6):
        cands["Tm=%.1f" % Tm] = (Tm, None, None, None, None, None, None)
    for P, U in itertools.product((0.05, 0.1, 0.3), (0.3, 0.5, 0.7)):
        cands["Tm0.5 + b(P=%.2f,U=%.1f)" % (P, U)] = (0.5, P, U, None, None, None, None)
    for P, U, C in itertools.product((0.1, 0.3), (0.5,), (0.7, 0.9)):
        cands["Tm0.5 + b(P=%.2f,U=%.1f,C=%.1f)" % (P, U, C)] = (0.5, P, U, C, None, None, None)
    for Pv, Uv, Cv in itertools.product((0.2, 0.3, 0.5), (0.3, 0.5), (None, 0.8)):
        cands["V(Pv=%.1f,Uv=%.1f,Cv=%s)" % (Pv, Uv, Cv)] = (0.3, None, None, None, Pv, Uv, Cv)
        cands["Tm0.5 + V(Pv=%.1f,Uv=%.1f,Cv=%s)" % (Pv, Uv, Cv)] = (0.5, None, None, None, Pv, Uv, Cv)
    res = {}
    for name, r in cands.items():
        res[name] = eval_rule(r)
        x = res[name]
        print("%-40s A %d/%d B %d/%d OIp %d/%d man %d | val AP %.3f rec %d/%d | test AP %.3f rec %d/%d" % (
            name, x["A_nham"], x["A_monkey_con"], x["B_nham"], x["B_monkey_con"], x["oi_person_nham"], x["oi_person_box"], x["manual_monkey"],
            x["val_ap50"], x["val_recall"][0], x["val_recall"][1], x["test_ap50"], x["test_recall"][0], x["test_recall"][1]), flush=True)
    json.dump(res, open(os.path.join(IND, "rules2_results.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1, default=str)
