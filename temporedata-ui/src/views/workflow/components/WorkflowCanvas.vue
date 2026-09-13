<template>
  <div class="wfe">
    <!-- toolbar: [name shown by parent] + operations -->
    <div class="wfe-toolbar">
      <span class="wfe-meta">
        <StatusBadge :status="workflow?.status || 'DRAFT'" />
        <el-tag v-if="readonly" size="small" type="success" effect="plain">已上线 · 只读</el-tag>
        <span class="wfe-id mono">{{ workflow?.id ? workflow.id.slice(0, 8) : '新工作流' }}</span>
      </span>
      <div class="wfe-spacer" />
      <el-button size="small" @click="layout">自动布局</el-button>
      <el-button size="small" type="warning" plain @click="validateGraph">DAG 校验</el-button>
      <el-divider direction="vertical" />
      <el-button size="small" @click="exportWorkflow">导出 JSON</el-button>
      <el-button size="small" @click="trigImport">导入 JSON</el-button>
      <input ref="importInput" type="file" accept=".json,application/json" style="display:none" @change="importWorkflow" />
      <el-divider direction="vertical" />
      <el-button size="small" type="primary" :disabled="readonly || saving" @click="save">保存</el-button>
      <el-divider direction="vertical" />
      <el-button size="small" type="success" plain :disabled="!workflow?.id" :loading="running" @click="run">
        执行
      </el-button>
      <el-button size="small" :disabled="!workflow?.id" @click="openDrawer">运行记录</el-button>
      <template v-if="activeInstanceId">
        <el-divider direction="vertical" />
        <el-button size="small" text @click="control('pause', activeInstanceId)">暂停</el-button>
        <el-button size="small" text @click="control('resume', activeInstanceId)">恢复</el-button>
        <el-button size="small" text type="danger" @click="control('stop', activeInstanceId)">停止</el-button>
      </template>
    </div>

    <div class="wfe-body">
      <div class="wfe-palette">
        <div class="wfe-palette-title">节点</div>
        <div
          v-for="t in templates"
          :key="t.type"
          class="wfe-palette-item"
          :style="{ borderColor: t.color }"
          draggable="true"
          @dragstart="onDragStart($event, t.type)"
        >
          <span class="wfe-palette-dot" :style="{ background: t.color }" />
          {{ t.label }}
        </div>
        <div class="wfe-palette-tip">拖拽左侧节点到画布</div>
      </div>

      <!-- canvas -->
      <div class="wfe-canvas" ref="canvasEl" @dragover.prevent @drop="onDrop" />

      <!-- property panel -->
      <div class="wfe-props">
        <div class="wfe-props-title">属性</div>
        <el-empty v-if="!sel" description="点击节点 / 连线编辑" :image-size="60" />
        <div v-else-if="sel.kind === 'node'" class="wfe-nodeform">
          <el-form label-position="top" size="small">
            <el-form-item label="节点名称">
              <el-input v-model="selProxy.name" @change="apply()" />
            </el-form-item>
            <el-form-item label="类型">
              <el-tag size="small">{{ selProxy.node.type }}</el-tag>
            </el-form-item>
            <template v-if="selProxy.node.type === 'SQL'">
              <el-form-item label="数据源 ID">
                <el-input v-model="selProxy.node.datasourceId" @change="apply()" />
              </el-form-item>
              <el-form-item label="SQL">
                <el-input v-model="selProxy.node.sql" type="textarea" :rows="5" @change="apply()" />
              </el-form-item>
            </template>
            <template v-else-if="selProxy.node.type === 'HTTP'">
              <el-form-item label="URL">
                <el-input v-model="selProxy.node.httpUrl" @change="apply()" />
              </el-form-item>
              <el-form-item label="Method">
                <el-select v-model="selProxy.node.httpMethod" style="width: 100%" @change="apply()">
                  <el-option v-for="m in ['GET','POST','PUT','DELETE']" :key="m" :value="m" />
                </el-select>
              </el-form-item>
              <el-form-item label="Headers(JSON)">
                <el-input v-model="selProxy.node.httpHeaders" type="textarea" :rows="3" @change="apply()" />
              </el-form-item>
            </template>
            <template v-else-if="selProxy.node.type === 'FLINK' || selProxy.node.type === 'SPARK'">
              <el-form-item label="引擎配置(JSON)">
                <el-input v-model="selProxy.node.sparkConf" type="textarea" :rows="3" @change="apply()" />
              </el-form-item>
              <el-form-item label="入口 / 参数">
                <el-input v-model="selProxy.node.params" type="textarea" :rows="4" @change="apply()" />
              </el-form-item>
            </template>
            <template v-else>
              <el-form-item label="脚本/参数">
                <el-input v-model="selProxy.node.params" type="textarea" :rows="5" @change="apply()" />
              </el-form-item>
            </template>

            <el-form-item label="重试次数">
              <el-input-number v-model="selProxy.node.retryCount" :min="0" :max="10" style="width: 100%" @change="apply()" />
            </el-form-item>
            <el-form-item label="超时(秒)">
              <el-input-number v-model="selProxy.node.timeoutSeconds" :min="0" :max="86400" style="width: 100%" @change="apply()" />
            </el-form-item>
            <el-form-item label="失败策略">
              <el-select v-model="selProxy.node.failStrategy" style="width: 100%" @change="apply()">
                <el-option v-for="s in ['FAIL','IGNORE','RETRY']" :key="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="告警邮箱">
              <el-input v-model="selProxy.node.emailTo" placeholder="a@x.com,b@x.com" @change="apply()" />
            </el-form-item>
            <el-form-item label="质量阈值">
              <el-input-number v-model="selProxy.node.qualityThreshold" :min="0" :max="100" style="width: 100%" @change="apply()" />
            </el-form-item>

            <el-button size="small" type="danger" text @click="removeNode">删除节点</el-button>
          </el-form>
        </div>
        <div v-else class="wfe-edgeform">
          <el-form label-position="top" size="small">
            <el-form-item label="边类型">
              <el-select v-model="selProxy.edge.edgeType" style="width: 100%" @change="apply()">
                <el-option v-for="s in ['SUCCESS','FAILURE','ALWAYS']" :key="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="条件表达式">
              <el-input v-model="selProxy.edge.conditionExpr" placeholder="如 ${status} == 'ok'" @change="apply()" />
            </el-form-item>
            <el-button size="small" type="danger" text @click="removeEdge">删除连线</el-button>
          </el-form>
        </div>
      </div>
    </div>

    <!-- running instance drawer -->
    <el-drawer v-model="drawerOpen" title="运行记录" size="520px">
      <template v-if="instances.length">
        <div class="wfe-inst-list">
          <div
            v-for="inst in instances"
            :key="inst.id"
            class="wfe-inst-item"
            :class="{ active: inst.id === activeInstanceId }"
            @click="selectInstance(inst.id)"
          >
            <StatusBadge :status="inst.status" :text="inst.status" />
            <span class="mono wfe-inst-time">{{ inst.startTime || '—' }}</span>
            <span class="wfe-inst-trigger">{{ inst.triggerType }}</span>
          </div>
        </div>

        <el-divider content-position="left">命令</el-divider>
        <div v-if="commands.length" class="wfe-cmd">
          <el-tag v-for="c in commands" :key="c.id" size="small" effect="plain" class="wfe-cmd-item">
            {{ c.type }} · {{ c.state }}
          </el-tag>
        </div>

        <el-divider content-position="left">节点状态</el-divider>
        <el-table :data="instanceNodes" size="small" height="220">
          <el-table-column prop="nodeName" label="节点" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
          </el-table-column>
          <el-table-column label="耗时(ms)" width="90">
            <template #default="{ row }"><span class="mono">{{ row.durationMs ?? '—' }}</span></template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">日志</el-divider>
        <pre v-if="logs.length" class="wfe-log">{{ logsText }}</pre>
        <el-empty v-else description="暂无日志" :image-size="50" />
      </template>
      <el-empty v-else description="暂无运行记录" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Graph, type Node as X6Node } from '@antv/x6'
