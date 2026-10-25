#!/bin/sh
# Tải mô hình YOLOv8s (ONNX, COCO + monkey, 81 lớp) từ GitHub Releases vào server/src/main/resources/models/ (xem ADR-0003).
# File .onnx không nằm trong git. Chạy từ thư mục ma-nguon, TRƯỚC khi build: scripts/download-model.sh
set -e
URL="https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR/releases/download/model-yolov8s-coco81-v1/yolov8s-coco81.onnx"
SHA256="5e20cb16bc95bef0ffcd84f507e8e387578541c465651c95cbc43d3a3609bbf7"
DEST="server/src/main/resources/models/yolov8s-coco81.onnx"
[ -d "$(dirname "$DEST")" ] || { echo "Hãy chạy script này từ thư mục ma-nguon." >&2; exit 2; }
curl -fL --retry 3 -o "$DEST" "$URL"
ACTUAL=$(sha256sum "$DEST" | cut -d' ' -f1)
if [ "$ACTUAL" != "$SHA256" ]; then
  rm -f "$DEST"
  echo "SHA-256 sai: $ACTUAL" >&2
  exit 1
fi
echo "Đã tải yolov8s-coco81.onnx, SHA-256 khớp."
