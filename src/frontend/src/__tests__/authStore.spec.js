import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/authStore'

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('stores the JWT for the current browser session and clears it on logout', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ token: 'signed-token', rol: 'VOLUNTARIO' })
    }))
    const authStore = useAuthStore()

    await authStore.login('persona@example.com', 'password')

    expect(authStore.isAuthenticated).toBe(true)
    expect(sessionStorage.getItem('authToken')).toBe('signed-token')
    expect(sessionStorage.getItem('authRole')).toBe('VOLUNTARIO')

    authStore.logout()

    expect(authStore.isAuthenticated).toBe(false)
    expect(sessionStorage.getItem('authToken')).toBeNull()
    expect(sessionStorage.getItem('authRole')).toBeNull()
  })
})
