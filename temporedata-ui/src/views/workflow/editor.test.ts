// @vitest-environment browser
/**
 * Browser-level tests for the Workflow DAG editor (AntV X6 canvas).
 * Runs against a real Chromium via Vitest's Playwright provider; the backend
 * API is stubbed so no server is required.
 */
import { describe, expect, it, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import ElementPlus from 'element-plus'

const { getMock, updateMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  updateMock: vi.fn(),
}))

const sample = {
  id: 'wf-test',
  name: '测试工作流',
  description: '',
  status: 'DRAFT',
  nodes: [
    { id: 'n1', name: '取数', type: 'SQL', sql: 'select 1', positionX: 40, positionY: 20 },
    { id: 'n2', name: '清洗', type: 'SQL', sql: 'select 2', positionX: 280, positionY: 20 },
  ],
  edges: [{ id: 'e1', sourceNodeId: 'n1', targetNodeId: 'n2', edgeType: 'SUCCESS' }],
}

vi.mock('@/api/workflow', () => ({
  workflowApi: {
    get: getMock,
    update: updateMock,
    run: vi.fn(),
    instances: vi.fn(),
    instanceDetail: vi.fn(),
    logs: vi.fn(),
    commands: vi.fn(),
    pause: vi.fn(),
    resume: vi.fn(),
    stop: vi.fn(),
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: 'wf-test' } }),
}))

import Editor from './editor.vue'

/** Wait until `selector` matches `count` elements (X6 renders its virtual DOM asynchronously). */
async function waitX6Nodes(
  selector: string,
  count: number,
  timeout = 3000,
): Promise<NodeListOf<Element>> {
  const container = document.body
  const start = Date.now()
  for (;;) {
    const found = container.querySelectorAll(selector)
    if (found.length === count) return found
    if (Date.now() - start > timeout) return found
    await new Promise((r) => setTimeout(r, 50))
  }
}

/** Build a DragEvent-like event carrying a mock DataTransfer + coords (real
 *  DragEvent refuses a hand-rolled DataTransfer, so we attach via defineProperty). */
function makeDrag(type: string, x: number, y: number): Event {
  const dt = { setData: () => {}, getData: () => 'SQL' }
  const ev = new Event(type, { bubbles: true, cancelable: true })
  Object.defineProperty(ev, 'dataTransfer', { value: dt })
  Object.defineProperty(ev, 'clientX', { value: x })
  Object.defineProperty(ev, 'clientY', { value: y })
  return ev
}

async function mountEditor() {
  getMock.mockResolvedValue(sample)
  const wrapper = mount(Editor, {
    attachTo: document.body,
    global: { plugins: [ElementPlus] },
  })
  await waitX6Nodes('.x6-node', 2)
  return wrapper
}

describe('Workflow DAG editor (browser)', () => {
  it('loads the workflow and renders one X6 node per DAG node', async () => {
    const wrap = await mountEditor()
    try {
      expect(document.querySelectorAll('.x6-node').length).toBe(2)
      // property panel hints "点击节点 / 连线编辑" initially
      expect(wrap.find('.wfe-props').exists()).toBe(true)
    } finally {
      wrap.unmount()
    }
  })

  it('renders the node templates in the palette and the canvas host', async () => {
    const wrap = await mountEditor()
    try {
      const labels = wrap.findAll('.wfe-palette-item').map((i) => i.text().trim())
      expect(labels).toContain('SQL')
      expect(labels).toContain('HTTP')
      expect(labels).toContain('Python')
      expect(labels.length).toBe(7)
      expect(wrap.find('.wfe-canvas').exists()).toBe(true)
      expect(wrap.find('.wfe-toolbar').exists()).toBe(true)
    } finally {
      wrap.unmount()
    }
  })

  it('exposes DAG validation without throwing on an acyclic graph', async () => {
    const wrap = await mountEditor()
    try {
      // toolbar exposes 校验 button
      const validate = wrap.findAll('.wfe-toolbar button').find((b) => b.text().includes('DAG 校验'))
      expect(validate?.exists()).toBe(true)
    } finally {
      wrap.unmount()
    }
  })

  it('adds a node when a palette template is dragged and dropped onto the canvas', async () => {
    const wrap = await mountEditor()
    try {
      const before = document.querySelectorAll('.x6-node').length
      const paletteItem = wrap.findAll('.wfe-palette-item')[0].element
      const canvas = wrap.find('.wfe-canvas').element

      paletteItem.dispatchEvent(makeDrag('dragstart', 0, 0))
      canvas.dispatchEvent(makeDrag('drop', 180, 160))

      const after = await waitX6Nodes('.x6-node', before + 1)
      expect(after.length).toBe(before + 1)
    } finally {
      wrap.unmount()
    }
  })

  it('opens the property panel when a node is clicked and shows its type', async () => {
    const wrap = await mountEditor()
    try {
      const firstNode = document.querySelectorAll('.x6-node')[0]
      // X6 reacts to a full pointer press + release on the node element
      firstNode.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))
      firstNode.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
      firstNode.dispatchEvent(new MouseEvent('click', { bubbles: true }))
      await waitForNodeForm(wrap)

      const typeTags = wrap.findAll('.wfe-nodeform .el-tag')
      expect(typeTags.length).toBeGreaterThan(0)
      expect(wrap.findAll('.wfe-nodeform textarea').length).toBeGreaterThan(0)
    } finally {
      wrap.unmount()
    }
  })

  it('saves the graph with UI-free DTOs (encode strips view state)', async () => {
    const wrap = await mountEditor()
    try {
      const canvas = wrap.find('.wfe-canvas').element
      wrap.findAll('.wfe-palette-item')[0].element.dispatchEvent(makeDrag('dragstart', 0, 0))
      canvas.dispatchEvent(makeDrag('drop', 400, 300))
      await waitX6Nodes('.x6-node', 3)

      updateMock.mockResolvedValue(sample)
      const save = wrap.findAll('.wfe-toolbar button').find((b) => b.text().trim() === '保存')
      await save!.trigger('click')
      await new Promise((r) => setTimeout(r, 100))

      expect(updateMock).toHaveBeenCalledTimes(1)
      const [wfId, req] = updateMock.mock.calls[0]
      expect(wfId).toBe('wf-test')
      expect(req.nodes.length).toBe(3)
      // transport layer must not leak view state
      expect(req.nodes[0]).not.toHaveProperty('_selected')
      expect(req.nodes[0]).not.toHaveProperty('_status')
    } finally {
      wrap.unmount()
    }
  })
})

/** Poll until the node property form appears after selection. */
async function waitForNodeForm(wrapper: VueWrapper, timeout = 2000) {
  const start = Date.now()
  for (;;) {
    if (wrapper.find('.wfe-nodeform').exists()) return
    if (Date.now() - start > timeout) return
    await new Promise((r) => setTimeout(r, 40))
  }
}