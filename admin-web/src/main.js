import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/main.css'
import { restoreAuth } from './store/auth'

// 启动时恢复登录态（token / 角色）
restoreAuth()

createApp(App).use(router).mount('#app')
