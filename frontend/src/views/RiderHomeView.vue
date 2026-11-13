<template>
  <div class="page theme-rider page--notab rider-home">
    <NavBar title="骑手工作台" :back="false" brand>
      <template #extra>
        <div class="rider-home__bell" @click="msgOpen = true">
          <Icon name="chat" :size="20" color="#fff" />
          <i v-if="unread > 0" class="rider-home__dot">{{ unread > 99 ? '99+' : unread }}</i>
        </div>
        <span class="rider-home__logout" @click="logout">退出</span>
      </template>
    </NavBar>

    <div class="page-body" v-if="ready">
      <!-- 骑手信息 + 上线 -->
      <div class="rider-card">
        <div class="rider-card__avatar"><Icon name="user" :size="22" color="#fff" /></div>
        <div class="flex-1" style="min-width: 0">
          <div class="row" style="gap: 8px">
            <span class="rider-card__name">{{ profile.name }}</span>
            <span class="rider-card__rating num">★ {{ profile.rating }}</span>
          </div>
          <div class="rider-card__phone num">{{ profile.phoneMasked }} · {{ profile.vehicle }}</div>
        </div>
        <button
          class="rider-online"
          :class="profile.online ? 'rider-online--on' : 'rider-online--off'"
          @click="toggleOnline"
        >
          {{ profile.online ? '接单中' : '已下班' }}
        </button>
      </div>

      <!-- 今日数据 -->
      <div class="stat-row">
        <div class="stat-row__item">
          <div class="price"><span class="price__sym">¥</span>{{ yuan(profile.todayIncome) }}</div>
          <span>今日收入</span>
        </div>
        <div class="stat-row__item">
          <div class="num stat-row__num">{{ profile.completedCount }}</div>
          <span>累计单量</span>
        </div>
        <div class="stat-row__item">
          <div class="num stat-row__num">{{ yuan(profile.balance) }}</div>
          <span>可用余额</span>
        </div>
      </div>

      <!-- 进行中任务 -->
      <div class="section-title">进行中任务 <small>已接单的配送</small></div>
      <div v-if="activeTasks.length" class="active-list">
        <div
          v-for="t in activeTasks"
          :key="t.orderNo"
          class="active-card"
          @click="$router.push(`/rider/task/${t.orderNo}`)"
        >
          <PhImage class="active-card__img" :image="t.shopHeaderImg" :label="t.shopName.slice(0, 2)" />
          <div class="flex-1" style="min-width: 0">
            <div class="row row--between">
              <span class="text-strong ellipsis">{{ t.shopName }}</span>
              <span class="tag" :class="statusTagClass(t.status)">{{ t.statusLabel }}</span>
            </div>
            <div class="text-desc ellipsis mt-4">{{ t.receiverAddress }}</div>
            <div class="text-aux mt-4 num">{{ t.itemCount }} 件 · {{ t.promiseText }}</div>
          </div>
        </div>
      </div>
      <div v-else class="text-aux mt-8">暂无进行中任务，去接单大厅看看吧</div>

      <!-- 接单大厅 -->
      <div class="section-title">接单大厅 <small>商家已出餐 / 即将出餐</small></div>
      <div v-if="hall.length" class="hall-list">
        <div v-for="t in hall" :key="t.orderNo" class="hall-card">
          <PhImage class="hall-card__img" :image="t.shopHeaderImg" :label="t.shopName.slice(0, 2)" />
          <div class="flex-1" style="min-width: 0">
            <div class="row row--between">
              <span class="text-strong ellipsis">{{ t.shopName }}</span>
              <span class="tag" :class="statusTagClass(t.status)">
                {{ t.status === 'PREPARING' ? '即将取货' : '待取货' }}
              </span>
            </div>
            <div class="text-desc ellipsis mt-4">{{ t.receiverAddress }}</div>
            <div class="text-aux mt-4 num">
              {{ t.itemCount }} 件 · 距您 {{ t.distanceKm }}km · {{ t.promiseText }}
            </div>
          </div>
          <button class="btn btn--primary btn--sm" @click="grab(t.orderNo)">抢单</button>
        </div>
      </div>
      <EmptyState
        v-else
        icon="truck"
        title="暂无可接订单"
        desc="商家出餐完成后会出现在这里"
      />
    </div>

    <!-- 骑手底部导航 -->
    <nav class="rider-tabbar">
      <div class="rider-tabbar__item rider-tabbar__item--active">
        <Icon name="home" :size="20" />
        <span>接单</span>
      </div>
      <div class="rider-tabbar__item" @click="$router.push('/rider/income')">
        <Icon name="wallet" :size="20" />
        <span>收入</span>
      </div>
      <div class="rider-tabbar__item" @click="$router.push('/mine')">
        <Icon name="user" :size="20" />
        <span>我的</span>
      </div>
    </nav>

    <!-- 消息抽屉 -->
    <div v-if="msgOpen" class="mask" @click.self="msgOpen = false">
      <div class="sheet">
        <div class="sheet__head">
          <div class="text-strong">消息中心</div>
          <button class="sheet__close" @click="msgOpen = false"><Icon name="close" :size="13" /></button>
        </div>
        <div class="sheet__body">
          <div v-for="m in messages" :key="m.id" class="rmsg" :class="{ 'rmsg--unread': !m.isRead }">
            <div class="rmsg__title row row--between">
              <span class="text-strong">{{ m.title }}</span>
              <span class="text-aux num">{{ fromNow(m.createdAt) }}</span>
            </div>
            <div class="text-desc mt-4">{{ m.content }}</div>
          </div>
          <EmptyState v-if="!messages.length" icon="chat" title="暂无消息" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import PhImage from '../components/PhImage.vue'
