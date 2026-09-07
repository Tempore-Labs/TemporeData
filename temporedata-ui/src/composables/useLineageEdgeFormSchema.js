/**
 * Shared schema for the three write dialogs (B2 schema normalisation).
 *
 * Exports a single declarative `edgeFormMeta` field list (8 fields) and the
 * derived `edgeFormRules` validation object. All three write dialogs
 * (new-edge addEdge / edit-edge editEdge / asset-library assetEdge) reuse these
 * two exports so validation and field definitions stay in one place (§5.3).
 *
 * Field `type` values drive the shared LgEdgeFormFields renderer:
 *   - assetSearch : remote asset search (source / target)
 *   - enum        : fixed-value select (relation type / priority)
 *   - input       : single-line text input
 *   - textarea    : multi-line textarea (description / sql)
 *   - taggable    : multiple allow-create tag select
 */

export const RELATION_TYPES = ['FLOWS_TO', 'DERIVED_FROM', 'PRODUCES', 'CONSUMES']

// Label lookup for enum fields, shared by the label function passed to renderers.
export const EDGE_ENUM_LABELS = {
  FLOWS_TO: '数据流转',
  DERIVED_FROM: '衍生',
  PRODUCES: '产出',
  CONSUMES: '消费'
}

/**
 * Edge form field metadata (8 fields, order preserved for rendering).
 */
export const edgeFormMeta = [
  { key: 'sourceId', label: '源节点', type: 'assetSearch', required: true, placeholder: '搜索源节点', },
  { key: 'targetId', label: '目标节点', type: 'assetSearch', required: true, placeholder: '搜索目标节点' },
  { key: 'relationType', label: '关系类型', type: 'enum', required: true, enum: RELATION_TYPES, default: 'FLOWS_TO' },
  { key: 'description', label: '说明', type: 'textarea', required: true, min: 8, rows: 2, placeholder: '描述性说明（至少 8 个字符）' },
  { key: 'sql', label: 'SQL 片段', type: 'textarea', rows: 3, placeholder: '产生该血缘的转换 SQL（可选）' },
  { key: 'taskName', label: '关联任务', type: 'input', placeholder: '任务名称（可选）' },
  { key: 'priority', label: '优先级', type: 'enum', enum: ['P0', 'P1', 'P2'], default: 'P1' },
  { key: 'customTags', label: '自定义标签', type: 'taggable', max: 8 }
]

// Cross-field equality check needs the live form; LineageV2 wires it via setEdgeForm
// right before validating. It is an optimisation of the derived rules.
let activeForm = null

/**
 * Attach the current form object so the sourceId !== targetId validator can
 * compare across fields. Call before validating any write dialog.
 *
 * @param {object} form - The form model being validated.
 */
export function setEdgeForm(form) {
  activeForm = form
}

function notSameSource(value, callback) {
  if (activeForm && value && activeForm.targetId === value) callback(new Error('源与目标节点不能相同'))
  else callback()
}
function notSameTarget(value, callback) {
  if (activeForm && value && activeForm.sourceId === value) callback(new Error('源与目标节点不能相同'))
  else callback()
}

/**
 * Validation rules derived from edgeFormMeta (required / min-length / cross-field).
 */
export const edgeFormRules = {
  sourceId: [
    { required: true, message: '请选择源节点', trigger: 'change' },
    { validator: (_r, v, cb) => notSameSource(v, cb), trigger: 'change' }
  ],
  targetId: [
    { required: true, message: '请选择目标节点', trigger: 'change' },
    { validator: (_r, v, cb) => notSameTarget(v, cb), trigger: 'change' }
  ],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }],
  description: [
    { required: true, message: '请填写说明', trigger: 'blur' },
    { min: 8, message: '说明至少 8 个字符', trigger: 'blur' }
  ],
  customTags: [{ type: 'array', max: 8, message: '最多 8 个标签', trigger: 'change' }]
}

/**
 * Build a fresh, empty edge form reflecting the schema defaults.
 *
 * @returns {object} Form model.
 */
export function emptyEdgeForm() {
  const out = {}
  for (const f of edgeFormMeta) {
    out[f.key] = f.type === 'taggable' ? [] : f.default || ''
  }
  // Ensure taggable arrays never start as a primitive string.
  out.customTags = Array.isArray(out.customTags) ? out.customTags : []
  return out
}

/**
 * Pick the subset of a form that is sent to the backend as edge attributes.
 * Source / target are passed as URL path parameters, never in the body.
 *
 * @param {object} form - Live form model.
 * @returns {object} Edge attributes payload.
 */
export function edgeAttrsFrom(form) {
  return {
    relationType: form.relationType,
    description: form.description,
    sql: form.sql,
    taskName: form.taskName,
    priority: form.priority,
    customTags: Array.isArray(form.customTags) ? form.customTags : []
  }
}