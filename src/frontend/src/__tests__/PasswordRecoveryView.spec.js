import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import PasswordRecoveryView from '../views/PasswordRecoveryView.vue'
import PasswordResetView from '../views/PasswordResetView.vue'
import { passwordResetService } from '../services/passwordResetService'

vi.mock('../services/passwordResetService', () => ({
  passwordResetService: {
    solicitar: vi.fn(),
    validar: vi.fn(),
    restablecer: vi.fn()
  }
}))

const createTestRouter = (component) => createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/', component }]
})

describe('password recovery views', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState(null, '', '/')
  })

  it('displays the same generic message returned by the recovery API', async () => {
    passwordResetService.solicitar.mockResolvedValue({
      message: 'Si el correo electrónico está registrado, recibirás un enlace para restablecer tu contraseña.'
    })
    const router = createTestRouter(PasswordRecoveryView)
    await router.push('/')
    await router.isReady()
    const wrapper = mount(PasswordRecoveryView, { global: { plugins: [router] } })

    await wrapper.get('input[name="email"]').setValue('persona@example.com')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(passwordResetService.solicitar).toHaveBeenCalledWith('persona@example.com')
    expect(wrapper.get('[role="status"]').text()).toContain('Si el correo electrónico está registrado')
    expect(wrapper.get('a').attributes('href')).toBe('/login')
  })

  it('validates the token and confirms matching passwords', async () => {
    window.history.replaceState(null, '', '/restablecer-contrasena#token=token-seguro')
    passwordResetService.validar.mockResolvedValue({ valid: true })
    passwordResetService.restablecer.mockResolvedValue({
      message: 'Tu contraseña fue restablecida correctamente.'
    })
    const router = createTestRouter(PasswordResetView)
    await router.push('/')
    await router.isReady()
    const wrapper = mount(PasswordResetView, { global: { plugins: [router] } })
    await flushPromises()

    expect(passwordResetService.validar).toHaveBeenCalledWith('token-seguro')
    await wrapper.get('input[name="newPassword"]').setValue('nueva-1234')
    await wrapper.get('input[name="confirmPassword"]').setValue('nueva-1234')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(passwordResetService.restablecer).toHaveBeenCalledWith(
      'token-seguro',
      'nueva-1234',
      'nueva-1234'
    )
    expect(wrapper.text()).toContain('Contraseña actualizada')
    expect(wrapper.text()).toContain('Tu contraseña fue restablecida correctamente.')
  })

  it('shows a clear message when the token is expired or invalid', async () => {
    window.history.replaceState(null, '', '/restablecer-contrasena#token=vencido')
    passwordResetService.validar.mockRejectedValue(new Error('El enlace expiró.'))
    const router = createTestRouter(PasswordResetView)
    await router.push('/')
    await router.isReady()
    const wrapper = mount(PasswordResetView, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('El enlace no es válido o expiró.')
    expect(wrapper.text()).toContain('Solicita uno nuevo')
  })

  it('rejects mismatched passwords without sending them to the backend', async () => {
    window.history.replaceState(null, '', '/restablecer-contrasena#token=token-seguro')
    passwordResetService.validar.mockResolvedValue({ valid: true })
    const router = createTestRouter(PasswordResetView)
    await router.push('/')
    await router.isReady()
    const wrapper = mount(PasswordResetView, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.get('input[name="newPassword"]').setValue('nueva-1234')
    await wrapper.get('input[name="confirmPassword"]').setValue('diferente-1234')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('[role="alert"]').text()).toBe('Las contraseñas no coinciden.')
    expect(passwordResetService.restablecer).not.toHaveBeenCalled()
  })
})
