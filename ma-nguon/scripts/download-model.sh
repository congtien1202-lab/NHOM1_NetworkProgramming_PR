#!/bin/sh
# Tải mô hình YOLOv8s (ONNX, COCO 80 lớp) từ GitHub Releases vào server/src/main/resources/models/ (xem ADR-0003).
# File .onnx không nằm trong git. Chạy từ thư mục ma-nguon, TRƯỚC khi build: scripts/download-model.sh
set -e
URL="https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR/releases/download/model-yolov8s-coco80-v1/yolov8s.onnx"
SHA256="fe23ba3902a1c10dc9a222486ec961c353d14c38b8686ba5967ef59e06d3bb62"
DEST="server/src/main/resources/models/yolov8s.onnx"
[ -d "$(dirname "$DEST")" ] || { echo "Hãy chạy script này từ thư mục ma-nguon." >&2; exit 2; }
curl -fL --retry 3 -o "$DEST" "$URL"
ACTUAL=$(sha256sum "$DEST" | cut -d' ' -f1)
if [ "$ACTUAL" != "$SHA256" ]; then
  rm -f "$DEST"
  echo "SHA-256 sai: $ACTUAL" >&2
  exit 1
fi
echo "Đã tải yolov8s.onnx, SHA-256 khớp."
