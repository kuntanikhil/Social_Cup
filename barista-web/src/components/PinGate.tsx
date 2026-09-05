import { useState, type FormEvent } from 'react'
import { authenticateDevice, isNetworkError } from '../api/baristaApi'
import type { DeviceSession } from '../types'
import { Brand } from './Brand'

type PinGateProps = {
  cafeId: number
  onAuthenticated: (session: DeviceSession) => void
}

export function PinGate({ cafeId, onAuthenticated }: PinGateProps) {
  const [pin, setPin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (!/^\d{4,6}$/.test(pin)) {
      setError('Enter the 4 to 6 digit cafe PIN.')
      return
    }

    setIsSubmitting(true)
    setError(null)
    try {
      const response = await authenticateDevice(cafeId, pin)
      setPin('')
      onAuthenticated(response)
    } catch (requestError) {
      setPin('')
      setError(
        isNetworkError(requestError)
          ? 'Unable to reach Social Cup. Check the backend and connection.'
          : 'PIN not recognised. Check the cafe PIN and try again.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="centered-page pin-page">
      <section className="pin-card">
        <Brand />
        <div className="pin-copy">
          <p className="eyebrow">Cafe {cafeId}</p>
          <h1>Ready for the next cup?</h1>
          <p>Enter this cafe’s PIN to create a trusted device session.</p>
        </div>
        <form onSubmit={(event) => void submit(event)}>
          <label htmlFor="cafe-pin">Cafe PIN</label>
          <input
            autoComplete="off"
            autoFocus
            id="cafe-pin"
            inputMode="numeric"
            maxLength={6}
            onChange={(event) => setPin(event.target.value.replace(/\D/g, '').slice(0, 6))}
            pattern="[0-9]*"
            placeholder="••••"
            type="password"
            value={pin}
          />
          {error ? <p aria-live="polite" className="form-error" role="alert">{error}</p> : null}
          <button className="primary-button" disabled={isSubmitting || pin.length < 4} type="submit">
            {isSubmitting ? 'Opening scanner…' : 'Open Scanner'}
          </button>
        </form>
        <p className="security-note">The PIN is verified securely and is never stored in this browser.</p>
      </section>
    </main>
  )
}
