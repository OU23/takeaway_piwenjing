# -*- coding: utf-8 -*-
"""
数创星光外卖 · 页面截图（Chrome DevTools Protocol 精确移动端视口）

为什么不用 `chrome --screenshot`：
  Windows 下 Chrome 存在**最小窗口宽度限制**，实测 `--window-size=390` 的实际布局视口
  是 500px，截图却按 390px 裁切，导致右对齐元素（结算页「提交订单」、店铺页「去结算」）
  被切掉看不见。`--headless=old` 与 `--force-device-scale-factor` 都无效。

本脚本改用 CDP：
  Emulation.setDeviceMetricsOverride  → 精确模拟 iPhone 视口（不受窗口限制）
  Page.captureScreenshot(captureBeyondViewport) → 直接出整页长图

依赖：pip install websocket-client
用法：python scripts/screenshot.py [前端地址] [--out 目录]
"""
import base64
import json
import os
import shutil
import socket
import subprocess
import sys
import time
import urllib.request

import websocket

BASE = "http://127.0.0.1:5173"
OUT_DIR = None
WIDTH, HEIGHT, SCALE = 390, 844, 2

CHROME_CANDIDATES = [
    r"C:\Users\EDY\.agent-browser\browsers\chrome-155.0.8059.84\chrome.exe",
    r"C:\Users\EDY\.agent-browser\browsers\chrome-152.0.7977.75\chrome.exe",
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
]

# (文件名, hash 路由, 页面说明)
PAGES = [
    ("01-home",         "/",  "首页"),
    ("02-nearby",       "/nearby",  "附近·地图找店"),
    ("03-shop",         "/shop/103",  "店铺详情·左分类右商品"),
    ("04-orders",       "/orders",  "订单列表"),
    ("05-messages",     "/messages",  "消息中心"),
    ("06-mine",         "/mine",  "我的"),
    ("07-admin",        "/admin",  "运营后台看板"),
]


# ----------------------------------------------------------------------
# 极简 CDP 客户端
# ----------------------------------------------------------------------
class CDP:
    def __init__(self, ws_url):
        self.ws = websocket.create_connection(ws_url, timeout=30)
        self.seq = 0
        self.session_id = None

    def send(self, method, params=None, session_id=None):
        self.seq += 1
        msg = {"id": self.seq, "method": method, "params": params or {}}
        sid = session_id or self.session_id
        if sid:
            msg["sessionId"] = sid
        self.ws.send(json.dumps(msg))
        while True:
            data = json.loads(self.ws.recv())
            if data.get("id") == self.seq:
                if "error" in data:
                    raise RuntimeError(f"{method}: {data['error']}")
                return data.get("result", {})

    def wait_event(self, event, timeout=30):
        deadline = time.time() + timeout
        self.ws.settimeout(1.0)
        while time.time() < deadline:
            try:
                data = json.loads(self.ws.recv())
            except Exception:
                continue
            if data.get("method") == event:
                self.ws.settimeout(30)
                return data
        self.ws.settimeout(30)
        return None

    def close(self):
        try:
            self.ws.close()
        except Exception:
            pass


def free_port():
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    p = s.getsockname()[1]
    s.close()
    return p


def find_chrome():
    for c in CHROME_CANDIDATES:
        if os.path.isfile(c):
            return c
    return None


