<template>
  <v-app>
    <v-app-bar v-if="user" color="primary" density="compact">
      <v-toolbar-title>E-Shop</v-toolbar-title>
      <v-btn variant="text" @click="view = 'catalog'">Catalog</v-btn>
      <v-btn variant="text" @click="view = 'cart'">Cart</v-btn>
      <v-spacer />
      <v-btn variant="text" prepend-icon="mdi-account">{{ user }}</v-btn>
      <v-btn variant="text" icon="mdi-logout" @click="doLogout" />
    </v-app-bar>

    <v-main>
      <v-container class="py-6">
        <LoginView v-if="!user" @logged-in="onLogin" />
        <CatalogView v-else-if="view === 'catalog'" @go-cart="view = 'cart'" />
        <CartView v-else-if="view === 'cart'" @go-catalog="view = 'catalog'" @go-order="view = 'order'" />
        <OrderView v-else-if="view === 'order'" @confirmed="onConfirmed" @back="view = 'cart'" />
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from './api/client.js'
import LoginView from './views/LoginView.vue'
import CatalogView from './views/CatalogView.vue'
import CartView from './views/CartView.vue'
import OrderView from './views/OrderView.vue'

const user = ref(null)
const view = ref('catalog')

onMounted(async () => {
  try {
    const data = await api('/auth/me')
    user.value = data.username
  } catch {
    // 401 = not authenticated yet; XSRF-TOKEN cookie is written regardless
  }
})

async function onLogin() {
  // Must re-call /me after login: Spring rotates the CSRF token on authentication
  // (CsrfAuthenticationStrategy clears the old token; /me forces the deferred cookie write)
  const data = await api('/auth/me')
  user.value = data.username
  view.value = 'catalog'
}

async function doLogout() {
  try {
    await api('/auth/logout', { method: 'POST' })
  } finally {
    user.value = null
    view.value = 'catalog'
  }
}

function onConfirmed() {
  view.value = 'catalog'
}
</script>
