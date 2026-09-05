import { useState, type FormEvent } from 'react'
import { createCafe, updateCafe } from '../api/adminApi'
import { getApiErrorMessage } from '../api/apiClient'
import type { AdminCafe, CafeWriteRequest, Neighbourhood } from '../types'
import { Alert, Modal } from './Ui'

export function CafeForm({ cafe, neighbourhoods, onClose, onSaved }: { cafe: AdminCafe | null; neighbourhoods: Neighbourhood[]; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState(cafe?.name ?? '')
  const [address, setAddress] = useState(cafe?.address ?? '')
  const [neighbourhoodId, setNeighbourhoodId] = useState(String(cafe?.neighbourhoodId ?? neighbourhoods[0]?.id ?? ''))
  const [latitude, setLatitude] = useState(cafe?.latitude?.toString() ?? '')
  const [longitude, setLongitude] = useState(cafe?.longitude?.toString() ?? '')
  const [perkLine, setPerkLine] = useState(cafe?.perkLine ?? '')
  const [payoutRate, setPayoutRate] = useState(String(cafe?.payoutRatePerCredit ?? 0))
  const [featured, setFeatured] = useState(cafe?.featured ?? false)
  const [active, setActive] = useState(cafe?.active ?? true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const lat = optionalNumber(latitude)
    const lng = optionalNumber(longitude)
    const payout = Number(payoutRate)
    if (!name.trim() || !address.trim() || !Number(neighbourhoodId)) return setError('Name, address, and neighbourhood are required.')
    if (latitude.trim() && lat === null) return setError('Latitude must be a valid number.')
    if (longitude.trim() && lng === null) return setError('Longitude must be a valid number.')
    if (lat !== null && (lat < -90 || lat > 90)) return setError('Latitude must be between -90 and 90.')
    if (lng !== null && (lng < -180 || lng > 180)) return setError('Longitude must be between -180 and 180.')
    if (!Number.isFinite(payout) || payout < 0) return setError('Payout rate must be zero or greater.')

    const request: CafeWriteRequest = {
      name: name.trim(), address: address.trim(), neighbourhoodId: Number(neighbourhoodId),
      latitude: lat, longitude: lng, perkLine: perkLine.trim() || null,
      payoutRatePerCredit: payout, featured, ...(cafe ? { active } : {}),
    }
    setIsSubmitting(true); setError(null)
    try {
      if (cafe) await updateCafe(cafe.id, request)
      else await createCafe(request)
      onSaved()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, `Unable to ${cafe ? 'update' : 'create'} cafe.`))
    } finally { setIsSubmitting(false) }
  }

  return (
    <Modal onClose={onClose} title={cafe ? 'Edit cafe' : 'Add cafe'}>
      <form className="admin-form" onSubmit={(event) => void submit(event)}>
        {error ? <Alert>{error}</Alert> : null}
        <div className="form-grid two">
          <label><span>Cafe name</span><input maxLength={255} onChange={(event) => setName(event.target.value)} required value={name} /></label>
          <label><span>Neighbourhood</span><select onChange={(event) => setNeighbourhoodId(event.target.value)} required value={neighbourhoodId}><option disabled value="">Select neighbourhood</option>{neighbourhoods.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
        </div>
        <label><span>Address</span><input maxLength={500} onChange={(event) => setAddress(event.target.value)} required value={address} /></label>
        <label><span>Perk line</span><textarea maxLength={500} onChange={(event) => setPerkLine(event.target.value)} rows={3} value={perkLine} /></label>
        <div className="form-grid three">
          <label><span>Latitude</span><input inputMode="decimal" onChange={(event) => setLatitude(event.target.value)} placeholder="Optional" value={latitude} /></label>
          <label><span>Longitude</span><input inputMode="decimal" onChange={(event) => setLongitude(event.target.value)} placeholder="Optional" value={longitude} /></label>
          <label><span>Payout / credit</span><input inputMode="decimal" min="0" onChange={(event) => setPayoutRate(event.target.value)} required step="0.01" type="number" value={payoutRate} /></label>
        </div>
        <div className="checkbox-row">
          <label><input checked={featured} onChange={(event) => setFeatured(event.target.checked)} type="checkbox" />Featured cafe</label>
          {cafe ? <label><input checked={active} onChange={(event) => setActive(event.target.checked)} type="checkbox" />Active</label> : null}
        </div>
        <div className="modal-actions"><button className="button secondary" onClick={onClose} type="button">Cancel</button><button className="button primary" disabled={isSubmitting} type="submit">{isSubmitting ? 'Saving…' : 'Save cafe'}</button></div>
      </form>
    </Modal>
  )
}

function optionalNumber(value: string): number | null {
  if (!value.trim()) return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}
