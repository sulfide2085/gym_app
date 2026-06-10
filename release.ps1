# 发布脚本 - 一键更新版本、提交、推送、部署
# 用法: ./release.ps1 1.3 "更新说明1" "更新说明2"

param(
    [Parameter(Mandatory=$true)]
    [string]$Version,

    [Parameter(Mandatory=$true)]
    [string[]]$Notes
)

$ErrorActionPreference = "Stop"

# 解析版本号
$versionName = $Version -replace '^v', ''
$parts = $versionName.Split('.')
$major = [int]$parts[0]
$minor = if ($parts.Length -gt 1) { [int]$parts[1] } else { 0 }
$versionCode = $major * 100 + $minor

Write-Host "==> 发布 v$versionName (versionCode=$versionCode)" -ForegroundColor Cyan

# 1. 更新 App 版本号
Write-Host "[1/5] 更新 App 版本号..." -ForegroundColor Yellow
$gradleFile = "app/build.gradle.kts"
$content = Get-Content $gradleFile -Raw
$content = $content -replace 'versionCode = \d+', "versionCode = $versionCode"
$content = $content -replace 'versionName = "[^"]*"', "versionName = `"$versionName`""
Set-Content $gradleFile -Value $content -NoNewline

# 2. 更新 Worker 版本信息
Write-Host "[2/5] 更新 Worker 版本信息..." -ForegroundColor Yellow
$workerFile = "cloudflare/worker/src/index.ts"
$workerContent = Get-Content $workerFile -Raw
$workerContent = $workerContent -replace 'const LATEST_ANDROID_VERSION_CODE = \d+', "const LATEST_ANDROID_VERSION_CODE = $versionCode"
$workerContent = $workerContent -replace 'const LATEST_ANDROID_VERSION_NAME = "[^"]*"', "const LATEST_ANDROID_VERSION_NAME = `"$versionName`""

# 构建 releaseNotes JSON 数组
$notesJson = ($Notes | ForEach-Object { "`"$_`"" }) -join ', '
$workerContent = $workerContent -replace 'const RELEASE_NOTES = \[.*?\];', "const RELEASE_NOTES = [$notesJson];"
Set-Content $workerFile -Value $workerContent -NoNewline

# 3. 提交
Write-Host "[3/5] 提交版本更新..." -ForegroundColor Yellow
git add $gradleFile $workerFile
git commit -m "Release v$versionName"

# 4. 推送
Write-Host "[4/5] 推送到 GitHub (触发 CI 自动构建)..." -ForegroundColor Yellow
git push origin main

# 5. 部署 Worker
Write-Host "[5/5] 部署 Worker..." -ForegroundColor Yellow
Push-Location cloudflare/worker
try {
    npx wrangler deploy
} finally {
    Pop-Location
}

Write-Host "`n==> 发布完成! v$versionName" -ForegroundColor Green
Write-Host "    CI 正在自动构建 APK 并部署到 Cloudflare Pages..." -ForegroundColor Green
Write-Host "    查看构建状态: gh run list --limit 1" -ForegroundColor Green
