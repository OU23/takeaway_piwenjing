<template>
  <div class="page page--notab">
    <NavBar title="确认订单" />

    <div v-if="!ready" style="padding: 12px">
      <div v-for="i in 3" :key="i" class="skeleton mt-12" style="height: 92px; border-radius: 10px" />
    </div>

    <template v-else>
      <div class="page-body">
        <!-- 双履约切换 -->
        <div class="fulfill">
          <button
            class="fulfill__item"
            :class="{ 'fulfill__item--active': fulfillType === 'DELIVERY' }"
            @click="switchFulfill('DELIVERY')"
          >
            <Icon name="truck" :size="15" />
            外卖配送
          </button>
          <button
            class="fulfill__item"
            :class="{
              'fulfill__item--active': fulfillType === 'PICKUP',
              'fulfill__item--disabled': !preview.pickupSupported
            }"
            @click="switchFulfill('PICKUP')"
          >
            <Icon name="store" :size="15" />
            到店自取
            <span v-if="preview.pickupSupported && shop.pickupDiscount > 0" class="tag tag--success">
              减 ¥{{ yuan(shop.pickupDiscount) }}
            </span>
          </button>
        </div>

        <!-- 配送：收货地址 -->
        <div v-if="fulfillType === 'DELIVERY'" class="card mt-12" @click="openAddrSheet">
          <div class="row row--top" style="gap: 10px">
            <Icon name="nearby" :size="19" color="#FF7A1A" />
            <div class="flex-1">
              <template v-if="preview.address">
                <div class="row" style="gap: 8px">
                  <span class="text-strong">{{ preview.address.contact }}</span>
                  <span class="text-desc num">{{ preview.address.phoneMasked }}</span>
                  <span v-if="preview.address.tag" class="tag tag--plain">{{ preview.address.tag }}</span>
                </div>
                <div class="text-desc mt-4">{{ preview.address.detail }}</div>
              </template>
              <template v-else>
                <span class="text-strong">请填写收货地址</span>
                <div class="text-aux mt-4">点击选择或新增收货地址</div>
              </template>
            </div>
            <Icon name="chevron" :size="15" color="#9CA0A8" />
          </div>

          <div class="divider" />
          <div class="row row--between">
            <span class="text-aux row" style="gap: 5px">
              <Icon name="clock" :size="13" /> 送达时间
            </span>
            <div class="seg">
              <button
                class="seg__item"
                :class="{ 'seg__item--active': !appointment }"
                @click.stop="appointment = false"
              >
                立即送出
              </button>
              <button class="seg__item" :class="{ 'seg__item--active': appointment }" @click.stop="onAppoint">
                预约送达
              </button>
            </div>
          </div>
          <div class="text-brand mt-8" style="font-size: 13px">
            {{ preview.promiseText }}
          </div>
        </div>

        <!-- 自取：门店信息 -->
        <div v-else class="card mt-12">
          <div class="row row--between">
            <span class="text-strong">{{ shop.name }}</span>
            <span class="tag tag--success">到店自取</span>
          </div>
          <div class="text-desc mt-8">{{ shop.address }}</div>
          <div class="row mt-8" style="gap: 12px">
            <span class="text-aux row" style="gap: 4px">
              <Icon name="clock" :size="12" /> {{ shop.openTime }}–{{ shop.closeTime }}
            </span>
            <span class="text-aux row" style="gap: 4px">
              <Icon name="nearby" :size="12" /> {{ shop.distanceKm }}km
            </span>
            <span class="text-aux row" style="gap: 4px">
              <Icon name="phone" :size="12" /> 022-8800-1003
            </span>
          </div>
          <div class="divider" />
          <div class="row row--between">
            <span class="text-aux">自取人手机号</span>
            <span class="text-desc num">{{ user?.phoneMasked || '138****1111' }}</span>
          </div>
          <div class="pickup-tip mt-8">
            <Icon name="info" :size="13" color="#0F8A78" />
            <span>{{ preview.pickupTip }}</span>
          </div>
          <div class="text-brand mt-8" style="font-size: 13px">
            {{ preview.promiseText }}
          </div>
        </div>

        <!-- 商品明细 -->
        <div class="card mt-12">
          <div class="card__title">
            <span class="row" style="gap: 6px">
              <PhImage style="width: 22px; height: 22px" :image="shop.headerImg" :label="''" />
              {{ shop.name }}
            </span>
            <small>共 {{ amount.totalQuantity }} 件</small>
          </div>
          <div v-for="it in preview.items" :key="it.cartItemId" class="row row--between" style="padding: 6px 0">
            <div class="flex-1" style="min-width: 0">
              <div class="ellipsis">{{ it.dishName }}</div>
              <div v-if="it.specText" class="text-aux ellipsis">{{ it.specText }}</div>
            </div>
            <div class="text-aux num" style="width: 44px; text-align: right">×{{ it.quantity }}</div>
            <div class="num" style="width: 62px; text-align: right">¥{{ yuan(it.amount) }}</div>
          </div>
        </div>

        <!-- 优惠与备注 -->
        <div class="card mt-12">
          <div class="list-row">
            <span class="list-row__label row" style="gap: 6px">
              <Icon name="coupon" :size="15" color="#FF7A1A" /> 优惠券
            </span>
            <span class="list-row__value">
              {{ amount.fullReduceAmount > 0 ? `已享 ${amount.fullReduceRule}` : '暂无可用' }}
              <Icon name="chevron" :size="13" color="#C4C7CE" />
            </span>
          </div>
          <div class="list-row">
            <span class="list-row__label row" style="gap: 6px">
              <Icon name="edit" :size="15" color="#FF7A1A" /> 订单备注
            </span>
            <input
              v-model="remark"
              class="remark-input"
              placeholder="口味 / 餐具份数等"
              maxlength="50"
              enterkeyhint="done"
              autocomplete="off"
            />
          </div>
        </div>

        <!-- 费用明细 -->
        <div class="card mt-12">
          <div class="card__title">
            <span>费用明细</span>
            <small>金额由服务端计算</small>
          </div>

          <div class="fee-row">
            <span>商品小计</span>
            <span class="fee-row__value num">¥{{ yuan(amount.itemAmount) }}</span>
          </div>
          <div class="fee-row">
            <span>打包费</span>
            <span class="fee-row__value num">¥{{ yuan(amount.packFee) }}</span>
          </div>
          <div class="fee-row">
            <span>
              配送费
              <span v-if="fulfillType === 'PICKUP'" class="text-success">（自取免配送费）</span>
            </span>
            <span class="fee-row__value num">
              <span v-if="fulfillType === 'PICKUP'" class="origin-price">¥{{ yuan(amount.deliveryFeeOrigin) }}</span>
              ¥{{ yuan(amount.deliveryFee) }}
            </span>
          </div>
          <div v-if="amount.fullReduceAmount > 0" class="fee-row fee-row--discount">
            <span>{{ amount.fullReduceRule }}</span>
            <span class="fee-row__value num">-¥{{ yuan(amount.fullReduceAmount) }}</span>
          </div>
          <div v-if="amount.pickupDiscountAmount > 0" class="fee-row fee-row--discount">
            <span>到店自取立减</span>
            <span class="fee-row__value num">-¥{{ yuan(amount.pickupDiscountAmount) }}</span>
          </div>
          <div class="fee-row fee-row--total">
            <span class="text-strong">实付</span>
            <span class="fee-row__value num">¥{{ yuan(amount.payAmount) }}</span>
          </div>

          <div v-if="!amount.reachMin" class="warnbar mt-8">
            <Icon name="alert" :size="13" color="#FF9500" />
            <span>差 ¥{{ yuan(amount.minPriceGap) }} 起送，请再选点商品</span>
          </div>
          <div v-else-if="amount.nextFullReduceHint" class="warnbar warnbar--brand mt-8">
            <Icon name="ticket" :size="13" color="#F5481F" />
            <span>{{ amount.nextFullReduceHint }}</span>
          </div>
        </div>

        <div class="text-aux mt-12" style="text-align: center; line-height: 1.6">
          提交订单前请确认履约方式与地址<br />
          金额以服务端最终计算为准（SRS 9.7 / 9.8）
        </div>
      </div>

      <!-- 底部提交栏 -->
      <div class="action-bar">
        <div class="flex-1">
          <span class="text-aux">实付</span>
          <span class="price" style="margin-left: 4px">
            <span class="price__sym">¥</span>{{ yuan(amount.payAmount) }}
          </span>
        </div>
        <button class="btn btn--primary" :class="{ 'btn--pickup': fulfillType === 'PICKUP' }" :disabled="submitting || !amount.reachMin" @click="submit">
          {{ submitting ? '提交中…' : '提交订单' }}
        </button>
      </div>
    </template>

    <!-- 地址选择 -->
    <div v-if="addrOpen" class="mask" @click.self="addrOpen = false">
      <div class="sheet">
        <div class="sheet__head">
          <div class="text-strong">选择收货地址</div>
          <button class="sheet__close" @click="addrOpen = false">
            <Icon name="close" :size="13" />
          </button>
        </div>
        <div class="sheet__body">
          <div
            v-for="a in addresses"
            :key="a.id"
            class="addr-opt"
            :class="{ 'addr-opt--active': a.id === addressId }"
            @click="chooseAddress(a)"
          >
            <div class="flex-1">
              <div class="row" style="gap: 8px">
                <span class="text-strong">{{ a.contact }}</span>
                <span class="text-desc num">{{ a.phoneMasked }}</span>
                <span v-if="a.isDefault" class="tag tag--promo">默认</span>
              </div>
              <div class="text-desc mt-4">{{ a.detail }}</div>
            </div>
            <Icon v-if="a.id === addressId" name="check" :size="16" color="#12BFA0" />
          </div>

          <div v-if="!addresses.length && !addrFormOpen" class="addr-empty">还没有收货地址，请先新增</div>

          <!-- 新增地址内联表单 -->
          <div v-if="addrFormOpen" class="addr-form">
            <div class="addr-form__row">
              <input v-model="form.contact" class="addr-field" maxlength="20" placeholder="联系人" />
              <input
                v-model="form.phone"
                class="addr-field"
                maxlength="11"
                inputmode="numeric"
                placeholder="手机号"
              />
            </div>
            <input
              v-model="form.detail"
              class="addr-field addr-field--block mt-8"
              maxlength="120"
              placeholder="详细地址（小区 / 楼栋 / 门牌号）"
            />
            <div class="row mt-8" style="gap: 6px; align-items: center">
              <button
                v-for="t in ['家', '公司', '学校']"
                :key="t"
                type="button"
                class="addr-tag"
                :class="{ 'addr-tag--on': form.tag === t }"
                @click="form.tag = form.tag === t ? '' : t"
              >
                {{ t }}
              </button>
              <label class="addr-default row" style="gap: 4px; margin-left: auto">
                <input v-model="form.isDefault" type="checkbox" /> 设为默认
              </label>
            </div>
            <div class="row mt-12" style="gap: 8px">
              <button class="btn btn--weak btn--sm flex-1" @click="addrFormOpen = false">取消</button>
              <button class="btn btn--primary btn--sm flex-1" :disabled="savingAddr" @click="saveAddress">
                {{ savingAddr ? '保存中…' : '保存并使用' }}
              </button>
            </div>
          </div>

          <button v-else class="btn btn--ghost btn--sm btn--block mt-12" @click="addrFormOpen = true">
            ＋ 新增收货地址
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import PhImage from '../components/PhImage.vue'
import { addAddress, getUserProfile, listAddresses, previewOrder, submitOrder } from '../api'
import { toast } from '../api/http'
import { uuid, yuan } from '../utils/format'
import { useAppStore } from '../store/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const shopId = route.params.shopId
const preview = ref({})
const addresses = ref([])
const addressId = ref(null)
const user = ref(null)
const fulfillType = ref('DELIVERY')
const remark = ref('')
const ready = ref(false)
const submitting = ref(false)
const addrOpen = ref(false)
const appointment = ref(false)
const addrFormOpen = ref(false)
const savingAddr = ref(false)
const form = ref({ contact: '', phone: '', detail: '', tag: '', isDefault: false })

