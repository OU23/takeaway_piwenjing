<template>
  <div class="page page--notab">
    <NavBar title="设置" />

    <div class="page-body">
      <div v-for="g in groups" :key="g.group" class="card st-card">
        <div class="st-group-title">{{ g.group }}</div>

        <template v-for="(item, i) in g.items" :key="item.path">
          <div v-if="!item.showIf || item.showIf(settings.data)" class="st-row" :class="{ 'st-row--divided': i > 0 }">
            <!-- 左侧：标签 + 说明 -->
            <div class="st-row__main">
              <div class="st-row__label">{{ item.label }}</div>
              <div v-if="item.desc" class="st-row__desc">{{ item.desc }}</div>
            </div>

            <!-- 右侧：按类型渲染控件 -->
            <!-- 开关：点击即生效 -->
            <button
              v-if="item.type === 'switch'"
              type="button"
              class="st-switch"
              role="switch"
              :aria-checked="get(item.path)"
              :class="{ 'st-switch--on': get(item.path) }"
              @click="toggle(item)"
            >
              <i class="st-switch__knob" />
            </button>

            <!-- 单选：点击弹出候选项弹层 -->
            <button v-else-if="item.type === 'choice'" type="button" class="st-picker" @click="openChoice(item)">
              <span class="st-picker__value">{{ choiceLabel(item) }}</span>
              <Icon name="chevron" :size="13" color="#C4C7CE" />
            </button>

            <!-- 时刻：原生 time 输入，跨端键盘一致 -->
            <input
              v-else-if="item.type === 'time'"
              class="st-time num"
              type="time"
              :value="get(item.path)"
              :aria-label="item.label"
              @change="onTime(item, $event)"
            />
          </div>
        </template>
      </div>

      <!-- 账户与安全 -->
      <div class="card st-card">
        <div class="st-group-title">账户与安全</div>
        <div class="st-row" @click="onAccountDelete">
          <div class="st-row__main">
            <div class="st-row__label st-row__label--danger">注销账号</div>
            <div class="st-row__desc">删除账号与全部数据，7 天冷静期内可联系客服撤回</div>
          </div>
          <Icon name="chevron" :size="13" color="#C4C7CE" />
        </div>
      </div>

      <!-- 关于 -->
      <div class="card st-card">
        <div class="st-group-title">关于</div>
        <div class="st-row">
          <div class="st-row__main"><div class="st-row__label">版本</div></div>
          <span class="st-ver num">v1.0.0</span>
        </div>
        <div class="st-row st-row--divided" @click="toast('用户协议为 SRS 9.11 交付物，由法务提供文本', 'info')">
          <div class="st-row__main"><div class="st-row__label">用户协议</div></div>
          <Icon name="chevron" :size="13" color="#C4C7CE" />
        </div>
        <div class="st-row st-row--divided" @click="toast('隐私政策为 SRS 9.11 交付物，由法务提供文本', 'info')">
          <div class="st-row__main"><div class="st-row__label">隐私政策</div></div>
          <Icon name="chevron" :size="13" color="#C4C7CE" />
        </div>
      </div>

      <!-- 恢复默认：危险操作，需确认 -->
      <button type="button" class="st-reset" @click="onReset">恢复默认设置</button>

      <div class="st-sync text-aux">
        {{ syncHint }}
      </div>
    </div>

    <!-- 单选弹层 -->
    <div v-if="choosing" class="mask" @click.self="choosing = null">
      <div class="sheet">
        <div class="sheet__head">
          <div class="text-strong">{{ choosing.label }}</div>
          <button class="sheet__close" aria-label="关闭" @click="choosing = null">
            <Icon name="close" :size="13" />
          </button>
        </div>
        <div class="sheet__body st-opts">
          <button
            v-for="o in choosing.options"
            :key="o.value"
            type="button"
            class="st-opt"
            :class="{ 'st-opt--active': o.value === get(choosing.path) }"
            @click="pick(o)"
          >
            <span>{{ o.label }}</span>
            <Icon v-if="o.value === get(choosing.path)" name="check" :size="15" color="#FF7A1A" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import { toast } from '../api/http'
import { useSettingsStore } from '../store/settings'
import { getAtPath, groupedItems } from '../store/settings-schema'
import { confirmDialog } from '../utils/confirm'

const settings = useSettingsStore()

/** 按分组渲染（showIf 条件过滤后仍归入原分组卡片） */
const groups = computed(() =>
  groupedItems().map((g) => ({ ...g, items: g.items }))
)

const get = (path) => getAtPath(settings.data, path)

/* ------------------------------ 开关 ------------------------------ */
async function toggle(item) {
  try {
    await settings.set(item.path, !get(item.path))
  } catch (e) {
    toast(e?.message || '设置失败，请重试', 'error')
  }
}

