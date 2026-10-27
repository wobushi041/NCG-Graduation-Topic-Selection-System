$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $projectRoot 'nfu-graduation-topic-selection-backend'
$frontendDir = Join-Path $projectRoot 'nfu-graduation-topic-selection-frontend'
$serverImage = 'nfu-topic-selection-server:local'
$webImage = 'nfu-topic-selection-web:local'

function Assert-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "缺少 $Name，请先安装后再构建。"
    }
}

function Invoke-Checked {
    param(
        [string]$Command,
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $Command @Arguments
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0) {
        throw "$Command 执行失败，退出码：$exitCode"
    }
}

# 后端 mise.toml 固定 JDK 8；安装了 mise 时自动使用该版本。
if (Get-Command mise -ErrorAction SilentlyContinue) {
    Push-Location $backendDir
    try {
        try {
            $projectJavaHome = (& mise where java 2>$null | Select-Object -First 1)
        }
        catch {
            $projectJavaHome = $null
        }
        if ($projectJavaHome -and
            (Test-Path -LiteralPath (Join-Path $projectJavaHome 'bin/java.exe'))) {
            $env:JAVA_HOME = $projectJavaHome
            $env:Path = "$(Join-Path $projectJavaHome 'bin');$env:Path"
            Write-Host "[info] 使用项目 JDK：$projectJavaHome"
        }
    }
    finally {
        Pop-Location
    }
}

Assert-Command 'java.exe'
Assert-Command 'javac.exe'
Assert-Command 'node.exe'
Assert-Command 'pnpm.cmd'
Assert-Command 'docker.exe'

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $javacVersion = (& javac.exe -version 2>&1).ToString().Trim()
    $javacExitCode = $LASTEXITCODE
}
finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
if ($javacExitCode -ne 0 -or $javacVersion -notmatch '^javac (1\.8\.|8\.)') {
    throw "后端需要 JDK 8，当前为：$javacVersion"
}

$nodeVersion = (& node.exe --version).Trim()
if ($LASTEXITCODE -ne 0 -or $nodeVersion -notmatch '^v24\.') {
    throw "前端需要 Node.js 24，当前为：$nodeVersion"
}

$pnpmVersion = (& pnpm.cmd --version).Trim()
if ($LASTEXITCODE -ne 0 -or $pnpmVersion -ne '11.19.0') {
    throw "前端需要 pnpm 11.19.0，当前为：$pnpmVersion"
}

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    & docker.exe info --format '{{.ServerVersion}}' *> $null
    $dockerExitCode = $LASTEXITCODE
}
finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
if ($dockerExitCode -ne 0) {
    throw '无法连接 Docker，请先启动 Docker Desktop。'
}

Push-Location $projectRoot
try {
    Write-Host '[info] 编译后端项目'
    Invoke-Checked (Join-Path $projectRoot 'mvnw.cmd') @(
        '-pl', 'nfu-graduation-topic-selection-backend', '-am', 'clean', 'package', '-DskipTests'
    )

    if (-not (Get-ChildItem -LiteralPath (Join-Path $backendDir 'target') -Filter '*-exec.jar' -File -ErrorAction SilentlyContinue)) {
        throw '后端构建完成，但未找到 target/*-exec.jar。'
    }

    Write-Host "[info] 构建后端镜像：$serverImage"
    Invoke-Checked 'docker.exe' @('build', '-t', $serverImage, $backendDir)

    Set-Location $frontendDir
    Write-Host '[info] 安装前端依赖并构建'
    Invoke-Checked 'pnpm.cmd' @('install', '--frozen-lockfile')
    Invoke-Checked 'pnpm.cmd' @('build')

    if (-not (Test-Path -LiteralPath (Join-Path $frontendDir 'dist/index.html'))) {
        throw '前端构建完成，但未找到 dist/index.html。'
    }

    Write-Host "[info] 构建前端镜像：$webImage"
    Invoke-Checked 'docker.exe' @('build', '-t', $webImage, '.')

    Write-Host "[info] 构建完成：$serverImage、$webImage"
}
finally {
    Pop-Location
}
