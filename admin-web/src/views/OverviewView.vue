<template>
  <div class="overview">
    <div class="overview__head">
      <div class="overview__logo"><Icon name="overview" :size="22" color="#fff" /></div>
      <div>
        <h2>数创星光外卖 · 交付总览</h2>
        <div class="sub">6 个系统 / 端 · 约 165 个页面 · 约 290 个功能点，全部可在个人开发者环境端到端完整实现与演示</div>
      </div>
    </div>

    <!-- ① 系统与端 -->
    <div class="sys-grid">
      <div v-for="s in systems" :key="s.name" class="sys-card" :style="{ '--accent': s.accent }">
        <div class="sys-card__head">
          <span class="sys-card__icon"><Icon :name="s.icon" :size="16" :color="s.accent" /></span>
          <span class="sys-card__title">{{ s.name }}</span>
        </div>
        <div class="sys-card__form">{{ s.form }}</div>
        <div class="sys-card__pages">页面量 <b>{{ s.pages }}</b></div>
        <div class="sys-card__tags">
          <span v-for="(c, i) in s.chips" :key="c" class="chip" :class="`chip--${s.tones[i] || 'muted'}`">{{ c }}</span>
        </div>
      </div>
    </div>

    <!-- ② 关键数字 -->
    <div class="kpi-grid">
      <div v-for="k in kpis" :key="k.label" class="kpi">
        <div class="kpi__num" :style="{ color: k.color }">{{ k.value }}</div>
        <div class="kpi__label">{{ k.label }}</div>
      </div>
    </div>

    <!-- ③ 业务功能域 -->
    <div class="card mt-16">
      <div class="card__title">
        <span>业务功能域（10 个）</span>
        <small>后台按业务域组织，与用户端 / 骑手端 / 商家端功能一一对应</small>
      </div>
      <div class="domain-grid">
        <span v-for="d in domains" :key="d.name" class="domain">
          <span class="domain__icon" :style="{ background: d.color }"><Icon :name="d.icon" :size="14" color="#fff" /></span>
          {{ d.name }}
        </span>
      </div>
    </div>

    <!-- ④ 技术栈 -->
    <div class="card mt-16">
      <div class="card__title">
        <span>技术栈</span>
        <small>个人开发者全免费 / 沙箱方案，见 1.5 与 14.8</small>
      </div>
      <div class="tech-grid">
        <div class="tech-col">
          <div v-for="t in techLeft" :key="t.label" class="tech-row">
            <span class="tech-row__label"><Icon :name="t.icon" :size="15" />{{ t.label }}</span>
            <span class="tech-row__value">{{ t.value }}</span>
          </div>
        </div>
        <div class="tech-col">
          <div v-for="t in techRight" :key="t.label" class="tech-row">
            <span class="tech-row__label"><Icon :name="t.icon" :size="15" />{{ t.label }}</span>
            <span class="tech-row__value">{{ t.value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import Icon from '../components/Icon.vue'

const systems = [
  {
    name: '平台运营后台',
    form: 'Web · 独立工程 · 8 类角色',
    pages: '约 82 个页面',
    accent: '#ff7a1a',
    icon: 'dashboard',
    chips: ['商家招商', '商品审核', '订单仲裁', '配送调度', '营销与会员', '资金结算', '内容运营', '客服工单', '数据报表'],
    tones: ['p', 'p', 'p', 's', 'i', 'p', 'p', 's', 'i']
  },
  {
    name: '商家中心 Web',
    form: '独立工程 · 商家自助服务',
    pages: '约 13 个页面',
    accent: '#12bfa0',
    icon: 'shop',
    chips: ['入驻', '接单出餐', '商品管理', '自取核销', '营销', '财务结算'],
    tones: ['s', 's', 'i', 's', 'p', 'i']
  },
  {
    name: '用户端 App / H5',
    form: '消费者 · UniApp + H5',
    pages: '约 49 个页面',
    accent: '#ff7a1a',
    icon: 'user',
    chips: ['首页附近', '店铺点餐', '双履约下单', '支付', '配送追踪', '取餐码', '会员中心', '消息 IM', '评价'],
    tones: ['p', 'p', 'p', 'p', 'p', 'p', 's', 'i', 'i']
  },
  {
    name: '骑手端 App',
    form: '骑手 · 平台统一调度',
    pages: '约 24 个页面',
    accent: '#3a6bf0',
    icon: 'rider',
    chips: ['实名认证', '听单抢单', '配送任务', '导航取货', '送达留证', '收入提现'],
    tones: ['i', 'i', 'i', 'i', 'i', 'i']
  },
  {
    name: '商家端 App',
    form: '商家 · UniApp',
    pages: '约 9 个页面',
    accent: '#12bfa0',
    icon: 'chart',
    chips: ['经营工作台', '接单拒单', '出餐', '小票预览', '自取核销', '数据概览'],
    tones: ['i', 'p', 'p', 's', 'i', 'i']
  }
]

const kpis = [
  { value: '6', label: '个系统 / 端', color: '#ff7a1a' },
  { value: '约 165', label: '个页面', color: '#ff7a1a' },
  { value: '约 290', label: '个功能点', color: '#12bfa0' },
  { value: '约 8', label: '类后台角色', color: '#3a6bf0' },
  { value: '5', label: '类第三方（沙箱 / 免费额度）', color: '#3a6bf0' }
]

const domains = [
  { name: '商家招商', icon: 'shop', color: '#ff7a1a' },
  { name: '商品中心', icon: 'order', color: '#ff9f2e' },
  { name: '订单中心', icon: 'order', color: '#ff7a1a' },
  { name: '配送中心', icon: 'rider', color: '#ff9f2e' },
  { name: '营销与会员', icon: 'alert', color: '#ff7a1a' },
  { name: '用户中心', icon: 'user', color: '#ff9f2e' },
  { name: '资金结算', icon: 'chart', color: '#ff7a1a' },
  { name: '内容运营', icon: 'search', color: '#ff9f2e' },
  { name: '客服工单', icon: 'alert', color: '#ff7a1a' },
  { name: '数据报表', icon: 'dashboard', color: '#ff9f2e' }
]

const techLeft = [
  { icon: 'user', label: '前端 · 用户 / 骑手 / 商家端', value: 'UniApp（Vue 3）+ H5 自适应' },
  { icon: 'shield', label: '后端', value: 'JDK 17 · Spring Boot 3 模块化单体 · MyBatis-Plus' },
  { icon: 'alert', label: '第三方（沙箱 / 个人额度）', value: '支付宝沙箱 · 高德/百度地图个人 Key · 邮件 SMTP · 模拟短信与推送' }
]

const techRight = [
  { icon: 'dashboard', label: '前端 · 后台 / 商家中心', value: 'Vue 3 + Vite + Element Plus + ECharts' },
  { icon: 'shield', label: '中间件', value: 'MySQL 8 · Redis 6+ · RabbitMQ · MinIO · 自建 WebSocket IM' },
  { icon: 'chart', label: '部署与运维', value: 'Docker Compose 一键启动 · Nginx · GitHub / Gitee · CI' }
]
</script>

<style scoped>
.overview__head { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.overview__logo {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  background: var(--grad-brand); display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 18px rgba(245, 72, 31, .22);
}
.overview__head h2 { margin: 0; font-size: 20px; letter-spacing: .2px; }
.overview__head .sub { color: var(--text-2); font-size: 13px; margin-top: 5px; }

/* ① 系统卡片 */
.sys-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; }
.sys-card {
  position: relative; overflow: hidden;
  background: var(--panel); border: 1px solid var(--line); border-radius: var(--radius);
  box-shadow: var(--shadow); padding: 14px 16px 16px 18px;
}
.sys-card::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--accent);
}
.sys-card__head { display: flex; align-items: center; gap: 8px; }
.sys-card__icon {
  width: 26px; height: 26px; border-radius: 7px; flex-shrink: 0;
  background: var(--panel-2); border: 1px solid var(--line);
  display: inline-flex; align-items: center; justify-content: center;
}
.sys-card__title { font-weight: 600; font-size: 14px; }
.sys-card__form { color: var(--text-3); font-size: 12px; margin-top: 6px; }
.sys-card__pages { color: var(--text-2); font-size: 12px; margin: 10px 0 8px; }
.sys-card__pages b { color: var(--accent); font-size: 13px; font-variant-numeric: tabular-nums; }
.sys-card__tags { display: flex; flex-wrap: wrap; gap: 5px; }

