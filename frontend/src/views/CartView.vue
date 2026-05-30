<template>
  <div>
    <h2 class="text-h5 mb-4">Your Cart</h2>

    <v-alert v-if="error" type="error" density="compact" closable class="mb-4"
      @click:close="error = null">{{ error }}</v-alert>

    <div v-if="loading" class="d-flex justify-center py-12">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <template v-else-if="items.length === 0">
      <p class="text-medium-emphasis mb-4">Your cart is empty.</p>
      <v-btn prepend-icon="mdi-arrow-left" @click="$emit('go-catalog')">Browse Products</v-btn>
    </template>

    <template v-else>
      <v-table>
        <thead>
          <tr>
            <th>Product</th>
            <th class="text-right">Unit Price</th>
            <th class="text-center">Qty</th>
            <th class="text-right">Subtotal</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.productReference">
            <td>{{ item.name }}</td>
            <td class="text-right">{{ formatPrice(item.price, item.currency) }}</td>
            <td class="text-center">
              <div class="d-flex align-center justify-center" style="gap:4px">
                <v-btn
                  icon="mdi-minus"
                  size="x-small"
                  variant="outlined"
                  :disabled="item.quantity <= 1"
                  @click="updateQty(item, item.quantity - 1)"
                />
                <span class="mx-1">{{ item.quantity }}</span>
                <v-btn
                  icon="mdi-plus"
                  size="x-small"
                  variant="outlined"
                  @click="updateQty(item, item.quantity + 1)"
                />
              </div>
            </td>
            <td class="text-right">{{ formatPrice(item.subtotal, item.currency) }}</td>
            <td class="text-center">
              <v-btn icon="mdi-delete-outline" size="small" variant="text" color="error"
                @click="removeItem(item)" />
            </td>
          </tr>
        </tbody>
      </v-table>

      <v-row justify="end" class="mt-3">
        <v-col cols="auto">
          <span class="text-h6">
            Total: {{ formatPrice(total, items[0]?.currency ?? 'EUR') }}
          </span>
        </v-col>
      </v-row>

      <v-row justify="space-between" class="mt-4">
        <v-col cols="auto">
          <v-btn variant="text" prepend-icon="mdi-arrow-left" @click="$emit('go-catalog')">
            Continue Shopping
          </v-btn>
        </v-col>
        <v-col cols="auto">
          <v-btn color="primary" append-icon="mdi-arrow-right" @click="$emit('go-order')">
            Proceed to Checkout
          </v-btn>
        </v-col>
      </v-row>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api/client.js'

defineEmits(['go-catalog', 'go-order'])

const items = ref([])
const loading = ref(false)
const error = ref(null)

const total = computed(() =>
  items.value.reduce((sum, i) => sum + Number(i.subtotal), 0)
)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await api('/cart')
    items.value = data.items
  } catch {
    error.value = 'Failed to load cart.'
  } finally {
    loading.value = false
  }
}

async function updateQty(item, qty) {
  try {
    const data = await api(`/cart/items/${item.productReference}`, {
      method: 'PATCH',
      body: { quantity: qty },
    })
    items.value = data.items
  } catch (e) {
    error.value = e.body?.detail ?? 'Could not update quantity.'
  }
}

async function removeItem(item) {
  try {
    await api(`/cart/items/${item.productReference}`, { method: 'DELETE' })
    // DELETE returns 204 — reload the cart
    const data = await api('/cart')
    items.value = data.items
  } catch (e) {
    error.value = e.body?.detail ?? 'Could not remove item.'
  }
}

function formatPrice(price, currency) {
  return new Intl.NumberFormat('el-GR', { style: 'currency', currency }).format(price)
}
</script>
