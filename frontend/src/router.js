import { createRouter, createWebHistory } from 'vue-router'
import { session } from './store.js'

const routes = [
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { public: true } },
  { path: '/', redirect: () => (session.user?.role === 'OBJECT' ? '/mobile' : '/dashboard') },
  { path: '/dashboard', name: 'dashboard', component: () => import('./views/DashboardView.vue'), meta: { staff: true } },
  { path: '/objects', name: 'objects', component: () => import('./views/ObjectListView.vue') },
  { path: '/objects/:id', name: 'object-detail', component: () => import('./views/ObjectDetailView.vue'), props: true },
  { path: '/mobile', name: 'mobile', component: () => import('./views/MobileView.vue'), meta: { object: true } },
  { path: '/forbidden', name: 'forbidden', component: () => import('./views/ErrorView.vue'), props: { kind: 'forbidden' } },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const user = session.user
  if (to.meta.public) return true
  if (!user || !session.token) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.staff && user.role === 'OBJECT') return { name: 'mobile' }
  if (to.meta.object && user.role !== 'OBJECT') return { name: 'dashboard' }
  return true
})

export default router
