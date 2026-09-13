<template>
  <header class="topbar">
    <!-- Sidebar toggle -->
    <button class="icon-btn" title="折叠/展开菜单" @click="sidebar.toggle()">
      <el-icon :size="16"><Fold v-if="!sidebar.collapsed" /><Expand v-else /></el-icon>
    </button>

    <!-- Breadcrumb -->
    <div class="crumb">
      <span class="crumb-root">TemporeData</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">{{ title }}</span>
    </div>

    <div class="flex-1" />

    <!-- Global search (navigates to lineage search) -->
    <div class="search">
      <el-input
        v-model="kw"
        placeholder="搜索数据资产... ⌘K"
        size="small"
        clearable
        @keyup.enter="onSearch"
        @keydown.ctrl.k.prevent="onSearch"
        @keydown.meta.k.prevent="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- Notification -->
    <button class="icon-btn pos-rel" title="通知">
      <el-icon :size="16"><Bell /></el-icon>
      <span class="dot" />
    </button>

    <!-- AI assistant -->
    <button class="ai-btn" title="AI 助手" @click="assistant.toggle()">
      <el-icon :size="12"><MagicStick /></el-icon>
      <span>智助</span>
    </button>

    <!-- User -->
    <el-dropdown trigger="click" @command="onUserCommand">
      <button class="user-btn">
        <div class="avatar">{{ avatarText }}</div>
        <span class="user-name">{{ auth.displayName }}</span>
        <el-icon :size="12" class="chevron"><ArrowDown /></el-icon>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">个人中心</el-dropdown-item>
          <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useSidebarStore } from '@/stores/sidebar'
import { useAuthStore } from '@/stores/auth'
import { useAssistantStore } from '@/stores/assistant'
import { navLabel } from '@/router/nav'

const sidebar = useSidebarStore()
const auth = useAuthStore()
const assistant = useAssistantStore()
const route = useRoute()
const router = useRouter()

const kw = ref('')
const title = computed(() => (route.meta.title as string) || navLabel(route.path))

const avatarText = computed(() => {
  const n = auth.displayName
  return n ? n.slice(0, 1).toUpperCase() : 'A'
})

function onSearch() {
  const q = kw.value.trim()
  if (!q) return
  router.push({ path: '/lineage', query: { kw: q } })
}

async function onUserCommand(cmd: string) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await auth.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/dashboard')
  }
}
</script>

<style scoped>
.topbar {
  height: var(--td-topbar-height);
  background: var(--td-surface);
  border-bottom: 1px solid var(--td-border);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  flex-shrink: 0;
}
.flex-1 {
  flex: 1;
}
.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: var(--td-text-3);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.15s;
}
.icon-btn:hover {
  background: var(--td-border-light);
}
.pos-rel {
  position: relative;
}
.dot {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--td-danger);
}
.crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.crumb-root {
  color: var(--td-text-3);
}
.crumb-sep {
  color: var(--td-border);
}
.crumb-current {
  color: var(--td-text-1);
  font-weight: 500;
}
.search {
  width: 220px;
}
.search :deep(.el-input__wrapper) {
  background: var(--td-bg);
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--td-border) inset;
}
.search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--td-primary) inset;
}
.ai-btn {
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  border: none;
  background: var(--td-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.ai-btn:hover {
  background: var(--td-primary-hover);
}
.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 8px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.user-btn:hover {
  background: var(--td-border-light);
}
.avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--td-primary), #7c3aed);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-name {
  font-size: 13px;
  color: var(--td-text-2);
}
.chevron {
  color: var(--td-text-4);
}
</style>
