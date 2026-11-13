# -*- coding: utf-8 -*-
"""
数创星光外卖 · 端到端链路验证脚本

覆盖：首页 → 店铺 → 加购（规格/合并/估清拦截）→ 结算试算（配送/自取/起送价）
      → 提交订单（幂等/越权拦截）→ 支付渠道 → 发起支付（模拟通道同步结算）
      → 回调验签拦截 → 状态机推进 → 到店自取全链路（取餐码）→ 余额支付与取消退款
      → 订单列表 → 骑手端全链路 → 运营后台

用法：python e2e_test.py [baseUrl]
默认 baseUrl = http://127.0.0.1:8090

=====================================================================
幂等约定（重要：改这个脚本时请守住下面三条，否则测试会随时间失效）
=====================================================================
1. 不依赖种子单状态。抢单/取货/送达会永久改写订单状态，断言种子单（如 9004/9005）
   等于「断言取决于这个库被跑过几次」。需要什么状态就自己下单造出来。
2. 不消耗不可再生的资源。余额支付会真的扣演示账号的钱，跑几十次就把余额耗干。
   需要测余额就走「支付 → 取消 → 退款」，净零；顺带覆盖退款路径。
3. 断言用相对值或 `>=`，不要写死累计量。订单数、流水数、收入明细每次都在涨，
   任何 `== 某个绝对值` 的累计断言都是定时炸弹。

=====================================================================
在「全新库」上验证（证明脚本真的自给自足）
=====================================================================
    # 1. 建平行库（替换库名，别动主库）
    sed 's/xingguang_delivery/xingguang_delivery_fresh/g' \\
        backend/src/main/resources/db/schema.sql > /tmp/schema_fresh.sql
    sed 's/xingguang_delivery/xingguang_delivery_fresh/g' \\
        backend/src/main/resources/db/data.sql   > /tmp/data_fresh.sql

    # 2. 导入 —— 必须指定 utf8mb4！
    #    否则 rider.vehicle 的中文默认值 '电动车' 会报
    #    ERROR 1067 Invalid default value for 'vehicle'
    mysql -uroot -p --default-character-set=utf8mb4 < /tmp/schema_fresh.sql
    mysql -uroot -p --default-character-set=utf8mb4 < /tmp/data_fresh.sql

    # 3. 起一个指向新库的实例
    mvn -o spring-boot:run -Dspring-boot.run.arguments=\\
      "--server.port=8091 --spring.datasource.url=jdbc:mysql://127.0.0.1:3306/xingguang_delivery_fresh?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"

    # 4. 跑
    python scripts/e2e_test.py http://127.0.0.1:8091

预期：全新库首次运行即 186 项通过 / 0 失败，连跑多次余额恒定。
"""
import json
import io
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

# 中文 Windows 控制台默认 GBK，打印「¥」等字符会抛 UnicodeEncodeError。
# 这里把标准输出/错误重定向为 UTF-8，保证脚本在任意环境可正常运行。
if hasattr(sys.stdout, "buffer"):
    try:
        if (sys.stdout.encoding or "").lower() != "utf-8":
            sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
            sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")
    except Exception:
        pass

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8090"
USER_HEADER = {"X-User-Id": "1"}

PASS, FAIL = [], []


def enc(path):
    """对查询串中的非 ASCII 值做 URL 编码（中文关键词等）。"""
    if "?" not in path:
        return path
    base, qs = path.split("?", 1)
    parts = []
    for kv in qs.split("&"):
        if "=" in kv:
            k, v = kv.split("=", 1)
            parts.append(k + "=" + urllib.parse.quote(v, safe=""))
        else:
            parts.append(kv)
    return base + "?" + "&".join(parts)


def call(method, path, body=None, headers=None, expect_ok=True, label=""):
    url = BASE + enc(path)
    data = None
    hdrs = {"Content-Type": "application/json"}
    hdrs.update(USER_HEADER)
    if headers:
        hdrs.update(headers)
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        payload = {"code": e.code, "message": e.read().decode("utf-8")}
    ok = payload.get("code") == 0
    tag = "OK " if ok else "ERR"
    extra = "" if ok else f"  <- {payload.get('message')}"
    print(f"[{tag}] {method:5s} {path}{extra}")
    name = label or path
    if expect_ok and not ok:
        FAIL.append(f"{name}: {payload.get('message')}")
    elif not expect_ok and ok:
        FAIL.append(f"{name}: 期望失败但成功了")
    else:
        PASS.append(name)
    return payload


