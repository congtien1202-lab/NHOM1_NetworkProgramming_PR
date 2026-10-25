"""Fine-tune YOLOv8s 81 lop (80 COCO + monkey) tren RTX 5060. Dung: train81.py [epochs] [batch] [ten_run]"""
import os, sys, time
from ultralytics import YOLO

ROOT = r"C:\Users\Asus\tools\yolo-data"


def main():
    epochs = int(sys.argv[1]) if len(sys.argv) > 1 else 30
    batch = int(sys.argv[2]) if len(sys.argv) > 2 else 16
    name = sys.argv[3] if len(sys.argv) > 3 else "ft81"
    m = YOLO(os.path.join(ROOT, "yolov8s_81_init.pt"))
    t = time.time()
    m.train(data=os.path.join(ROOT, os.environ.get("DATA_DIR", "yolo81"), "data81.yaml"), epochs=epochs, imgsz=640, batch=batch, device=0,
            workers=int(os.environ.get("WORKERS","8")), optimizer="SGD", lr0=float(os.environ.get("LR0","0.001")), lrf=0.1, freeze=int(os.environ.get("FREEZE","10")), cos_lr=True, warmup_epochs=1.0, close_mosaic=5,
            project=os.path.join(ROOT, "runs"), name=name, exist_ok=True, patience=100, plots=True, seed=0,
            amp=True, cache=False, val=True)
    print("TONG THOI GIAN HUAN LUYEN (s):", time.time() - t, flush=True)


if __name__ == "__main__":
    main()
