# Data Screen Studio

面向大屏 / 可视化场景的**低代码数据配置工具**：在界面里管理数据源与数据集，一键测试连接，支持 Mock，内置 **GraalVM JavaScript** 做轻量数据处理，并输出统一 JSON 接口供前端图表直接调用。

## 技术栈

- 后端：Spring Boot 3、MyBatis-Plus（MySQL 元数据）、WebClient、Lettuce（动态 Redis）、JDBC、Apache POI（Excel）、GraalVM JS（脚本）
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

1. **数据源**：新建 MySQL / PostgreSQL / HTTP / Redis / Excel / Mock，填写 JSON 配置，点「测试连接」。
2. **Excel**：在「Excel」类型下上传文件，将返回的 `fileId` 写入配置 JSON。
3. **数据集**：选择「Mock」可只填 Mock JSON 与脚本；选「实时」需绑定数据源，并在 `fetchSpec` 中填写 SQL / 路径 / Redis key / 工作表名等。
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

**HTTP API**（测试连接使用 `testPath`，如 `/get`）

```json
{
  "baseUrl": "https://httpbin.org",
  "testPath": "/get",
  "headers": {}
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
| HTTP API | 相对路径，拼在数据源 `baseUrl` 后 |
| Redis | Key |
| Excel | 工作表名，可留空使用第一张表 |
| Mock | 不需要 |

## 生产环境嵌入地址

将前端环境变量 `VITE_BACKEND_ORIGIN` 设为你的 API 域名，或在列表中手动替换为公网可访问的后端地址。

## 许可证

示例项目，按需修改使用。
