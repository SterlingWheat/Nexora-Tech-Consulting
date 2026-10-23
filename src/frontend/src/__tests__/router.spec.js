import { describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from '../router'
import { useAuthStore } from '../stores/authStore'

describe('router authentication guard', () => {
  it('redirects guests to login and allows authenticated users to open tasks', async () => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()

    await router.push('/tareas')
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/tareas')

    authStore.token = 'signed-token'
    await router.push('/tareas')
    expect(router.currentRoute.value.name).toBe('tareas')
    authStore.logout()
  })

  it('blocks task creation routes for TI and volunteers', async () => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.token = 'signed-token'

    for (const rol of ['TI', 'VOLUNTARIO']) {
      authStore.rol = rol
      await router.push('/tareas/nueva')
      expect(router.currentRoute.value.name).toBe('tareas')
      expect(router.currentRoute.value.query.acceso).toBe('denegado')
    }

    for (const rol of ['ADMINISTRADOR', 'GESTOR_TAREAS']) {
      authStore.rol = rol
      await router.push('/tareas/nueva')
      expect(router.currentRoute.value.name).toBe('nueva-tarea')
    }

    authStore.logout()
    await router.push('/')
  })

  it('allows every authenticated role to view their own profile, but only admins to view others', async () => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.token = 'signed-token'

    for (const rol of ['ADMINISTRADOR', 'TI', 'GESTOR_TAREAS', 'VOLUNTARIO']) {
      authStore.rol = rol
      await router.push('/perfil')
      expect(router.currentRoute.value.name).toBe('mi-perfil')
    }

    for (const rol of ['TI', 'GESTOR_TAREAS', 'VOLUNTARIO']) {
      authStore.rol = rol
      await router.push('/usuarios/7/perfil')
      expect(router.currentRoute.value.name).toBe('tareas')
      expect(router.currentRoute.value.query.acceso).toBe('denegado')
    }

    authStore.rol = 'ADMINISTRADOR'
    await router.push('/usuarios/7/perfil')
    expect(router.currentRoute.value.name).toBe('perfil-voluntario')

    authStore.logout()
    await router.push('/')
  })
})