def check(cond, label):
    if cond:
        PASS.append(label)
        print(f"[OK ] 断言通过 · {label}")
    else:
        FAIL.append(label)
        print(f"[FAIL] 断言失败 · {label}")


def yuan(fen):
    return f"¥{fen / 100:.2f}"


print("=" * 78)
print("数创星光外卖 · 端到端链路验证  " + BASE)
print("=" * 78)

# ---------------------------------------------------------------- 1. 首页
print("\n--- 1. 首页与门店 ---")
home = call("GET", "/api/v1/home", label="首页聚合")
d = home.get("data") or {}
check(len(d.get("categories", [])) == 10, "金刚区返回 10 个品类")
check(len(d.get("banners", [])) >= 3, "首页轮播 >= 3")
check(len(d.get("shops", [])) == 7, "附近好店返回 7 家门店")

shops = call("GET", "/api/v1/shops?sort=distance", label="门店列表按距离排序").get("data") or []
dists = [float(s["distanceKm"]) for s in shops]
check(dists == sorted(dists), "门店按距离升序")
call("GET", "/api/v1/shops?keyword=奶茶", label="门店搜索「奶茶」")

# ---------------------------------------------------------------- 2. 店铺详情
print("\n--- 2. 店铺详情与规格 ---")
detail = call("GET", "/api/v1/shops/103", label="门店详情").get("data") or {}
check(detail["shop"]["name"] == "蜜茶时光·奶茶", "门店名称正确")
check(len(detail["categories"]) == 3, "店内分类 3 个")
check(len(detail["dishes"]) == 6, "门店商品 6 个")

# Long 已序列化为字符串（雪花 ID 精度修复），比较时统一转字符串
tea = [x for x in detail["dishes"] if str(x["id"]) == "341"][0]
check(tea["hasSpec"] is True, "招牌珍珠奶茶带规格")
groups = [g["groupName"] for g in tea["specGroups"]]
check(groups == ["杯型", "温度", "糖度", "加料"], f"规格组顺序稳定 {groups}")

# ---------------------------------------------------------------- 3. 购物车
print("\n--- 3. 购物车 ---")
call("DELETE", "/api/v1/cart?shopId=103", label="清空购物车（门店 103）")

# 大杯(+300) / 少冰 / 七分糖 / 加珍珠(+200) → 1600 + 500 = 2100
r = call("POST", "/api/v1/cart/items",
         {"dishId": 341, "specIds": [4011, 4014, 4016, 4019], "quantity": 2},
         label="加购（带规格）").get("data") or {}
line = r["lines"][0]
check(line["unitPrice"] == 2100, f"规格加价计算正确：单价 {yuan(line['unitPrice'])}（1600+300+200）")
check(line["specText"] == "大杯 / 少冰 / 七分糖 / 加珍珠", f"规格文本 {line['specText']}")

r = call("POST", "/api/v1/cart/items",
         {"dishId": 341, "specIds": [4011, 4014, 4016, 4019], "quantity": 1},
         label="同款同规格再加购").get("data") or {}
check(len(r["lines"]) == 1 and r["lines"][0]["quantity"] == 3, "同款同规格自动合并为 3 件")
check(r["itemAmount"] == 6300, f"商品小计 {yuan(r['itemAmount'])} = 21.00 × 3")

r = call("POST", "/api/v1/cart/items", {"dishId": 346, "specIds": [], "quantity": 2},
         label="加购无规格商品（蛋挞 ×2）").get("data") or {}
check(len(r["lines"]) == 2, "购物车 2 个条目")
check(r["totalQuantity"] == 5, "总件数 5")
check(r["itemAmount"] == 7900, f"商品小计 {yuan(r['itemAmount'])} = 63.00 + 16.00")

call("POST", "/api/v1/cart/items", {"dishId": 305, "specIds": [], "quantity": 1},
     expect_ok=False, label="加购估清商品应被拦截")
