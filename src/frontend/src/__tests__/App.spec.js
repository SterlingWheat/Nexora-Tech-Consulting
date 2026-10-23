import { describe, it, expect } from 'vitest'

import { mount, RouterLinkStub } from '@vue/test-utils'
import App from '../App.vue'

describe('App', () => {
  it('renderiza la cabecera con la navegación principal', () => {
    const wrapper = mount(App, {
      global: { stubs: { RouterLink: RouterLinkStub, RouterView: true } },
    })

    expect(wrapper.text()).toContain('Casa Amandita')
    const destinos = wrapper.findAllComponents(RouterLinkStub).map((l) => l.props('to'))
    expect(destinos).toEqual(expect.arrayContaining(['/tareas', '/voluntarios', '/turnos']))
  })
})
