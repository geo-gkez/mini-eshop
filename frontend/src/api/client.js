function csrfToken() {
  const cookies = document.cookie ? document.cookie.split(/;\s*/) : []
  for (const c of cookies) {
    const eq = c.indexOf('=')
    if (eq !== -1 && c.substring(0, eq) === 'XSRF-TOKEN') {
      return c.substring(eq + 1)
    }
  }
  return undefined
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Accept': 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  if (method !== 'GET' && method !== 'HEAD') {
    const token = csrfToken()
    if (!token) {
      const err = new Error('CSRF token missing. Please refresh and try again.')
      err.name = 'ApiError'
      err.status = 0
      throw err
    }
    headers['X-XSRF-TOKEN'] = token
  }

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    credentials: 'same-origin',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  const data = res.status === 204 ? null : await res.json().catch(() => null)

  if (!res.ok) {
    const err = new Error(`${method} /api${path} → ${res.status}`)
    err.name = 'ApiError'
    err.status = res.status
    err.body = data
    throw err
  }
  return data
}
