<template>
  <div class="mask" @click.self="$emit('close')">
    <div class="sheet">
      <div class="sheet__head">
        <div class="row" style="gap: 10px">
          <PhImage style="width: 58px; height: 58px" :image="dish.image" :label="dish.name.slice(0, 2)" />
          <div>
            <div class="text-strong">{{ dish.name }}</div>
            <div class="text-aux mt-4">{{ dish.description || '请选择规格' }}</div>
            <div class="mt-4">
              <span class="price"><span class="price__sym">¥</span>{{ yuan(unitPrice) }}</span>
            </div>
          </div>
        </div>
        <button class="sheet__close" @click="$emit('close')">
          <Icon name="close" :size="13" />
        </button>
      </div>

      <div class="sheet__body">
        <div v-for="g in dish.specGroups" :key="g.groupName" class="spec-group">
          <div class="spec-group__title">
            {{ g.groupName }}
            <span class="text-aux">{{ g.type === 'ADDON' ? '（可多选）' : '（必选）' }}</span>
            <span v-if="missing(g)" class="text-error text-aux">请选择</span>
          </div>
          <div>
            <button
              v-for="o in g.options"
              :key="o.id"
              class="opt"
              :class="{ 'opt--active': isSelected(g, o) }"
              @click="toggle(g, o)"
            >
              {{ o.name }}
              <span v-if="o.priceDelta > 0" class="opt__delta">+{{ yuan(o.priceDelta) }}</span>
            </button>
          </div>
        </div>

        <div class="divider" />
        <div class="row row--between">
          <span class="text-strong">数量</span>
          <Stepper :quantity="quantity" @plus="quantity++" @minus="quantity = Math.max(1, quantity - 1)" />
        </div>
      </div>

      <div class="sheet__foot">
        <div class="row row--between mb-8">
          <span class="text-aux">
            {{ selectedText || '未选择规格' }}
          </span>
          <span>
            <span class="text-aux">小计</span>
            <span class="price" style="margin-left: 4px">
              <span class="price__sym">¥</span>{{ yuan(unitPrice * quantity) }}
            </span>
          </span>
        </div>
        <button class="btn btn--primary btn--block" :disabled="!valid" @click="confirm">
          加入购物车
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import Icon from './Icon.vue'
import PhImage from './PhImage.vue'
import Stepper from './Stepper.vue'
import { yuan } from '../utils/format'
import { toast } from '../api/http'

const props = defineProps({
  dish: { type: Object, required: true },
  initialSpecIds: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'confirm'])

const quantity = ref(1)

/** groupName → 已选 option id 数组 */
const selected = ref({})

// 初始化默认选中项
for (const g of props.dish.specGroups || []) {
  if (props.initialSpecIds.length) {
    const hit = g.options.filter(o => props.initialSpecIds.includes(o.id)).map(o => o.id)
    if (hit.length) {
      selected.value[g.groupName] = hit
      continue
    }
  }
  const defs = g.options.filter(o => o.isDefault).map(o => o.id)
  selected.value[g.groupName] = g.type === 'ADDON' ? defs : defs.slice(0, 1)
}

const flat = computed(() => (props.dish.specGroups || []).flatMap(g => g.options))

const chosenIds = computed(() =>
  Object.values(selected.value).flat().filter(id => id !== null && id !== undefined)
)

const unitPrice = computed(() => {
  const delta = flat.value
    .filter(o => chosenIds.value.includes(o.id))
    .reduce((sum, o) => sum + (o.priceDelta || 0), 0)
  return (props.dish.price || 0) + delta
})

const selectedText = computed(() =>
  flat.value
    .filter(o => chosenIds.value.includes(o.id))
    .map(o => o.name)
    .join(' / ')
)

const valid = computed(() =>
  (props.dish.specGroups || []).every(g => g.type === 'ADDON' || (selected.value[g.groupName] || []).length > 0)
)

function isSelected(g, o) {
  return (selected.value[g.groupName] || []).includes(o.id)
}

function missing(g) {
  return g.type !== 'ADDON' && !(selected.value[g.groupName] || []).length
}

function toggle(g, o) {
  const cur = selected.value[g.groupName] || []
  if (g.type === 'ADDON') {
    selected.value[g.groupName] = cur.includes(o.id) ? cur.filter(x => x !== o.id) : [...cur, o.id]
    return
  }
  // SPEC：组内单选
  if (cur.includes(o.id)) {
    selected.value[g.groupName] = []
  } else {
    selected.value[g.groupName] = [o.id]
  }
}

function confirm() {
  if (!valid.value) {
    toast('请先选择必选规格', 'error')
    return
  }
  emit('confirm', { specIds: chosenIds.value, quantity: quantity.value })
}
</script>

<style scoped>
.spec-group + .spec-group {
  margin-top: 14px;
}

.spec-group__title {
  font-size: var(--fs-body);
  font-weight: var(--fw-title);
  margin-bottom: 8px;
}

.opt__delta {
  margin-left: 4px;
  font-size: 10px;
}
</style>
