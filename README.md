

## 一、系统简介
（xingguang-delivery）是一套单仓多端的外卖/配送原型系统，覆盖消费者点餐、商家接单、骑手配送、平台运营的完整链路。

**多端架构一览：**

| 目录 | 端 | 技术栈 | 端口 |
|------|------|--------|------|
| `backend` | 后端 API | Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.5 + MySQL + MinIO | 8090 |
| `frontend` | 消费者 H5 | Vue3 + Vite + Pinia（含 Capacitor 6 Android 工程） | 5173 |
| `merchant-web` | 商家端 H5 | Vue3 + Vant 4 | 8086 |
| `admin-web` | 平台运营后台 | Vue3 + ECharts（桌面大屏） | 8081 |
| `design-preview` | 用户端高保真演示版 | Vue3 + Vite（演示数据，无需后端） | 8082 |
| `gateway` | Node 统一入口网关 | http-proxy 反向代理 | 9000 |
| `deploy` | Docker 版 Nginx 入口配置 | Nginx | 9000（容器映射） |

**后端业务模块**（12 个 Controller，均在 `backend/src/main/java/com/xingguang/delivery/controller/`）：

| 模块 | 接口前缀 | 功能 |
|------|----------|------|
| Auth | `/api/v1/auth` | 登录、短信验证码 |
| Shop | `/api/v1/shops` | 门店列表与详情 |
| Home | `/api/v1/home` | 首页推荐数据 |
| Cart | `/api/v1/cart` | 购物车管理 |
| Order | `/api/v1/orders` | 下单、订单状态流转 |
| Pay | `/api/v1/pay` | 支付（内置模拟支付） |
| Review | `/api/v1/reviews` | 订单评价 |
| User | `/api/v1/users` | 用户信息 |
| Merchant | `/api/v1/merchant` | 商家端接单、菜品管理 |
| Rider | `/api/v1/rider` | 骑手接单、配送状态 |
| File | `/api/v1/files` | 文件上传 |
| Admin | `/api/v1/admin` | 运营后台管理 |

## 二、环境准备

**本地开发方式**：JDK 17、Maven 3.6+、MySQL 8.x、Node.js 16+

**Docker 方式**：仅需安装 Docker（含 docker compose 插件）

## 三、方式一：本地开发启动

### 步骤 1：初始化数据库

按顺序在 MySQL 中执行 `backend/src/main/resources/db/` 下的脚本：

1. `01-schema.sql` —— 建库建表（`xingguang_delivery`）
2. `02-migration-auth.sql` —— 认证相关迁移
3. `03-migration-refund.sql` —— 退款相关迁移
4. `04-data.sql` —— 演示数据（门店、菜品、账号等）

### 步骤 2：检查后端配置

编辑 `backend/src/main/resources/application.yml`：

- 端口 **8090**，数据库 `127.0.0.1:3306/xingguang_delivery`
- 数据库密码默认 `123456`，可用环境变量 `XG_DB_PASSWORD` 覆盖
- 演示开关：模拟支付已开启（`mock-pay-enabled: true`）、短信验证码固定 `8888`
- MinIO OSS 配置（`oss` 配置节）用于文件存储，如未部署 MinIO 请关注启动时 `StartupGuard` 在控制台打印的自检告警

### 步骤 3：一键启动全部服务

推荐使用项目自带的一键脚本（PowerShell）：

```powershell
# 启动全部服务（后端 + 三个前端 + Node 网关）
.\restart-all.ps1

# 或只启动后端
.\restart-be.ps1
```

也可手动逐个启动：

```bash
# 后端
cd backend && mvn package
java -jar backend/target/xingguang-delivery.jar

# 消费者 H5（5173）
cd frontend && npm install && npm run dev

# 商家端（8086）
cd merchant-web && npm install && npm run dev

# 运营后台（8081）
cd admin-web && npm install && npm run dev

# Node 网关（9000）
cd gateway && npm install && npm start
```

### 步骤 4：通过网关访问

Node 网关（9000 端口）的路由规则：

| 入口 | 转发目标 |
|------|----------|
| `http://localhost:9000/` | 消费者 H5（5173） |
| `http://localhost:9000/api/*` | 后端 API（8090） |
| `http://localhost:9000/admin/*` | 运营后台（8081） |
| `http://localhost:9000/merchant/*` | 商家端（8086） |
| `http://localhost:9000/user/*` | 高保真演示版（8082） |

后端在线文档：**http://localhost:8090/swagger-ui.html**