import StatusBadge from '@/components/base/StatusBadge.vue'
import {
  workflowApi,
  type WorkflowInstanceRes,
  type WorkflowNodeDTO,
  type WorkflowRes,
  type InstanceLogItem,
  type RuntimeCommand,
} from '@/api/workflow'
import {
  NODE_TEMPLATES,
  NODE_STATUS_COLOR,
  autoLayout,
  danglingEdges,
  decode,
  encode,
  isDag,
  type DagEdgeVM,
  type DagNodeVM,
  type NodeStatus,
  type NodeTemplate,
  uid,
} from '@/utils/dag'

const props = withDefaults(
  defineProps<{
    workflow?: WorkflowRes | null
    name?: string
    readonly?: boolean
    focusNode?: string
  }>(),
  { workflow: null, name: '未命名工作流', readonly: false, focusNode: '' },
)
const emit = defineEmits<{ (e: 'saved', wf: WorkflowRes): void }>()

const saving = ref(false)
const running = ref(false)
const canvasEl = ref<HTMLElement>()
const importInput = ref<HTMLInputElement>()
const templates: NodeTemplate[] = NODE_TEMPLATES

let graph: Graph | null = null
const nodesStore = new Map<string, DagNodeVM>()
const edgesStore = new Map<string, DagEdgeVM>()