const shop = computed(() => preview.value.shop || {})
const amount = computed(() => preview.value.amount || {})

onMounted(async () => {
  try {
    const [addrs, profile] = await Promise.all([listAddresses(), getUserProfile()])
    addresses.value = addrs.data || []
    user.value = profile.data
    addressId.value = (addresses.value.find(a => a.isDefault) || addresses.value[0])?.id || null
    await refresh()
  } finally {
    ready.value = true
  }
})

async function refresh() {
  const body = {
    shopId: Number(shopId),
    fulfillType: fulfillType.value,
    remark: remark.value || null
  }
  if (fulfillType.value === 'DELIVERY') {
    // 没有收货地址时不做试算（后端会报错），先引导用户填写
    if (!addressId.value) {
      preview.value = {}
      return
    }
    body.addressId = addressId.value
  }
  const r = await previewOrder(body)
  preview.value = r.data || {}
  if (fulfillType.value === 'PICKUP') {
    // 门店不支持自取时回退
    if (!preview.value.pickupSupported) {
      fulfillType.value = 'DELIVERY'
      await refresh()
    }
  }
}

async function switchFulfill(type) {
  if (type === 'PICKUP' && !preview.value.pickupSupported) {
    toast('该门店未开通到店自取', 'error')
    return
  }
  if (type === fulfillType.value) return
  fulfillType.value = type
  await refresh()
}

