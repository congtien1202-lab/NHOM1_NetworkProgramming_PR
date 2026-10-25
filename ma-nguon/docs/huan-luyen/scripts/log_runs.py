"""Them vao nhat-ky.md bang so do tung epoch cua cac lan train (doc tu results.csv). Dung: log_runs.py"""
import csv, sys

LOG = r"E:\LTMangMayTinh\DoAnTest\docs\huan-luyen\nhat-ky.md"
ROOT = r"C:\Users\Asus\tools\yolo-data\runs"
RUNS = [("Lần 1 (lr0 0,003, không đóng băng, 12000 COCO + 2143 khỉ)", "ft81_lr003_stopped"),
        ("Lần 2 (freeze 10, lr0 0,001, 12000 COCO + 2143 khỉ)", "ft81_run2_freeze10_lr001")]


def rows(p):
    out = []
    for r in csv.DictReader(open(p)):
        r = {k.strip(): v for k, v in r.items()}
        out.append((int(r["epoch"]), float(r["time"]), float(r["metrics/mAP50(B)"]), float(r["metrics/mAP50-95(B)"]), float(r["lr/pg0"])))
    return out


md = "\n### 4.1 Số đo từng epoch trên val gộp (1155 ảnh: 1000 COCO + 155 khỉ)\n\nDo Ultralytics ghi trong `results.csv`; đây là val gộp có cả ảnh khỉ nên KHÔNG so trực tiếp với baseline 0,4575 (baseline chỉ đo trên COCO val). Số so sánh chính thức là cột val_coco ở bảng mục 4.\n\n"
for name, d in RUNS:
    rs = rows(ROOT + "\\" + d + "\\results.csv")
    md += "**%s** — %d epoch:\n\n| Epoch | Thời gian tích lũy (s) | mAP50 | mAP50-95 | lr |\n|---|---|---|---|---|\n" % (name, len(rs))
    for e, t, a, b, lr in rs:
        md += "| %d | %.0f | %.3f | %.3f | %.5f |\n" % (e, t, a, b, lr)
    md += "\n"
with open(LOG, "a", encoding="utf-8", newline="\n") as f:
    f.write(md)
print(md[:500])
