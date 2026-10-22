param(
    [string]$PermissionMode = 'acceptEdits',  # acceptEdits: tự sửa file, vẫn hỏi khi chạy lệnh lạ. Đổi thành 'auto' nếu muốn ít bị hỏi.
    [string]$Model = '',                      # ví dụ 'opus'; để trống = model mặc định của bạn
    [string[]]$Plans = @('A', 'B', 'C', 'D')  # chỉ mở một số plan: -Plans B,C
)

# Mở mỗi plan thành một tab trong Windows Terminal (nếu không có thì mở cửa sổ PowerShell riêng).
$runner = Join-Path $PSScriptRoot 'run-plan.ps1'
$root = Split-Path -Parent $PSScriptRoot

function Get-PlanCommand([string]$plan) {
    $cmd = "powershell -NoExit -ExecutionPolicy Bypass -File `"$runner`" -Plan $plan -PermissionMode $PermissionMode"
    if ($Model) { $cmd += " -Model $Model" }
    return $cmd
}

if (Get-Command wt -ErrorAction SilentlyContinue) {
    $parts = foreach ($plan in $Plans) {
        "new-tab --title `"PLAN-$plan`" -d `"$root`" " + (Get-PlanCommand $plan)
    }
    Start-Process wt -ArgumentList ($parts -join ' ; ')
} else {
    foreach ($plan in $Plans) {
        Start-Process powershell -ArgumentList "-NoExit -ExecutionPolicy Bypass -File `"$runner`" -Plan $plan -PermissionMode $PermissionMode" -WorkingDirectory $root
    }
}
