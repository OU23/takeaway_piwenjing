<template>
  <div class="shop-card" @click="$emit('click')">
    <PhImage class="shop-card__img" :image="shop.headerImg" :label="shop.name.slice(0, 2)" />
    <div class="shop-card__main">
      <div class="row row--between">
        <div class="shop-card__name ellipsis">{{ shop.name }}</div>
        <span v-if="shop.pickupEnabled" class="tag tag--success">到店自取</span>
      </div>

      <div class="shop-card__meta">
        <span class="shop-card__rating">
          <Icon name="star" :size="11" color="#FF9500" />
          <b>{{ shop.rating }}</b>
        </span>
        <span>月售 {{ shop.monthSales }}</span>
        <span>{{ shop.distanceKm }}km</span>
        <span v-if="shop.etaMinutes">约 {{ shop.etaMinutes }} 分钟</span>
      </div>

      <div class="shop-card__fees">
        <span>起送 ¥{{ yuan(shop.minPrice) }}</span>
        <i class="dot" />
        <span>配送 ¥{{ yuan(shop.deliveryFee) }}</span>
        <template v-if="shop.pickupEnabled && shop.pickupDiscount > 0">
          <i class="dot" />
          <span class="text-success">自取减 ¥{{ yuan(shop.pickupDiscount) }}</span>
        </template>
      </div>

      <div v-if="promoLabels.length" class="tag-list mt-4">
        <span v-for="(p, i) in promoLabels" :key="i" class="tag tag--promo">{{ p }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Icon from './Icon.vue'
import PhImage from './PhImage.vue'
import { yuan } from '../utils/format'

const props = defineProps({
  shop: { type: Object, required: true }
})

defineEmits(['click'])

const promoLabels = computed(() => {
  const list = [...(props.shop.promotions || [])]
  for (const t of props.shop.tags || []) {
    if (!/满|减/.test(t) || /支持到店自取/.test(t)) continue
    if (!list.includes(t)) list.push(t)
  }
  return list.slice(0, 3)
})
</script>

<style scoped>
.shop-card {
  display: flex;
  gap: 10px;
  background: var(--bg-card);
  border-radius: var(--r-card);
  padding: 10px;
  box-shadow: var(--sh-light);
}

.shop-card + .shop-card {
  margin-top: 10px;
}

.shop-card:active {
  background: #FCFCFD;
}

.shop-card__img {
  width: 74px;
  height: 74px;
  flex-shrink: 0;
}

.shop-card__main {
  flex: 1;
  min-width: 0;
}

.shop-card__name {
  font-size: var(--fs-title);
  font-weight: var(--fw-title);
  max-width: 62%;
}

.shop-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-aux);
  color: var(--t-3);
  margin-top: 5px;
}

.shop-card__rating {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--c-warn);
}

.shop-card__rating b {
  font-variant-numeric: tabular-nums;
}

.shop-card__fees {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-aux);
  color: var(--t-2);
  margin-top: 5px;
}

.dot {
  width: 1px;
  height: 9px;
  background: var(--line-strong);
}
</style>
