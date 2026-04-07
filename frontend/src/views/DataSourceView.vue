<template>
  <div class="page">
    <div class="toolbar card">
      <el-button type="primary" @click="openCreate">新建数据源</el-button>
    </div>
    <div class="card" style="margin-top: 16px">
      <el-table :data="rows" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link @click="testRow(row)">测试</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑数据源' : '新建数据源'" width="760px">
      <el-form label-width="110px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：生产 MySQL" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
            <el-option label="HTTP API" value="HTTP_API" />
            <el-option label="Redis" value="REDIS" />
            <el-option label="Excel 文件" value="EXCEL" />
            <el-option label="Mock" value="MOCK" />
          </el-select>
        </el-form-item>

        <template v-if="form.type === 'MYSQL' || form.type === 'POSTGRESQL'">
          <el-form-item label="Host"><el-input v-model="form.config.host" /></el-form-item>
          <el-form-item label="Port"><el-input-number v-model="form.config.port" :min="1" :max="65535" /></el-form-item>
          <el-form-item label="Database"><el-input v-model="form.config.database" /></el-form-item>
          <el-form-item label="Username"><el-input v-model="form.config.username" /></el-form-item>
          <el-form-item label="Password"><el-input v-model="form.config.password" show-password /></el-form-item>
        </template>

        <template v-else-if="form.type === 'HTTP_API'">
          <el-form-item label="Base URL"><el-input v-model="form.config.baseUrl" placeholder="https://api.example.com" /></el-form-item>
          <el-form-item label="Test Path"><el-input v-model="form.config.testPath" placeholder="/health" /></el-form-item>
          <el-form-item label="Headers(JSON)">
            <el-input v-model="form.config.headersText" type="textarea" :rows="4" class="mono" />
          </el-form-item>
        </template>

        <template v-else-if="form.type === 'REDIS'">
          <el-form-item label="Host"><el-input v-model="form.config.host" /></el-form-item>
          <el-form-item label="Port"><el-input-number v-model="form.config.port" :min="1" :max="65535" /></el-form-item>
          <el-form-item label="Password"><el-input v-model="form.config.password" show-password /></el-form-item>
          <el-form-item label="Database"><el-input-number v-model="form.config.database" :min="0" :max="64" /></el-form-item>
        </template>

        <template v-else-if="form.type === 'EXCEL'">
          <el-form-item label="上传 Excel">
            <el-upload :auto-upload="false" :show-file-list="true" :on-change="onFile">
              <el-button>选择 Excel</el-button>
            </el-upload>
            <div class="hint">上传后自动回填 fileId。</div>
          </el-form-item>
          <el-form-item label="fileId"><el-input v-model="form.config.fileId" /></el-form-item>
        </template>

        <template v-else-if="form.type === 'MOCK'">
          <el-form-item label="Mock(JSON)">
            <el-input v-model="form.config.mockText" type="textarea" :rows="8" class="mono" />
          </el-form-item>
        </template>

        <el-form-item label="配置预览(JSON)">
          <el-input :model-value="previewConfigJson" type="textarea" :rows="8" class="mono" readonly />
          <div class="hint">已禁用自由编辑，仅可通过上方字段填写，后端也会拒绝未知字段。</div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button @click="testDraft">测试连接</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDataSources, saveDataSource, removeDataSource } from '../api/datasourceApi'
import { testConnection } from '../api/connectionApi'
import { uploadExcel } from '../api/fileApi'

const rows = ref([])
const visible = ref(false)
const form = reactive({
  id: null,
  name: '',
  type: 'MYSQL',
  config: defaultConfigObject('MYSQL'),
  remark: ''
})
const suppressTypeWatch = ref(false)

function defaultConfigObject(type) {
  switch (type) {
    case 'MYSQL':
      return { host: '127.0.0.1', port: 3306, database: 'demo', username: 'root', password: 'secret' }
    case 'POSTGRESQL':
      return { host: '127.0.0.1', port: 5432, database: 'postgres', username: 'postgres', password: 'secret' }
    case 'HTTP_API':
      return { baseUrl: 'https://httpbin.org', testPath: '/get', headersText: '{}' }
    case 'REDIS':
      return { host: '127.0.0.1', port: 6379, password: '', database: 0 }
    case 'EXCEL':
      return { fileId: '' }
    case 'MOCK':
      return { mockText: '[{"x":1,"y":2}]' }
    default:
      return { host: '127.0.0.1', port: 3306, database: 'demo', username: 'root', password: 'secret' }
  }
}

