import { createApp } from 'vue'
import App from './App.vue'
import vuetify from './plugins/vuetify'

const app = createApp(App)

// Last-resort net for unexpected render/lifecycle errors so a single throw
// doesn't blank the page silently.
app.config.errorHandler = (err, _instance, info) => {
  console.error(`[app] unhandled error (${info}):`, err)
}

app.use(vuetify).mount('#app')
