// src/app/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/AppLayout.vue'
import { generateRoutesFromMenu } from './util'
import { menuItems } from '@/app/menu/menuItems'
import { setupAuthGuard } from './guard'
import Login from '@/features/auth/Login.vue'
import Forbidden from '@/features/forbidden/Forbidden.vue'

const routes = [
  {
    path: '/login',
    component: Login,
  },
  {
    path: '/forbidden',
    component: Forbidden,
  },
  {
    path: '/',
    component: Layout,
    children: [
      ...generateRoutesFromMenu(menuItems),
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

setupAuthGuard(router)