#requires -Version 5.1
<#
.SYNOPSIS
  初始化外卖项目在 MinIO 上的专用桶与读写凭证。

.DESCRIPTION
  用 MinIO root 账号登录，创建桶 xingguang-oss、设置匿名只读（便于 <img> 直链预览）、
  创建受限策略与专用 AK/SK（仅能读写该桶），最后打印需要注入后端的环境变量。

  为什么桶要匿名只读：商品图/门店 Logo 属于公开素材，直链最简单；
  私密素材（资质）后续可单独放私有桶或改用签名 URL。

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/minio-init.ps1 `
    -RootUser admin -RootPass 'your-root-password'

.EXAMPLE
  # 已有专用凭证时只建桶
  powershell -ExecutionPolicy Bypass -File scripts/minio-init.ps1 -SkipUser
#>
param(
  [string]$Endpoint  = "http://192.168.35.120:9000",
  [string]$Console   = "http://192.168.35.120:9001/",
  [string]$RootUser  = "minioadmin",
  [string]$RootPass  = "",
  [string]$Bucket    = "xingguang-oss",
  [string]$AccessKey = "xingguang-oss-admin",
  [string]$SecretKey = "",
  [switch]$SkipUser,
  [switch]$PrivateBucket
)

$ErrorActionPreference = "Stop"

function Find-Mc {
  $candidates = @(
    "$env:TEMP\xgmc\mc.exe",
    "$PSScriptRoot\mc.exe",
    "mc.exe"
  )
  foreach ($c in $candidates) {
    if (Test-Path $c) { return $c }
    $cmd = Get-Command $c -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
  }
  return $null
}

function Download-Mc {
  $dir = "$env:TEMP\xgmc"
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
  $exe = "$dir\mc.exe"
  $url = "https://dl.min.io/client/mc/release/windows-amd64/mc.exe"
  $mirror = "https://ghfast.top/https://github.com/minio/mc/releases/download/RELEASE.2025-08-13T08-35-41Z/mc.windows-amd64.RELEASE.2025-08-13T08-35-41Z.exe"
  Write-Host "未找到 mc，正在下载..." -ForegroundColor Yellow
  try {
    Invoke-WebRequest -Uri $url -OutFile $exe -TimeoutSec 180
  } catch {
    Write-Host "官方源不可用，尝试镜像..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $mirror -OutFile $exe -TimeoutSec 180
  }
  return $exe
}

if (-not $RootPass) {
  $sec = Read-Host "请输入 MinIO root 密码" -AsSecureString
  $RootPass = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}

if (-not $SkipUser -and -not $SecretKey) {
  $bytes = New-Object byte[] 24
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $SecretKey = [Convert]::ToBase64String($bytes).Replace("+", "A").Replace("/", "B").TrimEnd("=")
}

$mc = Find-Mc
if (-not $mc) { $mc = Download-Mc }

$env:MC_CONFIG_DIR = "$env:TEMP\xgmc\.mc"
Write-Host "==> 连接 $Endpoint" -ForegroundColor Cyan
& $mc alias set xgroot $Endpoint $RootUser $RootPass | Out-Null
if ($LASTEXITCODE -ne 0) { throw "root 账号连接失败，请核对用户名/密码与 endpoint（注意 9000 才是 API）" }

Write-Host "==> 创建桶 $Bucket" -ForegroundColor Cyan
& $mc mb --ignore-existing "xgroot/$Bucket" | Out-Null

if ($PrivateBucket) {
  Write-Host "==> 保持桶私有（前端需用签名 URL）" -ForegroundColor Cyan
  & $mc anonymous set none "xgroot/$Bucket" | Out-Null
} else {
  Write-Host "==> 设置匿名只读（便于图片直链预览）" -ForegroundColor Cyan
  & $mc anonymous set download "xgroot/$Bucket" | Out-Null
}

if (-not $SkipUser) {
  $policyFile = Join-Path $env:TEMP "xg-oss-policy.json"
  $policyJson = @"
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetBucketLocation", "s3:ListBucket"],
      "Resource": ["arn:aws:s3:::$Bucket"]
    },
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": ["arn:aws:s3:::$Bucket/*"]
    }
  ]
}
"@
  [System.IO.File]::WriteAllText($policyFile, $policyJson, [System.Text.UTF8Encoding]::new($false))

  Write-Host "==> 创建受限策略 xingguang-oss-rw" -ForegroundColor Cyan
  & $mc admin policy create xgroot xingguang-oss-rw $policyFile 2>$null | Out-Null
  if ($LASTEXITCODE -ne 0) {
    & $mc admin policy update xgroot xingguang-oss-rw $policyFile | Out-Null
  }

  Write-Host "==> 创建专用账号 $AccessKey" -ForegroundColor Cyan
  & $mc admin user add xgroot $AccessKey $SecretKey | Out-Null
  if ($LASTEXITCODE -ne 0) {
    & $mc admin user enable xgroot $AccessKey | Out-Null
  }

  Write-Host "==> 绑定策略" -ForegroundColor Cyan
  & $mc admin policy attach xgroot xingguang-oss-rw --user $AccessKey | Out-Null
}

Write-Host ""
Write-Host "================ 完成 ================" -ForegroundColor Green
Write-Host "控制台   : $Console"
Write-Host "S3 API   : $Endpoint"
Write-Host "桶       : $Bucket"
if (-not $SkipUser) {
  Write-Host "AccessKey: $AccessKey"
  Write-Host "SecretKey: $SecretKey" -ForegroundColor Yellow
  Write-Host ""
  Write-Host "把下面两行设置为系统环境变量后重启后端：" -ForegroundColor Cyan
  Write-Host "  [Environment]::SetEnvironmentVariable('XG_OSS_ACCESS_KEY','$AccessKey','User')"
  Write-Host "  [Environment]::SetEnvironmentVariable('XG_OSS_SECRET_KEY','$SecretKey','User')"
  Write-Host ""
  Write-Host "验证：" -ForegroundColor Cyan
  Write-Host "  $mc ls xgroot/$Bucket"
}
