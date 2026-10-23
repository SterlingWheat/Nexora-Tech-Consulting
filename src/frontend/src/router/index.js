import { createRouter, createWebHistory } from 'vue-router'

// Importaciones de las vistas (asegúrate de importar también tus otras vistas existentes como Home, Login, etc.)
import TareasView from '../views/TareasView.vue'
import TareaFormView from '../views/TareaFormView.vue'
import VoluntariosView from '../views/VoluntariosView.vue'
import VoluntarioFormView from '../views/VoluntarioFormView.vue'

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
  },

  // Voluntarios (KAN-14)
  {
    path: '/voluntarios',
    name: 'voluntarios',
    component: VoluntariosView
  },
  {
    path: '/voluntarios/nuevo',
    name: 'nuevo-voluntario',
    component: VoluntarioFormView
  },
  {
    path: '/voluntarios/editar/:id',
    name: 'editar-voluntario',
    component: VoluntarioFormView,
    props: true
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