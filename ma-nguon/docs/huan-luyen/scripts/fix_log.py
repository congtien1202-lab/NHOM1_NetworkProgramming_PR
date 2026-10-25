p = r"E:\LTMangMayTinh\DoAnTest\docs\huan-luyen\nhat-ky.md"
L = open(p, encoding="utf-8").read().split("\n")
i3 = next(i for i, l in enumerate(L) if l.startswith("| 3 | **40000 COCO**"))
row = L.pop(i3)
# bỏ dòng trống thừa sau khi rút dòng
if L[i3 - 1] == "" and L[i3] == "":
    L.pop(i3)
i2 = next(i for i, l in enumerate(L) if l.startswith("| 2 | `freeze=10`"))
L.insert(i2 + 1, row)
open(p, "w", encoding="utf-8", newline="\n").write("\n".join(L))
print("ok", i3, i2)