call("PUT", "/api/v1/cart/items/999999", {"quantity": 1}, expect_ok=False,
     label="修改不存在条目应被拦截")

# ---------------------------------------------------------------- 4. 结算试算
print("\n--- 4. 结算试算（配送 / 自取，服务端唯一计算） ---")
prev_d = call("POST", "/api/v1/orders/preview",
              {"shopId": 103, "fulfillType": "DELIVERY", "addressId": 1},
              label="试算 · 平台配送").get("data") or {}
a = prev_d["amount"]
print(f"      配送：商品 {yuan(a['itemAmount'])} + 打包 {yuan(a['packFee'])}"
      f" + 配送 {yuan(a['deliveryFee'])} - 优惠 {yuan(a['discountAmount'])}"
      f" = 实付 {yuan(a['payAmount'])}   [{a.get('fullReduceRule')}]")
check(a["itemAmount"] == 7900, "商品小计 79.00")
check(a["packFee"] == 500, "打包费 1.00/件 × 5 件 = 5.00")
check(a["deliveryFee"] == 200, "配送费 2.00")
check(a["fullReduceAmount"] == 400, "命中门店满 20 减 4")
check(a["payAmount"] == 8200, "配送实付 82.00")
check(a["payAmount"] == a["itemAmount"] + a["packFee"] + a["deliveryFee"] - a["discountAmount"],
      "恒等式：实付 = 小计 + 打包 + 配送 - 优惠")
check(prev_d["address"]["phoneMasked"] == "138****1111", "试算返回脱敏手机号")

prev_p = call("POST", "/api/v1/orders/preview", {"shopId": 103, "fulfillType": "PICKUP"},
              label="试算 · 到店自取").get("data") or {}
ap = prev_p["amount"]
print(f"      自取：商品 {yuan(ap['itemAmount'])} + 打包 {yuan(ap['packFee'])}"
      f" + 配送 {yuan(ap['deliveryFee'])} - 满减 {yuan(ap['fullReduceAmount'])}"
      f" - 自取立减 {yuan(ap['pickupDiscountAmount'])} = 实付 {yuan(ap['payAmount'])}")
check(ap["deliveryFee"] == 0, "自取单免配送费")
check(ap["pickupDiscountAmount"] == 300, "自取立减 3.00")
check(ap["payAmount"] == 7700, "自取实付 77.00")
check(prev_p.get("address") is None, "自取单不返回收货地址")
check("取餐码" in (prev_p.get("pickupTip") or ""), "自取单返回取餐码提示")

call("POST", "/api/v1/orders/preview", {"shopId": 104, "fulfillType": "PICKUP"},
     expect_ok=False, label="门店不支持自取应被拦截")

# 起送价：门店 104 起送 16.00，1 个卤蛋 3.00
call("DELETE", "/api/v1/cart?shopId=104", label="清空购物车（门店 104）")
call("POST", "/api/v1/cart/items", {"dishId": 365, "specIds": [], "quantity": 1}, label="加购 1 个卤蛋")
mp = call("POST", "/api/v1/orders/preview", {"shopId": 104, "fulfillType": "DELIVERY", "addressId": 1},
          label="试算 · 未达起送价").get("data") or {}
check(mp["amount"]["reachMin"] is False, "未达起送价：试算返回 reachMin=false（页面展示「还差 ¥X 起送」）")
check(mp["amount"]["minPriceGap"] == 1300, f"距起送价差额 {yuan(mp['amount']['minPriceGap'])}")
call("POST", "/api/v1/orders", {"shopId": 104, "fulfillType": "DELIVERY", "addressId": 1},
     headers={"Idempotency-Key": str(uuid.uuid4())},
     expect_ok=False, label="未达起送价提交订单应被拦截")
call("DELETE", "/api/v1/cart?shopId=104", label="清空购物车（门店 104）")

# ---------------------------------------------------------------- 5. 下单
print("\n--- 5. 提交订单（幂等 / 越权） ---")
idem = str(uuid.uuid4())
order = call("POST", "/api/v1/orders",
             {"shopId": 103, "fulfillType": "DELIVERY", "addressId": 1, "remark": "少冰七分糖，多给吸管"},
             headers={"Idempotency-Key": idem}, label="提交配送单").get("data") or {}
