<template>
  <div class="calendar-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">自定义日历</h2>
        <p class="page-desc">定义业务日历（工作日/节假日）与切日时间，用于调度业务日期计算</p>
      </div>
      <div class="page-header-actions">
        <el-button :icon="Refresh" @click="fetchCalendars">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建日历</el-button>
      </div>
    </div>

    <div class="calendar-layout">
      <!-- Calendar list -->
      <div class="cal-list" v-loading="loading">
        <div
          v-for="c in calendars"
          :key="c.id"
          class="cal-item"
          :class="{ active: current?.id === c.id }"
          @click="select(c)"
        >
          <div class="cal-name">{{ c.name }}</div>
          <div class="cal-meta">{{ c.workdayMode }} · 切日 {{ cutLabel(c) }}</div>
        </div>
        <el-empty v-if="!calendars.length && !loading" description="暂无日历" :image-size="60" />
      </div>

      <!-- Detail -->
      <div class="cal-detail" v-if="current">
        <el-form label-width="110px">
          <el-form-item label="名称">
            <el-input v-model="form.name" style="max-width: 340px" />
          </el-form-item>
          <el-form-item label="工作日模式">
            <el-select v-model="form.workdayMode" style="max-width: 220px">
              <el-option label="自定义 (CUSTOM)" value="CUSTOM" />
              <el-option label="每周固定 (WEEK)" value="WEEK" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.workdayMode === 'WEEK'" label="工作日(1-7)">
            <el-input v-model="form.weekWorkdays" placeholder="1,2,3,4,5" style="max-width: 200px" />
          </el-form-item>
          <el-form-item label="切日时间">
            <el-time-picker
              v-model="cutTime"
              format="HH:mm"
              value-format="HH:mm"
              placeholder="00:00"
              style="max-width: 160px"
            />
            <el-button style="margin-left: 8px" size="small" @click="saveCut">保存切日</el-button>
          </el-form-item>
          <el-form-item label="时区">
            <el-input v-model="form.timezone" style="max-width: 220px" placeholder="Asia/Shanghai" />
          </el-form-item>
          <el-form-item label="操作">
            <el-button type="primary" @click="saveCalendar">保存日历</el-button>
            <el-button type="danger" @click="deleteCalendar">删除</el-button>
          </el-form-item>
        </el-form>

        <el-divider content-position="left">日期预览 / 编辑（切换日 = 设为节假日）</el-divider>
        <div class="preview-toolbar">
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            start-placeholder="开始"
            end-placeholder="结束"
          />
          <el-button :icon="Search" @click="loadPreview">预览</el-button>
          <el-button type="warning" plain size="small" @click="applyToHoliday">将当前范围设为节假日</el-button>
        </div>
        <div class="day-grid" v-loading="previewLoading">
          <div
            v-for="d in previewDays"
            :key="d.date"
            class="day-cell"
            :class="{ weekend: !d.workday }"
            :title="d.remark"
          >
            <span class="day-date">{{ d.date }}</span>
            <el-tag :type="d.workday ? 'success' : 'danger'" size="small" effect="plain">
              {{ d.workday ? '工作日' : '休' }}
            </el-tag>
          </div>
        </div>
      </div>
      <div v-else class="cal-empty">选择左侧日历查看详情</div>
    </div>

    <!-- Create dialog -->
    <el-dialog v-model="createVisible" title="新建日历" width="460px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="例如：交易日历" />
        </el-form-item>
        <el-form-item label="工作日模式">
          <el-select v-model="createForm.workdayMode" style="width: 100%">
            <el-option label="自定义 (CUSTOM)" value="CUSTOM" />
            <el-option label="每周固定 (WEEK)" value="WEEK" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.workdayMode === 'WEEK'" label="工作日列表">
          <el-input v-model="createForm.weekWorkdays" placeholder="1,2,3,4,5" />
        </el-form-item>
        <el-form-item label="切日时间">
          <el-time-picker v-model="createForm.cutTime" format="HH:mm" value-format="HH:mm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Refresh, Search, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { calendarApi } from '@/api/modules/calendar'

const calendars = ref([])
const loading = ref(false)
const current = ref(null)
const form = ref({})
const cutTime = ref('')
const range = ref([])
const previewDays = ref([])
const previewLoading = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const createForm = ref({ name: '', workdayMode: 'CUSTOM', weekWorkdays: '1,2,3,4,5', cutTime: '00:00' })

