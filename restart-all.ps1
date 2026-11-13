function Up($p){ (Test-NetConnection 127.0.0.1 -Port $p -WarningAction SilentlyContinue).TcpTestSucceeded }

# 后端
# 安全提示：MinIO 等外部服务凭据不再写在本脚本里，请先在当前会话或系统环境变量中设置：
#   $env:XG_OSS_ACCESS_KEY / $env:XG_OSS_SECRET_KEY  （对象存储）
#   $env:XG_AMAP_KEY                                 （高德地图）
#   $env:XG_AUTH_SECRET / $env:XG_PAY_NOTIFY_SECRET  （令牌与回调验签密钥）
#   $env:XG_DB_PASSWORD                              （数据库口令，默认 123456 仅限本地开发库）
# 未设置 OSS 凭据时后端仍可启动，仅文件上传接口不可用。
if(-not (Up 8090)){
  $p=Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue
  if($p){Stop-Process -Id $p.OwningProcess -Force -ErrorAction SilentlyContinue}
  Start-Sleep 2
  # 注意：Start-Process 的 WorkingDirectory / 重定向路径必须用反斜杠，
  # 正斜杠路径在部分 Windows/PowerShell 组合下会抛 DirectoryNotFoundException
  Start-Process 'java' -ArgumentList '-jar','D:\外卖\backend\target\xingguang-delivery.jar','--server.port=8090' -WorkingDirectory 'D:\外卖\backend\target' -WindowStyle Hidden -RedirectStandardOutput 'D:\外卖\backend\target\run-out.log' -RedirectStandardError 'D:\外卖\backend\target\run-err.log'
}

# 前端（仅启动未运行的）
$pairs = @(
  @('D:\外卖\admin-web',8081),
  @('D:\外卖\frontend',5173),
  @('D:\外卖\design-preview',8082),
  @('D:\外卖\merchant-web',8086)
)
foreach($pr in $pairs){
  if(-not (Up $pr[1])){
    Start-Process 'node' -ArgumentList 'node_modules/vite/bin/vite.js','--port',$pr[1],'--host','127.0.0.1' -WorkingDirectory $pr[0] -WindowStyle Hidden -RedirectStandardOutput ($pr[0]+'\vite.log') -RedirectStandardError ($pr[0]+'\vite-err.log')
  }
}

# 网关
if(-not (Up 9000)){
  Start-Process 'node' -ArgumentList 'server.js' -WorkingDirectory 'D:\外卖\gateway' -WindowStyle Hidden -RedirectStandardOutput 'D:\外卖\gateway\gw.log' -RedirectStandardError 'D:\外卖\gateway\gw-err.log'
}

Start-Sleep 15
Write-Host ("be8090=" + (Up 8090))
Write-Host ("admin8081=" + (Up 8081))
Write-Host ("fe5173=" + (Up 5173))
Write-Host ("user8082=" + (Up 8082))
Write-Host ("merchant8086=" + (Up 8086))
Write-Host ("gw9000=" + (Up 9000))