order_no = order["orderNo"]
print(f"      订单号 {order_no} · 状态 {order['statusLabel']} · 实付 {yuan(order['payAmount'])}"
      f" · 预计 {order.get('promiseText')}")
check(order["status"] == "UNPAID", "下单后状态为待支付")
check(order["payAmount"] == prev_d["amount"]["payAmount"], "下单金额与试算一致（服务端口径）")
check(order["receiverPhone"] == "138****1111", "收货手机号已脱敏")
check(order["actions"] == ["PAY"], "待支付订单可执行操作为 PAY")
check(len(order["items"]) == 2, "订单明细 2 行")
check("提交订单" in order["timeline"][0]["remark"], "状态流水首条为提交订单")

again = call("POST", "/api/v1/orders",
             {"shopId": 103, "fulfillType": "DELIVERY", "addressId": 1},
             headers={"Idempotency-Key": idem}, label="同 Idempotency-Key 重复提交").get("data") or {}
check(again["orderNo"] == order_no, f"幂等命中，返回同一订单 {again['orderNo']}")

cart_after = call("GET", "/api/v1/cart?shopId=103", label="下单后购物车").get("data") or {}
check(cart_after["totalQuantity"] == 0, "已下单条目从购物车移除")

# 水平越权拦截（S2 修复后）：X-User-Id 请求头不再被信任，
# 改用「另一个真实用户的令牌」访问他人订单，必须被拒绝。
call("POST", "/api/v1/auth/sms/code", {"phone": "13900001234"},
     label="越权用例 · 第二用户发送验证码")
rival = call("POST", "/api/v1/auth/sms/login",
             {"phone": "13900001234", "code": "8888"}, label="越权用例 · 登录第二用户")
rival_token = (rival.get("data") or {}).get("token") or ""
call("GET", "/api/v1/orders/" + order_no, headers={"Authorization": "Bearer " + rival_token},
     expect_ok=False, label="他人订单不可见（水平越权拦截）")

# ---------------------------------------------------------------- 6. 支付
print("\n--- 6. 支付（三条通道） ---")
channels = call("GET", "/api/v1/pay/channels", label="支付渠道列表").get("data") or []
for c in channels:
    print(f"      {c['code']:16s} {c['label']:10s} mock={str(c['mock']):5s} {c['remark']}")
check(len(channels) == 3, "三通道：支付宝沙箱 / 微信(模拟) / 余额")

prepay = call("POST", "/api/v1/pay/prepay", {"orderNo": order_no, "channel": "WECHAT_MOCK"},
              label="发起支付（微信模拟）").get("data") or {}
pay_no = prepay["payNo"]
# S1 修复：模拟通道由服务端在 prepay 事务内直接结算，前端不再调用回调接口
check(prepay["status"] == "SUCCESS", "模拟通道同步结算成功")
check(prepay["amount"] == order["payAmount"], "支付金额与订单实付一致")

# S1 修复：回调入口必须验签，未携带签名的调用一律拒绝
call("POST", f"/api/v1/pay/notify/{pay_no}", expect_ok=False,
     label="未签名回调应被拒绝（验签拦截）")
call("POST", f"/api/v1/pay/notify/{pay_no}?amount=1", headers={"X-Pay-Sign": "bad-signature"},
     expect_ok=False, label="伪造签名回调应被拒绝")
paid = call("GET", "/api/v1/orders/" + order_no, label="支付后订单详情").get("data") or {}
check(paid["status"] == "WAIT_ACCEPT", "支付成功后状态推进为待接单")
check(len([x for x in paid["timeline"] if "支付成功" in (x["remark"] or "")]) == 1,
      "支付回调幂等：状态流水只记一条「支付成功」")
call("POST", "/api/v1/pay/prepay", {"orderNo": order_no, "channel": "BALANCE"},
     expect_ok=False, label="重复支付应被拦截")

# ---------------------------------------------------------------- 7. 状态机
print("\n--- 7. 状态机推进（模拟商家 / 骑手） ---")
for expect in ["PREPARING", "WAIT_PICKUP", "DELIVERING", "DELIVERED", "COMPLETED"]:
    o = call("POST", f"/api/v1/admin/orders/{order_no}/advance", label=f"推进 → {expect}").get("data") or {}
    check(o["status"] == expect, f"状态推进到「{o['statusLabel']}」")
