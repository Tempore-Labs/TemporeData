<template>
  <div class="node-config-panel">
    <div class="panel-header">
      <h4>节点配置</h4>
      <el-button text :icon="Close" @click="$emit('close')" />
    </div>

    <el-form v-if="node" :model="form" label-width="80px" size="small" class="panel-form">
      <el-form-item label="节点名称">
        <el-input v-model="form.name" @change="emitChange" />
      </el-form-item>
      <el-form-item label="节点类型">
        <el-select v-model="form.type" @change="emitChange" style="width: 100%">
          <el-option-group label="数据处理">
            <el-option label="SQL" value="SQL" />
            <el-option label="Spark SQL" value="SPARK_SQL" />
            <el-option label="Shell" value="SHELL" />
            <el-option label="Python" value="PYTHON" />
          </el-option-group>
          <el-option-group label="数据集成">
            <el-option label="DataX" value="DATAX" />
          </el-option-group>
          <el-option-group label="流程控制">
            <el-option label="条件分支" value="CONDITION" />
            <el-option label="子工作流" value="SUB_WORKFLOW" />
            <el-option label="依赖检查" value="DEPENDENCY" />
          </el-option-group>
          <el-option-group label="外部服务">
            <el-option label="HTTP" value="HTTP" />
            <el-option label="邮件通知" value="EMAIL" />
          </el-option-group>
          <el-option-group label="数据质量">
            <el-option label="数据质量检查" value="DATA_QUALITY" />
          </el-option-group>
        </el-select>
      </el-form-item>
      <el-form-item label="数据源" v-if="needsDatasource">
        <el-select v-model="form.datasourceId" @change="emitChange" placeholder="选择数据源" style="width: 100%" clearable>
          <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
        </el-select>
      </el-form-item>

      <!-- SQL / Spark SQL / Shell / Python script -->
      <el-form-item label="脚本内容" v-if="needsScript">
        <el-input
          v-model="form.sql"
          type="textarea"
          :rows="6"
          :placeholder="scriptPlaceholder"
          @change="emitChange"
        />
      </el-form-item>

      <!-- Spark SQL config -->
      <el-form-item label="Spark配置" v-if="form.type === 'SPARK_SQL'">
        <el-input
          v-model="form.sparkConf"
          type="textarea"
          :rows="3"
          placeholder="spark.executor.memory=4g&#10;spark.executor.cores=2&#10;spark.executor.instances=3"
          @change="emitChange"
        />
      </el-form-item>

      <!-- DataX config -->
      <el-form-item label="DataX配置" v-if="form.type === 'DATAX'">
        <el-input
          v-model="form.sql"
          type="textarea"
          :rows="8"
          placeholder='{"job":{"content":[{"reader":{"name":"mysqlreader","parameter":{"username":"","password":"","connection":[{"table":[""],"jdbcUrl":[""]}]}},"writer":{"name":"hdfswriter","parameter":{"defaultFS":"hdfs://","path":"","fileName":"","column":[],"writeMode":"append"}}}],"setting":{"speed":{"channel":3}}}}'
          @change="emitChange"
        />
      </el-form-item>

      <!-- HTTP config -->
      <template v-if="form.type === 'HTTP'">
        <el-form-item label="请求URL">
          <el-input v-model="form.httpUrl" placeholder="https://api.example.com/endpoint" @change="emitChange" />
        </el-form-item>
        <el-form-item label="请求方法">
          <el-select v-model="form.httpMethod" @change="emitChange" style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求头">
          <el-input v-model="form.httpHeaders" type="textarea" :rows="2" placeholder='{"Content-Type":"application/json"}' @change="emitChange" />
        </el-form-item>
        <el-form-item label="请求体">
          <el-input v-model="form.sql" type="textarea" :rows="3" placeholder='{"key":"value"}' @change="emitChange" />
        </el-form-item>
      </template>

      <!-- Sub-workflow config -->
      <el-form-item label="子工作流ID" v-if="form.type === 'SUB_WORKFLOW'">
        <el-input v-model="form.sql" placeholder="输入目标工作流 ID" @change="emitChange" />
      </el-form-item>

      <!-- Condition config -->
      <el-form-item label="条件表达式" v-if="form.type === 'CONDITION'">
        <el-input v-model="form.sql" placeholder="例如: ${node_A.status} == 'SUCCESS'" @change="emitChange" />
        <span style="color:#9ca3af;font-size:11px">满足条件走 SUCCESS 边，不满足走 FAILURE 边</span>
      </el-form-item>

      <!-- Dependency config -->
      <template v-if="form.type === 'DEPENDENCY'">
        <el-form-item label="依赖类型">
          <el-select v-model="form.dependencyType" @change="emitChange" style="width: 100%">
            <el-option label="时间点" value="TIME_POINT" />
            <el-option label="文件到达" value="FILE_ARRIVAL" />
            <el-option label="外部任务" value="EXTERNAL_TASK" />
          </el-select>
        </el-form-item>
        <el-form-item label="依赖配置">
          <el-input v-model="form.sql" placeholder="时间点: 2026-01-01 08:00 或 文件路径: /data/signal.txt" @change="emitChange" />
        </el-form-item>
        <el-form-item label="超时时间">
          <el-input-number v-model="form.dependencyTimeout" :min="0" :max="86400" @change="emitChange" style="width: 100%">
            <template #suffix>秒</template>
          </el-input-number>
          <span style="color:#9ca3af;font-size:11px">0 表示无限等待</span>
        </el-form-item>
      </template>

      <!-- Email config -->
      <template v-if="form.type === 'EMAIL'">
        <el-form-item label="收件人">
          <el-input v-model="form.emailTo" placeholder="user@example.com" @change="emitChange" />
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="form.emailSubject" placeholder="邮件主题" @change="emitChange" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.sql" type="textarea" :rows="3" placeholder="邮件正文内容" @change="emitChange" />
        </el-form-item>
      </template>

      <!-- Data Quality config -->
      <template v-if="form.type === 'DATA_QUALITY'">
        <el-form-item label="数据源">
          <el-select v-model="form.datasourceId" @change="emitChange" placeholder="选择数据源" style="width: 100%" clearable>
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查类型">
          <el-select v-model="form.qualityType" @change="emitChange" style="width: 100%">
            <el-option label="行数校验" value="ROW_COUNT" />
            <el-option label="空值检查" value="NULL_CHECK" />
            <el-option label="重复值检查" value="DUPLICATE" />
            <el-option label="自定义SQL" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查规则">
          <el-input v-model="form.sql" type="textarea" :rows="3" :placeholder="qualityPlaceholder" @change="emitChange" />
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="form.qualityThreshold" :min="0" :max="100" @change="emitChange" style="width: 100%">
            <template #suffix>%</template>
          </el-input-number>
          <span style="color:#9ca3af;font-size:11px">达到阈值视为通过</span>
        </el-form-item>
      </template>

      <el-divider content-position="left">执行策略</el-divider>

      <el-form-item label="优先级">
        <el-select v-model="form.priority" @change="emitChange" style="width: 100%">
          <el-option label="最高" value="HIGHEST" />
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
          <el-option label="最低" value="LOWEST" />
        </el-select>
      </el-form-item>

      <el-form-item label="失败策略">
        <el-select v-model="form.failStrategy" @change="emitChange" style="width: 100%">
          <el-option label="停止执行" value="STOP">
            <span>停止执行</span>
            <span style="color:#9ca3af;font-size:11px;margin-left:8px">失败后停止整个工作流</span>
          </el-option>
          <el-option label="继续执行" value="CONTINUE">
            <span>继续执行</span>
            <span style="color:#9ca3af;font-size:11px;margin-left:8px">失败后继续执行后续节点</span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="重试次数">
        <el-input-number v-model="form.retryCount" :min="0" :max="10" @change="emitChange" style="width: 100%" />
      </el-form-item>

      <el-form-item label="重试间隔">
        <el-input-number v-model="form.retryInterval" :min="1" :max="600" @change="emitChange" style="width: 100%">
          <template #suffix>秒</template>
        </el-input-number>
      </el-form-item>

      <el-form-item label="超时时间">
        <el-input-number v-model="form.timeoutSeconds" :min="0" :max="86400" @change="emitChange" style="width: 100%">
          <template #suffix>秒</template>
        </el-input-number>
        <span style="color:#9ca3af;font-size:11px">0 表示不限制</span>
      </el-form-item>

      <el-divider content-position="left">位置</el-divider>

      <el-form-item label="X 坐标">
        <el-input-number v-model="form.positionX" :step="10" @change="emitChange" style="width: 100%" />
      </el-form-item>
      <el-form-item label="Y 坐标">
        <el-input-number v-model="form.positionY" :step="10" @change="emitChange" style="width: 100%" />
      </el-form-item>

      <el-divider />

      <el-button type="danger" plain size="small" @click="$emit('delete')" style="width: 100%">
        <el-icon><Delete /></el-icon> 删除节点
      </el-button>
    </el-form>

    <div v-else class="panel-empty">
      <el-icon :size="32" color="#d1d5db"><InfoFilled /></el-icon>
      <p>点击画布中的节点以编辑</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, watch, computed } from 'vue'
