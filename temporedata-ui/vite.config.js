import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 独立 dist，由后端以 file: 外部静态目录按需读取（免重打包/免重启）。
    // 生产发布由 CI 决定内嵌进 jar 或挂卷，见「运行模式重设计」§6。
    outDir: './dist',
    emptyOutDir: true
  }
})