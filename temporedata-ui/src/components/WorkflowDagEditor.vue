<template>
  <div class="dag-editor">
    <!-- Toolbar -->
    <div class="dag-toolbar">
      <div class="toolbar-left">
        <el-dropdown size="small" trigger="click">
          <el-button size="small" :icon="Plus">添加节点</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled><span style="color:#9ca3af;font-size:11px">数据处理</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('SQL')"><span class="node-opt sql">SQL</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('SPARK_SQL')"><span class="node-opt spark_sql">Spark SQL</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('SHELL')"><span class="node-opt shell">Shell</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('PYTHON')"><span class="node-opt python">Python</span></el-dropdown-item>
              <el-dropdown-item disabled><span style="color:#9ca3af;font-size:11px">数据集成</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('DATAX')"><span class="node-opt datax">DataX</span></el-dropdown-item>
              <el-dropdown-item disabled><span style="color:#9ca3af;font-size:11px">流程控制</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('CONDITION')"><span class="node-opt condition">条件分支</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('SUB_WORKFLOW')"><span class="node-opt sub_workflow">子工作流</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('DEPENDENCY')"><span class="node-opt dependency">依赖检查</span></el-dropdown-item>
              <el-dropdown-item disabled><span style="color:#9ca3af;font-size:11px">外部服务</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('HTTP')"><span class="node-opt http">HTTP</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('EMAIL')"><span class="node-opt email">邮件通知</span></el-dropdown-item>
              <el-dropdown-item disabled><span style="color:#9ca3af;font-size:11px">数据质量</span></el-dropdown-item>
              <el-dropdown-item @click="addNode('DATA_QUALITY')"><span class="node-opt data_quality">数据质量检查</span></el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-divider direction="vertical" />
        <el-button size="small" :icon="Delete" @click="deleteSelected" :disabled="!selectedNode">删除选中</el-button>
        <el-button size="small" :icon="CopyDocument" @click="copySelected" :disabled="!selectedNode">复制</el-button>
        <el-divider direction="vertical" />
        <el-button size="small" :icon="RefreshLeft" @click="undo" :disabled="undoStack.length === 0">撤销</el-button>
        <el-button size="small" :icon="RefreshRight" @click="redo" :disabled="redoStack.length === 0">重做</el-button>
      </div>
      <div class="toolbar-right">
        <el-button size="small" :icon="FullScreen" @click="fitView">适应画布</el-button>
        <el-button size="small" :icon="WarningFilled" @click="validateDag" type="warning" plain v-if="validationErrors.length > 0">
          校验 ({{ validationErrors.length }})
        </el-button>
        <el-button size="small" :icon="RefreshLeft" @click="$emit('reload')">重载</el-button>
      </div>
    </div>

    <!-- Validation errors -->
    <div class="validation-bar" v-if="showValidation && validationErrors.length > 0">
      <el-alert
        v-for="(err, i) in validationErrors"
        :key="i"
        :title="err"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 4px"
      />
    </div>

    <!-- Canvas + Sidebar -->
    <div class="dag-body">
      <div class="dag-canvas" @keydown="onKeydown" tabindex="0" ref="canvasRef">
        <VueFlow
          v-model:nodes="nodes"
          v-model:edges="edges"
          :node-types="nodeTypes"
          :default-viewport="{ x: 0, y: 0, zoom: 1 }"
          :min-zoom="0.2"
          :max-zoom="4"
          :snap-to-grid="true"
          :snap-grid="[20, 20]"
          :connection-line-style="{ stroke: '#3b82f6', strokeWidth: 2 }"
          :default-edge-options="{ type: 'smoothstep', animated: true, style: { stroke: '#94a3b8', strokeWidth: 2 } }"
          @node-click="onNodeClick"
          @pane-click="onPaneClick"
          @connect="onConnect"
          @node-drag-stop="onNodeDragStop"
          @edge-click="onEdgeClick"
          fit-view-on-init
        >
          <Background :gap="20" :size="1" />
          <Controls position="bottom-right" />
          <MiniMap position="bottom-left" :pannable="true" :zoomable="true" />
        </VueFlow>
      </div>

      <!-- Node config panel -->
      <NodeConfigPanel
        :node="selectedNode"
        :datasources="datasources"
        @update="onNodeUpdate"
        @delete="onNodeDelete"
        @close="selectedNode = null"
      />

      <!-- Edge type selector (shown when edge is selected) -->
      <div class="edge-config-panel" v-if="selectedEdge">
        <div class="panel-header">
          <h4>连线配置</h4>
          <el-button text :icon="Close" @click="selectedEdge = null" />
        </div>
        <div class="edge-config-body">
          <el-form label-width="70px" size="small">
            <el-form-item label="触发条件">
              <el-select v-model="selectedEdgeEdgeType" @change="onEdgeTypeChange" style="width: 100%">
                <el-option label="成功" value="SUCCESS">
                  <el-tag type="success" size="small" effect="plain">成功</el-tag>
                  <span style="color:#9ca3af;font-size:11px;margin-left:4px">上游成功时执行</span>
                </el-option>
                <el-option label="失败" value="FAILURE">
                  <el-tag type="danger" size="small" effect="plain">失败</el-tag>
                  <span style="color:#9ca3af;font-size:11px;margin-left:4px">上游失败时执行</span>
                </el-option>
                <el-option label="总是" value="ALWAYS">
                  <el-tag type="info" size="small" effect="plain">总是</el-tag>
                  <span style="color:#9ca3af;font-size:11px;margin-left:4px">无论成功与否都执行</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-divider />
            <el-button type="danger" plain size="small" @click="deleteEdge" style="width: 100%">
              <el-icon><Delete /></el-icon> 删除连线
            </el-button>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, markRaw, watch, computed } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { Plus, Delete, FullScreen, RefreshLeft, RefreshRight, CopyDocument, WarningFilled, Close } from '@element-plus/icons-vue'
