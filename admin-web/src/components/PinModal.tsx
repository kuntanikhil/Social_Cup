import { useState, type FormEvent } from 'react'
import { updateBaristaPin } from '../api/adminApi'
import { getApiErrorMessage } from '../api/apiClient'
import type { AdminCafe } from '../types'
import { Alert, Modal } from './Ui'

export function PinModal({ cafe, onClose }: { cafe: AdminCafe; onClose: () => void }) {
  const [pin, setPin] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (!/^\d{4,6}$/.test(pin)) return setError('PIN must contain 4 to 6 numeric digits.')
    setIsSubmitting(true); setError(null)
    try {
      await updateBaristaPin(cafe.id, pin)
      setPin(''); setSuccess(true)
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to update the barista PIN.')) }
    finally { setIsSubmitting(false) }
  }

  return (
    <Modal onClose={onClose} title={`Barista PIN · ${cafe.name}`}>
      {success ? (
        <div className="pin-success"><Alert tone="success">Barista PIN updated successfully. Existing trusted devices may need to authenticate again.</Alert><button className="button primary" onClick={onClose} type="button">Done</button></div>
      ) : (
        <form className="admin-form" onSubmit={(event) => void submit(event)}>
          <p className="form-help">Enter a new 4–6 digit numeric PIN. The existing PIN and hash are never displayed.</p>
          {error ? <Alert>{error}</Alert> : null}
          <label><span>New PIN</span><input autoComplete="new-password" autoFocus className="pin-input" inputMode="numeric" maxLength={6} onChange={(event) => setPin(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="••••" type="password" value={pin} /></label>
          <div className="modal-actions"><button className="button secondary" onClick={onClose} type="button">Cancel</button><button className="button primary" disabled={isSubmitting || pin.length < 4} type="submit">{isSubmitting ? 'Updating…' : 'Update PIN'}</button></div>
        </form>
      )}
    </Modal>
  )
}