function onAppoint() {
  toast('预约送达为 SRS 1.6 中的 P2 选做项，本期原型仅实现「立即送出」', 'info')
}

function chooseAddress(a) {
  addressId.value = a.id
  addrOpen.value = false
  refresh()
}

/** 打开地址弹层：没有地址时直接展开新增表单，避免弹层空白 */
function openAddrSheet() {
  addrOpen.value = true
  if (!addresses.value.length) addrFormOpen.value = true
}

async function saveAddress() {
  const f = form.value
  if (!f.contact.trim()) {
    toast('请输入联系人', 'error')
    return
  }
  if (!/^1\d{10}$/.test(f.phone.trim())) {
    toast('请输入有效的 11 位手机号', 'error')
    return
  }
  if (!f.detail.trim()) {
    toast('请输入详细地址', 'error')
    return
  }
  savingAddr.value = true
  try {
    const r = await addAddress({
      contact: f.contact.trim(),
      phone: f.phone.trim(),
      detail: f.detail.trim(),
      tag: f.tag || null,
      isDefault: !!f.isDefault
    })
    const created = r.data || {}
    const list = await listAddresses()
    addresses.value = list.data || []
    addressId.value = created.id || (addresses.value.find(a => a.detail === f.detail.trim()) || {}).id || null
    addrFormOpen.value = false
    form.value = { contact: '', phone: '', detail: '', tag: '', isDefault: false }
    addrOpen.value = false
    toast('收货地址已添加', 'success')
    await refresh()
  } catch (e) {
    /* 错误提示由拦截器统一处理 */
  } finally {
    savingAddr.value = false
  }
}