import EmptyState from '../components/EmptyState.vue'
import {
  getRiderProfile,
  setRiderOnline,
  getRiderHall,
  getRiderTasks,
  getRiderMessages,
  grabOrder,
  readRiderMessage
} from '../api/rider'
import { toast } from '../api/http'
import { yuan, fromNow, statusTagClass } from '../utils/format'
import { clearRiderToken } from '../utils/rider'

const router = useRouter()

const ready = ref(false)
const profile = ref({})
const hall = ref([])
const activeTasks = ref([])
const messages = ref([])
const msgOpen = ref(false)

const unread = computed(() => messages.value.filter(m => !m.isRead).length)

async function load() {
  const [p, h, t, m] = await Promise.all([
    getRiderProfile(),
    getRiderHall(),
    getRiderTasks({ group: '进行中', pageSize: 10 }),
    getRiderMessages()
  ])
  profile.value = p.data || {}
  hall.value = h.data || []
  activeTasks.value = (t.data?.list) || []
  messages.value = m.data || []
  ready.value = true
}

async function logout() {
  clearRiderToken()
  toast('已退出骑手端', 'info')
  router.replace('/rider/login')
}

async function toggleOnline() {
  const online = !profile.value.online
  const r = await setRiderOnline(online)
  profile.value = r.data || profile.value
  toast(online ? '已上线，开始接单' : '已下班', online ? 'success' : 'info')
}

async function grab(orderNo) {
  await grabOrder(orderNo)
  toast('抢单成功', 'success')
  await load()
}

// 打开消息抽屉时自动标记已读
watch(msgOpen, async (open) => {
  if (!open) return
  const r = await getRiderMessages()
  const list = r.data || []
  list.forEach(m => { if (!m.isRead) readRiderMessage(m.id) })
  messages.value = list
})

onMounted(load)
</script>

<style scoped>
.rider-home__bell { position: relative; }
.rider-home__logout {
  margin-left: 14px;
  font-size: 12px;
  color: rgba(255, 255, 255, .85);
  -webkit-user-select: none;
  user-select: none;
}
.rider-home__dot {
  position: absolute; top: -4px; right: -6px;
  min-width: 15px; height: 15px; padding: 0 4px;
  border-radius: 8px; background: var(--c-error); color: #fff;
  font-size: 9px; line-height: 15px; text-align: center;
}

.rider-card {
  display: flex; align-items: center; gap: 12px;
  background: linear-gradient(135deg, #2A3040, #171B26);
  border-radius: var(--r-card); padding: 14px; color: #fff;
}
.rider-card__avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: rgba(255, 255, 255, .18);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.rider-card__name { font-size: var(--fs-title-lg); font-weight: var(--fw-title); }
.rider-card__rating { font-size: var(--fs-aux); color: #FFC46B; }
.rider-card__phone { font-size: var(--fs-aux); opacity: .85; margin-top: 3px; }
.rider-online {
  height: 30px; padding: 0 14px; border-radius: 15px; font-size: var(--fs-aux); font-weight: var(--fw-title);
}
.rider-online--on { background: #12BFA0; color: #fff; }
.rider-online--off { background: rgba(255, 255, 255, .16); color: #fff; }

.stat-row {
  display: flex; margin-top: 10px; background: var(--bg-card);
  border-radius: var(--r-card); padding: 14px 0; box-shadow: var(--sh-light);
}
.stat-row__item { flex: 1; text-align: center; display: flex; flex-direction: column; gap: 3px; font-size: var(--fs-aux); color: var(--t-3); }
.stat-row__num { font-size: var(--fs-price); font-weight: var(--fw-title); color: var(--t-1); }

.active-list, .hall-list { display: flex; flex-direction: column; gap: 8px; }
.active-card, .hall-card {
  display: flex; gap: 10px; align-items: center;
  background: var(--bg-card); border-radius: var(--r-card); padding: 11px; box-shadow: var(--sh-light);
}
.active-card__img, .hall-card__img { width: 48px; height: 48px; flex-shrink: 0; }

.rider-tabbar {
  position: fixed; left: 50%; transform: translateX(-50%); bottom: 0;
  width: 100%; max-width: var(--app-max);
  height: calc(var(--tabbar-h) + env(safe-area-inset-bottom, 0px));
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: var(--bg-card); border-top: 1px solid var(--line);
  display: flex; z-index: 40;
}
.rider-tabbar__item {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px;
  font-size: 10px; color: var(--t-3);
}
.rider-tabbar__item--active { color: var(--tab-active); }

.rmsg { background: var(--bg-card); border-radius: var(--r-card); padding: 11px; box-shadow: var(--sh-light); }
.rmsg + .rmsg { margin-top: 8px; }
.rmsg--unread { box-shadow: inset 0 0 0 1px #FFE0B2; }
</style>
