<template>
  <div class="rc">
    <div class="rc__head" @click="$emit('click')">
      <PhImage class="rc__img" :image="shop.headerImg || shop.logo" :label="shop.name.slice(0, 2)" />
      <div class="rc__main">
        <div class="rc__row1">
          <span class="rc__wm">外卖</span>
          <span class="rc__name ellipsis">{{ shop.name }}</span>
          <span v-if="shop.etaMinutes" class="rc__ontime">准时速+</span>
        </div>
        <div class="rc__meta">
          <span class="rc__rating"><b>{{ shop.rating }}</b>分</span>
          <span>月售{{ shop.monthSales }}</span>
          <span v-if="shop.etaMinutes">约{{ shop.etaMinutes }}分钟</span>
          <span v-if="shop.distanceKm != null">{{ shop.distanceKm }}km</span>
        </div>
        <div class="rc__fees">
          <span>起送¥{{ yuan(shop.minPrice) }}</span>
          <span class="rc__free">{{ shop.deliveryFee === 0 ? '免运费' : '配送¥' + yuan(shop.deliveryFee) }}</span>
          <span v-if="firstPromo" class="rc__promo ellipsis">{{ firstPromo }}</span>
        </div>
        <div v-if="cardTags.length" class="rc__tags">
          <span v-for="(t, i) in cardTags" :key="i" class="rc__tag" :class="{ 'rc__tag--pick': i === 0 && shop.pickupEnabled }">{{ t }}</span>
        </div>
      </div>
    </div>

    <!-- 券卡 + 爆款商品横滑行 -->
    <div v-if="dishes.length" class="rc__dishes">
      <div class="rc__coupon" @click.stop="$emit('coupon')">
        <div class="rc__coupon-title">超级补贴</div>
        <div class="rc__coupon-price">¥<b>12</b></div>
        <div class="rc__coupon-plus">+ 超级月卡</div>
        <div class="rc__coupon-sale">9.8折</div>
        <button class="rc__coupon-btn">去开卡</button>
      </div>
      <div class="rc__scroll">
        <div v-for="d in dishes" :key="d.id" class="rc__dish" @click="$emit('click')">
          <div class="rc__dish-pic">
            <PhImage style="width: 100%; height: 100%" :image="d.image" :label="d.name.slice(0, 2)" />
            <i v-if="hasCut(d)" class="rc__dish-corner">爆品一口价</i>
          </div>
          <span v-if="hasCut(d)" class="rc__dish-cut">减{{ yuan(d.originPrice - d.price) }}</span>
          <div class="rc__dish-name ellipsis">{{ d.name }}</div>
          <div class="rc__dish-price">
            <span class="rc__dish-p">¥{{ yuan(d.price) }}</span>
            <span class="rc__dish-sales">月售{{ d.monthSales || 0 }}</span>
            <button class="rc__dish-add" :disabled="d.soldOut" @click.stop="$emit('add', d)">＋</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PhImage from './PhImage.vue'
import { yuan } from '../utils/format'

const props = defineProps({
  shop: { type: Object, required: true },
  /** 店内热销商品（搜索页按店取详情后传入） */
  dishes: { type: Array, default: () => [] }
})

defineEmits(['click', 'add', 'coupon'])

const firstPromo = computed(() => (props.shop.promotions || [])[0] || '')

const cardTags = computed(() => {
  const list = props.shop.pickupEnabled ? ['自提更省'] : []
  for (const t of [...(props.shop.tags || []), ...(props.shop.promotions || [])]) {
    if (!list.includes(t)) list.push(t)
  }
  return list.slice(0, 3)
})

/** 划线价高于现价才算真补贴，不凭空造折扣 */
function hasCut(d) {
  return d.originPrice && d.originPrice > d.price
}
</script>