type Selection = { kind: 'node'; node: DagNodeVM } | { kind: 'edge'; edge: DagEdgeVM }
const sel = ref<Selection | null>(null)
const selProxy = reactive<any>({ node: null, edge: null, name: '' })
let pollTimer: ReturnType<typeof setInterval> | null = null

const drawerOpen = ref(false)
const instances = ref<WorkflowInstanceRes[]>([])
const commands = ref<RuntimeCommand[]>([])
const logs = ref<InstanceLogItem[]>([])
const activeInstanceId = ref<string>('')

const instanceNodes = computed(() => {
  const inst = instances.value.find((i) => i.id === activeInstanceId.value)
  return inst?.nodes || []
})
const logsText = computed(() => logs.value.map((l) => `[${l.level}] ${l.createTime || ''} ${l.message}`).join('\n'))

function registerShape() {
  Graph.registerNode(
    'wf-node',
    {
      inherit: 'rect',
      width: 180,
      height: 64,
      attrs: {
        body: { rx: 8, ry: 8, stroke: '#c0c4cc', strokeWidth: 1, fill: '#ffffff' },
        label: {
          refX: 0.5,
          refY: 0.5,
          textAnchor: 'middle',
          textVerticalAnchor: 'middle',
          fontSize: 12,
          fill: '#303133',
        },
      },
      ports: {
        groups: {
          in: { position: 'left', attrs: { circle: { r: 5, magnet: true, stroke: '#409eff', fill: '#fff', visibility: 'hidden' } } },
          out: { position: 'right', attrs: { circle: { r: 5, magnet: true, stroke: '#409eff', fill: '#fff', visibility: 'hidden' } } },
        },
        items: [
          { id: 'in', group: 'in' },
          { id: 'out', group: 'out' },
        ],
      },
    },
    true,
  )
}

function initGraph() {
  if (!canvasEl.value) return
  registerShape()
  graph = new Graph({
    container: canvasEl.value,
    grid: { visible: true, size: 10 },
    panning: { enabled: true, eventTypes: ['leftMouseDown', 'mouseWheel'] },
    mousewheel: { enabled: true, modifiers: ['ctrl'] },
    connecting: {
      snap: true,
      allowBlank: false,
      allowLoop: false,
      allowMulti: true,
      highlight: true,
      router: 'manhattan',
      connector: 'rounded',
      validateConnection({ sourceMagnet, targetMagnet }) { return !!(sourceMagnet && targetMagnet) },
    },
  })

  graph.on('node:move:ended', ({ node }: { node: X6Node }) => {
    const dto = nodesStore.get(node.id)
    if (dto) {
      dto.positionX = node.position().x
      dto.positionY = node.position().y
    }
  })
  const g = graph
  g.on('node:click', ({ node }) => {
    clearHighlights(g)
    const dto = nodesStore.get(node.id)
    if (dto) {
      sel.value = { kind: 'node', node: dto }
      selectNode(dto)
      g.getCellById(node.id)?.attr('body/strokeWidth', 2)
    }
  })
  g.on('edge:click', ({ edge }) => {
    clearHighlights(g)
    const evm = edgesStore.get(edge.id)
    if (evm) {
      sel.value = { kind: 'edge', edge: evm }
      selectEdge(evm)
      edge.attr('line/stroke', '#409eff')
    }
  })
  g.on('blank:click', () => {
    clearHighlights(g)
    sel.value = null
  })
  g.on('edge:connected', ({ edge }) => {
    const source = edge.getSource() as { cell: string }
    const target = edge.getTarget() as { cell: string }
    if (source?.cell && target?.cell) {
      const evm: DagEdgeVM = { id: edge.id, sourceNodeId: source.cell, targetNodeId: target.cell, edgeType: 'SUCCESS' }
      edgesStore.set(edge.id, evm)
    }
  })
  g.on('edge:removed', ({ edge }) => edgesStore.delete(edge.id))
}

