import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import Vant from 'vant'
import 'vant/lib/index.css'
import './styles/main.css'

createApp(App).use(router).use(Vant).mount('#app')