call("POST", f"/api/v1/admin/orders/{order_no}/advance", expect_ok=False,
     label="终态后继续推进应被拦截")
call("POST", f"/api/v1/orders/{order_no}/cancel", expect_ok=False,
     label="已完成订单不可取消（状态机拦截）")

# ---------------------------------------------------------------- 8. 自取链路
print("\n--- 8. 到店自取全链路（取餐码核销） ---")
call("DELETE", "/api/v1/cart?shopId=102", label="清空购物车（门店 102）")
call("POST", "/api/v1/cart/items", {"dishId": 321, "specIds": [4031], "quantity": 1}, label="加购沙拉")
pk_prev = call("POST", "/api/v1/orders/preview", {"shopId": 102, "fulfillType": "PICKUP"},
               label="试算 · 门店 102 自取").get("data") or {}
print(f"      自取试算：商品 {yuan(pk_prev['amount']['itemAmount'])} - 满减"
      f" {yuan(pk_prev['amount']['fullReduceAmount'])} - 自取立减"
      f" {yuan(pk_prev['amount']['pickupDiscountAmount'])} = {yuan(pk_prev['amount']['payAmount'])}")

pk = call("POST", "/api/v1/orders", {"shopId": 102, "fulfillType": "PICKUP", "remark": "油醋汁另装"},
          headers={"Idempotency-Key": str(uuid.uuid4())}, label="提交自取单").get("data") or {}
pno = pk["orderNo"]
check(pk["fulfillType"] == "PICKUP", "履约方式为到店自取")
check(pk["deliveryFee"] == 0, "自取单配送费为 0")
check(pk["payAmount"] == 2950, f"自取实付 {yuan(pk['payAmount'])}（36.00 + 0.50 - 5.00 - 2.00）")
check(pk.get("pickupCode") is None, "取餐码在备餐完成前不下发")

# 自取单走模拟通道：余额支付会永久扣掉演示账号的钱，跑几十次就把余额耗干，
# 于是「测试能不能过」变成「这个库被跑过多少次」。余额支付单独在 8b 里测（会退款，净零）。
pk_pay = call("POST", "/api/v1/pay/prepay", {"orderNo": pno, "channel": "WECHAT_MOCK"},
              label="发起支付（自取单 · 模拟通道）").get("data") or {}
check(pk_pay.get("status") == "SUCCESS", "自取单模拟支付同步结算")
p1 = call("GET", "/api/v1/orders/" + pno, label="支付后订单").get("data") or {}
check(p1["status"] == "WAIT_ACCEPT", "支付回调后推进为待接单")
call("GET", "/api/v1/pay/order/" + pno, label="查询订单支付单")

for step in ["PREPARING", "WAIT_MEAL", "PICKED", "COMPLETED"]:
    o = call("POST", f"/api/v1/admin/orders/{pno}/advance", label=f"自取推进 → {step}").get("data") or {}
    check(o["status"] == step, f"自取状态「{o['statusLabel']}」")
    if step == "WAIT_MEAL":
        check(bool(o["pickupCode"]), f"备餐完成下发取餐码 {o['pickupCode']}")

call("POST", f"/api/v1/orders/{pno}/pickup", expect_ok=False,
     label="已完成自取单不可再次核销（状态机拦截）")

# ---------------------------------------------------------------- 8b. 余额支付 + 取消退款
print("\n--- 8b. 余额支付与取消退款（净零余额变动） ---")
# 这条用例曾经是真实 bug：余额支付后取消订单，钱只扣不退，用户白丢一笔。
# 现在退款与取消在同一事务内，且退款发生在改状态之前 —— 退款失败则整笔回滚，
# 不会出现「订单已取消但钱没退」的中间态。
b0 = call("GET", "/api/v1/user/profile", label="退款用例 · 支付前余额").get("data") or {}

call("DELETE", "/api/v1/cart?shopId=103", label="清空购物车（门店 103）")
call("POST", "/api/v1/cart/items", {"dishId": 346, "specIds": [], "quantity": 2},
     label="加购蛋挞 ×2（退款用例）")