function clearHighlights(g: Graph) {
  g.getCells().forEach((cell) => {
    if (cell.isNode()) cell.attr('body/strokeWidth', 1)
    else if (cell.isEdge()) cell.attr('line/stroke', '#a8abb2')
  })
}

function addNodeFromTemplate(tpl: NodeTemplate, x: number, y: number): void {
  if (!graph) return
  const id = uid('n')
  const dto: DagNodeVM = { id, name: `${tpl.label}_${id.slice(-4)}`, type: tpl.type, positionX: x, positionY: y, ...tpl.defaults, _status: 'idle' }
  nodesStore.set(id, dto)
  graph.addNode({
    id,
    shape: 'wf-node',
    position: { x, y },
    attrs: { body: { stroke: tpl.color, fill: '#fff' }, label: { text: dto.name } },
  })
}

let dragType: WorkflowNodeDTO['type'] | null = null
function onDragStart(e: DragEvent, type: WorkflowNodeDTO['type']) {
  dragType = type
  if (e.dataTransfer) e.dataTransfer.setData('text/plain', type)
}
function onDrop(e: DragEvent) {
  if (props.readonly) return
  const type = dragType || (e.dataTransfer?.getData('text/plain') as WorkflowNodeDTO['type'])
  const tpl = templates.find((t) => t.type === type)
  if (!tpl || !graph) return
  const p = graph.clientToLocal(e.clientX, e.clientY)
  addNodeFromTemplate(tpl, p.x, p.y)
  dragType = null
}

function selectNode(dto: DagNodeVM) {
  selProxy.node = dto
  selProxy.name = dto.name
}
function selectEdge(evm: DagEdgeVM) {
  selProxy.edge = evm
}
function apply() {
  if (!graph) return
  const s = sel.value
  if (s?.kind === 'node') {
    const cell = graph.getCellById(s.node.id)
    if (cell) cell.attr('label/text', s.node.name)
  }
}
function removeNode() {
  if (props.readonly) { ElMessage.warning('已上线只读'); return }
  const s = sel.value
  if (!graph || s?.kind !== 'node') return
  if (s.node._status === 'RUNNING') { ElMessage.warning('运行中节点不可删除'); return }
  const cell = graph.getCellById(s.node.id)
  const edges = graph.getConnectedEdges(cell)
  edges.forEach((ed) => ed.remove())
  cell?.remove()
  nodesStore.delete(s.node.id)
  sel.value = null
}
function removeEdge() {
  if (props.readonly) { ElMessage.warning('已上线只读'); return }
  const s = sel.value
  if (!graph || s?.kind !== 'edge') return
  graph.getCellById(s.edge.id)?.remove()
  edgesStore.delete(s.edge.id)
  sel.value = null
}

function validateGraph() {
  if (!graph) return
  const nodes = [...nodesStore.values()]
  const edges = [...edgesStore.values()]
  if (!nodes.length) { ElMessage.warning('画布为空'); return }
  if (!isDag(nodes, edges)) { ElMessage.error('存在环路，不是合法的 DAG'); return }
  const dangling = danglingEdges(nodes, edges)
  if (dangling.length) { ElMessage.warning(`${dangling.length} 条关联节点缺失`); return }
  ElMessage.success('校验通过：合法 DAG')
}
function layout() {
  if (!graph || !nodesStore.size) return
  const g = graph
  const coords = autoLayout([...nodesStore.values()], [...edgesStore.values()])
  const all = [...nodesStore.values()]
  all.forEach((dto, i) => {
    const node = g.getCellById(dto.id) as X6Node | null
    node && node.setPosition(coords[i].x, coords[i].y)
    dto.positionX = coords[i].x
    dto.positionY = coords[i].y
  })
  g.zoomToFit({ padding: 20 })
}

