import { defineStore } from 'pinia'
import { ref } from 'vue'

/** Global control for the AI assistant chat panel (opened from the Topbar). */
export const useAssistantStore = defineStore('assistant', () => {
  const open = ref(false)

  function show() {
    open.value = true
  }
  function hide() {
    open.value = false
  }
  function toggle() {
    open.value = !open.value
  }

  return { open, show, hide, toggle }
})