rf = call("POST", "/api/v1/orders",
          {"shopId": 103, "fulfillType": "DELIVERY", "addressId": 1, "remark": "退款用例"},
          headers={"Idempotency-Key": str(uuid.uuid4())}, label="提交退款用例订单").get("data") or {}
rf_no = rf["orderNo"]

call("POST", "/api/v1/pay/prepay", {"orderNo": rf_no, "channel": "BALANCE"},
     label="余额支付（同步成功）")
rf_paid = call("GET", "/api/v1/orders/" + rf_no, label="余额支付后订单").get("data") or {}
check(rf_paid["status"] == "WAIT_ACCEPT", "余额支付同步推进为待接单")

b1 = call("GET", "/api/v1/user/profile", label="余额支付后余额").get("data") or {}
check(b1["balance"] == b0["balance"] - rf["payAmount"],
      f"余额扣减正确：{yuan(b0['balance'])} → {yuan(b1['balance'])}（扣 {yuan(rf['payAmount'])}）")

call("POST", f"/api/v1/orders/{rf_no}/cancel", {"reason": "不想吃了"},
     label="取消已支付订单")
b2 = call("GET", "/api/v1/user/profile", label="取消后退款余额").get("data") or {}
check(b2["balance"] == b0["balance"],
      f"取消后余额回到原值：{yuan(b1['balance'])} → {yuan(b2['balance'])}")

rf_pay = call("GET", "/api/v1/pay/order/" + rf_no, label="退款后支付单").get("data") or {}
check(rf_pay.get("status") == "REFUNDED", f"支付单标记为 REFUNDED（实际 {rf_pay.get('status')}）")
check(bool(rf_pay.get("refundedAt")), f"支付单记录了退款时间 {rf_pay.get('refundedAt')}")

rf_done = call("GET", "/api/v1/orders/" + rf_no, label="退款后订单").get("data") or {}
check(rf_done["status"] == "CANCELED", "订单状态为已取消")
check(any("已原路退款" in (x["remark"] or "") for x in rf_done["timeline"]),
      "状态流水标注「已原路退款」，客服可据此核对")

# 幂等：再取消一次应被状态机拦截，不会退第二笔
call("POST", f"/api/v1/orders/{rf_no}/cancel", {"reason": "再取消一次"},
     expect_ok=False, label="重复取消应被状态机拦截")
b3 = call("GET", "/api/v1/user/profile", label="重复取消后余额").get("data") or {}
check(b3["balance"] == b0["balance"], f"重复取消不会重复退款（余额仍 {yuan(b3['balance'])}）")

# 未支付订单取消：不应产生退款，余额不变
call("DELETE", "/api/v1/cart?shopId=103", label="清空购物车（门店 103）")
call("POST", "/api/v1/cart/items", {"dishId": 346, "specIds": [], "quantity": 2},
     label="加购蛋挞 ×2（未支付取消用例）")
up = call("POST", "/api/v1/orders",
          {"shopId": 103, "fulfillType": "DELIVERY", "addressId": 1, "remark": "未支付取消"},
          headers={"Idempotency-Key": str(uuid.uuid4())}, label="提交未支付订单").get("data") or {}
call("POST", f"/api/v1/orders/{up['orderNo']}/cancel", {"reason": "改主意了"},
     label="取消未支付订单")
b4 = call("GET", "/api/v1/user/profile", label="未支付取消后余额").get("data") or {}
check(b4["balance"] == b0["balance"], "未支付订单取消不触发退款，余额不变")

# ---------------------------------------------------------------- 9. 列表与后台
print("\n--- 9. 订单列表 / 运营后台 ---")
lst = call("GET", "/api/v1/orders?group=待评价", label="订单列表 · 待评价").get("data") or {}
check(int(lst["total"]) >= 1, f"待评价分组命中 {lst['total']} 单")
call("GET", "/api/v1/orders?pageNum=1&pageSize=3", label="订单列表 · 全部（分页）")

cnt = call("GET", "/api/v1/orders/group-count", label="分组角标").get("data") or {}
print(f"      分组统计 {json.dumps(cnt, ensure_ascii=False)}")

