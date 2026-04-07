<template>
  <div class="page">
    <div class="toolbar card">
      <el-button type="primary" @click="openCreate">新建数据集</el-button>
    </div>
    <div class="card" style="margin-top: 16px">
      <el-table :data="rows" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="fetchMode" label="模式" width="100" />
        <el-table-column prop="dataSourceId" label="数据源 ID" width="110" />
        <el-table-column label="嵌入" min-width="200">
          <template #default="{ row }">
            <span class="mono token">{{ embedUrl(row.publicToken) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link @click="preview(row)">预览</el-button>
            <el-button link @click="regen(row)">换 token</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑数据集' : '新建数据集'" width="800px">
      <el-form label-width="110px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="模式">
          <el-radio-group v-model="form.fetchMode">
            <el-radio-button label="LIVE">实时数据</el-radio-button>
            <el-radio-button label="MOCK">Mock</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.fetchMode === 'LIVE'" label="数据源">
          <el-select v-model="form.dataSourceId" clearable placeholder="选择数据源" style="width: 100%">
            <el-option v-for="s in sources" :key="s.id" :label="`${s.id} · ${s.name} (${s.type})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="fetchSpec">
          <el-input v-model="form.fetchSpec" type="textarea" :rows="4" class="mono" />
          <div class="hint">
            JDBC：填 SELECT 语句。HTTP：填路径（会拼到数据源的 baseUrl）。Redis：填 key。Excel：可填工作表名，留空用第一张表。
          </div>
        </el-form-item>
        <el-form-item label="Mock JSON">
          <el-input v-model="form.mockJson" type="textarea" :rows="6" class="mono" />
          <div class="hint">MOCK 模式或联调占位时使用，例如：<code>[{"a":1}]</code></div>
        </el-form-item>
        <el-form-item label="脚本 (JS)">
          <el-input v-model="form.scriptText" type="textarea" :rows="8" class="mono" />
          <div class="hint">
            函数体：接收 <code>input</code>，须 <code>return</code> 结果。示例：
            <code>return input.filter(r => r.amount &gt; 0);</code>
          </div>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="预览结果" width="720px">
      <pre class="mono preview">{{ previewJson }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDataSets, saveDataSet, removeDataSet, previewDataSet, regenerateToken } from '../api/datasetApi'
import { listDataSources } from '../api/datasourceApi'

const rows = ref([])
const sources = ref([])
const visible = ref(false)
const previewVisible = ref(false)
const previewJson = ref('')

const form = reactive({
  id: null,
  name: '',
  dataSourceId: null,
  fetchMode: 'MOCK',
  fetchSpec: '',
  mockJson: '[\n  { "name": "系列A", "value": 120 },\n  { "name": "系列B", "value": 86 }\n]',
  scriptText: 'return input;',
  enabled: true
})

/** 大屏嵌入应请求后端地址（非 Vite 端口） */
const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN || 'http://127.0.0.1:8088'

function embedUrl(token) {
  return `${backendOrigin}/embed/data/${token}`
}

async function load() {
  rows.value = await listDataSets()
  sources.value = await listDataSources()
}

function openCreate() {
  form.id = null
  form.name = '未命名数据集'
  form.dataSourceId = null
  form.fetchMode = 'MOCK'
  form.fetchSpec = ''
  form.mockJson = form.mockJson
  form.scriptText = 'return input;'
  form.enabled = true
  visible.value = true
}

function openEdit(row) {
  form.id = row.id
  form.name = row.name
  form.dataSourceId = row.dataSourceId
  form.fetchMode = row.fetchMode
  form.fetchSpec = row.fetchSpec || ''
  form.mockJson = row.mockJson || '[]'
  form.scriptText = row.scriptText || 'return input;'
  form.enabled = row.enabled
  visible.value = true
}

async function submit() {
  await saveDataSet({
    id: form.id,
    name: form.name,
    dataSourceId: form.fetchMode === 'LIVE' ? form.dataSourceId : null,
    fetchMode: form.fetchMode,
    fetchSpec: form.fetchSpec,
    mockJson: form.mockJson,
    scriptText: form.scriptText,
    enabled: form.enabled
  })
  ElMessage.success('已保存')
  visible.value = false
  await load()
}

async function remove(row) {
  await ElMessageBox.confirm(`删除数据集「${row.name}」？`, '确认', { type: 'warning' })
  await removeDataSet(row.id)
  ElMessage.success('已删除')
  await load()
}

async function preview(row) {
  try {
    const data = await previewDataSet(row.id)
    previewJson.value = JSON.stringify(data, null, 2)
    previewVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '预览失败')
  }
}

async function regen(row) {
  await ElMessageBox.confirm('重新生成公开 token？旧链接将失效。', '确认', { type: 'warning' })
  await regenerateToken(row.id)
  ElMessage.success('已更新 token')
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.token {
  word-break: break-all;
  font-size: 11px;
  color: #9fb0ff;
}
.hint {
  margin-top: 6px;
  font-size: 12px;
  color: #8a93b8;
  line-height: 1.5;
}
.preview {
  max-height: 480px;
  overflow: auto;
  background: rgba(0, 0, 0, 0.35);
  padding: 12px;
  border-radius: 8px;
}
code {
  background: rgba(0, 0, 0, 0.35);
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