async function save() {
  if (props.readonly) { ElMessage.warning('工作流已上线，处于只读状态'); return }
  const { nodes, edges } = encode([...nodesStore.values()], [...edgesStore.values()])
  if (nodes.length && !isDag(nodes, edges)) { ElMessage.error('存在环路，禁止保存'); return }
  saving.value = true
  try {
    const payload = {
      name: props.name || '未命名工作流',
      description: props.workflow?.description || '',
      nodes,
      edges,
    }
    const res = props.workflow?.id
      ? await workflowApi.update(props.workflow.id, payload)
      : await workflowApi.create(payload)
    emit('saved', res)
    ElMessage.success('已保存')
  } finally {
    saving.value = false
  }
}

async function run() {
  if (!props.workflow?.id) { ElMessage.warning('请先保存工作流'); return }
  const { nodes, edges } = encode([...nodesStore.values()], [...edgesStore.values()])
  if (nodes.length && !isDag(nodes, edges)) { ElMessage.error('存在环路，无法执行'); return }
  running.value = true
  try {
    const res = await workflowApi.run(props.workflow.id)
    activeInstanceId.value = res.instanceId
    ElMessage.success(`已提交执行: ${res.status}`)
    startPolling()
  } finally {
    running.value = false
  }
}
function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!activeInstanceId.value) return
    try {
      const inst = await workflowApi.instanceDetail(activeInstanceId.value)
      applyInstanceStatus(inst)
      if (inst.status === 'SUCCESS' || inst.status === 'FAILED') stopPolling()
    } catch { /* transient */ }
  }, 1500)
}
function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}
function applyInstanceStatus(inst: WorkflowInstanceRes) {
  if (!graph) return
  const byId = new Map((inst.nodes || []).map((n) => [n.nodeId, n]))
  graph.getNodes().forEach((cell) => {
    const instNode = byId.get(cell.id)
    let status: NodeStatus = 'idle'
    if (instNode) {
      status = instNode.status as NodeStatus
      const dto = nodesStore.get(cell.id)
      if (dto) { dto._status = status; dto._errorMsg = instNode.errorMsg }
    }
    cell.attr('body/fill', NODE_STATUS_COLOR[status] ?? '#fff')
  })
  graph.resetCells(graph.getCells())
}
async function control(cmd: 'pause' | 'resume' | 'stop', instanceId: string) {
  const fns: any = { pause: workflowApi.pause, resume: workflowApi.resume, stop: workflowApi.stop }
  await fns[cmd](instanceId)
  ElMessage.success({ pause: '已暂停', resume: '已恢复', stop: '已停止' }[cmd])
}

async function openDrawer() {
  drawerOpen.value = true
  await loadInstances()
}
async function loadInstances() {
  if (!props.workflow?.id) return
  instances.value = await workflowApi.instances(props.workflow.id)
  if (instances.value.length && !activeInstanceId.value) activeInstanceId.value = instances.value[0].id
  await loadDetail()
}
async function selectInstance(id: string) {
  activeInstanceId.value = id
  await loadDetail()
}
async function loadDetail() {
  if (!activeInstanceId.value) return
  commands.value = await workflowApi.commands(activeInstanceId.value)
  logs.value = await workflowApi.logs(activeInstanceId.value)
}

function applyDecoded(nodes: DagNodeVM[], edges: DagEdgeVM[]) {
  if (!graph) return
  const g = graph
  nodes.forEach((dto) => {
    nodesStore.set(dto.id, dto)
    g.addNode({
      id: dto.id,
      shape: 'wf-node',
      position: { x: dto.positionX ?? 40, y: dto.positionY ?? 40 },
      attrs: {
        body: { stroke: NODE_TEMPLATES.find((t) => t.type === dto.type)?.color ?? '#409eff', fill: '#fff' },
        label: { text: dto.name },
      },
    })
  })
  edges.forEach((evm) => {
    edgesStore.set(evm.id, evm)
    const [source, target] = [evm.sourceNodeId, evm.targetNodeId]
    if (g.hasCell(source) && g.hasCell(target)) {
      g.addEdge({
        id: evm.id,
        source: { cell: source, port: 'out' },
        target: { cell: target, port: 'in' },
        attrs: { line: { stroke: '#a8abb2', strokeWidth: 1.5, targetMarker: { name: 'block', size: 6 } } },
      })
    }
  })
  g.zoomToFit({ padding: 20 })
}

function buildGraph() {
  const { nodes, edges } = decode(props.workflow?.nodes, props.workflow?.edges)
  applyDecoded(nodes, edges)
}

