"""Tao trong so khoi tao 81 lop tu yolov8s.pt: mo rong lop cuoi cua dau phan loai (cv3.*.2) tu 80 len 81 dau ra,
giu nguyen 80 hang trong so da huan luyen tren COCO, hang thu 81 (monkey) khoi tao mac dinh cua ultralytics."""
import os, torch
from ultralytics import YOLO
from ultralytics.nn.tasks import DetectionModel

ROOT = r"C:\Users\Asus\tools\yolo-data"


def main():
    src = YOLO(os.path.join(ROOT, "yolov8s.pt"))
    old = src.model.state_dict()
    names = dict(src.names)
    names[80] = "monkey"
    new = DetectionModel("yolov8s.yaml", nc=81, verbose=False)
    sd = new.state_dict()
    copied = expanded = 0
    for k, v in sd.items():
        o = old.get(k)
        if o is None:
            continue
        if o.shape == v.shape:
            sd[k] = o.clone(); copied += 1
        elif o.shape[0] == 80 and v.shape[0] == 81 and o.shape[1:] == v.shape[1:]:
            t = v.clone(); t[:80] = o; sd[k] = t; expanded += 1
        else:
            print("khong khop:", k, tuple(o.shape), tuple(v.shape))
    new.load_state_dict(sd)
    new.names = names
    src.model = new
    src.model.names = names
    out = os.path.join(ROOT, "yolov8s_81_init.pt")
    src.save(out)
    print("da chep", copied, "tensor, mo rong", expanded, "tensor ->", out)


if __name__ == "__main__":
    main()
