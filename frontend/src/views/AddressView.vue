<template>
  <div class="page page--notab">
    <NavBar title="收货地址">
      <template #extra>
        <button class="addr-add" @click="openAdd">新增</button>
      </template>
    </NavBar>

    <div class="page-body">
      <!-- 加载态：骨架屏，不用「加载中…」这种闪一下就消失的文案 -->
      <div v-if="loading">
        <div v-for="i in 2" :key="i" class="skeleton addr-skeleton" />
      </div>

      <EmptyState
        v-else-if="!list.length"
        icon="nearby"
        title="还没有收货地址"
        desc="添加地址后，下单时可直接选择"
        action="添加地址"
        @action="openAdd"
      />

      <div v-else>
        <div v-for="a in list" :key="a.id" class="card addr-card">
          <div class="row row--between">
            <div class="row addr-card__who">
              <span class="addr-card__name">{{ a.contact }}</span>
              <span class="addr-card__phone num">{{ a.phoneMasked }}</span>
            </div>
            <span v-if="a.isDefault" class="tag tag--promo">默认</span>
          </div>

          <div class="addr-card__detail">
            <span v-if="a.tag" class="tag tag--plain addr-card__tag">{{ a.tag }}</span>
            <span class="flex-1">{{ a.detail }}</span>
          </div>

          <div class="addr-card__ops">
            <button v-if="!a.isDefault" class="btn btn--xs btn--weak" @click="setDefault(a)">
              设为默认
            </button>
            <button class="btn btn--xs btn--ghost" @click="openEdit(a)">编辑</button>
            <button class="btn btn--xs btn--weak addr-op-del" @click="remove(a)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增 / 编辑：底部弹层 -->
    <div v-if="formOpen" class="mask" @click.self="closeForm">
      <div class="sheet">
        <div class="sheet__head">
          <div>
            <div class="text-strong">{{ editing ? '编辑地址' : '新增地址' }}</div>
            <div class="text-aux mt-4">
              {{ restored ? '已恢复上次未保存的修改' : '配送员将按此地址送达' }}
            </div>
          </div>
          <button class="sheet__close" aria-label="关闭" @click="closeForm">
            <Icon name="close" :size="13" />
          </button>
        </div>

        <div class="sheet__body">
          <div class="fld">
            <label class="fld__label" for="addr-contact">联系人</label>
            <input
              id="addr-contact"
              v-model.trim="form.contact"
              class="fld__input"
              placeholder="收货人姓名"
              maxlength="20"
              autocomplete="name"
              enterkeyhint="next"
            />
          </div>

          <div class="fld">
            <label class="fld__label" for="addr-phone">手机号</label>
            <input
              id="addr-phone"
              v-model.trim="form.phone"
              class="fld__input"
              placeholder="11 位手机号"
              inputmode="numeric"
              maxlength="11"
              autocomplete="tel"
              enterkeyhint="next"
            />
          </div>

          <div class="fld">
            <label class="fld__label" for="addr-detail">详细地址</label>
            <input
              id="addr-detail"
              v-model.trim="form.detail"
              class="fld__input"
              placeholder="楼栋 / 单元 / 门牌号"
              maxlength="60"
              autocomplete="street-address"
              enterkeyhint="done"
            />
          </div>

          <div class="fld">
            <span class="fld__label">地址标签</span>
            <div>
              <button
                v-for="t in tagOptions"
                :key="t"
                type="button"
                class="opt"
                :class="{ 'opt--active': form.tag === t }"
                @click="form.tag = t"
              >
                {{ t }}
              </button>
            </div>
          </div>

          <div class="list-row">
            <span class="list-row__label">设为默认地址</span>
            <button
              class="switch"
              :class="{ 'switch--on': form.isDefault }"
              role="switch"
              :aria-checked="form.isDefault"
              aria-label="设为默认地址"
              @click="form.isDefault = !form.isDefault"
            >
              <i class="switch__dot" />
            </button>
          </div>
        </div>

        <div class="sheet__foot">
          <button class="btn btn--primary btn--block" :disabled="saving" @click="save">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import {
  listAddresses,
  addAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
} from '../api'
import { toast } from '../api/http'
import { confirmDialog } from '../utils/confirm'
import { clearDraft, loadDraft, saveDraft } from '../utils/draft'
import { lockScroll, unlockScroll } from '../utils/ui'

/** 草稿键：登录态失效跳登录后回来，据此恢复未保存的地址表单 */
const DRAFT_KEY = 'address-form'

const list = ref([])
const loading = ref(false)
const formOpen = ref(false)
const editing = ref(null)
const saving = ref(false)
/** 本次弹层内容是否来自草稿恢复（用于标题下方提示） */
const restored = ref(false)
const tagOptions = ['家', '公司', '学校']

const form = reactive({
  contact: '',
  phone: '',
  detail: '',
  tag: '家',
  isDefault: false
})

