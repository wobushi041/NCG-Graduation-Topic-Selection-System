# 将仓库根目录 .env 写入当前 PowerShell 进程环境变量。请使用点调用：. .\env.ps1
if ($MyInvocation.InvocationName -ne '.') {
    Write-Error "请使用点调用加载环境变量：. .\env.ps1"
    exit 1
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $projectRoot ".env"

if (-not (Test-Path -LiteralPath $envFile)) {
    Write-Error "未找到 $envFile，请先复制 .env.example 为 .env。"
    return
}

foreach ($rawLine in Get-Content -LiteralPath $envFile -Encoding UTF8) {
    $line = $rawLine.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
        continue
    }
    $eqIndex = $line.IndexOf('=')
    if ($eqIndex -le 0) {
        continue
    }
    $key = $line.Substring(0, $eqIndex).Trim()
    $value = $line.Substring($eqIndex + 1)
    [System.Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)
}
