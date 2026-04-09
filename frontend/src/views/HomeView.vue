<template>
  <div class="page">
    <div class="hero card">
      <h1>零代码 / 低代码可视化大屏数据配置</h1>
      <p class="lead">
        统一管理 MySQL、PostgreSQL、HTTP API、Redis、Kafka、Excel 与 Mock；在线测试连接；用轻量 JavaScript
        做过滤、聚合与结构转换；输出标准 JSON 供大屏或图表直接消费。
      </p>
      <div class="actions">
        <el-button type="primary" @click="$router.push('/datasources')">配置数据源</el-button>
        <el-button @click="$router.push('/datasets')">配置数据集</el-button>
        <el-button @click="$router.push('/pipelines')">配置流程</el-button>
        <el-button @click="$router.push('/external')">外部调用管理</el-button>
      </div>
    </div>
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <div class="card feat">
          <h3>一键测试</h3>
          <p>保存前验证 JDBC / HTTP / Redis / Kafka / 文件是否可用，减少联调等待。</p>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card feat">
          <h3>Mock 先行</h3>
          <p>数据集可切到 Mock 模式，用 JSON 占位，脚本与预览流程与真实数据一致。</p>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card feat">
          <h3>可嵌入接口</h3>
          <p>每个数据集生成公开 token，GET 即可拿到标准 <code>{ code, data, meta }</code> 响应。</p>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.hero h1 {
  margin: 0 0 12px;
  font-size: 22px;
}
.lead {
  margin: 0 0 16px;
  color: #b4bdda;
  line-height: 1.6;
}
.actions {
  display: flex;
  gap: 10px;
}
.feat h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #7cf2ff;
}
.feat p {
  margin: 0;
  color: #a8b0cc;
  line-height: 1.55;
  font-size: 13px;
}
code {
  background: rgba(0, 0, 0, 0.35);
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