def main():
    global BASE, OUT_DIR
    args = [a for a in sys.argv[1:]]
    if args and not args[0].startswith("--"):
        BASE = args[0].rstrip("/")
        args = args[1:]
    if "--out" in args:
        OUT_DIR = args[args.index("--out") + 1]
    # 追加自定义页面： --page 名称:路由:说明（可重复）
    pages = list(PAGES)
    for i, a in enumerate(args):
        if a == "--page" and i + 1 < len(args):
            parts = args[i + 1].split(":", 2)
            pages.append((parts[0], parts[1], parts[2] if len(parts) > 2 else ""))
    # 只跑自定义页面： --only
    if "--only" in args:
        pages = [p for p in pages if p[0] in args[args.index("--only") + 1].split(",")]
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    OUT_DIR = OUT_DIR or os.path.join(root, "docs", "screenshots")

    chrome = find_chrome()
    if not chrome:
        print("[错误] 未找到 Chrome/Edge")
        sys.exit(1)

    os.makedirs(OUT_DIR, exist_ok=True)
    tmp = os.path.join(os.environ.get("TEMP", "/tmp"), "xg-cdp-shots")
    if os.path.isdir(tmp):
        shutil.rmtree(tmp, ignore_errors=True)
    os.makedirs(tmp, exist_ok=True)
    profile = os.path.join(tmp, "profile")

    port = free_port()
    print(f"浏览器：{chrome}")
    print(f"前端：  {BASE}")
    print(f"视口：  {WIDTH}x{HEIGHT} @{SCALE}x（CDP 精确模拟，不受窗口最小宽度限制）")
    print()

    proc = subprocess.Popen(
        [chrome, "--headless=new", "--disable-gpu", "--no-first-run",
         "--no-default-browser-check", "--hide-scrollbars",
         "--remote-allow-origins=*",
         f"--remote-debugging-port={port}", f"--user-data-dir={profile}",
         "about:blank"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )

    try:
        ws_browser = None
        for _ in range(60):
            try:
                with urllib.request.urlopen(f"http://127.0.0.1:{port}/json/version", timeout=2) as r:
                    ws_browser = json.loads(r.read().decode())["webSocketDebuggerUrl"]
                    break
            except Exception:
                time.sleep(0.5)
        if not ws_browser:
            raise RuntimeError("CDP 端口未就绪，Chrome 可能被沙箱拦截")

        cdp = CDP(ws_browser)
        target = cdp.send("Target.createTarget", {"url": "about:blank"})["targetId"]
        session = cdp.send("Target.attachToTarget",
                           {"targetId": target, "flatten": True})["sessionId"]
        cdp.session_id = session
        cdp.send("Page.enable")
        cdp.send("Emulation.setDeviceMetricsOverride", {
            "width": WIDTH, "height": HEIGHT,
            "deviceScaleFactor": SCALE, "mobile": True,
        })

        for i, (name, route, desc) in enumerate(pages):
            # 带变化的 query 强制真实导航（hash 变化不会触发 load 事件）
            url = f"{BASE}/?shot={i}#{route}"
            cdp.send("Page.navigate", {"url": url})
            cdp.wait_event("Page.loadEventFired", timeout=20)
            time.sleep(3.0)  # 等 Vue 挂载 + 接口返回

            metrics = cdp.send("Page.getLayoutMetrics")
            css_h = metrics["cssContentSize"]["height"]
            shot = cdp.send("Page.captureScreenshot", {
                "format": "png",
                "captureBeyondViewport": True,
                "clip": {"x": 0, "y": 0, "width": WIDTH,
                         "height": min(css_h, 3000), "scale": 1},
            })
            path = os.path.join(OUT_DIR, name + ".png")
            with open(path, "wb") as f:
                f.write(base64.b64decode(shot["data"]))
            # 顺手取标题做校验
            title = cdp.send("Runtime.evaluate", {
                "expression": "document.querySelector('.navbar__title, .app-shell') ? "
                              "(document.querySelector('.navbar__title')?.textContent || 'root-ok') : 'MISSING'",
                "returnByValue": True,
            })["result"].get("value")
            size_kb = os.path.getsize(path) // 1024
            print(f"  [ ok ] {name+'.png':<22} {size_kb:>4}K  整页高 {int(css_h)}px  {desc}  <{title}>")

        cdp.send("Target.closeTarget", {"targetId": target})
        cdp.close()
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=8)
        except Exception:
            proc.kill()
        shutil.rmtree(tmp, ignore_errors=True)

    print()
    print(f"产物目录：{OUT_DIR}")


if __name__ == "__main__":
    main()
