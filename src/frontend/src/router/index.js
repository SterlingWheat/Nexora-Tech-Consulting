import { createRouter, createWebHistory } from 'vue-router'

// Importaciones de las vistas (asegúrate de importar también tus otras vistas existentes como Home, Login, etc.)
import TareasView from '../views/TareasView.vue'
import TareaFormView from '../views/TareaFormView.vue'

const routes = [
  /*
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue')
  },
  */

  // Nuevas rutas
  {
    path: '/tareas',
    name: 'tareas',
    component: TareasView
  },
  {
    path: '/tareas/nueva',
    name: 'nueva-tarea',
    component: TareaFormView
  },
  {
    path: '/tareas/editar/:id',
    name: 'editar-tarea',
    component: TareaFormView,
    props: true // Permite pasar el parámetro :id como prop a la vista si lo necesitas
  }
]

const router = createRouter({
  // Se asume el uso de Vite, que proporciona import.meta.env.BASE_URL
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0, behavior: 'smooth' }
    }
  }
})

export default router