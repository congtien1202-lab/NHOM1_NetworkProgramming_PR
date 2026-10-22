# Build the full release zip for team members: program (client app-image + jars), user guide and source code.
# Output: NhanDangAnh-Nhom01.zip in the project root (overwritten each run).
# Requires JDK 21+ with jpackage and git on the packaging machine.
# Usage (project root): powershell -ExecutionPolicy Bypass -File scripts\package-release.ps1
# (ASCII only: Windows PowerShell 5.1 reads BOM-less scripts as ANSI.)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root 'dist'
$releaseRoot = Join-Path $dist 'release'
$pkgName = 'NhanDangAnh-Nhom01'
$pkg = Join-Path $releaseRoot $pkgName
$guide = Join-Path $root 'docs\HUONG-DAN-SU-DUNG.md'
$zip = Join-Path $root "$pkgName.zip"

if (-not (Test-Path $guide)) { throw "Missing $guide" }

# 1. Client app-image + client jar (this script wipes dist\).
& (Join-Path $PSScriptRoot 'package-client.ps1')
if ($LASTEXITCODE -ne 0) { throw 'package-client.ps1 failed' }

# 2. Server jar.
Push-Location $root
try {
    & .\mvnw.cmd -q -pl server package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Server build failed' }
} finally {
    Pop-Location
}

# 3. Staging layout.
$prog = Join-Path $pkg 'chuong-trinh'
$src = Join-Path $pkg 'ma-nguon'
New-Item -ItemType Directory -Force $prog, $src | Out-Null
Copy-Item $guide (Join-Path $pkg 'HUONG-DAN-SU-DUNG.md')
Copy-Item -Recurse (Join-Path $dist 'NhanDangAnh') (Join-Path $prog 'NhanDangAnh')
Copy-Item (Join-Path $root 'client\target\client-1.0-SNAPSHOT-all.jar') $prog
Copy-Item (Join-Path $root 'server\target\server-1.0-SNAPSHOT-all.jar') $prog

$cmd = @(
    '@echo off',
    'cd /d "%~dp0"',
    'java -jar server-1.0-SNAPSHOT-all.jar --detector=djl --port=5000 %*',
    'pause'
) -join "`r`n"
[IO.File]::WriteAllText((Join-Path $prog 'chay-server.cmd'), $cmd + "`r`n", (New-Object Text.ASCIIEncoding))

# 4. Source: committed files only (no target, dist, Support, logs).
$srcZip = Join-Path $releaseRoot 'source.zip'
Push-Location $root
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