ov = call("GET", "/api/v1/admin/overview", label="后台数据看板").get("data") or {}
print(f"      门店 {ov['shopCount']} · 商家 {ov['merchantCount']} · 商品 {ov['dishCount']}"
      f" · 订单 {ov['totalOrderCount']} · 累计 GMV {yuan(int(ov['totalGmv']))}"
      f" · 自取单 {ov['pickupOrderCount']}")
print(f"      状态分布 {json.dumps(ov['statusDistribution'], ensure_ascii=False)}")

call("GET", "/api/v1/admin/orders?pageNum=1&pageSize=5", label="后台订单列表")
call("GET", "/api/v1/admin/shops", label="后台门店列表")
call("GET", "/api/v1/admin/dishes?shopId=103", label="后台门店商品")
call("GET", "/api/v1/admin/payments", label="后台支付流水")
call("GET", "/api/v1/user/addresses", label="收货地址列表")

# ---------------------------------------------------------------- 10. 骑手端链路
print("\n--- 10. 骑手端：接单大厅 / 抢单 / 取货 / 送达 / 结算 ---")
RIDER_HEADER = {"X-Rider-Id": "1"}

prof = call("GET", "/api/v1/rider/profile", headers=RIDER_HEADER, label="骑手档案")
check((prof.get("data") or {}).get("name") == "王骑手", "骑手档案返回演示骑手")

call("GET", "/api/v1/rider/hall", headers=RIDER_HEADER, label="接单大厅（造数据前）")

# 自建一张「待取货」的配送单来跑骑手链路。
# 原来这里断言种子单 9004/9005 在大厅里，但抢单/取货/送达会永久改写它们的状态，
# 于是「断言是否通过」取决于数据库被跑过几次 —— 第二次跑必挂，且看起来像功能坏了。
# 测试必须自己造数据。
call("DELETE", "/api/v1/cart?shopId=103", label="清空购物车（门店 103）")
call("POST", "/api/v1/cart/items", {"dishId": 341, "specIds": [4011, 4014, 4016, 4019], "quantity": 1},
     label="加购（骑手用例专用单）")
rt = call("POST", "/api/v1/orders",
          {"shopId": 103, "fulfillType": "DELIVERY", "addressId": 1, "remark": "骑手端用例"},
          headers={"Idempotency-Key": str(uuid.uuid4())}, label="提交骑手用例订单").get("data") or {}
rt_no = rt["orderNo"]

# 走微信模拟通道（服务端同步结算），避免消耗演示账号的余额（余额支付每次会扣钱，跑多了会耗干）
rt_pay = call("POST", "/api/v1/pay/prepay", {"orderNo": rt_no, "channel": "WECHAT_MOCK"},
              label="发起支付（骑手用例单）").get("data") or {}
check(rt_pay.get("status") == "SUCCESS", "骑手用例单模拟支付同步结算")

for step in ["PREPARING", "WAIT_PICKUP"]:
    call("POST", f"/api/v1/admin/orders/{rt_no}/advance", label=f"推进骑手用例单 → {step}")

hall = call("GET", "/api/v1/rider/hall", headers=RIDER_HEADER, label="接单大厅（复查）")
hall_nos = [h["orderNo"] for h in (hall.get("data") or [])]
check(rt_no in hall_nos, f"自建的待取货单 {rt_no} 出现在接单大厅")

# 抢单（WAIT_PICKUP）
g = call("POST", f"/api/v1/rider/tasks/{rt_no}/grab", headers=RIDER_HEADER, label="骑手抢单")
check(g.get("code") == 0 and (g.get("data") or {}).get("canPickup") is True, "抢单成功且进入待取货（可取货）")

# 重复抢单应失败（已派单）
call("POST", f"/api/v1/rider/tasks/{rt_no}/grab", headers=RIDER_HEADER, expect_ok=False, label="重复抢单被拒")

# 未取货直接送达应失败
call("POST", f"/api/v1/rider/tasks/{rt_no}/deliver", headers=RIDER_HEADER, expect_ok=False, label="未取货送达被拒")