import DagNode from './DagNode.vue'
import NodeConfigPanel from './NodeConfigPanel.vue'

import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'

const props = defineProps({
  workflowNodes: { type: Array, default: () => [] },
  workflowEdges: { type: Array, default: () => [] },
  datasources: { type: Array, default: () => [] }
})

const emit = defineEmits(['change', 'reload'])

const nodeTypes = { dag: markRaw(DagNode) }

const { fitView } = useVueFlow()

const nodes = ref([])
const edges = ref([])
const selectedNode = ref(null)
const selectedEdge = ref(null)
const canvasRef = ref(null)
const showValidation = ref(false)
const validationErrors = ref([])
let nodeCounter = 0

// Edge type for selected edge
const selectedEdgeEdgeType = ref('SUCCESS')

// Undo/Redo stacks
const undoStack = ref([])
const redoStack = ref([])

// Watch prop changes to sync with parent
watch(() => props.workflowNodes, (val) => {
  if (val && val.length > 0) {
    nodes.value = val.map(n => toFlowNode(n))
    nodeCounter = val.length
  }
}, { immediate: true })

watch(() => props.workflowEdges, (val) => {
  if (val && val.length > 0) {
    edges.value = val.map(e => toFlowEdge(e))
  }
}, { immediate: true })

// Convert backend node to vue-flow node
function toFlowNode(n) {
  return {
    id: n.id,
    type: 'dag',
    position: { x: n.positionX || 0, y: n.positionY || 0 },
    data: {
      label: n.name,
      type: n.type || 'SQL',
      datasourceId: n.datasourceId,
      datasourceName: n.datasourceName,
      sql: n.sql,
      priority: n.priority || 'MEDIUM',
      failStrategy: n.failStrategy || 'STOP',
      retryCount: n.retryCount ?? 0,
      retryInterval: n.retryInterval ?? 1,
      timeoutSeconds: n.timeoutSeconds ?? 0,
      sparkConf: n.sparkConf || '',
      httpUrl: n.httpUrl || '',
      httpMethod: n.httpMethod || 'GET',
      httpHeaders: n.httpHeaders || '',
      dependencyType: n.dependencyType || 'TIME_POINT',
      dependencyTimeout: n.dependencyTimeout ?? 0,
      emailTo: n.emailTo || '',
      emailSubject: n.emailSubject || '',
      qualityType: n.qualityType || 'ROW_COUNT',
      qualityThreshold: n.qualityThreshold ?? 0
    },
    selected: false
  }
}

