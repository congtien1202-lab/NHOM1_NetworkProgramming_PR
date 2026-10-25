@echo off
rem Tai mo hinh YOLOv8s (ONNX, COCO 80 lop) tu GitHub Releases vao server\src\main\resources\models\ (xem ADR-0003).
rem File .onnx khong nam trong git. Chay tu thu muc ma-nguon, TRUOC khi build: scripts\download-model.cmd
setlocal
set "URL=https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR/releases/download/model-yolov8s-coco80-v1/yolov8s.onnx"
set "SHA256=fe23ba3902a1c10dc9a222486ec961c353d14c38b8686ba5967ef59e06d3bb62"
set "DEST=server\src\main\resources\models\yolov8s.onnx"
if not exist "server\src\main\resources\models" (
  echo Hay chay script nay tu thu muc ma-nguon.
  exit /b 2
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%URL%' -OutFile '%DEST%'; $h=(Get-FileHash '%DEST%' -Algorithm SHA256).Hash.ToLower(); if ($h -ne '%SHA256%') { Remove-Item '%DEST%'; Write-Error ('SHA-256 sai: ' + $h); exit 1 }; Write-Host 'Da tai yolov8s.onnx, SHA-256 khop.'"
exit /b %errorlevel%
