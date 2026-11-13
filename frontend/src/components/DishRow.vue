<template>
  <div class="dish" :class="{ 'dish--out': dish.soldOut }">
    <PhImage class="dish__img" :image="dish.image" :label="dish.name.slice(0, 2)" />
    <div class="dish__main">
      <div class="dish__name ellipsis">{{ dish.name }}</div>
      <div v-if="dish.description" class="dish__desc clamp-2">{{ dish.description }}</div>
      <div class="dish__meta">
        <span>月售 {{ dish.monthSales }}</span>
        <template v-if="dish.tags && dish.tags.length">
          <span v-for="t in dish.tags" :key="t" class="tag tag--promo">{{ t }}</span>
        </template>
      </div>
      <div class="row row--between mt-4">
        <div>
          <span class="price"><span class="price__sym">¥</span>{{ yuan(dish.price) }}</span>
          <span v-if="dish.originPrice" class="origin-price">¥{{ yuan(dish.originPrice) }}</span>
        </div>
        <div class="dish__act">
          <span v-if="dish.soldOut" class="dish__out">已售完</span>
          <Stepper
            v-else
            :quantity="quantity"
            @plus="$emit('plus', $event)"
            @minus="$emit('minus', $event)"
          />
          <span
            v-if="dish.hasSpec && !dish.soldOut"
            class="dish__spec"
            role="button"
            tabindex="0"
            aria-label="选规格"
            @click.stop="$emit('spec')"
            @keydown.enter.prevent="$emit('spec')"
            @keydown.space.prevent="$emit('spec')"
          >选规格</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import PhImage from './PhImage.vue'
import Stepper from './Stepper.vue'
import { yuan } from '../utils/format'

defineProps({
  dish: { type: Object, required: true },
  quantity: { type: Number, default: 0 }
})

defineEmits(['plus', 'minus', 'spec'])
</script>

<style scoped>
.dish {
  display: flex;
  gap: 10px;
  padding: 12px 0;
}

.dish + .dish {
  border-top: 1px solid var(--line);
}

.dish--out .dish__name,
.dish--out .dish__desc {
  color: var(--t-3);
}

.dish__img {
  width: 74px;
  height: 74px;
  flex-shrink: 0;
}

.dish__main {
  flex: 1;
  min-width: 0;
}

.dish__name {
  font-size: var(--fs-title);
  font-weight: var(--fw-title);
}

.dish__desc {
  font-size: var(--fs-aux);
  color: var(--t-3);
  margin-top: 3px;
  line-height: 1.35;
}

.dish__meta {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: var(--fs-aux);
  color: var(--t-3);
  margin-top: 4px;
}

.dish__act {
  display: flex;
  /* 竖向排列：步进器在上，「选规格」提示在下，避免绝对定位溢出到卡片外被裁切 */
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}

.dish__spec {
  font-size: 10px;
  line-height: 1.2;
  color: var(--t-3);
  white-space: nowrap;
}

.dish__out {
  font-size: var(--fs-aux);
  color: var(--t-3);
  border: 1px solid var(--line-strong);
  border-radius: var(--r-tag);
  padding: 2px 6px;
}
</style>
