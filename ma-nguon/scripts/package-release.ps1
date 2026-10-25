# Build the full release zip for team members: program (client + server app-images, jars), user guide,
# report/slides and source code.
# The user guide and the report/slides are not in the repository, so both locations are required.
# Output: dist\NhanDangAnh-Nhom01.zip (overwritten each run).
# Requires JDK 21+ with jpackage and git on the packaging machine, and the ONNX model downloaded
# (scripts\download-model.cmd).
# Usage (from ma-nguon):
#   powershell -ExecutionPolicy Bypass -File scripts\package-release.ps1 -Guide <HUONG-DAN-SU-DUNG.md> -DocsDir <folder with NHOM01-*.docx/pdf/pptx>
# (ASCII only: Windows PowerShell 5.1 reads BOM-less scripts as ANSI.)

param(
    [Parameter(Mandatory = $true)][string]$Guide,
    [Parameter(Mandatory = $true)][string]$DocsDir
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root 'dist'
$releaseRoot = Join-Path $dist 'release'
$pkgName = 'NhanDangAnh-Nhom01'
$pkg = Join-Path $releaseRoot $pkgName
$zip = Join-Path $dist "$pkgName.zip"
$docFiles = 'NHOM01-BAOCAO.docx', 'NHOM01-BAOCAO.pdf', 'NHOM01-SLIDE.pptx', 'NHOM01-SLIDE.pdf', 'NHOM01-PHIEUTHUYETMINH.docx'

if (-not (Test-Path $Guide)) { throw "Missing user guide: $Guide" }
if (-not (Test-Path $DocsDir)) { throw "Missing documents folder: $DocsDir" }
foreach ($f in $docFiles) {
    if (-not (Test-Path (Join-Path $DocsDir $f))) { throw "Missing document $f in $DocsDir" }
}

# 1. Client app-image + client jar (this script wipes dist\).
& (Join-Path $PSScriptRoot 'package-client.ps1')
if ($LASTEXITCODE -ne 0) { throw 'package-client.ps1 failed' }

# 2. Server app-image (also builds the server jar; keeps dist\ intact).
& (Join-Path $PSScriptRoot 'package-server.ps1')
if ($LASTEXITCODE -ne 0) { throw 'package-server.ps1 failed' }

# 3. Staging layout.
$prog = Join-Path $pkg 'chuong-trinh'
$src = Join-Path $pkg 'ma-nguon'
$docOut = Join-Path $pkg 'tai-lieu'
New-Item -ItemType Directory -Force $prog, $src, $docOut | Out-Null
Copy-Item $Guide (Join-Path $pkg 'HUONG-DAN-SU-DUNG.md')
Copy-Item -Recurse (Join-Path $dist 'NhanDangAnh') (Join-Path $prog 'NhanDangAnh')
Copy-Item -Recurse (Join-Path $dist 'NhanDangAnh-Server') (Join-Path $prog 'NhanDangAnh-Server')
Copy-Item (Join-Path $root 'client\target\client-1.0-SNAPSHOT-all.jar') $prog
Copy-Item (Join-Path $root 'server\target\server-1.0-SNAPSHOT-all.jar') $prog
foreach ($f in $docFiles) { Copy-Item (Join-Path $DocsDir $f) $docOut }

# 4. Source: committed files only (no target, dist, logs, model), taken from the repository root.
$srcZip = Join-Path $releaseRoot 'source.zip'
Push-Location $root
try {
    $repo = (& git rev-parse --show-toplevel).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Not inside a git repository' }
} finally {
    Pop-Location
}
Push-Location $repo
try {
    & git archive --format=zip -o $srcZip HEAD
    if ($LASTEXITCODE -ne 0) { throw 'git archive failed' }
} finally {
    Pop-Location
}
Expand-Archive $srcZip $src
Remove-Item $srcZip

# 5. Final zip.
if (Test-Path $zip) { Remove-Item -Force $zip }
Compress-Archive -Path $pkg -DestinationPath $zip
Remove-Item -Recurse -Force $releaseRoot
Write-Host "Done: $zip ($([math]::Round((Get-Item $zip).Length / 1MB, 1)) MB)"
