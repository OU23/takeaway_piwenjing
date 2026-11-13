# -*- coding: utf-8 -*-
"""
数创星光外卖 · 截图前置数据准备

分两步，因为「创建订单会清空购物车」，所以必须先在购物车状态下截结算页：

  python scripts/prepare_demo_shots.py cart     # 只灌购物车（用于截结算页）
  python scripts/prepare_demo_shots.py orders   # 下单 + 支付 + 推进状态（用于截收银台/订单详情）
  python scripts/prepare_demo_shots.py reset    # 把演示数据恢复到初始态

默认后端 http://127.0.0.1:8090
"""
import json
import sys
import urllib.request
import uuid

BASE = "http://127.0.0.1:8090/api/v1"
H = {"Content-Type": "application/json", "X-User-Id": "1"}


def call(method, path, body=None, headers=None):
    hdrs = dict(H)
    if headers:
        hdrs.update(headers)
    data = json.dumps(body, ensure_ascii=False).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, headers=hdrs, method=method)
    with urllib.request.urlopen(req, timeout=20) as r:
        return json.loads(r.read().decode())


def setup_cart():
    """门店 103（蜜茶时光）购物车：2 杯大杯珍珠奶茶 + 2 个蛋挞"""
    call("DELETE", "/cart?shopId=103")
    # 大杯(4011) / 少冰(4014) / 七分糖(4016) / 加珍珠(4019) -> 2100 分
    call("POST", "/cart/items", {"dishId": 341, "specIds": [4011, 4014, 4016, 4019], "quantity": 2})
    call("POST", "/cart/items", {"dishId": 346, "specIds": [], "quantity": 2})
    cart = call("GET", "/cart?shopId=103")["data"]
    print(f"购物车就绪：{len(cart['lines'])} 条 / {cart['totalQuantity']} 件 / 小计 {cart['itemAmount']} 分")


def make_orders():
    # ---------- 配送单：推到「配送中」（用于截骑手卡 + 地图 + 时间轴）----------
    d = call("POST", "/orders", {"shopId": 103, "fulfillType": "DELIVERY", "addressId": 1,
                                 "remark": "少冰七分糖，多给吸管"},
             {"Idempotency-Key": str(uuid.uuid4())})["data"]
    dno = d["orderNo"]
    pv = call("POST", "/pay/prepay", {"orderNo": dno, "channel": "WECHAT_MOCK"})["data"]
    call("POST", f"/pay/notify/{pv['payNo']}")
    for _ in range(3):  # WAIT_ACCEPT -> PREPARING -> WAIT_PICKUP -> DELIVERING
        call("POST", f"/admin/orders/{dno}/advance")
    print(f"配送单：{dno}  状态={call('GET', f'/orders/{dno}')['data']['statusLabel']}  实付={d['payAmount']}分")

    # ---------- 自取单：推到「待取餐」（用于截取餐码 + 二维码）----------
    call("DELETE", "/cart?shopId=102")
    call("POST", "/cart/items", {"dishId": 321, "specIds": [4031], "quantity": 1})
    p = call("POST", "/orders", {"shopId": 102, "fulfillType": "PICKUP", "remark": "油醋汁另装"},
             {"Idempotency-Key": str(uuid.uuid4())})["data"]
    pno = p["orderNo"]
    call("POST", "/pay/prepay", {"orderNo": pno, "channel": "BALANCE"})
    for _ in range(2):  # WAIT_ACCEPT -> PREPARING -> WAIT_MEAL
        call("POST", f"/admin/orders/{pno}/advance")
    detail = call("GET", f"/orders/{pno}")["data"]
    print(f"自取单：{pno}  状态={detail['statusLabel']}  取餐码={detail['pickupCode']}  实付={p['payAmount']}分")

    # ---------- 第三单：保持「待支付」，用于截收银台 ----------
    call("DELETE", "/cart?shopId=103")
    call("POST", "/cart/items", {"dishId": 341, "specIds": [4011, 4014, 4016, 4019], "quantity": 2})
    call("POST", "/cart/items", {"dishId": 346, "specIds": [], "quantity": 2})
    u = call("POST", "/orders", {"shopId": 103, "fulfillType": "DELIVERY", "addressId": 1,
                                 "remark": "少冰七分糖，多给吸管"},
             {"Idempotency-Key": str(uuid.uuid4())})["data"]
    uno = u["orderNo"]
    print(f"待支付单：{uno}  状态={u['statusLabel']}  应付={u['payAmount']}分")

    print()
    print(f"PAY_ORDER_NO={uno}")
    print(f"DELIVERY_ORDER_NO={dno}")
    print(f"PICKUP_ORDER_NO={pno}")


def reset():
    MY_SQL = [
        "use xingguang_delivery",
    ]
    print("reset 需要重跑 data.sql，请执行：")
    print("  mysql -uroot -p123456 --default-character-set=utf8mb4 < backend/src/main/resources/db/data.sql")
    print(f"（{len(MY_SQL)}）")


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "cart"
    if mode == "cart":
        setup_cart()
    elif mode == "orders":
        make_orders()
    elif mode == "reset":
        reset()
    else:
        print(__doc__)
