import csv
p = r"E:\LTMangMayTinh\DoAnTest\docs\huan-luyen\nhat-ky.md"
rs = []
for r in csv.DictReader(open(r"C:\Users\Asus\tools\yolo-data\runs\ft81c\results.csv")):
    r = {k.strip(): v for k, v in r.items()}
    rs.append((int(r["epoch"]), float(r["time"]), float(r["metrics/mAP50(B)"]), float(r["metrics/mAP50-95(B)"]), float(r["lr/pg0"])))
md = "**Lần 3 (freeze 10, lr0 0,001, 40000 COCO + 2143 khỉ)** — %d epoch (val gộp, không so trực tiếp với baseline 0,4575):\n\n| Epoch | Thời gian tích lũy (s) | mAP50 | mAP50-95 | lr |\n|---|---|---|---|---|\n" % len(rs)
for e, t, a, b, lr in rs:
    md += "| %d | %.0f | %.3f | %.3f | %.5f |\n" % (e, t, a, b, lr)
md += "\n"
s = open(p, encoding="utf-8").read()
k = s.index("## 5. Kết quả cuối")
s = s[:k] + md + s[k:]
open(p, "w", encoding="utf-8", newline="\n").write(s)
print("ok")
