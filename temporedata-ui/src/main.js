import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { hasPerm } from './lib/permission'
import { useAuthStore } from './stores/auth'
import './styles/global.css'

const app = createApp(App)

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// P3-11 按钮级权限：v-perm="'cluster:create'" —— 无权限即从 DOM 移除
app.directive('perm', {
  mounted(el, binding) {
    const store = useAuthStore()
    if (!hasPerm(binding.value, store.user)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

app.mount('#app')