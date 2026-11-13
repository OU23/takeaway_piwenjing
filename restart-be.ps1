$up = (Test-NetConnection 127.0.0.1 -Port 8090 -WarningAction SilentlyContinue).TcpTestSucceeded
Write-Host ("be8090_before=" + $up)
if (-not $up) {
  $p = Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue
  if ($p) { Stop-Process -Id $p.OwningProcess -Force -ErrorAction SilentlyContinue }
  Start-Sleep 2
  Set-Location 'D:/外卖/backend'
  $env:XG_OSS_ACCESS_KEY = 'xingguang-oss-admin'
  $env:XG_OSS_SECRET_KEY = 'XgDev2026OssSecretKeyAaBbCcDdEeFfGg'
  $env:SERVER__PORT = $null
  Start-Process 'java' -ArgumentList '-jar','target/xingguang-delivery.jar','--server.port=8090' -WorkingDirectory 'D:/外卖/backend' -WindowStyle Hidden -RedirectStandardOutput 'D:/外卖/backend/target/run-out.log' -RedirectStandardError 'D:/外卖/backend/target/run-err.log'
  for ($n = 0; $n -lt 50; $n++) {
    if ((Test-NetConnection 127.0.0.1 -Port 8090 -WarningAction SilentlyContinue).TcpTestSucceeded) { break }
    Start-Sleep 1
  }
}
Write-Host ("be8090_after=" + (Test-NetConnection 127.0.0.1 -Port 8090 -WarningAction SilentlyContinue).TcpTestSucceeded)
