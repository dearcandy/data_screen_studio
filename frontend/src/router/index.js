import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DataSourceView from '../views/DataSourceView.vue'
import DataSetView from '../views/DataSetView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/datasources', name: 'datasources', component: DataSourceView },
    { path: '/datasets', name: 'datasets', component: DataSetView }
  ]
})

export default router