.chip {
  font-size: 12px; line-height: 18px; padding: 1px 7px;
  border-radius: var(--radius-tag); border: 1px solid transparent; white-space: nowrap;
}
.chip--p { color: #d85a00; background: var(--primary-weak); border-color: #ffd9bd; }
.chip--s { color: #0a9c84; background: var(--success-weak); border-color: #b9ebdf; }
.chip--i { color: #2f5ad4; background: #eef2ff; border-color: #d5deff; }
.chip--w { color: #b9760f; background: var(--warn-weak); border-color: #ffe1b3; }

/* ② 关键数字 */
.kpi-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-top: 16px; }
.kpi {
  background: var(--panel); border: 1px solid var(--line); border-radius: var(--radius);
  box-shadow: var(--shadow); padding: 16px 18px;
}
.kpi__num { font-size: 34px; font-weight: 700; line-height: 1.15; font-variant-numeric: tabular-nums; letter-spacing: .5px; }
.kpi__label { color: var(--text-2); font-size: 13px; margin-top: 4px; }

/* ③ 业务功能域 */
.domain-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; }
.domain {
  display: flex; align-items: center; gap: 7px; min-width: 0;
  padding: 7px 14px 7px 8px; font-size: 13px; color: var(--text-1);
  background: var(--panel-2); border: 1px solid var(--line); border-radius: var(--radius-sm);
}
.domain__icon {
  width: 22px; height: 22px; border-radius: 6px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}

/* ④ 技术栈 */
.tech-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 44px; }
.tech-row {
  display: grid; grid-template-columns: 208px 1fr; gap: 12px; align-items: center;
  padding: 9px 0; border-bottom: 1px dashed var(--line); font-size: 13px;
}
.tech-col:last-child .tech-row:last-child,
.tech-col:first-child .tech-row:last-child { border-bottom: none; }
.tech-row__label { display: inline-flex; align-items: center; gap: 8px; color: var(--text-3); }
.tech-row__value { color: var(--text-1); }

@media (max-width: 1380px) {
  .sys-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .kpi-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 1100px) {
  .sys-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .domain-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .tech-grid { grid-template-columns: 1fr; }
}
</style>
