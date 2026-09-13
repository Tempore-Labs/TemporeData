/**
 * Sidebar collapse state, persisted to localStorage.
 */
import { defineStore } from 'pinia'

const KEY = 'td_sider_collapsed'

export const useSidebarStore = defineStore('sidebar', {
  state: () => ({
    collapsed: localStorage.getItem(KEY) === '1',
  }),
  actions: {
    toggle() {
      this.collapsed = !this.collapsed
      localStorage.setItem(KEY, this.collapsed ? '1' : '0')
    },
  },
})
