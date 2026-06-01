<template>
  <v-app>
    <v-app-bar v-if="user" color="primary" density="compact">
      <v-toolbar-title>E-Shop</v-toolbar-title>
      <v-btn variant="text" @click="view = 'catalog'">Catalog</v-btn>
      <v-btn variant="text" prepend-icon="mdi-cart" @click="view = 'cart'">
        <v-badge :content="cartCount" :model-value="cartCount > 0" color="error" floating>
          Cart
        </v-badge>
      </v-btn>
      <v-spacer />
      <v-btn variant="text" prepend-icon="mdi-account">{{ user }}</v-btn>
      <v-btn variant="text" icon="mdi-logout" @click="doLogout" />
    </v-app-bar>

    <v-main>
      <v-container class="py-6">
        <LoginView v-if="!user" @logged-in="onLogin" />
        <CatalogView
          v-else-if="view === 'catalog'"
          :cart-count="cartCount"
          @go-cart="view = 'cart'"
          @cart-changed="loadCartCount"
        />
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
const cartCount = ref(0)

onMounted(async () => {
  try {
    const data = await api('/auth/me')
    user.value = data.username
    await loadCartCount()
  } catch {
    // 401 = not authenticated yet; XSRF-TOKEN cookie is written regardless
  }
})

async function onLogin() {
  // Must re-call /me after login: Spring rotates the CSRF token on authentication
  // (CsrfAuthenticationStrategy clears the old token; /me forces the deferred cookie write)
  const data = await api('/auth/me')
  user.value = data.username
  await loadCartCount()
  view.value = 'catalog'
}

async function loadCartCount() {
  try {
    const data = await api('/cart')
    cartCount.value = data.items.reduce((sum, item) => sum + item.quantity, 0)
  } catch {
    cartCount.value = 0
  }
}

async function doLogout() {
  try {
    await api('/auth/logout', { method: 'POST' })
  } finally {
    user.value = null
    cartCount.value = 0
    view.value = 'catalog'
    // The logout response carries Clear-Site-Data: "cookies", which wipes all cookies
    // including XSRF-TOKEN. Re-seed it so the login form can POST immediately.
    try { await api('/auth/me') } catch { /* 401 expected; cookie is still written */ }
  }
}

function onConfirmed() {
  cartCount.value = 0
  view.value = 'catalog'
}
</script>
