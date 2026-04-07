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

    <el-dialog v-model="visible" :title="form.id ? '编辑数据集' : '新建数据集'" width="880px">
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
          <el-select
            v-model="form.dataSourceId"
            clearable
            placeholder="选择数据源"
            style="width: 100%"
            @change="onDataSourceChange"
          >
            <el-option v-for="s in sources" :key="s.id" :label="`${s.id} · ${s.name} (${s.type})`" :value="s.id" />
          </el-select>
        </el-form-item>

        <template v-if="form.fetchMode === 'LIVE' && showHttpFetchForm">
          <el-form-item label="路径">
            <el-input v-model="form.httpPath" placeholder="例如 /users 或 /api/v1/query" />
          </el-form-item>
          <el-form-item label="Method">
            <el-select v-model="form.httpMethod" style="width: 100%">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
            </el-select>
          </el-form-item>
          <el-form-item label="Query 参数">
            <el-input v-model="form.httpParamsText" type="textarea" :rows="4" class="mono" placeholder='JSON 对象，如 {"id":"1"}' />
          </el-form-item>
          <el-form-item label="Headers">
            <el-input v-model="form.httpHeadersText" type="textarea" :rows="3" class="mono" placeholder='JSON 对象，如 {"Authorization":"Bearer ..."}' />
          </el-form-item>
          <el-form-item label="Body">
            <el-input v-model="form.httpBodyText" type="textarea" :rows="4" class="mono" placeholder="仅 POST 时作为 JSON 请求体发送；留空则不传 body" />
          </el-form-item>
          <el-form-item label="fetchSpec 预览">
            <el-input :model-value="previewHttpFetchSpec" type="textarea" :rows="5" class="mono" readonly />
            <div class="hint">保存时会自动生成并写入 fetchSpec，无需手写 JSON。</div>
          </el-form-item>
        </template>

        <el-form-item v-else label="fetchSpec">
          <el-input v-model="form.fetchSpec" type="textarea" :rows="4" class="mono" />
          <div class="hint">
            <template v-if="form.fetchMode !== 'LIVE'">实时模式外可留空；Mock 模式主要用下方 Mock JSON。</template>
            <template v-else-if="!form.dataSourceId">请先选择数据源。</template>
            <template v-else>
              JDBC：填 SELECT 语句。Redis：填 key。Excel：工作表名，留空用第一张表。
            </template>
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
import { computed, onMounted, reactive, ref } from 'vue'
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
  httpPath: '/',
  httpMethod: 'GET',
  httpParamsText: '{}',
  httpHeadersText: '{}',
  httpBodyText: '',
  mockJson: '[\n  { "name": "系列A", "value": 120 },\n  { "name": "系列B", "value": 86 }\n]',
  scriptText: 'return input;',
  enabled: true
})

const selectedSource = computed(() => sources.value.find((s) => s.id === form.dataSourceId) || null)
const selectedSourceType = computed(() => selectedSource.value?.type ?? null)
const showHttpFetchForm = computed(() => form.fetchMode === 'LIVE' && selectedSourceType.value === 'HTTP_API')

function resetHttpFormDefault() {
  form.httpPath = '/'
  form.httpMethod = 'GET'
  form.httpParamsText = '{}'
  form.httpHeadersText = '{}'
  form.httpBodyText = ''
}

/** 将已保存的 fetchSpec 解析到 HTTP 表单（路径或 JSON）。 */
function parseFetchSpecToHttpForm(fetchSpec) {
  const raw = (fetchSpec || '').trim()
  if (!raw) {
    resetHttpFormDefault()
    return
  }
  if (raw.startsWith('{')) {
    try {
      const o = JSON.parse(raw)
      if (o && typeof o === 'object' && !Array.isArray(o)) {
        form.httpPath = o.path != null ? String(o.path) : '/'
        form.httpMethod = String(o.method || 'GET').trim().toUpperCase() === 'POST' ? 'POST' : 'GET'
        form.httpParamsText = JSON.stringify(
          o.params && typeof o.params === 'object' && !Array.isArray(o.params) ? o.params : {},
          null,
          2
        )
        form.httpHeadersText = JSON.stringify(
          o.headers && typeof o.headers === 'object' && !Array.isArray(o.headers) ? o.headers : {},
          null,
          2
        )
        if (o.body !== undefined && o.body !== null) {
          form.httpBodyText = JSON.stringify(o.body, null, 2)
        } else {
          form.httpBodyText = ''
        }
        return
      }
    } catch {
      /* fall through */
    }
  }
  form.httpPath = raw || '/'
  form.httpMethod = 'GET'
  form.httpParamsText = '{}'
  form.httpHeadersText = '{}'
  form.httpBodyText = ''
}

