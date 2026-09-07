<template>
  <div class="layout">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- Logo (expanded only, no toggle button inside) -->
      <div class="sidebar-logo-area" v-show="!sidebarCollapsed">
        <router-link to="/dashboard" class="sidebar-logo-link">
          <svg class="sidebar-logo-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
          </svg>
          <span class="sidebar-logo-text">TemporeData</span>
        </router-link>
      </div>

      <!-- Menu Search -->
      <div class="sidebar-search" v-show="!sidebarCollapsed">
        <el-input
          v-model="menuSearch"
          placeholder="搜索菜单..."
          :prefix-icon="Search"
          clearable
          size="small"
          class="menu-search-input"
        />
      </div>

      <!-- Menu Groups -->
      <nav class="sidebar-nav">
        <!-- Collapse/Expand toggle (styled exactly like nav items) -->
        <div class="nav-item sidebar-toggle-item" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开' : '收起'">
          <span class="nav-icon"><component :is="sidebarCollapsed ? Expand : Fold" /></span>
          <span class="nav-title">{{ sidebarCollapsed ? '展开' : '收起' }}</span>
        </div>

        <!-- Dashboard -->
        <router-link to="/dashboard" class="nav-item" :class="{ active: activeMenu === '/dashboard' }" :title="'总览'">
          <span class="nav-icon"><DataBoard /></span>
          <span class="nav-title">总览</span>
        </router-link>

        <template v-for="group in filteredMenu" :key="group.group">
          <!-- Group header (click to expand/collapse workspace domain) -->
          <div
            class="nav-group-header"
            :class="{ open: isGroupOpen(group.group) }"
            :title="group.group"
            @click="toggleGroup(group.group)"
          >
            <span class="nav-group-icon"><component :is="group.icon" /></span>
            <span class="nav-title">{{ group.group }}</span>
            <span class="nav-group-arrow" v-show="!sidebarCollapsed">
              <el-icon><ArrowRight /></el-icon>
            </span>
          </div>

          <template v-if="isSearching || isGroupOpen(group.group)">
            <router-link
              v-for="item in group.children"
              :key="item.path"
              :to="item.path"
              class="nav-item sub"
              :class="{ active: activeMenu === item.path }"
              :title="sidebarCollapsed ? item.title : ''"
            >
              <span class="nav-icon"><component :is="item.icon" /></span>
              <span class="nav-title">{{ item.title }}</span>
            </router-link>
          </template>
        </template>
      </nav>

      <!-- Sidebar Footer -->
      <div class="sidebar-footer" v-show="!sidebarCollapsed">
        <span class="sidebar-version">TemporeData v1.0.0</span>
      </div>
    </aside>

    <!-- Main Area -->
    <div class="main-area" :style="{ marginLeft: sidebarCollapsed ? '64px' : '208px' }">
      <!-- Header -->
      <header class="topbar">
        <div class="topbar-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <!-- Global Search -->
          <div class="global-search" @click="showGlobalSearch = true">
            <el-icon><Search /></el-icon>
            <span class="search-hint">搜索菜单、页面...</span>
            <kbd>Ctrl+K</kbd>
          </div>
          <!-- 通知 -->
          <router-link to="/message" class="topbar-icon-btn" title="消息中心">
            <el-icon :size="18"><Bell /></el-icon>
          </router-link>
          <!-- 智助（全局智能入口） -->
          <router-link to="/agent" class="topbar-assistant-btn" title="智助">
            <el-icon :size="18"><ChatDotRound /></el-icon>
            <span>智助</span>
          </router-link>
          <!-- User -->
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" style="background: #1a6ff5">{{ userInitial }}</el-avatar>
              <span class="user-name">{{ authStore.user?.username || '管理员' }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <span style="font-size:12px;color:#9ca3af">{{ authStore.user?.username || 'admin' }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Tab Bar -->
      <div class="tab-bar" v-if="tabPages.length > 0">
        <div
          v-for="tab in tabPages"
          :key="tab.path"
          class="tab-item"
          :class="{ active: tab.path === activeMenu }"
          @click="router.push(tab.path)"
        >
          <span class="tab-title">{{ tab.title }}</span>
          <el-icon class="tab-close" @click.stop="closeTab(tab.path)"><Close /></el-icon>
        </div>
      </div>

      <!-- Page Content -->
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <!-- keep-alive removed: its `include` used page titles instead of component
               names, and caching-heavy table pages triggered Vue runtime patch errors
               (parentNode/subTree) on restore. Pages re-fetch on each visit. -->
          <component :is="Component" />
        </router-view>
      </main>
    </div>

    <!-- Global Search Dialog -->
    <el-dialog
      v-model="showGlobalSearch"
      title="全局搜索"
      width="560px"
      :show-close="true"
      :close-on-click-modal="true"
      destroy-on-close
      class="global-search-dialog"
    >
      <el-input
        v-model="globalSearchText"
        placeholder="输入关键词搜索菜单或页面..."
        :prefix-icon="Search"
        size="large"
        clearable
        autofocus
        @keydown.enter="navigateToFirstResult"
      />
      <div class="search-results" v-if="globalSearchText">
        <div class="search-result-group" v-for="group in globalSearchResults" :key="group.group">
          <div class="search-result-group-title">{{ group.group }}</div>
          <div
            v-for="item in group.children"
            :key="item.path"
            class="search-result-item"
            :class="{ active: globalActiveIndex === item._index }"
            @click="navigateTo(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
            <span class="search-result-path">{{ item.path }}</span>
          </div>
        </div>
        <el-empty v-if="globalSearchFlat.length === 0" description="未找到匹配的菜单" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { menuConfig } from '@/config/menu.config'
import { hasPerm } from '@/lib/permission'

// Icons
import {
  DataBoard, Search, ArrowDown, SwitchButton, Close, ChatDotRound,
  Fold, Expand, ArrowRight, Bell
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// Sidebar state
const sidebarCollapsed = ref(false)
const menuSearch = ref('')

// Active menu
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')

// Admin gate for "管理中心" (permission-driven menu, v2.0 §19)
const isAdminUser = computed(() => {
  const u = authStore.user
  if (!u) return false
  const roles = u.roles || u.role || u.permissions
  if (Array.isArray(roles)) return roles.some(r => /admin/i.test(String(r)))
  if (roles) return /admin/i.test(String(roles))
  // No role data available -> fall back to the built-in admin account
  return u.username === 'admin'
})

// Menu visible to the current user: admin group gated by isAdminUser,
// each child additionally gated by its `perm` code (P3-11 动态菜单过滤；超级管理员恒可见).
const visibleMenu = computed(() => {
  const u = authStore.user
  return menuConfig
    .filter(g => !g.admin || isAdminUser.value)
    .map(g => ({
      ...g,
      children: g.children.filter(item => item.perm == null || isAdminUser.value || hasPerm(item.perm, u))
    }))
    .filter(g => g.children.length > 0)
})

// Workspace domains expandable/collapsible (v2.0 §5.2)
const openGroups = ref([])
function isGroupOpen(name) {
  return openGroups.value.includes(name)
}
function toggleGroup(name) {
  const i = openGroups.value.indexOf(name)
  if (i >= 0) openGroups.value.splice(i, 1)
  else openGroups.value.push(name)
}
const isSearching = computed(() => menuSearch.value.trim() !== '')

// Auto-expand the workspace domain that owns the current route
watch(() => route.path, (path) => {
  const g = visibleMenu.value.find(gp => gp.children.some(c => c.path === path))
  if (g && !isGroupOpen(g.group)) openGroups.value.push(g.group)
}, { immediate: true })

// Filtered menu
const filteredMenu = computed(() => {
  if (!isSearching.value) return visibleMenu.value
  const kw = menuSearch.value.toLowerCase()
  return visibleMenu.value
    .map(g => ({
      ...g,
      children: g.children.filter(c => c.title.toLowerCase().includes(kw))
    }))
    .filter(g => g.children.length > 0)
})

// User
const userInitial = computed(() => {
  const name = authStore.user?.username || 'A'
  return name.charAt(0).toUpperCase()
})

function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// Tab bar
const tabPages = ref([])
const cachedPages = ref([])

watch(() => route.path, (path) => {
  if (path === '/dashboard') return // don't tab dashboard
  const title = route.meta?.title || path
  const exists = tabPages.value.find(t => t.path === path)
  if (!exists) {
    tabPages.value.push({ path, title })
    // Keep heavy table pages out of keep-alive cache: restoring a cached
    // el-table triggers Vue runtime patch errors (`parentNode`/`subTree`).
    const noCache = ['/message']
    if (!noCache.includes(path) && !cachedPages.value.includes(title)) {
      cachedPages.value.push(title)
    }
  }
}, { immediate: true })

function closeTab(path) {
  const idx = tabPages.value.findIndex(t => t.path === path)
  if (idx === -1) return
  tabPages.value.splice(idx, 1)
  if (path === activeMenu.value) {
    const next = tabPages.value[idx] || tabPages.value[idx - 1]
    if (next) router.push(next.path)
    else router.push('/dashboard')
  }
}

// Global search
const showGlobalSearch = ref(false)
const globalSearchText = ref('')
const globalActiveIndex = ref(0)

const globalSearchFlat = computed(() => {
  if (!globalSearchText.value) return []
  const kw = globalSearchText.value.toLowerCase()
  let idx = 0
  return visibleMenu.value
    .map(g => ({
      ...g,
      children: g.children
        .filter(c => c.title.toLowerCase().includes(kw))
        .map(c => ({ ...c, _index: idx++ }))
    }))
    .filter(g => g.children.length > 0)
})

const globalSearchResults = computed(() => globalSearchFlat.value)

function navigateTo(path) {
  showGlobalSearch.value = false
  globalSearchText.value = ''
  router.push(path)
}

function navigateToFirstResult() {
  const first = globalSearchFlat.value[0]?.children[0]
  if (first) navigateTo(first.path)
}

// Keyboard shortcut
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    showGlobalSearch.value = true
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ========== Layout ========== */
.layout {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}

/* ========== Sidebar ========== */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  width: 208px;
  background: #fff;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s;
  overflow: hidden;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.04);
}
.sidebar.collapsed {
  width: 64px;
}

/* Logo */
.sidebar-logo-area {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.sidebar-logo-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #1f2937;
}
.sidebar-logo-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  color: #1a6ff5;
}
.sidebar-logo-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.5px;
  flex-shrink: 0;
  line-height: 1;
  color: #1f2937;
}