<style scoped>
.rc {
  background: var(--bg-card, #fff);
  border-radius: 12px;
  box-shadow: var(--sh-light);
  padding: 10px 10px 8px;
}

/* ---------- 店铺信息 ---------- */
.rc__head {
  display: flex;
  gap: 10px;
  -webkit-user-select: none;
  user-select: none;
}
.rc__head:active {
  opacity: .75;
}

.rc__img {
  width: 82px;
  height: 82px;
  flex-shrink: 0;
  border-radius: 8px;
}

.rc__main {
  flex: 1;
  min-width: 0;
}

.rc__row1 {
  display: flex;
  align-items: center;
  gap: 5px;
}

.rc__wm {
  flex-shrink: 0;
  font-size: 10px;
  line-height: 15px;
  padding: 0 3px;
  border-radius: 3px;
  color: #fff;
  background: linear-gradient(135deg, #FF5B4A, #E02E24);
  font-weight: 600;
}

.rc__name {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--t-1, #1a1a1a);
}

.rc__ontime {
  flex-shrink: 0;
  font-size: 10px;
  color: #E02E24;
  border: 1px solid #f3c4c0;
  border-radius: 3px;
  padding: 0 3px;
  line-height: 14px;
}

.rc__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--t-2, #555);
  white-space: nowrap;
  overflow: hidden;
}

.rc__rating {
  color: #E02E24;
  font-weight: 700;
}
.rc__rating b {
  font-variant-numeric: tabular-nums;
  font-size: 14px;
}

.rc__fees {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--t-2, #555);
  white-space: nowrap;
}
.rc__free {
  color: var(--t-2, #555);
}
.rc__promo {
  flex: 1;
  min-width: 0;
  color: #E02E24;
  text-align: right;
}

.rc__tags {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  overflow: hidden;
}
.rc__tag {
  flex-shrink: 0;
  font-size: 10px;
  line-height: 16px;
  padding: 0 5px;
  border-radius: 3px;
  color: #C0392B;
  background: #FDECEC;
}
.rc__tag--pick {
  color: #B9760F;
  background: #FFF5E6;
}

/* ---------- 券卡 + 商品行 ---------- */
.rc__dishes {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.rc__coupon {
  flex-shrink: 0;
  width: 76px;
  border: 1px solid #f3c4c0;
  border-radius: 8px;
  background: linear-gradient(180deg, #FFF7F2 0%, #FFEFE9 100%);
  padding: 6px 4px 8px;
  text-align: center;
  -webkit-user-select: none;
  user-select: none;
}
.rc__coupon:active {
  opacity: .8;
}
.rc__coupon-title {
  font-size: 11px;
  font-weight: 700;
  color: #C0392B;
}
.rc__coupon-price {
  margin-top: 3px;
  color: #E02E24;
  font-size: 11px;
}
.rc__coupon-price b {
  font-size: 20px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.rc__coupon-plus {
  font-size: 10px;
  color: #B9760F;
  margin-top: 1px;
}
.rc__coupon-sale {
  font-size: 15px;
  font-weight: 800;
  color: #C0392B;
}
.rc__coupon-btn {
  margin-top: 4px;
  width: 100%;
  height: 20px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #FF7A1A, #E02E24);
  color: #fff;
  font-size: 11px;
}

.rc__scroll {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.rc__dish {
  position: relative;
  flex-shrink: 0;
  width: 84px;
  -webkit-user-select: none;
  user-select: none;
}

.rc__dish-pic {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: 8px;
  overflow: hidden;
}

.rc__dish-corner {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 1;
  font-style: normal;
  font-size: 9px;
  line-height: 14px;
  padding: 0 4px;
  border-radius: 8px 0 6px 0;
  color: #fff;
  background: linear-gradient(135deg, #FF7A1A, #E02E24);
}

.rc__dish-cut {
  position: absolute;
  left: 0;
  top: 84px;
  transform: translateY(-100%);
  font-size: 10px;
  line-height: 16px;
  padding: 0 5px;
  border-radius: 0 0 6px 0;
  color: #fff;
  background: linear-gradient(135deg, #FF5B4A, #E02E24);
}

.rc__dish-name {
  font-size: 12px;
  color: var(--t-1, #1a1a1a);
  margin-top: 6px;
}

.rc__dish-price {
  display: flex;
  align-items: baseline;
  gap: 3px;
  margin-top: 2px;
}

.rc__dish-p {
  font-size: 14px;
  font-weight: 700;
  color: #E02E24;
  font-variant-numeric: tabular-nums;
}

.rc__dish-sales {
  flex: 1;
  min-width: 0;
  font-size: 10px;
  color: var(--t-3, #999);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rc__dish-add {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #FF7A1A, #E02E24);
  color: #fff;
  font-size: 13px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.rc__dish-add:disabled {
  opacity: .4;
}
</style>
