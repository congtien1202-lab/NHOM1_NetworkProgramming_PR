#!/bin/sh
# Tải các mô hình YOLOv8s (ONNX) từ GitHub Releases vào server/src/main/resources/models/ (xem ADR-0003):
#  - yolov8s-coco81.onnx: COCO + monkey, 81 lớp (mô hình chính)
#  - guard/yolov8s-coco80.onnx: YOLOv8s gốc 80 lớp cho MonkeyGuard
# File .onnx không nằm trong git. Chạy từ thư mục ma-nguon, TRƯỚC khi build: scripts/download-model.sh
set -e
BASE="https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR/releases/download"
DIR="server/src/main/resources/models"
[ -d "$DIR" ] || { echo "Hãy chạy script này từ thư mục ma-nguon." >&2; exit 2; }
mkdir -p "$DIR/guard"

fetch() { # url sha256 đích
  curl -fL --retry 3 -o "$3" "$1"
  ACTUAL=$(sha256sum "$3" | cut -d' ' -f1)
  if [ "$ACTUAL" != "$2" ]; then
    rm -f "$3"
    echo "SHA-256 sai ($3): $ACTUAL" >&2
    exit 1
  fi
  echo "Đã tải $3, SHA-256 khớp."
}

fetch "$BASE/model-yolov8s-coco81-v1/yolov8s-coco81.onnx"   "5e20cb16bc95bef0ffcd84f507e8e387578541c465651c95cbc43d3a3609bbf7" "$DIR/yolov8s-coco81.onnx"
fetch "$BASE/model-yolov8s-coco80-guard-v1/yolov8s-coco80.onnx"   "a2c55be9d2285c9d0013b50dafaec7d42f5d82a9f501d04ff6034d803e995952" "$DIR/guard/yolov8s-coco80.onnx"
