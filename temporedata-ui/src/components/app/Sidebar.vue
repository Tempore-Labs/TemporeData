<template>
  <aside class="sider" :class="{ collapsed }">
    <!-- Brand / logo -->
    <div class="brand">
      <div class="brand-mark">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M8 2L13 5V11L8 14L3 11V5L8 2Z" fill="white" fill-opacity="0.3" stroke="white" stroke-width="1.2" />
          <path d="M8 5L11 7V10L8 12L5 10V7L8 5Z" fill="white" />
        </svg>
      </div>
      <span v-if="!collapsed" class="brand-name">TemporeData</span>
    </div>

    <!-- Navigation groups -->
    <nav class="nav">
      <template v-for="g in groups" :key="g">
        <div v-if="!collapsed" class="nav-group-title">{{ g }}</div>
        <router-link
          v-for="item in itemsOf(g)"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :title="collapsed ? item.label : undefined"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
        </router-link>
      </template>
    </nav>

    <!-- Footer -->
    <div v-if="!collapsed" class="sider-footer">TemporeData v3.0.0</div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useSidebarStore } from '@/stores/sidebar'
import { NAV, NAV_GROUPS } from '@/router/nav'

const sidebar = useSidebarStore()
const { collapsed } = storeToRefs(sidebar)

const groups = computed(() => NAV_GROUPS)
const itemsOf = (g: string) => NAV.filter((n) => n.group === g)
</script>

<style scoped>
.sider {
  width: var(--td-sider-width);
  flex-shrink: 0;
  height: 100%;
  background: var(--td-surface);
  border-right: 1px solid var(--td-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.sider.collapsed {
  width: var(--td-sider-collapsed);
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--td-border);
  flex-shrink: 0;
  gap: 10px;
}
.brand-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--td-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.brand-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--td-text-1);
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 8px;
}
.nav-group-title {
  padding: 8px 10px 4px;
  font-size: 10px;
  font-weight: 600;
  color: var(--td-text-4);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.nav-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  margin-bottom: 2px;
  font-size: 13px;
  color: var(--td-text-2);
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
  cursor: pointer;
  white-space: nowrap;
}
.nav-link:hover {
  background: var(--td-bg);
}
.nav-link.router-link-active {
  background: var(--td-primary-soft);
  color: var(--td-primary);
  font-weight: 600;
}
.nav-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.sider-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--td-border);
  font-size: 10px;
  color: var(--td-text-4);
  flex-shrink: 0;
}
</style>
