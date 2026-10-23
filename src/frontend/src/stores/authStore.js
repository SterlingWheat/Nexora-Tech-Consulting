import { defineStore } from 'pinia'
import { authService } from '../services/authService'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: sessionStorage.getItem('authToken'),
    rol: sessionStorage.getItem('authRole')
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },

  actions: {
    async login(email, password) {
      const session = await authService.login(email, password)
      this.token = session.token
      this.rol = session.rol
      sessionStorage.setItem('authToken', session.token)
      sessionStorage.setItem('authRole', session.rol)
    },

    logout() {
      this.token = null
      this.rol = null
      sessionStorage.removeItem('authToken')
      sessionStorage.removeItem('authRole')
    }
  }
})
