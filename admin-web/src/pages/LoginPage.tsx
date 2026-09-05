import axios from 'axios'
import { useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../api/apiClient'
import { ADMIN_ACCESS_MESSAGE, useAuth } from '../auth/AuthContext'

export function LoginPage() {
  const { login, authMessage, clearAuthMessage } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setIsSubmitting(true)
    setError(null)
    clearAuthMessage()
    try {
      await login(email.trim(), password)
    } catch (requestError) {
      setError(
        requestError instanceof Error && requestError.message === ADMIN_ACCESS_MESSAGE
          ? ADMIN_ACCESS_MESSAGE
          : axios.isAxiosError(requestError) && requestError.response?.status === 401 && requestError.config?.url === '/api/auth/login'
            ? 'Email or password is incorrect.'
            : getApiErrorMessage(requestError, 'Unable to sign in.'),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <div className="login-brand"><span>SC</span><div><strong>Social Cup Admin</strong><small>Operations console</small></div></div>
        <div className="login-copy"><h1>Welcome back</h1><p>Sign in with an existing Social Cup account.</p></div>
        <form onSubmit={(event) => void submit(event)}>
          <label htmlFor="email">Email</label>
          <input autoComplete="email" id="email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
          <label htmlFor="password">Password</label>
          <input autoComplete="current-password" id="password" minLength={8} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
          {error || authMessage ? <div aria-live="polite" className="alert error" role="alert">{error ?? authMessage}</div> : null}
          <button className="button primary wide" disabled={isSubmitting || !email || password.length < 8} type="submit">{isSubmitting ? 'Signing in…' : 'Sign in'}</button>
        </form>
        <p className="login-note">Use your existing Social Cup account. Administrator role is verified by the backend.</p>
      </section>
    </main>
  )
}