// Convert backend edge to vue-flow edge
function toFlowEdge(e) {
  const edgeType = e.edgeType || 'SUCCESS'
  const colorMap = { SUCCESS: '#10b981', FAILURE: '#ef4444', ALWAYS: '#6b7280' }
  const labelMap = { SUCCESS: '成功', FAILURE: '失败', ALWAYS: '总是' }

  return {
    id: e.id,
    source: e.sourceNodeId,
    target: e.targetNodeId,
    animated: true,
    style: { stroke: colorMap[edgeType] || '#94a3b8', strokeWidth: 2 },
    type: 'smoothstep',
    label: labelMap[edgeType] || '',
    labelStyle: { fill: '#6b7280', fontSize: 10, fontWeight: 600 },
    labelBgStyle: { fill: '#f3f4f6', fillOpacity: .9 },
    labelBgPadding: [4, 2],
    labelBgBorderRadius: 3,
    data: { edgeType }
  }
}

// Generate unique node ID
function genId() {
  return 'node_' + Date.now() + '_' + (++nodeCounter)
}

// Push current state to undo stack
function pushUndo() {
  undoStack.value.push({
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    edges: JSON.parse(JSON.stringify(edges.value))
  })
  // Limit undo stack to 50
  if (undoStack.value.length > 50) undoStack.value.shift()
  redoStack.value = []
}

// Add a new node to the canvas
function addNode(type) {
  pushUndo()
  const id = genId()
  const newNode = {
    id,
    type: 'dag',
    position: { x: 100 + Math.random() * 200, y: 100 + Math.random() * 200 },
    data: {
      label: '新节点 ' + nodeCounter,
      type,
      datasourceId: '',
      datasourceName: '',
      sql: '',
      priority: 'MEDIUM',
      failStrategy: 'STOP',
      retryCount: 0,
      retryInterval: 1,
      timeoutSeconds: 0
    }
  }
  nodes.value.push(newNode)
  emitChange()
}

// Handle node click
function onNodeClick({ node }) {
  selectedNode.value = node
  selectedEdge.value = null
  nodes.value.forEach(n => { n.selected = n.id === node.id })
}

// Handle pane click (deselect)
function onPaneClick() {
  selectedNode.value = null
  selectedEdge.value = null
  nodes.value.forEach(n => { n.selected = false })
}

// Handle edge click
function onEdgeClick({ edge }) {
  selectedNode.value = null
  selectedEdge.value = edge
  selectedEdgeEdgeType.value = edge.data?.edgeType || 'SUCCESS'
  nodes.value.forEach(n => { n.selected = false })
}

// Handle edge creation
function onConnect(connection) {
  pushUndo()
  const edge = {
    id: 'edge_' + connection.source + '_' + connection.target,
    source: connection.source,
    target: connection.target,
    animated: true,
    style: { stroke: '#10b981', strokeWidth: 2 },
    type: 'smoothstep',
    label: '成功',
    labelStyle: { fill: '#6b7280', fontSize: 10, fontWeight: 600 },
    labelBgStyle: { fill: '#f3f4f6', fillOpacity: .9 },
    labelBgPadding: [4, 2],
    labelBgBorderRadius: 3,
    data: { edgeType: 'SUCCESS' }
  }
  edges.value.push(edge)
  emitChange()
}

// Handle edge type change
function onEdgeTypeChange(newType) {
  if (!selectedEdge.value) return
  pushUndo()
  const colorMap = { SUCCESS: '#10b981', FAILURE: '#ef4444', ALWAYS: '#6b7280' }
  const labelMap = { SUCCESS: '成功', FAILURE: '失败', ALWAYS: '总是' }

  const idx = edges.value.findIndex(e => e.id === selectedEdge.value.id)
  if (idx === -1) return

  edges.value[idx].style = { ...edges.value[idx].style, stroke: colorMap[newType] || '#94a3b8' }
  edges.value[idx].label = labelMap[newType] || ''
  edges.value[idx].data = { edgeType: newType }
  emitChange()
}

