<template>
  <div>
    <div class="list-wrap">
      <van-card
        v-for="d in dishes"
        :key="d.id"
        :title="d.name"
        :desc="'月售 ' + d.monthSales + ' ｜ ' + (d.status === 'ON_SALE' ? '在售' : '已下架')"
        :price="yuan(d.price)"
        :tag="d.status === 'ON_SALE' ? '在售' : '已下架'"
        :tag-type="d.status === 'ON_SALE' ? 'success' : 'default'"
        :thumb="d.imageUrl || undefined"
      >
        <template #footer>
          <van-button size="mini" @click="toggle(d)">{{ d.status === 'ON_SALE' ? '下架' : '上架' }}</van-button>
          <van-button size="mini" plain type="primary" @click="openPrice(d)">改价</van-button>
          <van-uploader :after-read="(f) => onDishImage(d, f)" :max-count="1" accept="image/*" />
        </template>
      </van-card>
    </div>
    <van-empty v-if="dishes.length === 0" description="暂无菜品" />

    <van-dialog v-model:show="priceShow" title="修改单价" show-cancel-button @confirm="confirmPrice">
      <div style="padding:16px;">
        <van-field v-model="priceInput" type="number" label="单价(元)" placeholder="如 16" />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { merchantApi, yuan } from '../api/merchant'

const dishes = ref([])
const priceShow = ref(false)
const priceInput = ref('')
const current = ref(null)

async function load() {
  const list = await merchantApi.myDishes()
  // 已上传过真实图片（objectKey 形式）则解析成可访问地址回显
  for (const d of list) {
    if (d.image && d.image.includes('/')) {
      try {
        const u = await merchantApi.ossUrl(d.image)
        d.imageUrl = u.url
      } catch (e) {
        d.imageUrl = ''
      }
    } else {
      d.imageUrl = ''
    }
  }
  dishes.value = list
}

async function onDishImage(d, item) {
  const f = Array.isArray(item) ? item[0] : item
  try {
    const r = await merchantApi.upload(f.file, 'dish')
    const bind = await merchantApi.bindDishImage(d.id, r.objectKey)
    d.image = r.objectKey
    d.imageUrl = bind.url
    showToast('菜品图已更新')
  } catch (e) {
    showToast(e.message || '上传失败')
  }
}

async function toggle(d) {
  try {
    const r = await merchantApi.toggleDish(d.id)
    d.status = r.status
    showToast(d.status === 'ON_SALE' ? '已上架' : '已下架')
  } catch (e) {
    showToast(e.message)
  }
}

function openPrice(d) {
  current.value = d
  priceInput.value = yuan(d.price)
  priceShow.value = true
}

async function confirmPrice() {
  const price = Math.round(parseFloat(priceInput.value) * 100)
  if (isNaN(price) || price < 0) { showToast('价格不合法'); return }
  try {
    const r = await merchantApi.updateDish(current.value.id, price)
    current.value.price = r.price
    showToast('改价成功')
  } catch (e) {
    showToast(e.message)
  }
}

onMounted(load)
</script>
