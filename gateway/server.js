import http from 'node:http'
import httpProxy from 'http-proxy'

/**
 * 数创星光外卖 · 统一入口网关
 * 单端口（默认 9000）按路径前缀分发：
 *   /api      -> 后端 API        (8090)
 *   /admin    -> 运营后台        (8081, vite base=/admin)
 *   /merchant -> 商家端 H5       (8086, vite base=/merchant)
 *   /user     -> 用户端 H5       (8082, vite base=/user)
 *   /         -> 消费者 H5       (5173, vite base=/)
 * 同时透传 Vite 的 HMR WebSocket（upgrade）。
 */

const proxy = httpProxy.createProxyServer({ ws: true, changeOrigin: true })

const ROUTES = [
  { prefix: '/api', target: 'http://127.0.0.1:8090' },
  { prefix: '/admin', target: 'http://127.0.0.1:8081' },
  { prefix: '/merchant', target: 'http://127.0.0.1:8086' },
  { prefix: '/user', target: 'http://127.0.0.1:8082' },
  { prefix: '/', target: 'http://127.0.0.1:5173' }
]

function pick(url) {
  const u = url || '/'
  return ROUTES.find(r => u.startsWith(r.prefix)) || ROUTES[ROUTES.length - 1]
}

const server = http.createServer((req, res) => {
  const route = pick(req.url)
  proxy.web(req, res, { target: route.target }, (err) => {
    res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' })
    res.end('网关错误：无法连接到 ' + route.target + '\n' + (err && err.message))
  })
})

server.on('upgrade', (req, socket, head) => {
  const route = pick(req.url)
  proxy.ws(req, socket, head, { target: route.target }, () => socket.destroy())
})

const PORT = Number(process.env.PORT) || 9000
server.listen(PORT, '0.0.0.0', () => {
  console.log('[gateway] 统一入口已启动: http://0.0.0.0:' + PORT)
  ROUTES.forEach(r => console.log('  ' + r.prefix.padEnd(10) + ' -> ' + r.target))
})