/* Toggle item at the top of nav: subtle separator so it reads as a utility action,
   not a nav route. Matches .nav-item exactly in size/icon/title. */
.sidebar-toggle-item {
  margin-bottom: 4px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 4px;
}

/* Search */
.sidebar-search {
  padding: 8px 12px;
  flex-shrink: 0;
}
.menu-search-input :deep(.el-input__wrapper) {
  background: #f5f6f8;
  border-radius: 6px;
  box-shadow: none;
}

/* Nav */
.sidebar-nav {
  flex: 1 1 0%;
  /* Key: allow the flex child to shrink below its content height so it becomes a
     scroll container instead of overflowing into the (overflow:hidden) sidebar and
     clipping the bottom menu items (e.g. 个人空间). */
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 8px;
}
.sidebar-nav::-webkit-scrollbar {
  width: 6px;
}
.sidebar-nav::-webkit-scrollbar-thumb {
  background: #d0d5dd;
  border-radius: 3px;
}
.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.nav-group-header {
  display: flex;
  align-items: center;
  height: 34px;
  padding: 0 12px;
  margin-top: 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  cursor: pointer;
  gap: 12px;
  white-space: nowrap;
  overflow: hidden;
  user-select: none;
  transition: color 0.15s, background 0.15s;
}
.nav-group-header:hover {
  color: #4b5563;
  background: #f5f6f8;
}
.nav-group-header .nav-title {
  font-size: 12px;
  font-weight: 600;
}
.nav-group-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}
.nav-group-arrow {
  margin-left: auto;
  display: flex;
  align-items: center;
  font-size: 12px;
  transition: transform 0.2s;
  color: #c0c4cc;
}
.nav-group-header.open .nav-group-arrow {
  transform: rotate(90deg);
}
.sidebar.collapsed .nav-group-header {
  justify-content: center;
  padding: 0;
}

