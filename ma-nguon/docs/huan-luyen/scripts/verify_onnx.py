"""Kiem tra ONNX: opset, input/output shape, so nhan, va dung onnxruntime du doan 1 anh khi so voi PyTorch."""
import glob, os, sys
import numpy as np, onnx, onnxruntime as ort
from PIL import Image
from ultralytics import YOLO

F = r"C:\Users\Asus\tools\yolo-data\final"
m = onnx.load(os.path.join(F, "yolov8s-coco81.onnx"))
print("opset:", [(o.domain or "ai.onnx", o.version) for o in m.opset_import])
print("input:", [(i.name, [d.dim_value for d in i.type.tensor_type.shape.dim]) for i in m.graph.input])
print("output:", [(o.name, [d.dim_value for d in o.type.tensor_type.shape.dim]) for o in m.graph.output])
syn = open(os.path.join(F, "synset.txt"), encoding="utf-8").read().split("\n")
print("synset dong:", len([s for s in syn if s]), "| dau:", syn[0], "| cuoi:", syn[-2] if syn[-1] == "" else syn[-1], "| cuoi file co newline:", syn[-1] == "")
img = sorted(glob.glob(r"C:\Users\Asus\tools\yolo-data\manual_person\*.jpg"))[0]
monk = open(r"C:\Users\Asus\tools\yolo-data\yolo81b\test_monkey.txt").read().split("\n")[3]
sess = ort.InferenceSession(os.path.join(F, "yolov8s-coco81.onnx"), providers=["CPUExecutionProvider"])
pt = YOLO(os.path.join(F, "best.pt"))
for p in (monk, img):
    im = Image.open(p).convert("RGB").resize((640, 640))
    x = np.asarray(im, dtype=np.float32).transpose(2, 0, 1)[None] / 255.0
    out = sess.run(None, {sess.get_inputs()[0].name: x})[0][0]  # [85,8400]
    scores = out[4:, :]
    cls = scores.argmax(0); conf = scores.max(0)
    top = np.argsort(-conf)[:3]
    r = pt.predict(np.asarray(im), conf=0.25, imgsz=640, verbose=False, device="cpu")[0]
    print(os.path.basename(p), "ONNX top3:", [(syn[int(cls[i])], round(float(conf[i]), 2)) for i in top],
          "| PyTorch:", [(pt.names[int(c)], round(float(s), 2)) for c, s in zip(r.boxes.cls.tolist(), r.boxes.conf.tolist())][:3])
