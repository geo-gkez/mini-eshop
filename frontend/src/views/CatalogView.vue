<template>
  <div>
    <v-row align="center" class="mb-4">
      <v-col cols="12" sm="9">
        <v-text-field
          v-model="search"
          label="Search products"
          clearable
          hide-details
          @keyup.enter="fetchProducts(1)"
          @click:clear="onClear"
        />
      </v-col>
      <v-col cols="12" sm="3">
        <v-btn color="primary" block @click="fetchProducts(1)">Search</v-btn>
      </v-col>
    </v-row>

    <p class="text-caption text-medium-emphasis mb-4">
      <span v-if="searched">Search results for "{{ activeSearch }}"</span>
      <span v-else>Showing all products</span>
    </p>

    <v-alert v-if="addError" type="error" density="compact" closable class="mb-4"
      @click:close="addError = null">{{ addError }}</v-alert>

    <v-snackbar v-model="snackbar" :timeout="2000" color="success" location="bottom end">
      {{ snackbarMsg }}
    </v-snackbar>

    <div v-if="loading" class="d-flex justify-center py-12">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <template v-else>
      <v-row v-if="products.length">
        <v-col v-for="p in products" :key="p.reference" cols="12" sm="6" md="4">
          <v-card height="100%" class="d-flex flex-column">
            <v-card-title>{{ p.name }}</v-card-title>
            <v-card-subtitle class="text-caption">{{ p.reference }}</v-card-subtitle>
            <v-card-text class="flex-grow-1">
              <p v-if="p.description" class="text-body-2 mb-2">{{ p.description }}</p>
              <span class="text-h6">{{ formatPrice(p.price, p.currency) }}</span>
            </v-card-text>
            <v-card-actions>
              <v-btn
                color="primary"
                size="small"
                prepend-icon="mdi-cart-plus"
                :loading="adding === p.reference"
                @click="addToCart(p)"
              >Add to cart</v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>

      <v-row v-else class="mt-4">
        <v-col class="text-center text-medium-emphasis">No products found.</v-col>
      </v-row>

      <v-row v-if="totalPages > 1" justify="center" class="mt-6">
        <v-pagination v-model="page" :length="totalPages" @update:model-value="fetchProducts" />
      </v-row>
    </template>

    <v-divider class="mt-8 mb-4" />
    <v-row justify="space-between" align="center">
      <v-col>
        <span class="text-body-2 text-medium-emphasis">
          <span v-if="cartCount === 0">Your cart is empty</span>
          <span v-else>{{ cartCount }} item{{ cartCount !== 1 ? 's' : '' }} in your cart</span>
        </span>
      </v-col>
      <v-col cols="auto">
        <v-btn color="secondary" prepend-icon="mdi-cart" @click="$emit('go-cart')">View Cart</v-btn>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/client.js'

const props = defineProps({
  cartCount: { type: Number, default: 0 },
})

const emit = defineEmits(['go-cart', 'cart-changed'])

const search = ref('')
const activeSearch = ref('')
const searched = ref(false)
const products = ref([])
const page = ref(1)
const totalPages = ref(1)
const loading = ref(false)
const adding = ref(null)
const addError = ref(null)
const snackbar = ref(false)
const snackbarMsg = ref('')

onMounted(() => fetchProducts(1))

async function fetchProducts(p = page.value) {
  loading.value = true
  addError.value = null
  page.value = p
  try {
    const params = new URLSearchParams({ page: p - 1, size: 9 })
    if (search.value) params.set('search', search.value)
    const data = await api(`/products?${params}`)
    products.value = data.products
    totalPages.value = data.pagination.totalPages
    searched.value = data.searchActive
    activeSearch.value = data.searchTerm
  } catch {
    addError.value = 'Failed to load products.'
  } finally {
    loading.value = false
  }
}

function onClear() {
  search.value = ''
  fetchProducts(1)
}

async function addToCart(product) {
  adding.value = product.reference
  addError.value = null
  try {
    await api('/cart/items', {
      method: 'POST',
      body: { productReference: product.reference, quantity: 1 },
    })
    snackbarMsg.value = `"${product.name}" added to cart.`
    snackbar.value = true
    emit('cart-changed')
  } catch (e) {
    addError.value = e.body?.detail ?? 'Could not add to cart.'
  } finally {
    adding.value = null
  }
}

function formatPrice(price, currency) {
  return new Intl.NumberFormat('el-GR', { style: 'currency', currency }).format(price)
}
</script>
