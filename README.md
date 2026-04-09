# Data Screen Studio

面向大屏与可视化场景的低代码数据编排工具。  
你可以在界面中管理数据源、配置数据集、编排流程（DAG）、在线预览执行结果，并发布为可外部调用的 HTTP 接口。

---

## 当前能力概览

- 数据源管理：MySQL、PostgreSQL、HTTP API、Redis、Kafka、Excel、Mock
- 数据源测试：统一在“数据源”页面调用连接测试接口
- 数据集管理：支持 `LIVE / MOCK`、脚本加工、预览执行、外部 token 调用
- 流程（Pipeline）管理：
  - `nodes + dependsOn` 的 DAG 定义
  - 同步执行、异步执行、草稿预览
  - 定义校验（节点配置、依赖、环检测）
  - 外部 token 调用（可开关）
- 外部调用管理页面：统一管理数据集与流程的调用 URL、开关、token 重置

---

## 技术栈

- 后端：Spring Boot 3、MyBatis-Plus、JDBC、Kafka Clients、Lettuce、Apache POI、GraalVM JS
- 前端：Vue 3、Vite、Element Plus、Axios
- 数据库：MySQL（元数据存储）

---

## 快速开始

### 1) 初始化数据库

开发阶段建议直接使用全量建表脚本：

```sql
DROP DATABASE IF EXISTS data_screen_studio;
SOURCE backend/db/schema.sql;
```

### 2) 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8088`

### 3) 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认通过 Vite 代理 `/api`、`/embed` 到 `http://127.0.0.1:8088`。

---

## 前端页面

- `/datasources`：数据源管理
- `/datasets`：数据集管理
- `/pipelines`：流程编排与执行
- `/external`：外部调用管理（数据集 + 流程）

---

## 后端接口（核心）

### 数据源

- `GET /api/datasources`
- `GET /api/datasources/{id}`
- `POST /api/datasources`
- `PUT /api/datasources/{id}`
- `DELETE /api/datasources/{id}`
- `POST /api/datasources/test`

### 数据集

- `GET /api/datasets`
- `GET /api/datasets/{id}`
- `POST /api/datasets`
- `PUT /api/datasets/{id}`
- `DELETE /api/datasets/{id}`
- `POST /api/datasets/{id}/preview`
- `POST /api/datasets/{id}/regenerate-token`

### 流程（Pipeline）

- `POST /api/pipeline/save`
- `GET /api/pipeline/{id}`
- `GET /api/pipeline/list?page=1&size=10&keyword=...`
- `DELETE /api/pipeline/{id}`
- `POST /api/pipeline/{id}/publish`
- `POST /api/pipeline/{id}/preview`（草稿可用）
- `POST /api/pipeline/execute/sync`
- `POST /api/pipeline/execute/async`
- `GET /api/pipeline/execution/{executionId}`
- `GET /api/pipeline/{pipelineId}/executions`
- `POST /api/pipeline/{id}/regenerate-token`
- `POST /api/pipeline/{id}/external-enabled`
- `POST /api/pipeline/validate`

### 外部调用（Embed）

- 数据集：
  - `GET /embed/data/{token}`
- 流程：
  - `GET /embed/pipeline/{token}`
  - `POST /embed/pipeline/{token}`（可传 `params`）

---

## 流程定义格式（当前）

后端执行引擎以 `nodes` 为准：

```json
{
  "nodes": [
    {
      "id": "fetch_orders",
      "type": "fetch",
      "dependsOn": [],
      "config": {
        "dataSourceId": 1,
        "fetchSpec": "SELECT * FROM orders LIMIT 100"
      }
    },
    {
      "id": "transform",
      "type": "script",
      "dependsOn": ["fetch_orders"],
      "config": {
        "language": "javascript",
        "source": "return input;"
      }
    },
    {
      "id": "out",
      "type": "output",
      "dependsOn": ["transform"],
      "config": {
        "format": "json"
      }
    }
  ]
}
```

支持节点类型：

- `dataSource`
- `fetch`
- `dataSet`
- `script`
- `condition`
- `parallel`
- `output`

---

## 数据集 `fetchSpec` 说明

| 数据源类型 | `fetchSpec` 含义 |
|---|---|
| MySQL / PostgreSQL | 只读 SQL（`SELECT` / `WITH`） |
| HTTP API | 路径字符串，或 JSON（`path/method/params/headers/body`） |
| Kafka | topic 字符串，或 JSON（`topic/maxRecords/pollTimeoutMs/autoOffsetReset`） |
| Redis | key |
| Excel | sheet 名称（可空，默认第一张） |
| Mock | 可不填 |

---

## 统一响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "meta": {
    "timestamp": "2026-01-01T00:00:00Z"
  }
}
```

---

## 目录说明（简）

- `backend/db/schema.sql`：开发期全量建表脚本
- `backend/src/main/java/.../web`：控制器层
- `backend/src/main/java/.../service/source`：数据源类型处理器
- `backend/src/main/java/.../service/pipeline/executor`：流程节点执行器
- `frontend/src/views`：页面
- `frontend/src/api`：前端 API 封装

---

## 注意事项

- 流程预览接口允许草稿执行；同步/异步执行接口默认要求流程已发布。
- 外部流程调用需满足：
  - 流程有 `public_token`
  - `external_enabled = true`
- 若前后端分离部署，请设置前端环境变量 `VITE_BACKEND_ORIGIN` 为后端可访问地址。
