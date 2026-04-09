import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DataSourceView from '../views/DataSourceView.vue'
import DataSetView from '../views/DataSetView.vue'
import PipelineView from '../views/PipelineView.vue'
import ExternalAccessView from '../views/ExternalAccessView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/datasources', name: 'datasources', component: DataSourceView },
    { path: '/datasets', name: 'datasets', component: DataSetView },
    { path: '/pipelines', name: 'pipelines', component: PipelineView },
    { path: '/external', name: 'external', component: ExternalAccessView }
  ]
})

export default router