import { Close, Delete, InfoFilled } from '@element-plus/icons-vue'

const props = defineProps({
  node: { type: Object, default: null },
  datasources: { type: Array, default: () => [] }
})

const emit = defineEmits(['update', 'close', 'delete'])

const form = reactive({
  name: '',
  type: 'SQL',
  datasourceId: '',
  sql: '',
  priority: 'MEDIUM',
  failStrategy: 'STOP',
  retryCount: 0,
  retryInterval: 1,
  timeoutSeconds: 0,
  positionX: 0,
  positionY: 0,
  // New node type fields
  sparkConf: '',
  httpUrl: '',
  httpMethod: 'GET',
  httpHeaders: '',
  dependencyType: 'TIME_POINT',
  dependencyTimeout: 0,
  emailTo: '',
  emailSubject: '',
  qualityType: 'ROW_COUNT',
  qualityThreshold: 0
})

// Computed: show datasource for SQL, Spark SQL, Data Quality
const needsDatasource = computed(() => {
  return ['SQL', 'SPARK_SQL', 'DATA_QUALITY'].includes(form.type)
})

// Computed: show script for SQL, Spark SQL, Shell, Python
const needsScript = computed(() => {
  return ['SQL', 'SPARK_SQL', 'SHELL', 'PYTHON'].includes(form.type)
})