function clearAll() {
  nodesStore.clear()
  edgesStore.clear()
  sel.value = null
  graph?.clearCells()
}

function exportWorkflow() {
  const { nodes, edges } = encode([...nodesStore.values()], [...edgesStore.values()])
  const payload = { name: props.name, description: props.workflow?.description || '', nodes, edges }
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${props.name || 'workflow'}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出 JSON')
}

function trigImport() {
  importInput.value?.click()
}

async function importWorkflow(e: Event) {
  if (props.readonly) return
  const file = (e.target as HTMLInputElement).files?.[0]
  ;(e.target as HTMLInputElement).value = ''
  if (!file) return
  try {
    const dto = JSON.parse(await file.text())
    if (!Array.isArray(dto.nodes) || !Array.isArray(dto.edges)) {
      ElMessage.error('JSON 缺少 nodes/edges 结构')
      return
    }
    const { nodes, edges } = decode(dto.nodes, dto.edges)
    clearAll()
    applyDecoded(nodes, edges)
    ElMessage.success(`导入成功：${nodes.length} 节点 / ${edges.length} 边`)
  } catch (err) {
    ElMessage.error('JSON 文件格式错误')
  }
}

onMounted(() => {
  initGraph()
  if (graph) buildGraph()
})
onBeforeUnmount(() => {
  stopPolling()
  graph?.dispose()
})

watch(
  () => props.focusNode,
  (id) => {
    if (!id || !graph) return
    const cell = graph.getCellById(id)
    if (!cell) return
    graph.centerCell(cell)
    clearHighlights(graph)
    ;(cell as X6Node).attr('body/strokeWidth', 3)
    ;(cell as X6Node).attr('body/stroke', '#f0823b')
    sel.value = { kind: 'node', node: nodesStore.get(id) || { id, name: id, type: 'SQL' } }
    selectNode(nodesStore.get(id) || { id, name: id, type: 'SQL' })
    // reset highlight after a beat
    setTimeout(() => {
      if (graph && graph.hasCell(id)) (graph.getCellById(id) as X6Node)?.attr('body/strokeWidth', 1)
    }, 1600)
  },
)
</script>

<style scoped>
.wfe { display: flex; flex-direction: column; height: 100%; }
.wfe-toolbar { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border-bottom: 1px solid var(--el-border-color-lighter); }
.wfe-meta { display: flex; align-items: center; gap: 8px; }
.wfe-id { font-size: 12px; color: var(--el-text-color-secondary); }
.wfe-spacer { flex: 1; }
.wfe-body { display: flex; flex: 1; min-height: 0; }
.wfe-palette { width: 150px; border-right: 1px solid var(--el-border-color-lighter); padding: 8px; box-sizing: border-box; overflow: auto; }
.wfe-palette-title { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 6px; }
.wfe-palette-item { display: flex; align-items: center; gap: 8px; cursor: grab; border: 1px solid; border-radius: 6px; padding: 6px 8px; margin-bottom: 6px; background: #fff; font-size: 13px; user-select: none; }
.wfe-palette-dot { width: 8px; height: 8px; border-radius: 50%; }
.wfe-palette-tip { font-size: 11px; color: var(--el-text-color-placeholder); margin-top: 8px; line-height: 1.5; }
.wfe-canvas { flex: 1; min-width: 0; background: #fafafa; }
.wfe-props { width: 280px; border-left: 1px solid var(--el-border-color-lighter); padding: 10px; overflow: auto; }
.wfe-props-title { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 8px; }
.wfe-inst-list { display: flex; flex-direction: column; gap: 6px; }
.wfe-inst-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 6px; cursor: pointer; }
.wfe-inst-item.active { background: var(--el-color-primary-light-9); }
.wfe-inst-time { color: var(--el-text-color-secondary); font-size: 12px; flex: 1; }
.wfe-inst-trigger { font-size: 12px; }
.wfe-cmd { display: flex; flex-wrap: wrap; gap: 4px; }
.wfe-cmd-item { margin: 0; }
.wfe-log { max-height: 220px; overflow: auto; background: #1e1e1e; color: #d4d4d4; font-size: 12px; padding: 8px; border-radius: 6px; white-space: pre-wrap; word-break: break-all; }
.mono { font-family: var(--td-font-mono); }
</style>