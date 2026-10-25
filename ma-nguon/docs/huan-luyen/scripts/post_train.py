"""Sau khi train xong: danh gia, ma tran nham lan cua khi, kiem tra tay 10 anh khi + 10 anh nguoi, xuat ONNX + synset.
Dung: post_train.py <best.pt>
"""
import json, os, random, subprocess, sys
import numpy as np
from ultralytics import YOLO

ROOT = r"C:\Users\Asus\tools\yolo-data"
PY = r"C:\Users\Asus\tools\yolo\Scripts\python.exe"
S = os.path.join(ROOT, "scripts")
best = sys.argv[1]


def run(args):
    r = subprocess.run([PY] + args, capture_output=True, text=True, encoding="utf-8", errors="replace")
    print((r.stdout.strip().splitlines() or [""])[-1], flush=True)


def main():
    for tag, y in (("ft_val_coco", "eval_val_coco.yaml"), ("ft_val_monkey", "eval_val_monkey.yaml"),
                   ("ft_test_coco", "eval_test_coco.yaml"), ("ft_test_monkey", "eval_test_monkey.yaml"),
                   ("base_test_coco", "eval_test_coco.yaml")):
        w = "yolov8s.pt" if tag.startswith("base") else best
        run([os.path.join(S, "evaluate.py"), w, os.path.join(ROOT, "yolo81b", y), tag])
    run([os.path.join(S, "monkey_probe.py"), best, os.path.join(ROOT, "yolo81b", "test_monkey.txt"), "ft_test_monkey"])
    run([os.path.join(S, "monkey_probe.py"), best, os.path.join(ROOT, "yolo81b", "val_monkey.txt"), "ft_val_monkey"])
    run([os.path.join(S, "monkey_probe.py"), "yolov8s.pt", os.path.join(ROOT, "yolo81b", "test_monkey.txt"), "base_test_monkey"])
    r = subprocess.run([PY, os.path.join(S, "manual_check.py"), best, "ft"], capture_output=True, text=True, encoding="utf-8", errors="replace"); print(r.stdout[-1800:], flush=True)
    r = subprocess.run([PY, os.path.join(S, "manual_check.py"), "yolov8s.pt", "base"], capture_output=True, text=True, encoding="utf-8", errors="replace"); print(r.stdout[-300:], flush=True)
    # ONNX
    m = YOLO(best)
    onnx = m.export(format="onnx", opset=12, imgsz=640, simplify=True, dynamic=False, device="cpu")
    names = [m.names[i] for i in range(len(m.names))]
    out = os.path.join(ROOT, "final")
    os.makedirs(out, exist_ok=True)
    import shutil
    shutil.copy2(onnx, os.path.join(out, "yolov8s-coco81.onnx"))
    open(os.path.join(out, "synset.txt"), "w", encoding="utf-8", newline="\n").write("\n".join(names) + "\n")
    shutil.copy2(best, os.path.join(out, "best.pt"))
    print("ONNX:", os.path.join(out, "yolov8s-coco81.onnx"), "nhan:", len(names), names[-1], flush=True)


if __name__ == "__main__":
    main()