const scriptPlaceholder = computed(() => {
  const map = {
    SQL: '输入 SQL 语句',
    SPARK_SQL: '输入 Spark SQL 语句',
    SHELL: '输入 Shell 脚本',
    PYTHON: '输入 Python 脚本'
  }
  return map[form.type] || '输入脚本内容'
})

const qualityPlaceholder = computed(() => {
  const map = {
    ROW_COUNT: 'SQL: SELECT COUNT(*) FROM table_name',
    NULL_CHECK: 'SQL: SELECT COUNT(*) FROM table_name WHERE col IS NULL',
    DUPLICATE: 'SQL: SELECT col, COUNT(*) FROM table_name GROUP BY col HAVING COUNT(*) > 1',
    CUSTOM: '输入自定义 SQL 检查规则'
  }
  return map[form.qualityType] || '输入检查规则'
})

watch(() => props.node, (n) => {
  if (n) {
    form.name = n.data?.label || n.label || ''
    form.type = n.data?.type || 'SQL'
    form.datasourceId = n.data?.datasourceId || ''
    form.sql = n.data?.sql || ''
    form.priority = n.data?.priority || 'MEDIUM'
    form.failStrategy = n.data?.failStrategy || 'STOP'
    form.retryCount = n.data?.retryCount ?? 0
    form.retryInterval = n.data?.retryInterval ?? 1
    form.timeoutSeconds = n.data?.timeoutSeconds ?? 0
    form.positionX = n.position?.x || n.data?.positionX || 0
    form.positionY = n.position?.y || n.data?.positionY || 0
    // New fields
    form.sparkConf = n.data?.sparkConf || ''
    form.httpUrl = n.data?.httpUrl || ''
    form.httpMethod = n.data?.httpMethod || 'GET'
    form.httpHeaders = n.data?.httpHeaders || ''
    form.dependencyType = n.data?.dependencyType || 'TIME_POINT'
    form.dependencyTimeout = n.data?.dependencyTimeout ?? 0
    form.emailTo = n.data?.emailTo || ''
    form.emailSubject = n.data?.emailSubject || ''
    form.qualityType = n.data?.qualityType || 'ROW_COUNT'
    form.qualityThreshold = n.data?.qualityThreshold ?? 0
  }
}, { immediate: true })

function emitChange() {
  emit('update', {
    name: form.name,
    type: form.type,
    datasourceId: form.datasourceId,
    sql: form.sql,
    priority: form.priority,
    failStrategy: form.failStrategy,
    retryCount: form.retryCount,
    retryInterval: form.retryInterval,
    timeoutSeconds: form.timeoutSeconds,
    positionX: form.positionX,
    positionY: form.positionY,
    // New fields
    sparkConf: form.sparkConf,
    httpUrl: form.httpUrl,
    httpMethod: form.httpMethod,
    httpHeaders: form.httpHeaders,
    dependencyType: form.dependencyType,
    dependencyTimeout: form.dependencyTimeout,
    emailTo: form.emailTo,
    emailSubject: form.emailSubject,
    qualityType: form.qualityType,
    qualityThreshold: form.qualityThreshold
  })
}
</script>

<style scoped>
.node-config-panel {
  width: 300px;
  border-left: 1px solid #e5e7eb;
  background: #fff;
  display: flex;
  flex-direction: column;
  height: 100%;
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
.panel-form {
  padding: 16px;
  flex: 1;
}
.panel-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  gap: 8px;
  font-size: 13px;
  padding: 20px;
  text-align: center;
}
</style>