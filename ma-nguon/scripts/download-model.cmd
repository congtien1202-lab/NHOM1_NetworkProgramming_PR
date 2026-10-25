@echo off
rem Tai cac mo hinh YOLOv8s (ONNX) tu GitHub Releases vao server\src\main\resources\models\ (xem ADR-0003):
rem  - yolov8s-coco81.onnx: COCO + monkey, 81 lop (mo hinh chinh)
rem  - guard\yolov8s-coco80.onnx: YOLOv8s goc 80 lop cho MonkeyGuard
rem File .onnx khong nam trong git. Chay tu thu muc ma-nguon, TRUOC khi build: scripts\download-model.cmd
setlocal
set "BASE=https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR/releases/download"
set "DIR=server\src\main\resources\models"
if not exist "%DIR%" (
  echo Hay chay script nay tu thu muc ma-nguon.
  exit /b 2
)
if not exist "%DIR%\guard" mkdir "%DIR%\guard"
call :fetch "%BASE%/model-yolov8s-coco81-v1/yolov8s-coco81.onnx" 5e20cb16bc95bef0ffcd84f507e8e387578541c465651c95cbc43d3a3609bbf7 "%DIR%\yolov8s-coco81.onnx"
if errorlevel 1 exit /b 1
call :fetch "%BASE%/model-yolov8s-coco80-guard-v1/yolov8s-coco80.onnx" a2c55be9d2285c9d0013b50dafaec7d42f5d82a9f501d04ff6034d803e995952 "%DIR%\guard\yolov8s-coco80.onnx"
exit /b %errorlevel%

:fetch
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%~1' -OutFile '%~3'; $h=(Get-FileHash '%~3' -Algorithm SHA256).Hash.ToLower(); if ($h -ne '%~2') { Remove-Item '%~3'; Write-Error ('SHA-256 sai: ' + $h); exit 1 }; Write-Host 'Da tai %~3, SHA-256 khop.'"
exit /b %errorlevel%