function buildConfigObject() {
  const t = form.type
  if (t === 'MYSQL' || t === 'POSTGRESQL') {
    return {
      host: String(form.config.host || '').trim(),
      port: Number(form.config.port || 0),
      database: String(form.config.database || '').trim(),
      username: String(form.config.username || '').trim(),
      password: String(form.config.password || '')
    }
  }
  if (t === 'HTTP_API') {
    let headers = {}
    try {
      headers = form.config.headersText ? JSON.parse(form.config.headersText) : {}
    } catch {
      throw new Error('Headers 必须是合法 JSON 对象')
    }
    if (typeof headers !== 'object' || headers === null || Array.isArray(headers)) {
      throw new Error('Headers 必须是 JSON 对象')
    }
    return {
      baseUrl: String(form.config.baseUrl || '').trim(),
      testPath: String(form.config.testPath || '').trim() || '/',
      headers
    }
  }
  if (t === 'REDIS') {
    return {
      host: String(form.config.host || '').trim(),
      port: Number(form.config.port || 0),
      password: String(form.config.password || ''),
      database: Number(form.config.database || 0)
    }
  }
  if (t === 'EXCEL') {
    return { fileId: String(form.config.fileId || '').trim() }
  }
  if (t === 'MOCK') {
    let mock
    try {
      mock = JSON.parse(form.config.mockText || '[]')
    } catch {
      throw new Error('Mock 必须是合法 JSON')
    }
    return { mock }
  }
  return {}
}

const previewConfigJson = computed(() => {
  try {
    return JSON.stringify(buildConfigObject(), null, 2)
  } catch (e) {
    return `配置错误: ${e.message}`
  }
})

function applyRowConfig(type, configJson) {
  const parsed = (() => {
    try {
      return JSON.parse(configJson || '{}')
    } catch {
      return null
    }
  })()
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    form.config = defaultConfigObject(type)
    ElMessage.warning('历史配置 JSON 无法解析，已按当前类型重置模板')
    return
  }

  if (type === 'MYSQL' || type === 'POSTGRESQL') {
    form.config = {
      host: parsed.host ?? '',
      port: Number(parsed.port ?? (type === 'MYSQL' ? 3306 : 5432)),
      database: parsed.database ?? '',
      username: parsed.username ?? '',
      password: parsed.password ?? ''
    }
  } else if (type === 'HTTP_API') {
    form.config = {
      baseUrl: parsed.baseUrl ?? '',
      testPath: parsed.testPath ?? '/',
      headersText: JSON.stringify(parsed.headers ?? {}, null, 2)
    }
  } else if (type === 'REDIS') {
    form.config = {
      host: parsed.host ?? '',
      port: Number(parsed.port ?? 6379),
      password: parsed.password ?? '',
      database: Number(parsed.database ?? 0)
    }
  } else if (type === 'EXCEL') {
    form.config = { fileId: parsed.fileId ?? '' }
  } else if (type === 'MOCK') {
    form.config = { mockText: JSON.stringify(parsed.mock ?? [], null, 2) }
  }
}

async function load() {
  rows.value = await listDataSources()
}

function openCreate() {
  suppressTypeWatch.value = true
  form.id = null
  form.name = ''
  form.type = 'MYSQL'
  form.config = defaultConfigObject('MYSQL')
  form.remark = ''
  visible.value = true
  suppressTypeWatch.value = false
}

function openEdit(row) {
  suppressTypeWatch.value = true
  form.id = row.id
  form.name = row.name
  form.type = row.type
  applyRowConfig(row.type, row.configJson)
  form.remark = row.remark || ''
  visible.value = true
  suppressTypeWatch.value = false
}

async function submit() {
  const configJson = JSON.stringify(buildConfigObject())
  await saveDataSource({
    id: form.id,
    name: form.name,
    type: form.type,
    configJson,
    remark: form.remark
  })
  ElMessage.success('已保存')
  visible.value = false
  await load()
}

async function remove(row) {
  await ElMessageBox.confirm(`删除数据源「${row.name}」？`, '确认', { type: 'warning' })
  await removeDataSource(row.id)
  ElMessage.success('已删除')
  await load()
}

async function testDraft() {
  try {
    const configJson = JSON.stringify(buildConfigObject())
    await testConnection(form.type, configJson)
    ElMessage.success('连接成功')
  } catch (e) {
    ElMessage.error(e.message || '失败')
  }
}

async function testRow(row) {
  try {
    await testConnection(row.type, row.configJson)
    ElMessage.success('连接成功')
  } catch (e) {
    ElMessage.error(e.message || '失败')
  }
}

async function onFile(file) {
  const raw = file.raw
  if (!raw) return
  try {
    const data = await uploadExcel(raw)
    form.config.fileId = data.fileId
    ElMessage.success('上传成功，fileId 已回填')
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  }
}

onMounted(load)

watch(
  () => form.type,
  (newType, oldType) => {
    if (suppressTypeWatch.value || !oldType || newType === oldType) return
    form.config = defaultConfigObject(newType)
    ElMessage.info(`已切换为 ${newType} 模板字段`) 
  }
)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.hint {
  margin-top: 6px;
  font-size: 12px;
  color: #8a93b8;
}
</style>