// Delete edge
function deleteEdge() {
  if (!selectedEdge.value) return
  pushUndo()
  edges.value = edges.value.filter(e => e.id !== selectedEdge.value.id)
  selectedEdge.value = null
  emitChange()
}

// Handle node drag stop
function onNodeDragStop() {
  emitChange()
}

// Handle node config update from panel
function onNodeUpdate(config) {
  if (!selectedNode.value) return
  pushUndo()
  const idx = nodes.value.findIndex(n => n.id === selectedNode.value.id)
  if (idx === -1) return

  nodes.value[idx].data = {
    ...nodes.value[idx].data,
    label: config.name,
    type: config.type,
    datasourceId: config.datasourceId,
    sql: config.sql,
    priority: config.priority,
    failStrategy: config.failStrategy,
    retryCount: config.retryCount,
    retryInterval: config.retryInterval,
    timeoutSeconds: config.timeoutSeconds,
    sparkConf: config.sparkConf,
    httpUrl: config.httpUrl,
    httpMethod: config.httpMethod,
    httpHeaders: config.httpHeaders,
    dependencyType: config.dependencyType,
    dependencyTimeout: config.dependencyTimeout,
    emailTo: config.emailTo,
    emailSubject: config.emailSubject,
    qualityType: config.qualityType,
    qualityThreshold: config.qualityThreshold
  }
  if (config.positionX !== undefined) {
    nodes.value[idx].position = { x: config.positionX, y: config.positionY }
  }

  selectedNode.value = nodes.value[idx]
  emitChange()
}

// Delete selected node
function onNodeDelete() {
  if (!selectedNode.value) return
  pushUndo()
  const nodeId = selectedNode.value.id
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)
  selectedNode.value = null
  emitChange()
}

// Copy selected node
function copySelected() {
  if (!selectedNode.value) return
  pushUndo()
  const id = genId()
  const copy = JSON.parse(JSON.stringify(selectedNode.value))
  copy.id = id
  copy.position = { x: copy.position.x + 50, y: copy.position.y + 50 }
  copy.data.label = copy.data.label + ' (副本)'
  copy.selected = false
  nodes.value.push(copy)
  emitChange()
}

// Undo
function undo() {
  if (undoStack.value.length === 0) return
  redoStack.value.push({
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    edges: JSON.parse(JSON.stringify(edges.value))
  })
  const state = undoStack.value.pop()
  nodes.value = state.nodes
  edges.value = state.edges
  selectedNode.value = null
  selectedEdge.value = null
  emitChange()
}

// Redo
function redo() {
  if (redoStack.value.length === 0) return
  undoStack.value.push({
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    edges: JSON.parse(JSON.stringify(edges.value))
  })
  const state = redoStack.value.pop()
  nodes.value = state.nodes
  edges.value = state.edges
  selectedNode.value = null
  selectedEdge.value = null
  emitChange()
}

// Delete selected node from toolbar
function deleteSelected() {
  onNodeDelete()
}

// Keyboard shortcuts
function onKeydown(e) {
  if (e.ctrlKey || e.metaKey) {
    switch (e.key.toLowerCase()) {
      case 'z': e.preventDefault(); undo(); break
      case 'y': e.preventDefault(); redo(); break
      case 'c': e.preventDefault(); copySelected(); break
      case 'a': e.preventDefault(); break // prevent select all
      case 's': e.preventDefault(); emitChange(); break // save
      case 'delete': case 'backspace': e.preventDefault(); deleteSelected(); break
    }
  } else if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    deleteSelected()
  }
}