async function submit() {
  if (submitting.value) return
  if (fulfillType.value === 'DELIVERY' && !addressId.value) {
    addrOpen.value = true
    addrFormOpen.value = true
    toast('请先填写收货地址', 'info')
    return
  }
  submitting.value = true
  try {
    const body = {
      shopId: Number(shopId),
      fulfillType: fulfillType.value,
      remark: remark.value || null
    }
    if (fulfillType.value === 'DELIVERY') body.addressId = addressId.value
    // 幂等键：重复点击 / 网络重试不会产生重复订单（SRS 9.6）
    const r = await submitOrder(body, uuid())
    app.refreshCartCount()
    toast('下单成功，请完成支付', 'success')
    router.replace(`/pay/${r.data.orderNo}`)
  } catch (e) {
    /* 错误提示已由拦截器统一处理 */
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.seg {
  display: inline-flex;
  background: var(--bg-sunken);
  border-radius: 6px;
  padding: 2px;
}

.seg__item {
  height: 24px;
  padding: 0 10px;
  border-radius: 5px;
  font-size: var(--fs-aux);
  color: var(--t-2);
}

.seg__item--active {
  background: var(--bg-card);
  color: var(--brand-3);
  font-weight: var(--fw-title);
}

.pickup-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #E6FAF6;
  color: #0F8A78;
  border-radius: var(--r-img);
  padding: 8px;
  font-size: var(--fs-aux);
}

.remark-input {
  flex: 1;
  text-align: right;
  font-size: var(--fs-body);
  color: var(--t-1);
}

.remark-input::placeholder {
  color: var(--t-3);
}

.warnbar {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #FFF6E5;
  color: #A5690A;
  border-radius: var(--r-img);
  padding: 8px;
  font-size: var(--fs-aux);
}

.warnbar--brand {
  background: #FFF1E6;
  color: var(--brand-3);
}

.addr-opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 10px;
  border-radius: var(--r-img);
}

.addr-opt--active {
  background: #E6FAF6;
}

.addr-empty {
  padding: 20px 10px;
  text-align: center;
  font-size: 13px;
  color: var(--t-3, #999);
}

.addr-form {
  padding: 10px 4px 4px;
}

.addr-form__row {
  display: flex;
  gap: 8px;
}

.addr-field {
  flex: 1;
  min-width: 0;
  height: 38px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid var(--line, #ebedf0);
  background: #fff;
  font-size: 14px;
  color: var(--t-1, #17181a);
  outline: none;
}

.addr-field--block {
  width: 100%;
}

.addr-field::placeholder {
  color: var(--t-3, #999);
}

.addr-tag {
  height: 26px;
  padding: 0 12px;
  border-radius: 13px;
  border: 1px solid var(--line, #ebedf0);
  background: #fff;
  font-size: 12px;
  color: var(--t-2, #555);
}

.addr-tag--on {
  color: #0F8A78;
  border-color: #12BFA0;
  background: #E6FAF6;
}

.addr-default {
  font-size: 12px;
  color: var(--t-2, #555);
}
</style>
