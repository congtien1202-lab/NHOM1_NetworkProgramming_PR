#!/bin/bash
# Theo doi mot run: in moi epoch; o epoch 5, 10, 15, 20 danh gia val_coco + val_monkey tren ban sao last.pt.
# Dung: watch_train.sh <ten_run> <yaml_dir>
RUN=${1:-ft81}; YD=${2:-yolo81}
cd /c/Users/Asus/tools/yolo-data
PY=/c/Users/Asus/tools/yolo/Scripts/python.exe
last=0
while true; do
  if grep -a -q "TONG THOI GIAN" train.log; then echo TRAIN_DONE; tr '\r' '\n' < train.log | grep -a "TONG THOI"; break; fi
  if grep -a -qi "Traceback" train.err; then echo TRAIN_ERROR; tail -n 6 train.err; break; fi
  n=$(( $(wc -l < runs/$RUN/results.csv 2>/dev/null || echo 1) - 1 ))
  if [ "$n" != "$last" ] && [ "$n" -ge 1 ]; then
    tail -n 1 runs/$RUN/results.csv | awk -F, '{printf "epoch %s t=%.0fs mAP50=%.3f mAP50-95=%.3f\n",$1,$2,$8,$9}'
    last=$n
    if [ $((n % 5)) -eq 0 ]; then
      cp runs/$RUN/weights/last.pt runs/tmp_${RUN}_e$n.pt
      $PY scripts/evaluate.py runs/tmp_${RUN}_e$n.pt $YD/eval_val_coco.yaml ${RUN}_e${n}_coco 2>&1 | tail -n 1
      $PY scripts/evaluate.py runs/tmp_${RUN}_e$n.pt $YD/eval_val_monkey.yaml ${RUN}_e${n}_monkey 2>&1 | tail -n 1
    fi
  fi
  sleep 20
done
