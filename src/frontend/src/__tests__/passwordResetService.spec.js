import { afterEach, describe, expect, it, vi } from 'vitest'
import { passwordResetService } from '../services/passwordResetService'

describe('passwordResetService', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('sends a reset request and returns the generic response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      text: () => Promise.resolve('{"message":"Si el correo está registrado."}')
    }))

    const response = await passwordResetService.solicitar('persona@example.com')

    expect(response.message).toBe('Si el correo está registrado.')
    expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/auth\/password-reset\/request$/),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'persona@example.com' })
      })
    )
  })

  it('validates tokens and submits both password values', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      text: () => Promise.resolve('{"valid":true}')
    }))

    await passwordResetService.validar('token')
    await passwordResetService.restablecer('token', 'nueva-1234', 'nueva-1234')

    expect(fetch).toHaveBeenNthCalledWith(
      1,
      expect.stringMatching(/\/auth\/password-reset\/validate$/),
      expect.objectContaining({ body: JSON.stringify({ token: 'token' }) })
    )
    expect(fetch).toHaveBeenNthCalledWith(
      2,
      expect.stringMatching(/\/auth\/password-reset\/confirm$/),
      expect.objectContaining({
        body: JSON.stringify({
          token: 'token',
          newPassword: 'nueva-1234',
          confirmPassword: 'nueva-1234'
        })
      })
    )
  })

  it('uses backend plain-text validation errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      text: () => Promise.resolve('El enlace expiró.')
    }))

    await expect(passwordResetService.validar('token')).rejects.toThrow('El enlace expiró.')
  })
})
