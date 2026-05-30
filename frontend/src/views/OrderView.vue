<template>
  <div>
    <!-- Step 1: Shipping address -->
    <template v-if="step === 'address'">
      <h2 class="text-h5 mb-4">Shipping Address</h2>

      <v-alert v-if="error" type="error" density="compact" closable class="mb-4"
        @click:close="error = null">{{ error }}</v-alert>

      <v-row>
        <v-col cols="12">
          <v-text-field v-model="address.street" label="Street" :error-messages="fieldError('street')"
            :disabled="submitting" autocomplete="street-address" />
        </v-col>
        <v-col cols="12" sm="6">
          <v-text-field v-model="address.city" label="City" :error-messages="fieldError('city')"
            :disabled="submitting" autocomplete="address-level2" />
        </v-col>
        <v-col cols="12" sm="3">
          <v-text-field v-model="address.postalCode" label="Postal Code"
            :error-messages="fieldError('postalCode')" :disabled="submitting" autocomplete="postal-code" />
        </v-col>
        <v-col cols="12" sm="3">
          <v-text-field v-model="address.country" label="Country" :error-messages="fieldError('country')"
            :disabled="submitting" autocomplete="country-name" />
        </v-col>
      </v-row>

      <v-row justify="space-between" class="mt-4">
        <v-col cols="auto">
          <v-btn variant="text" prepend-icon="mdi-arrow-left" @click="$emit('back')">Back to Cart</v-btn>
        </v-col>
        <v-col cols="auto">
          <v-btn color="primary" :loading="submitting" @click="submitOrder">Review Order</v-btn>
        </v-col>
      </v-row>
    </template>

    <!-- Step 2: Order review + confirm -->
    <template v-else-if="step === 'review'">
      <h2 class="text-h5 mb-4">Order Review</h2>

      <v-card class="mb-4">
        <v-card-title>Shipping To</v-card-title>
        <v-card-text>
          <p>{{ review.address.street }}</p>
          <p>{{ review.address.postalCode }} {{ review.address.city }}</p>
          <p>{{ review.address.country }}</p>
        </v-card-text>
      </v-card>

      <v-table class="mb-4">
        <thead>
          <tr>
            <th>Item</th>
            <th class="text-center">Qty</th>
            <th class="text-right">Unit Price</th>
            <th class="text-right">Subtotal</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(line, i) in review.lines" :key="i">
            <td>{{ line.name }}</td>
            <td class="text-center">{{ line.quantity }}</td>
            <td class="text-right">{{ formatPrice(line.price, line.currency) }}</td>
            <td class="text-right">{{ formatPrice(line.subtotal, line.currency) }}</td>
          </tr>
        </tbody>
      </v-table>

      <v-row justify="end" class="mb-4">
        <v-col cols="auto">
          <span class="text-h6">Total: {{ formatPrice(review.total, review.lines[0]?.currency ?? 'EUR') }}</span>
        </v-col>
      </v-row>

      <v-alert v-if="error" type="error" density="compact" closable class="mb-4"
        @click:close="error = null">{{ error }}</v-alert>

      <v-row justify="space-between">
        <v-col cols="auto">
          <v-btn variant="text" prepend-icon="mdi-arrow-left" @click="step = 'address'">Edit Address</v-btn>
        </v-col>
        <v-col cols="auto">
          <v-btn color="success" :loading="confirming" prepend-icon="mdi-check" @click="confirmOrder">
            Confirm Order
          </v-btn>
        </v-col>
      </v-row>
    </template>

    <!-- Step 3: Success -->
    <template v-else-if="step === 'done'">
      <v-row justify="center" class="mt-8">
        <v-col cols="12" sm="6" class="text-center">
          <v-icon size="64" color="success" class="mb-4">mdi-check-circle-outline</v-icon>
          <h2 class="text-h5 mb-2">Order Confirmed!</h2>
          <p class="text-medium-emphasis mb-6">A confirmation email has been sent.</p>
          <v-btn color="primary" @click="$emit('confirmed')">Continue Shopping</v-btn>
        </v-col>
      </v-row>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { api } from '../api/client.js'

defineEmits(['confirmed', 'back'])

const step = ref('address')
const submitting = ref(false)
const confirming = ref(false)
const error = ref(null)
const validationErrors = ref({})
const review = ref(null)

const address = reactive({
  street: '',
  city: '',
  postalCode: '',
  country: '',
})

function fieldError(field) {
  return validationErrors.value[field] ?? []
}

async function submitOrder() {
  error.value = null
  validationErrors.value = {}
  submitting.value = true
  try {
    review.value = await api('/order/submit', {
      method: 'POST',
      body: { ...address },
    })
    step.value = 'review'
  } catch (e) {
    if (e.status === 400 && e.body?.errors) {
      // Bean Validation errors from ProblemDetail
      const errs = {}
      for (const err of e.body.errors) {
        const field = err.pointer?.replace('#/', '') ?? err.field ?? 'general'
        errs[field] = errs[field] ?? []
        errs[field].push(err.detail ?? err.defaultMessage ?? 'Invalid value')
      }
      validationErrors.value = errs
    } else {
      error.value = e.body?.detail ?? 'Could not submit order.'
    }
  } finally {
    submitting.value = false
  }
}

async function confirmOrder() {
  error.value = null
  confirming.value = true
  try {
    await api('/order/confirm', { method: 'POST' })
    step.value = 'done'
  } catch (e) {
    error.value = e.body?.detail ?? 'Could not confirm order.'
  } finally {
    confirming.value = false
  }
}

function formatPrice(price, currency) {
  return new Intl.NumberFormat('el-GR', { style: 'currency', currency }).format(price)
}
</script>
