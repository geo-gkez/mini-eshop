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
      <v-btn variant="text" icon="mdi-logout" aria-label="Log out" @click="doLogout" />
    </v-app-bar>

    <v-main>
      <v-container class="py-6">
        <LoginView v-if="!user" :notice="authNotice" @logged-in="onLogin" />
        <!-- Keep CatalogView alive so search term and page survive a trip to the
             cart and back; Cart/Order are intentionally remounted fresh. -->
        <keep-alive v-else include="CatalogView">
          <CatalogView
            v-if="view === 'catalog'"
            :cart-count="cartCount"
            @go-cart="view = 'cart'"
            @cart-changed="loadCartCount"
          />
          <CartView
            v-else-if="view === 'cart'"
            @go-catalog="view = 'catalog'"
            @go-order="view = 'order'"
            @cart-changed="loadCartCount"
          />
          <OrderView v-else-if="view === 'order'" @confirmed="onConfirmed" @back="view = 'cart'" />
        </keep-alive>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { api } from './api/client.js'
import LoginView from './views/LoginView.vue'
import CatalogView from './views/CatalogView.vue'
import CartView from './views/CartView.vue'
import OrderView from './views/OrderView.vue'

const user = ref(null)
const view = ref('catalog')
const cartCount = ref(0)
const authNotice = ref(null)

onMounted(async () => {
  window.addEventListener('auth:expired', onAuthExpired)
  try {
    const data = await api('/auth/me')
    user.value = data.username
    await loadCartCount()
  } catch {
    // 401 = not authenticated yet; XSRF-TOKEN cookie is written regardless
  }
})

onUnmounted(() => {
  window.removeEventListener('auth:expired', onAuthExpired)
})

// Force Spring to write a fresh XSRF-TOKEN cookie. Needed whenever the session
// (and its CSRF token) is gone — after expiry or logout — so the login form can POST.
async function reseedCsrf() {
  try { await api('/auth/me') } catch { /* 401 expected; cookie is still written */ }
}

async function onAuthExpired(e) {
  if (!user.value) return // already on login; ignore
  user.value = null
  cartCount.value = 0
  view.value = 'catalog'
  authNotice.value = e.detail
  await reseedCsrf()
}

async function onLogin() {
  // Must re-call /me after login: Spring rotates the CSRF token on authentication
  // (CsrfAuthenticationStrategy clears the old token; /me forces the deferred cookie write)
  const data = await api('/auth/me')
  user.value = data.username
  authNotice.value = null
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
    await reseedCsrf()
  }
}

function onConfirmed() {
  cartCount.value = 0
  view.value = 'catalog'
}
</script>