// DAG validation
function validateDag() {
  const errors = []
  const nodeIds = new Set(nodes.value.map(n => n.id))

  // Check for disconnected nodes (no edges)
  const connectedNodes = new Set()
  edges.value.forEach(e => {
    connectedNodes.add(e.source)
    connectedNodes.add(e.target)
  })

  if (nodes.value.length > 1) {
    nodes.value.forEach(n => {
      if (!connectedNodes.has(n.id)) {
        errors.push(`节点 "${n.data.label}" 未连接任何连线`)
      }
    })
  }

  // Check for nodes with same name
  const names = new Map()
  nodes.value.forEach(n => {
    const name = n.data.label
    if (names.has(name)) {
      errors.push(`节点名称重复: "${name}"`)
    }
    names.set(name, true)
  })

  // Check for empty names
  nodes.value.forEach(n => {
    if (!n.data.label || n.data.label.trim() === '') {
      errors.push(`存在未命名的节点`)
    }
  })

  // Check for edges referencing missing nodes
  edges.value.forEach(e => {
    if (!nodeIds.has(e.source)) {
      errors.push(`连线引用了不存在的源节点: ${e.source}`)
    }
    if (!nodeIds.has(e.target)) {
      errors.push(`连线引用了不存在的目标节点: ${e.target}`)
    }
  })

  validationErrors.value = errors
  showValidation.value = true

  if (errors.length === 0) {
    ElMessage.success('DAG 校验通过')
  }

  return errors.length === 0
}

// Emit change event with current nodes/edges
function emitChange() {
  const outputNodes = nodes.value.map(n => ({
    id: n.id,
    name: n.data.label,
    type: n.data.type,
    datasourceId: n.data.datasourceId,
    sql: n.data.sql,
    positionX: n.position.x,
    positionY: n.position.y,
    priority: n.data.priority,
    failStrategy: n.data.failStrategy,
    retryCount: n.data.retryCount,
    retryInterval: n.data.retryInterval,
    timeoutSeconds: n.data.timeoutSeconds,
    sparkConf: n.data.sparkConf,
    httpUrl: n.data.httpUrl,
    httpMethod: n.data.httpMethod,
    httpHeaders: n.data.httpHeaders,
    dependencyType: n.data.dependencyType,
    dependencyTimeout: n.data.dependencyTimeout,
    emailTo: n.data.emailTo,
    emailSubject: n.data.emailSubject,
    qualityType: n.data.qualityType,
    qualityThreshold: n.data.qualityThreshold
  }))
  const outputEdges = edges.value.map(e => ({
    id: e.id,
    sourceNodeId: e.source,
    targetNodeId: e.target,
    edgeType: e.data?.edgeType || 'SUCCESS'
  }))
  emit('change', { nodes: outputNodes, edges: outputEdges })
}

// Expose validateDag
import { ElMessage } from 'element-plus'
defineExpose({ validateDag })
</script>

<style scoped>
.dag-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #fafbfc;
}
.dag-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 6px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.validation-bar {
  padding: 8px 12px;
  background: #fffbeb;
  border-bottom: 1px solid #fde68a;
  flex-shrink: 0;
}
.dag-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}
.dag-canvas {
  flex: 1;
  min-width: 0;
  outline: none;
}
.dag-canvas:focus {
  outline: 2px solid #3b82f6;
  outline-offset: -2px;
}

/* Edge config panel */
.edge-config-panel {
  width: 260px;
  border-left: 1px solid #e5e7eb;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.panel-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}
.edge-config-body {
  padding: 16px;
}

/* Node option colors in dropdown */
.node-opt {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.node-opt.sql { background: #dbeafe; color: #2563eb; }
.node-opt.shell { background: #fef3c7; color: #d97706; }
.node-opt.python { background: #d1fae5; color: #059669; }
.node-opt.spark_sql { background: #fce7f3; color: #db2777; }
.node-opt.datax { background: #e0e7ff; color: #4f46e5; }
.node-opt.http { background: #cffafe; color: #0891b2; }
.node-opt.sub_workflow { background: #f3e8ff; color: #7c3aed; }
.node-opt.condition { background: #fef9c3; color: #ca8a04; }
.node-opt.email { background: #dcfce7; color: #16a34a; }
.node-opt.dependency { background: #fee2e2; color: #dc2626; }
.node-opt.data_quality { background: #e0f2fe; color: #0284c7; }
</style>