.nav-item.sub {
  height: 36px;
}

.nav-item {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 12px;
  margin: 2px 0;
  border-radius: 8px;
  text-decoration: none;
  color: #4b5563;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
  gap: 12px;
  overflow: hidden;
  white-space: nowrap;
}
.nav-item:hover {
  background: #f3f4f6;
  color: #1f2937;
}
.nav-item.active {
  background: #e8f1fe;
  color: #1a6ff5;
  font-weight: 500;
}
.nav-item.active .nav-icon {
  color: #1a6ff5;
}

.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
  color: #6b7280;
  transition: color 0.15s;
}
.nav-item:hover .nav-icon {
  color: #4b5563;
}
.nav-item.active .nav-icon {
  color: #1a6ff5;
}

.nav-title {
  white-space: nowrap;
}
.sidebar.collapsed .nav-title {
  display: none;
}
/* Collapsed mode: center each nav item's icon (title is hidden) so all entries —
   including the expand/collapse toggle — align with the centered group headers. */
.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 0;
}

/* Footer */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.sidebar-version {
  font-size: 11px;
  color: #c0c4cc;
}

/* ========== Main Area ========== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ========== Topbar ========== */
.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 50;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Global Search */
.global-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: #f5f6f8;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #9ca3af;
  transition: all 0.2s;
  min-width: 220px;
  border: 1px solid transparent;
}
.global-search:hover {
  background: #eceff3;
  border-color: #d0d5dd;
  color: #6b7280;
}
.global-search kbd {
  margin-left: auto;
  padding: 1px 6px;
  font-size: 11px;
  background: #e5e7eb;
  border-radius: 4px;
  color: #6b7280;
  font-family: inherit;
}
.search-hint {
  flex: 1;
}

