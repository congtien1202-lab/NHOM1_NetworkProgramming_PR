param(
    [Parameter(Mandatory = $true)][ValidateSet('A', 'B', 'C', 'D')][string]$Plan,
    [string]$PermissionMode = 'acceptEdits',
    [string]$Model = ''
)

# Chạy một cửa sổ Claude Code cho một plan, với prompt lấy từ scripts/prompts/<Plan>.txt
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$prompt = Get-Content -Raw -Encoding UTF8 (Join-Path $PSScriptRoot "prompts\$Plan.txt")
$host.UI.RawUI.WindowTitle = "PLAN-$Plan"

$claudeArgs = @('--permission-mode', $PermissionMode)
if ($Model) { $claudeArgs += @('--model', $Model) }
& claude @claudeArgs $prompt