async function load() {
  loading.value = true
  try {
    const resp = await listAddresses()
    list.value = resp.data || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.contact = ''
  form.phone = ''
  form.detail = ''
  form.tag = '家'
  form.isDefault = false
}

function openAdd() {
  editing.value = null
  restored.value = false
  resetForm()
  formOpen.value = true
}

function openEdit(a) {
  editing.value = a.id
  restored.value = false
  form.contact = a.contact
  // 后端只返回脱敏手机号，编辑时必须重新输入完整号码
  form.phone = ''
  form.detail = a.detail
  form.tag = a.tag || '家'
  form.isDefault = !!a.isDefault
  formOpen.value = true
}

/** 主动关闭弹层＝放弃修改，清掉草稿，避免下次进来又弹出旧内容 */
function closeForm() {
  formOpen.value = false
  restored.value = false
  clearDraft(DRAFT_KEY)
}

/** 有未提交的草稿（登录态失效被跳转前自动暂存）则恢复并提示 */
function restoreDraft() {
  const d = loadDraft(DRAFT_KEY)
  if (!d || !d.form) return
  const f = d.form
  if (!f.contact && !f.phone && !f.detail) return
  editing.value = d.editing || null
  form.contact = f.contact || ''
  form.phone = f.phone || ''
  form.detail = f.detail || ''
  form.tag = f.tag || '家'
  form.isDefault = !!f.isDefault
  restored.value = true
  formOpen.value = true
  toast('已恢复上次未保存的地址修改', 'info')
}

async function save() {
  if (!form.contact) return toast('请填写联系人', 'error')
  if (!/^1\d{10}$/.test(form.phone)) return toast('请填写正确的 11 位手机号', 'error')
  if (!form.detail) return toast('请填写详细地址', 'error')

  const payload = {
    contact: form.contact,
    phone: form.phone,
    detail: form.detail,
    tag: form.tag,
    isDefault: form.isDefault
  }
  saving.value = true
  try {
    if (editing.value) await updateAddress(editing.value, payload)
    else await addAddress(payload)
    clearDraft(DRAFT_KEY)
    restored.value = false
    formOpen.value = false
    toast('已保存', 'success')
    await load()
  } catch (e) {
    // 错误已由响应拦截器 toast；未保存成功，草稿保留，登录后回来可继续
  } finally {
    saving.value = false
  }
}

async function setDefault(a) {
  try {
    await setDefaultAddress(a.id)
    toast('已设为默认', 'success')
    await load()
  } catch (e) {}
}

async function remove(a) {
  const ok = await confirmDialog({
    title: '删除地址',
    message: `确定删除「${a.contact}」的地址吗？删除后不可恢复。`,
    confirmText: '删除',
    danger: true
  })
  if (!ok) return
  try {
    await deleteAddress(a.id)
    toast('已删除', 'success')
    await load()
  } catch (e) {}
}

/* 表单弹层打开时锁定背景滚动，避免表单内滚动时把整页一起带走 */
let locked = false
watch(
  () => formOpen.value,
  (open) => {
    if (open && !locked) {
      lockScroll()
      locked = true
    } else if (!open && locked) {
      unlockScroll()
      locked = false
    }
  }
)

/**
 * 表单内容变化即暂存草稿。
 *
 * 关键场景：用户填到一半时令牌失效，http 拦截器会跳登录页、当前组件被卸载；
 * 有了这层暂存，登录失败或暂时放弃登录后回到本页，内容仍在（见 restoreDraft）。
 */
watch(
  form,
  () => {
    if (!formOpen.value) return
    saveDraft(DRAFT_KEY, {
      editing: editing.value,
      form: {
        contact: form.contact,
        phone: form.phone,
        detail: form.detail,
        tag: form.tag,
        isDefault: form.isDefault
      }
    }, '/address')
  },
  { deep: true }
)

onBeforeUnmount(() => {
  if (locked) {
    unlockScroll()
    locked = false
  }
})

onMounted(async () => {
  await load()
  restoreDraft()
})
</script>

<style scoped>
.addr-add {
  height: 28px;
  padding: 0 10px;
  border-radius: var(--r-btn);
  background: var(--bg-sunken);
  color: var(--brand-3);
  font-size: var(--fs-aux);
  font-weight: var(--fw-title);
  transition: transform .12s ease, opacity .12s ease;
}

.addr-add:active {
  transform: scale(.96);
  opacity: .8;
}

.addr-skeleton {
  height: 96px;
  border-radius: var(--r-card);
}

.addr-skeleton + .addr-skeleton {
  margin-top: var(--sp-card-gap);
}

/* ---------------- 地址卡 ---------------- */
.addr-card__who {
  gap: 8px;
  min-width: 0;
}

.addr-card__name {
  font-size: var(--fs-title);
  font-weight: var(--fw-title);
  color: var(--t-1);
}

.addr-card__phone {
  font-size: var(--fs-body);
  color: var(--t-2);
}

.addr-card__detail {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin-top: 8px;
  font-size: var(--fs-body);
  line-height: 1.5;
  color: var(--t-2);
}

.addr-card__tag {
  flex-shrink: 0;
  margin-top: 1px;
}

.addr-card__ops {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}

.addr-op-del {
  color: var(--c-error);
}

/* ---------------- 表单 ---------------- */
.fld + .fld {
  margin-top: 14px;
}

.fld__label {
  display: block;
  font-size: var(--fs-aux);
  color: var(--t-2);
  margin-bottom: 6px;
}

.fld__input {
  width: 100%;
  height: 42px;
  padding: 0 10px;
  border-radius: var(--r-btn);
  background: var(--bg-sunken);
  font-size: var(--fs-body);
  color: var(--t-1);
  transition: box-shadow .15s ease, background-color .15s ease;
}

.fld__input::placeholder {
  color: var(--t-3);
}

.fld__input:focus {
  background: var(--bg-card);
  box-shadow: inset 0 0 0 1px var(--brand-2);
}

/* 弹层里最后一行与上方表单留出间距 */
.sheet__body .list-row {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

/* ---------------- 开关 ---------------- */
.switch {
  width: 44px;
  height: 26px;
  border-radius: 13px;
  background: var(--line-strong);
  position: relative;
  flex-shrink: 0;
  transition: background-color .2s ease;
}

.switch__dot {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(23, 24, 26, .2);
  transition: transform .2s cubic-bezier(.32, .72, 0, 1);
}

.switch--on {
  background: var(--brand-2);
}

.switch--on .switch__dot {
  transform: translateX(18px);
}
</style>
