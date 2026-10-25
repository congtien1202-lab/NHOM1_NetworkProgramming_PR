# Package the Server as a self-contained Windows folder (trimmed Java runtime) so it runs without installing Java.
# The Oracle JDK ships an old msvcp140/vcruntime140 that breaks ONNX Runtime (ADR-0003), so the newer
# copies from C:\Windows\System32 are copied over the ones in the app-image runtime\bin.
# Output: dist\NhanDangAnh-Server\NhanDangAnh-Server.exe (starts with --detector=djl --port=5000, opens the Server window).
# Requires JDK 21+ with jpackage on the packaging machine, and the ONNX models (main + MonkeyGuard)
# downloaded first (scripts\download-model.cmd), because they are packed into the Server jar.
# Usage (from ma-nguon): powershell -ExecutionPolicy Bypass -File scripts\package-server.ps1
# (ASCII only: Windows PowerShell 5.1 reads BOM-less scripts as ANSI.)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$appName = 'NhanDangAnh-Server'
$dist = Join-Path $root 'dist'
$staging = Join-Path $dist 'input-server'
$app = Join-Path $dist $appName
$jar = Join-Path $root 'server\target\server-1.0-SNAPSHOT-all.jar'

$model = Join-Path $root 'server\src\main\resources\models\yolov8s-coco81.onnx'
if (-not (Test-Path $model)) { throw "Missing $model. Run scripts\download-model.cmd first." }
$guard = Join-Path $root 'server\src\main\resources\models\guard\yolov8s-coco80.onnx'
if (-not (Test-Path $guard)) { throw "Missing $guard. Run scripts\download-model.cmd first." }

Push-Location $root
try {
    & .\mvnw.cmd -q -pl common,server package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Server build failed' }
} finally {
    Pop-Location
}

foreach ($path in @($app, $staging)) {
    if (Test-Path $path) { Remove-Item -Recurse -Force $path }
}
New-Item -ItemType Directory -Force $staging | Out-Null
Copy-Item $jar $staging

# Modules found with: jdeps --multi-release 17 --ignore-missing-deps --print-module-deps <server jar>
# (java.base,java.compiler,java.desktop,java.management,java.sql) plus java.logging, java.naming, java.xml, jdk.unsupported.
$modules = 'java.base,java.compiler,java.desktop,java.logging,java.management,java.naming,java.sql,java.xml,jdk.unsupported'

& jpackage --type app-image --name $appName --dest $dist `
    --input $staging --main-jar (Split-Path -Leaf $jar) `
    --main-class com.nhom01.imagerec.server.ServerMain `
    --arguments '--detector=djl --port=5000' `
    --java-options '-Xms256m' --java-options '-Xmx1g' `
    --add-modules $modules `
    --jlink-options '--strip-debug --no-header-files --no-man-pages --compress=zip-6' `
    --app-version 1.0 --vendor 'Nhom 01'
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed' }

$bin = Join-Path $app 'runtime\bin'
foreach ($dll in @('msvcp140.dll', 'vcruntime140.dll', 'vcruntime140_1.dll')) {
    $from = Join-Path $env:SystemRoot "System32\$dll"
    if (-not (Test-Path $from)) { throw "Missing $from" }
    Copy-Item -Force $from (Join-Path $bin $dll)
}
Remove-Item -Recurse -Force $staging
Write-Host "Done: $app ($([math]::Round(((Get-ChildItem $app -Recurse | Measure-Object Length -Sum).Sum) / 1MB, 1)) MB)"