/* ------------------------------ 单选 ------------------------------ */
const choosing = ref(null)

function openChoice(item) {
  choosing.value = item
}

function choiceLabel(item) {
  const v = get(item.path)
  return item.options.find((o) => o.value === v)?.label || v
}

async function pick(option) {
  const path = choosing.value.path
  choosing.value = null
  if (option.value === get(path)) return
  try {
    await settings.set(path, option.value)
  } catch (e) {
    toast(e?.message || '设置失败，请重试', 'error')
  }
}

/* ------------------------------ 免打扰时段 ------------------------------ */
async function onTime(item, e) {
  const value = e.target.value
  e.target.value = get(item.path) // 校验失败时回显旧值
  if (!value) return
  try {
    await settings.set(item.path, value)
  } catch (err) {
    toast(err?.message || '时刻格式不正确', 'error')
  }
}

/* ------------------------------ 注销账号 ------------------------------ */
async function onAccountDelete() {
  const ok = await confirmDialog({
    title: '注销账号',
    message: '注销后账号、订单与钱包余额将被删除且不可恢复，7 天冷静期内可联系客服撤回。确定继续吗？',
    confirmText: '继续注销',
    danger: true
  })
  if (ok) toast('账号注销为 P2 能力（SRS 9.11），当前版本请联系在线客服处理', 'info')
}

/* ------------------------------ 恢复默认 ------------------------------ */
async function onReset() {
  const ok = await confirmDialog({
    title: '恢复默认设置',
    message: '将把外观、通知、隐私等全部设置恢复为默认值。',
    confirmText: '恢复默认'
  })
  if (!ok) return
  try {
    await settings.reset()
    toast('已恢复默认设置', 'success')
  } catch (e) {
    // 本地已生效，仅服务端清理失败：提示但不阻塞
    toast('已恢复本地默认，云端同步稍后自动重试', 'info')
  }
}

/** 同步状态提示：让用户知道设置是否已漫游到云端 */
const syncHint = computed(() => ({
  idle: '设置保存在本机',
  syncing: '正在同步设置…',
  ok: '设置已云端同步，换设备登录后自动恢复',
  error: '云端暂不可用，设置已保存在本机'
}[settings.syncState]))
</script>

<style scoped>
.st-card {
  padding: 4px 12px;
}

.st-group-title {
  padding: 10px 0 4px;
  font-size: var(--fs-aux);
  color: var(--t-3);
}

/* ------------------------------ 行 ------------------------------ */
.st-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 0;
  min-height: 44px;
}

.st-row--divided {
  border-top: 1px solid var(--line);
}

.st-row__main {
  flex: 1;
  min-width: 0;
}

.st-row__label {
  font-size: var(--fs-body);
  color: var(--t-1);
}

.st-row__label--danger {
  color: var(--c-error);
}

.st-row__desc {
  margin-top: 2px;
  font-size: var(--fs-aux);
  color: var(--t-3);
  line-height: 1.5;
}

/* ------------------------------ 开关 ------------------------------ */
.st-switch {
  position: relative;
  width: 44px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 13px;
  background: var(--bg-sunken);
  border: 0;
  transition: background .2s ease;
}

.st-switch--on {
  background: var(--brand-2);
}

.st-switch__knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: var(--sh-light);
  transition: transform .2s ease;
}

.st-switch--on .st-switch__knob {
  transform: translateX(18px);
}

/* ------------------------------ 单选 / 时刻 ------------------------------ */
.st-picker {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: 0;
  padding: 0;
  flex-shrink: 0;
}

.st-picker__value {
  font-size: var(--fs-body);
  color: var(--t-2);
}

.st-time {
  border: 0;
  background: none;
  padding: 0;
  font-size: var(--fs-body);
  color: var(--t-2);
  text-align: right;
  flex-shrink: 0;
}

.st-ver {
  font-size: var(--fs-body);
  color: var(--t-3);
}

/* ------------------------------ 底部操作 ------------------------------ */
.st-reset {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  border-radius: var(--r-btn);
  background: var(--bg-card);
  border: 1px solid var(--line-strong);
  font-size: var(--fs-body);
  font-weight: var(--fw-title);
  color: var(--c-error);
}

.st-reset:active {
  background: rgba(229, 72, 77, .06);
}

.st-sync {
  margin-top: 12px;
  text-align: center;
}

/* ------------------------------ 单选弹层 ------------------------------ */
.st-opts {
  padding: 0;
}

.st-opt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 13px 4px;
  background: none;
  border: 0;
  border-top: 1px solid var(--line);
  font-size: var(--fs-body);
  color: var(--t-1);
}

.st-opt:first-child {
  border-top: 0;
}

.st-opt--active {
  font-weight: var(--fw-title);
}
</style>
