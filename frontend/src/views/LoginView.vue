<template>
  <v-row justify="center" class="mt-8">
    <v-col cols="12" sm="8" md="4">
      <v-card>
        <v-card-title class="pt-4 px-4">Sign In</v-card-title>
        <v-card-text>
          <v-alert v-if="notice" type="info" density="compact" class="mb-4">{{ notice }}</v-alert>
          <v-alert v-if="error" type="error" density="compact" class="mb-4">{{ error }}</v-alert>
          <v-text-field
            v-model="username"
            label="Username"
            autocomplete="username"
            :disabled="loading"
            @keyup.enter="submit"
          />
          <v-text-field
            v-model="password"
            label="Password"
            type="password"
            autocomplete="current-password"
            :disabled="loading"
            @keyup.enter="submit"
          />
        </v-card-text>
        <v-card-actions class="px-4 pb-4">
          <v-spacer />
          <v-btn color="primary" :loading="loading" @click="submit">Login</v-btn>
        </v-card-actions>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup>
import { ref } from 'vue'
import { api } from '../api/client.js'

defineProps({
  notice: { type: String, default: null },
})

const emit = defineEmits(['logged-in'])

const username = ref('')
const password = ref('')
const error = ref(null)
const loading = ref(false)

async function submit() {
  if (!username.value || !password.value) return
  error.value = null
  loading.value = true
  try {
    await api('/auth/login', {
      method: 'POST',
      body: { username: username.value, password: password.value },
    })
    emit('logged-in')
  } catch {
    // Generic message — no information about whether username or password was wrong
    error.value = 'Invalid credentials.'
  } finally {
    loading.value = false
  }
}
</script>
