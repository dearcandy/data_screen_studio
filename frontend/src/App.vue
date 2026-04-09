<template>
  <el-config-provider namespace="el">
    <div class="shell">
      <header class="top">
        <div class="brand">
          <span class="logo">◆</span>
          <span>Data Screen Studio</span>
          <span class="sub">低代码数据配置 · 大屏接口</span>
        </div>
        <el-menu
          class="nav"
          mode="horizontal"
          :default-active="active"
          background-color="transparent"
          text-color="#c8d0ff"
          active-text-color="#7cf2ff"
          router
        >
          <el-menu-item index="/">概览</el-menu-item>
          <el-menu-item index="/datasources">数据源</el-menu-item>
          <el-menu-item index="/datasets">数据集</el-menu-item>
          <el-menu-item index="/pipelines">流程</el-menu-item>
          <el-menu-item index="/external">外部调用</el-menu-item>
        </el-menu>
      </header>
      <main class="main">
        <router-view />
      </main>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const active = computed(() => route.path || '/')
</script>

<style scoped>
.shell {
  min-height: 100vh;
  background: radial-gradient(1200px 600px at 20% -10%, rgba(80, 120, 255, 0.25), transparent),
    radial-gradient(900px 500px at 90% 10%, rgba(0, 255, 200, 0.12), transparent),
    #0b1020;
}
.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(120, 140, 255, 0.15);
  backdrop-filter: blur(8px);
}
.brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.logo {
  color: #7cf2ff;
}
.sub {
  font-size: 12px;
  color: #8a93b8;
  font-weight: 400;
}
.nav {
  border-bottom: none !important;
  flex: 1;
  justify-content: flex-end;
}
.main {
  padding-bottom: 48px;
}
</style>
