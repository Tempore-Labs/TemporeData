<template>
  <div class="shell">
    <Sidebar />
    <div class="shell-main">
      <Topbar />
      <main class="shell-content">
        <router-view />
      </main>
    </div>
    <AssistantPanel />
  </div>
</template>

<script setup lang="ts">
import Sidebar from '@/components/app/Sidebar.vue'
import Topbar from '@/components/app/Topbar.vue'
import AssistantPanel from '@/components/app/AssistantPanel.vue'
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--td-bg);
}
.shell-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.shell-content {
  flex: 1;
  overflow: auto;
}
</style>