## 四、方式二：Docker 一键部署

```bash
docker compose up -d --build
```

该命令会启动三个服务：

1. **mysql**（mysql:8.0）—— 首次启动自动按序执行 `backend/src/main/resources/db/` 下的 SQL 完成建库建表灌数据，数据持久化在 `mysql-data` 卷
2. **backend** —— 后端 API（8090），自动连接 compose 内的 MySQL
3. **web** —— Nginx 静态服务 + 反向代理，监听容器 80 端口映射到宿主 **9000**，直接替代 Node 网关

部署完成后统一访问 **http://localhost:9000**。

## 五、演示账号与演示数据

演示账号密码均为 **123456**（来自 `db/04-data.sql`）：

| 端 | 账号 | 说明 |
|------|------|------|
| 运营后台 | `admin` | 平台超管 |
| 运营后台 | `operator` | 运营人员 |
| 运营后台 | `cs` | 客服 |
| 商家端 | `m101` ~ `m107` | 各演示门店的商家账号 |
| 消费者端 | 无需注册 | 未登录自动回落到演示用户；短信验证码固定 **8888** |

演示模式开关（`application.yml`）：
- `mock-pay-enabled: true` —— 模拟支付，无需真实支付渠道
- `mock-sms-code: 8888` —— 短信验证码固定值
- `XG_DEMO_IDENTITY` 环境变量可关闭演示身份回落

## 六、典型业务流程

1. **消费者**打开 `http://localhost:9000/`，浏览门店与菜品，加入购物车并下单，使用模拟支付完成付款；
2. **商家**登录商家端（`/merchant`，账号如 `m101`），接单并出餐；
3. **骑手**通过骑手接口接单、更新配送状态；
4. **消费者**确认收货后可对订单进行评价；
5. **平台运营**登录后台（`/admin`，账号 `admin`），通过 ECharts 大屏查看经营数据，管理门店、菜品与订单；
6. 全程可通过 `/api/v1/*` 接口在 Swagger 文档中查看与调试。

## 七、Android App 打包（可选）

消费者端内置 Capacitor 6 Android 工程（`frontend/android/`），提供快捷脚本：

```bash
cd frontend
npm run app:sync   # vite build + cap sync android
npm run app:apk    # 构建并生成 Debug APK
npm run app:open   # 在 Android Studio 中打开工程
```

APK 产物位于 `frontend/android/app/build/outputs/apk/debug/app-debug.apk`。

> 注意：Android 构建需要 JDK 17 环境，请勿使用 JDK 21。

## 八、常见问题

| 问题 | 解决方法 |
|------|----------|
| 后端启动失败 / 数据库连接报错 | 确认 MySQL 已启动且已执行 `db/` 下全部 SQL 脚本；密码不符时设置环境变量 `XG_DB_PASSWORD` |
| 端口被占用 | 各端口均为固定/严格端口（5173 为 strictPort），请释放 8090/5173/8086/8081/8082/9000 后重试 |
| 登录短信验证码 | 演示模式固定为 `8888` |
| 支付走不通 | 确认 `mock-pay-enabled: true`（演示模式内置模拟支付，无需真实渠道） |
| 图片/文件上传异常 | 检查 `application.yml` 中 MinIO `oss` 配置，或运行 `scripts/minio-init.ps1` 初始化存储桶 |
| Docker 首次启动无数据 | compose 的 MySQL 仅在**首次**初始化时执行 SQL；如需重灌数据，删除 `mysql-data` 卷后重新 `docker compose up -d --build` |
| 想看纯前端演示 | 访问 `design-preview`（网关路由 `/user`），演示数据渲染，无需启动后端 |

## 九、目录结构

```
外卖/
├── backend/            后端 API（Spring Boot 3.2.5，端口 8090）
│   └── src/main/resources/db/    建库建表与演示数据 SQL
├── frontend/           消费者 H5（Vue3 + Vite，5173，含 Android 工程）
├── merchant-web/       商家端 H5（Vant4，8086）
├── admin-web/          运营后台大屏（ECharts，8081）
├── design-preview/     高保真演示版（8082）
├── gateway/            Node 反向代理网关（9000）
├── deploy/             Docker 版 Nginx 配置
├── scripts/            辅助脚本（e2e 测试、截图、MinIO 初始化、种子图片等）
├── docs/               项目文档与本说明
├── docker-compose.yml  Docker 一键部署（mysql + backend + web）
├── restart-all.ps1     Windows 一键启动全部服务
└── restart-be.ps1      仅重启后端
```