/* Topbar icon & assistant entries */
.topbar-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: #6b7280;
  text-decoration: none;
  transition: all 0.15s;
}
.topbar-icon-btn:hover {
  background: #f5f6f8;
  color: #1f2937;
}
.topbar-assistant-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #1a6ff5;
  background: #e8f1fe;
  text-decoration: none;
  transition: all 0.15s;
}
.topbar-assistant-btn:hover {
  background: #d8e7fd;
}

/* User */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.15s;
}
.user-info:hover {
  background: #f5f6f8;
}
.user-name {
  color: #1f2937;
  font-weight: 500;
}
.user-arrow {
  font-size: 12px;
  color: #9ca3af;
}

/* ========== Tab Bar ========== */
.tab-bar {
  display: flex;
  align-items: center;
  height: 40px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  padding: 0 12px;
  gap: 4px;
  overflow-x: auto;
  flex-shrink: 0;
  position: sticky;
  top: 56px;
  z-index: 49;
}
.tab-bar::-webkit-scrollbar {
  height: 2px;
}
.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
  background: transparent;
  border: 1px solid transparent;
}
.tab-item:hover {
  background: #f3f4f6;
  color: #1f2937;
}
.tab-item.active {
  background: #e8f1fe;
  color: #1a6ff5;
  border-color: #d0e2fd;
}
.tab-close {
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.15s;
  border-radius: 3px;
  padding: 1px;
}
.tab-item:hover .tab-close {
  opacity: 0.5;
}
.tab-close:hover {
  opacity: 1 !important;
  background: #d0d5dd;
}

/* ========== Content ========== */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f0f2f5;
}

/* ========== Global Search Dialog ========== */
.global-search-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}
.search-results {
  margin-top: 16px;
  max-height: 420px;
  overflow-y: auto;
}
.search-result-group {
  margin-bottom: 8px;
}
.search-result-group-title {
  font-size: 11px;
  color: #9ca3af;
  padding: 8px 12px 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #4b5563;
  transition: background 0.15s;
}
.search-result-item:hover,
.search-result-item.active {
  background: #f3f4f6;
  color: #1a6ff5;
}
.search-result-path {
  margin-left: auto;
  font-size: 12px;
  color: #c0c4cc;
}
</style>