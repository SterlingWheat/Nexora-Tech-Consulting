import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import UsuariosView from '../views/UsuariosView.vue'

const { listar, crear, actualizar, desactivar } = vi.hoisted(() => ({
  listar: vi.fn(),
  crear: vi.fn(),
  actualizar: vi.fn(),
  desactivar: vi.fn()
}))

vi.mock('../services/usuarioService', () => ({
  usuarioService: { listar, crear, actualizar, desactivar }
}))

const usuario = {
  idUsuario: 7,
  nombre: 'Ana Voluntaria',
  email: 'ana@example.com',
  rol: 'VOLUNTARIO',
  activo: true
}

const montarVista = async () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/usuarios', component: UsuariosView },
      { path: '/usuarios/:id/perfil', component: { template: '<div />' } }
    ]
  })
  await router.push('/usuarios')
  await router.isReady()
  const wrapper = mount(UsuariosView, {
    global: { plugins: [createPinia(), router] }
  })
  await flushPromises()
  return wrapper
}

describe('UsuariosView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listar.mockResolvedValue([usuario])
    crear.mockResolvedValue(usuario)
    actualizar.mockResolvedValue(usuario)
    desactivar.mockResolvedValue('Usuario desactivado correctamente.')
  })

  it('permite crear una cuenta con rol y credenciales', async () => {
    const wrapper = await montarVista()
    await wrapper.get('button').trigger('click')

    const inputs = wrapper.findAll('.usuarios-formulario input')
    await inputs[0].setValue('Luis Voluntario')
    await inputs[1].setValue('luis@example.com')
    await inputs[2].setValue('password-seguro')
    await wrapper.get('.usuarios-formulario select').setValue('VOLUNTARIO')
    await wrapper.get('.usuarios-formulario').trigger('submit')
    await flushPromises()

    expect(crear).toHaveBeenCalledWith({
      nombre: 'Luis Voluntario',
      email: 'luis@example.com',
      rol: 'VOLUNTARIO',
      password: 'password-seguro'
    })
    expect(listar).toHaveBeenCalledTimes(2)
  })

  it('permite editar una cuenta y filtrar por rol y estado', async () => {
    const wrapper = await montarVista()
    await wrapper.get('.usuarios-enlace').trigger('click')

    const inputs = wrapper.findAll('.usuarios-formulario input')
    await inputs[0].setValue('Ana Actualizada')
    await wrapper.get('.usuarios-formulario').trigger('submit')
    await flushPromises()

    expect(actualizar).toHaveBeenCalledWith(7, {
      nombre: 'Ana Actualizada',
      email: 'ana@example.com',
      rol: 'VOLUNTARIO'
    })

    const filtros = wrapper.findAll('.usuarios-filtros select')
    await filtros[0].setValue('VOLUNTARIO')
    await filtros[1].setValue('false')
    await filtros[1].trigger('change')
    await flushPromises()
    expect(listar).toHaveBeenLastCalledWith({ rol: 'VOLUNTARIO', activo: 'false' })
  })
})