# 进行中任务列表应含刚抢下的单
tk = call("GET", "/api/v1/rider/tasks?group=进行中", headers=RIDER_HEADER, label="骑手进行中任务")
tk_nos = [t["orderNo"] for t in ((tk.get("data") or {}).get("list") or [])]
check(rt_no in tk_nos, f"进行中任务含刚抢下的 {rt_no}")

# 取货 → 送达
call("POST", f"/api/v1/rider/tasks/{rt_no}/pickup", headers=RIDER_HEADER, label="骑手取货")
dv = call("POST", f"/api/v1/rider/tasks/{rt_no}/deliver", headers=RIDER_HEADER, label="骑手送达")
check(dv.get("code") == 0 and (dv.get("data") or {}).get("status") == "DELIVERED", "送达后状态 DELIVERED")

# 送达后结算：收入明细应含这张单
inc = call("GET", "/api/v1/rider/income", headers=RIDER_HEADER, label="骑手收入")
inc_nos = [i["orderNo"] for i in ((inc.get("data") or {}).get("list") or [])]
check(rt_no in inc_nos, f"收入明细含已送达单 {rt_no}")

# 送达后再次抢单应失败（终态不可接单）
call("POST", f"/api/v1/rider/tasks/{rt_no}/grab", headers=RIDER_HEADER, expect_ok=False,
     label="已送达订单不可再接单")

# 非配送单 / 非本人订单不可查看
call("GET", "/api/v1/rider/tasks/SG20261016100003", headers=RIDER_HEADER, expect_ok=False, label="自取单不可作为骑手任务")

# ---------------------------------------------------------------- 11. 运营后台（独立 admin-web 对接，只读冒烟）
print("\n--- 11. 运营后台接口冒烟 ---")
# 登录（演示账号 admin/123456），返回 token 写入后续请求
login = call("POST", "/api/v1/admin/login", body={"username": "admin", "password": "123456"}, label="运营登录")
ADMIN_HEADER = {}
if login.get("code") == 0 and login.get("data"):
    ADMIN_HEADER = {"X-Admin-Token": str(login["data"]["token"])}
ov2 = call("GET", "/api/v1/admin/overview", headers=ADMIN_HEADER, label="后台看板")
check((ov2.get("data") or {}).get("shopCount") is not None, "看板返回门店数等指标")
call("GET", "/api/v1/admin/dashboard/trend?days=7", headers=ADMIN_HEADER, label="趋势数据")
call("GET", "/api/v1/admin/shops", headers=ADMIN_HEADER, label="门店列表")
call("GET", "/api/v1/admin/dishes", headers=ADMIN_HEADER, label="商品列表")
rm = call("GET", "/api/v1/admin/rating-monitor", headers=ADMIN_HEADER, label="评分监控")
check(isinstance(rm.get("data"), list) and len(rm.get("data")) > 0, "评分监控返回门店列表")
apps = call("GET", "/api/v1/admin/merchant-applications?status=PENDING", headers=ADMIN_HEADER, label="入驻申请(待审核)")
check(isinstance(apps.get("data"), list), "入驻申请列表返回")
disp = call("GET", "/api/v1/admin/dispatch/overview", headers=ADMIN_HEADER, label="骑手调度概览")
check((disp.get("data") or {}).get("pendingOrders") is not None, "调度概览含待分配单")
us = call("GET", "/api/v1/admin/users", headers=ADMIN_HEADER, label="用户列表")
check((us.get("data") or {}).get("list") is not None, "用户列表返回")
cp = call("GET", "/api/v1/admin/complaints", headers=ADMIN_HEADER, label="投诉列表")
check((cp.get("data") or {}).get("list") is not None, "投诉列表返回")
od = call("GET", "/api/v1/admin/orders/SG20261015100001", headers=ADMIN_HEADER, label="后台订单详情")
check((od.get("data") or {}).get("orderNo") == "SG20261015100001", "后台订单详情返回")

# ---------------------------------------------------------------- 汇总
print("\n" + "=" * 78)
print(f"通过 {len(PASS)} 项 · 失败 {len(FAIL)} 项")
if FAIL:
    print("失败项：")
    for f in FAIL:
        print("  - " + f)
    sys.exit(1)
print("端到端链路全部通过")
print("=" * 78)