function cutLabel(c) {
  const h = c.cutHour ?? 0
  const m = c.cutMinute ?? 0
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

async function fetchCalendars() {
  loading.value = true
  try {
    calendars.value = await calendarApi.list() || []
    if (current.value) {
      current.value = calendars.value.find(c => c.id === current.value.id) || null
      if (current.value) form.value = current.value
    }
  } catch {
    ElMessage.error('加载日历失败')
  } finally { loading.value = false }
}

function select(c) {
  current.value = c
  form.value = { ...c }
  cutTime.value = cutLabel(c)
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth(), 1)
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  range.value = [fmt(from), fmt(to)]
  loadPreview()
}

function fmt(d) {
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

async function loadPreview() {
  if (!current.value || !range.value?.length) return
  previewLoading.value = true
  try {
    previewDays.value = await calendarApi.preview(current.value.id, range.value[0], range.value[1]) || []
  } catch {
    ElMessage.error('预览失败')
  } finally { previewLoading.value = false }
}

async function saveLimitPayload() {
  return {
    name: form.value.name,
    description: form.value.description,
    workdayMode: form.value.workdayMode,
    timezone: form.value.timezone,
    weekWorkdays: form.value.weekWorkdays
  }
}

async function saveCalendar() {
  try {
    await calendarApi.update(current.value.id, await saveLimitPayload())
    ElMessage.success('日历已保存')
    await fetchCalendars()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function saveCut() {
  const [h, m] = (cutTime.value || '00:00').split(':').map(Number)
  try {
    await calendarApi.setCut(current.value.id, h, m)
    ElMessage.success('切日已保存')
    current.value.cutHour = h
    current.value.cutMinute = m
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function applyToHoliday() {
  if (!range.value?.length) return
  try {
    await ElMessageBox.confirm(`将 ${range.value[0]} ~ ${range.value[1]} 全部设为节假日？`, '设为节假日', { type: 'warning' })
  } catch {
    return
  }
  const days = []
  let d = new Date(range.value[0])
  const end = new Date(range.value[1])
  while (d <= end) {
    days.push({ date: fmt(d), workday: false, remark: '' })
    d.setDate(d.getDate() + 1)
  }
  try {
    await calendarApi.setDays(current.value.id, days)
    ElMessage.success('已设为节假日')
    await loadPreview()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function deleteCalendar() {
  try {
    await ElMessageBox.confirm(`删除日历「${current.value.name}」？`, '删除', { type: 'warning' })
    await calendarApi.delete(current.value.id)
    current.value = null
    ElMessage.success('已删除')
    await fetchCalendars()
  } catch (e) {
    /* cancelled */
  }
}

function openCreate() {
  createForm.value = { name: '', workdayMode: 'CUSTOM', weekWorkdays: '1,2,3,4,5', cutTime: '00:00' }
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.value.name) {
    ElMessage.warning('请输入名称')
    return
  }
  creating.value = true
  try {
    current.value = await calendarApi.create({
      name: createForm.value.name,
      workdayMode: createForm.value.workdayMode,
      weekWorkdays: createForm.value.weekWorkdays
    })
    const [h, m] = (createForm.value.cutTime || '00:00').split(':').map(Number)
    await calendarApi.setCut(current.value.id, h, m)
    createVisible.value = false
    ElMessage.success('日历已创建')
    await fetchCalendars()
    select(calendars.value.find(c => c.id === current.value.id))
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  } finally { creating.value = false }
}

onMounted(async () => {
  await fetchCalendars()
})
</script>

<style scoped>
.calendar-page {
  max-width: 1280px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}
.page-desc {
  font-size: 13px;
  color: #9ca3af;
  margin: 4px 0 0;
}
.page-header-actions {
  display: flex;
  gap: 8px;
}
.calendar-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.cal-list {
  width: 240px;
  min-width: 240px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 8px;
  max-height: 70vh;
  overflow-y: auto;
}
.cal-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
}
.cal-item:hover { background: #f3f4f6; }
.cal-item.active { background: #eff6ff; border: 1px solid #bfdbfe; }
.cal-name { font-size: 13px; font-weight: 600; color: #1f2937; }
.cal-meta { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.cal-detail {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 20px;
}
.cal-empty {
  flex: 1;
  text-align: center;
  color: #9ca3af;
  padding: 80px 0;
  background: #fff;
  border-radius: 8px;
}
.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.day-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
  gap: 8px;
}
.day-cell {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.day-cell.weekend {
  background: #fef2f2;
  border-color: #fecaca;
}
.day-date {
  font-size: 12px;
  color: #374151;
}
</style>