# Package the Client as a self-contained folder (with a trimmed Java runtime) plus a zip for testers.
# Testers unzip and run NhanDangAnh\NhanDangAnh.exe; no Java installation needed.
# Requires JDK 21+ with jpackage on the packaging machine.
# Usage (project root): powershell -ExecutionPolicy Bypass -File scripts\package-client.ps1
# (ASCII only: Windows PowerShell 5.1 reads BOM-less scripts as ANSI.)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$appName = 'NhanDangAnh'
$dist = Join-Path $root 'dist'
$staging = Join-Path $dist 'input'
$jar = Join-Path $root 'client\target\client-1.0-SNAPSHOT-all.jar'

Push-Location $root
try {
    & .\mvnw.cmd -q -pl common,client package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Client build failed' }
} finally {
    Pop-Location
}

if (Test-Path $dist) { Remove-Item -Recurse -Force $dist }
New-Item -ItemType Directory -Force $staging | Out-Null
Copy-Item $jar $staging

# jdeps: the client only needs java.base + java.desktop.
& jpackage --type app-image --name $appName --dest $dist `
    --input $staging --main-jar (Split-Path -Leaf $jar) `
    --main-class com.nhom01.imagerec.client.ui.ClientMain `
    --add-modules java.base,java.desktop `
    --jlink-options '--strip-debug --no-header-files --no-man-pages --compress=zip-6' `
    --app-version 1.0 --vendor 'Nhom 01'
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed' }

$zip = Join-Path $dist "$appName-windows.zip"
Compress-Archive -Path (Join-Path $dist $appName) -DestinationPath $zip
Remove-Item -Recurse -Force $staging
Write-Host "Done: $zip ($([math]::Round((Get-Item $zip).Length / 1MB, 1)) MB)"
