import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import PerfilView from '../views/PerfilView.vue'
import TareasView from '../views/TareasView.vue'
import TareaFormView from '../views/TareaFormView.vue'
import UsuariosView from '../views/UsuariosView.vue'
import VoluntariosView from '../views/VoluntariosView.vue'
import VoluntarioFormView from '../views/VoluntarioFormView.vue'
import { useAuthStore } from '../stores/authStore'

const routes = [
  {
    path: '/',
    redirect: '/tareas'
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true }
  },
  {
    path: '/tareas',
    name: 'tareas',
    component: TareasView,
    meta: { requiresAuth: true }
  },
  {
    path: '/perfil',
    name: 'mi-perfil',
    component: PerfilView,
    meta: { requiresAuth: true }
  },
  {
    path: '/usuarios',
    name: 'usuarios',
    component: UsuariosView,
    meta: { requiresAuth: true, roles: ['ADMINISTRADOR'] }
  },
  {
    path: '/usuarios/:id/perfil',
    name: 'perfil-voluntario',
    component: PerfilView,
    props: true,
    meta: { requiresAuth: true, roles: ['ADMINISTRADOR'] }
  },
  {
    path: '/tareas/nueva',
    name: 'nueva-tarea',
    component: TareaFormView,
    meta: { requiresAuth: true, roles: ['ADMINISTRADOR', 'GESTOR_TAREAS'] }
  },
  {
    path: '/tareas/editar/:id',
    name: 'editar-tarea',
    component: TareaFormView,
    props: true,
    meta: { requiresAuth: true, roles: ['ADMINISTRADOR', 'GESTOR_TAREAS'] }
  },
  
  // Rutas de Voluntarios y Turnos (de la rama main)
  {
    path: '/voluntarios',
    name: 'voluntarios',
    component: VoluntariosView,
    meta: { requiresAuth: true }
  },
  {
    path: '/voluntarios/nuevo',
    name: 'nuevo-voluntario',
    component: VoluntarioFormView,
    meta: { requiresAuth: true }
  },
  {
    path: '/voluntarios/editar/:id',
    name: 'editar-voluntario',
    component: VoluntarioFormView,
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/voluntarios/:id/disponibilidad',
    name: 'disponibilidad-voluntario',
    component: () => import('../views/DisponibilidadView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/turnos',
    name: 'turnos',
    component: () => import('../views/TurnosView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/turnos/nuevo',
    name: 'nuevo-turno',
    component: () => import('../views/TurnoFormView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0, behavior: 'smooth' }
  }
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles && !to.meta.roles.includes(authStore.rol)) {
    return { name: 'tareas', query: { acceso: 'denegado' } }
  }
  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'tareas' }
  }
  return true
})

export default router