/** 从 HTTP 表单生成保存用的 fetchSpec JSON 字符串。 */
function buildHttpFetchSpec() {
  let params = {}
  let headers = {}
  try {
    params = form.httpParamsText?.trim() ? JSON.parse(form.httpParamsText) : {}
  } catch {
    throw new Error('Query 参数必须是合法 JSON 对象')
  }
  try {
    headers = form.httpHeadersText?.trim() ? JSON.parse(form.httpHeadersText) : {}
  } catch {
    throw new Error('Headers 必须是合法 JSON 对象')
  }
  if (typeof params !== 'object' || params === null || Array.isArray(params)) {
    throw new Error('Query 参数必须是 JSON 对象')
  }
  if (typeof headers !== 'object' || headers === null || Array.isArray(headers)) {
    throw new Error('Headers 必须是 JSON 对象')
  }

  let body
  if (form.httpBodyText && form.httpBodyText.trim()) {
    try {
      body = JSON.parse(form.httpBodyText)
    } catch {
      throw new Error('Body 必须是合法 JSON')
    }
  }

  const method = String(form.httpMethod || 'GET').trim().toUpperCase()
  if (method !== 'GET' && method !== 'POST') {
    throw new Error('Method 仅支持 GET / POST')
  }

  const out = {
    path: String(form.httpPath || '').trim() || '/',
    method
  }
  if (Object.keys(params).length) out.params = params
  if (Object.keys(headers).length) out.headers = headers
  if (method === 'POST' && body !== undefined) out.body = body

  return JSON.stringify(out)
}

const previewHttpFetchSpec = computed(() => {
  if (!showHttpFetchForm.value) return ''
  try {
    return JSON.stringify(JSON.parse(buildHttpFetchSpec()), null, 2)
  } catch (e) {
    return `填写有误: ${e.message}`
  }
})

function onDataSourceChange() {
  if (selectedSourceType.value !== 'HTTP_API') return
  const raw = (form.fetchSpec || '').trim()
  if (raw && !raw.startsWith('{') && /\s/.test(raw)) {
    resetHttpFormDefault()
    form.fetchSpec = ''
    ElMessage.info('已切换为 HTTP 数据源，请填写路径与参数')
    return
  }
  parseFetchSpecToHttpForm(form.fetchSpec)
}

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
  resetHttpFormDefault()
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
  const src = sources.value.find((s) => s.id === row.dataSourceId)
  if (src?.type === 'HTTP_API') {
    parseFetchSpecToHttpForm(row.fetchSpec || '')
  } else {
    resetHttpFormDefault()
  }
  form.mockJson = row.mockJson || '[]'
  form.scriptText = row.scriptText || 'return input;'
  form.enabled = row.enabled
  visible.value = true
}

async function submit() {
  let fetchSpec = form.fetchSpec
  if (form.fetchMode === 'LIVE' && selectedSourceType.value === 'HTTP_API') {
    try {
      fetchSpec = buildHttpFetchSpec()
    } catch (e) {
      ElMessage.error(e.message || 'HTTP 取数配置有误')
      return
    }
  }
  await saveDataSet({
    id: form.id,
    name: form.name,
    dataSourceId: form.fetchMode === 'LIVE' ? form.dataSourceId : null,
    fetchMode: form.fetchMode,
    fetchSpec,
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
