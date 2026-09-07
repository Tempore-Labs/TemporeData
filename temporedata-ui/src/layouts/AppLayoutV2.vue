<template>
  <div class="v2-layout">
    <aside class="v2-sider">
      <div class="v2-brand">Tempore<span>Data</span></div>
      <nav class="v2-nav" aria-label="主导航">
        <div v-for="(group, gi) in groups" :key="gi" class="v2-nav-group">
          <div class="v2-nav-group-title">{{ group }}</div>
          <router-link
            v-for="item in groupItems(group)"
            :key="item.key"
            :to="item.path"
            class="v2-nav-link"
            active-class="is-active"
          >{{ item.label }}</router-link>
        </div>
      </nav>
    </aside>

    <div class="v2-main">
      <header class="v2-topbar">
        <span class="v2-command-hint" @click="openPalette">⨐ 命令 (Ctrl+K)</span>
        <div class="v2-top-spacer" />
        <span class="v2-top-user">{{ (auth?.user?.username) || '未登录' }}</span>
      </header>
      <main class="v2-content">
        <slot />
      </main>
    </div>

    <CommandPalette ref="paletteRef" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { NAV } from './navigation'
import CommandPalette from '@/components/CommandPalette.vue'

const auth = useAuthStore()
const paletteRef = ref(null)

const groups = computed(() => [...new Set(Object.values(NAV).map((n) => n.group))])
const groupItems = (g) => Object.values(NAV).filter((n) => n.group === g)

function openPalette() { paletteRef.value?.toggle?.() }
defineExpose({ openPalette })
</script>

<style scoped>
@import '@/design-system/tokens.css';
.v2-layout { display: flex; height: 100vh; background: var(--td-bg-canvas); color: var(--td-text); }
.v2-sider { width: 208px; flex-shrink: 0; background: var(--td-bg-surface); border-right: 1px solid var(--td-border); display: flex; flex-direction: column; }
.v2-brand { height: 54px; display: flex; align-items: center; padding: 0 16px; font-weight: 700; font-size: var(--td-fs-lg); }
.v2-brand span { color: var(--td-color-primary); }
.v2-nav { flex: 1; overflow: auto; padding: 8px 10px; }
.v2-nav-group-title { font-size: var(--td-fs-xs); color: var(--td-text-muted); padding: 10px 8px 4px; text-transform: uppercase; }
.v2-nav-link { display: block; padding: 8px 10px; border-radius: var(--td-radius-md); color: var(--td-text-sub); text-decoration: none; }
.v2-nav-link.is-active, .v2-nav-link:hover { background: #eff6ff; color: var(--td-color-primary); }
.v2-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.v2-topbar { height: 48px; background: var(--td-bg-surface); border-bottom: 1px solid var(--td-border); display: flex; align-items: center; padding: 0 16px; gap: 8px; }
.v2-command-hint { cursor: pointer; color: var(--td-text-sub); }
.v2-top-spacer { flex: 1; }
.v2-content { flex: 1; overflow: auto; padding: var(--td-gap); }
</style>