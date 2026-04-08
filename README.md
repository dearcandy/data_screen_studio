# Data Screen Studio

面向大屏 / 可视化场景的**低代码数据配置工具**：在界面里管理数据源与数据集，一键测试连接，支持 Mock，内置 **GraalVM JavaScript** 做轻量数据处理，并输出统一 JSON 接口供前端图表直接调用。

## 技术栈

- 后端：Spring Boot 3、MyBatis-Plus（MySQL 元数据）、WebClient、Lettuce（动态 Redis）、Kafka Clients、JDBC、Apache POI（Excel）、GraalVM JS（脚本）
- 前端：Vue 3、Vite、Element Plus、Axios

## 快速开始

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认端口 **8088**，元数据与上传文件保存在 `backend/data/`。

### 前端

```bash
cd frontend
npm install
npm run dev
```

开发环境默认通过 Vite 代理转发 `/api`、`/embed` 到 `http://127.0.0.1:8088`。

### 典型流程

1. **数据源**：新建 MySQL / PostgreSQL / HTTP / Redis / Kafka / Excel / Mock，填写 JSON 配置，点「测试连接」。
2. **Excel**：在「Excel」类型下上传文件，将返回的 `fileId` 写入配置 JSON。
3. **数据集**：选择「Mock」可只填 Mock JSON 与脚本；选「实时」需绑定数据源。HTTP 数据源可直接在页面填写 `path/method/params/headers/body`，系统自动生成 `fetchSpec`。
4. **脚本**：编写 JavaScript 片段；引擎会先注入 `const input = <原始数据的 JSON>`，请使用 `return` 返回结果（例如 `return input.filter(...)`）。
5. **嵌入**：保存后在列表中复制 `http://127.0.0.1:8088/embed/data/{token}`，大屏侧用 HTTP GET 取数。

## 响应格式（标准）

```json
{
  "code": 0,
  "message": "ok",
  "data": { },
  "meta": { "timestamp": "..." }
}
```

## 数据源配置 JSON 示例

**MySQL / PostgreSQL**

```json
{
  "host": "127.0.0.1",
  "port": 3306,
  "database": "demo",
  "username": "root",
  "password": "secret"
}
```

**HTTP API**（支持 GET/POST、Query 参数、Headers、Body）

```json
{
  "baseUrl": "https://httpbin.org",
  "method": "GET",
  "testPath": "/get",
  "params": {
    "tenant": "demo"
  },
  "headers": {
    "Authorization": "Bearer xxx"
  },
  "body": {
    "name": "demo"
  }
}
```

**Redis**

```json
{
  "host": "127.0.0.1",
  "port": 6379,
  "password": "",
  "database": 0
}
```

**Kafka**（测试连接：`AdminClient`；取数：短时 `Consumer#poll`）

```json
{
  "bootstrapServers": "127.0.0.1:9092",
  "testTopic": "可选，存在则校验 topic",
  "securityProtocol": "PLAINTEXT",
  "defaultTopic": "可选，数据集 fetchSpec 为空时用",
  "defaultMaxRecords": 100,
  "defaultPollTimeoutMs": 8000,
  "defaultAutoOffsetReset": "latest"
}
```

SASL 示例（`securityProtocol` 需为 `SASL_PLAINTEXT` 或 `SASL_SSL`）：

```json
{
  "bootstrapServers": "kafka.example.com:9093",
  "securityProtocol": "SASL_SSL",
  "saslMechanism": "PLAIN",
  "saslUsername": "user",
  "saslPassword": "secret"
}
```

**Excel**

```json
{
  "fileId": "上传接口返回的 fileId"
}
```

**Mock 数据源（LIVE 时读取 `mock` 字段）**

```json
{
  "mock": [{ "a": 1, "b": 2 }]
}
```

## 数据集 `fetchSpec` 说明

| 数据源类型 | fetchSpec 含义 |
|-----------|----------------|
| MySQL / PostgreSQL | 仅允许 `SELECT` / `WITH` SQL |
| HTTP API | 可填相对路径；也可填 JSON（`path/method/params/headers/body`） |
| Kafka | Topic 名；或 JSON（`topic`、`maxRecords`、`pollTimeoutMs`、`autoOffsetReset`） |
| Redis | Key |
| Excel | 工作表名，可留空使用第一张表 |
| Mock | 不需要 |

HTTP 数据源 `fetchSpec` JSON 示例：

```json
{
  "path": "/post",
  "method": "POST",
  "params": {
    "tenant": "cn"
  },
  "headers": {
    "Authorization": "Bearer xxx"
  },
  "body": {
    "keyword": "screen"
  }
}
```

> 说明：`fetchSpec` 与数据源配置可同时存在同名字段时，以 `fetchSpec` 为准（更适合做数据集级别的覆盖）。

## 后端结构（当前）

- `ConnectionTestService` / `DataFetchService`：负责流程编排与入口校验
- `DataSourceHandlerManager`：维护 `SourceType -> Handler` 映射
- `service/source/*SourceHandler`：按类型实现 `validateConfig / testConnection / fetch`
- `DataSourceConfigValidator`：兼容保留原接口，内部已委托到各 Handler 的 `validateConfig`

## 生产环境嵌入地址

将前端环境变量 `VITE_BACKEND_ORIGIN` 设为你的 API 域名，或在列表中手动替换为公网可访问的后端地址。

## 许可证

示例项目，按需修改使用。
