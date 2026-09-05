import { useState, type FormEvent } from 'react'
import { createDrink, updateDrink } from '../api/adminApi'
import { getApiErrorMessage } from '../api/apiClient'
import type { AdminDrink, DrinkType, DrinkWriteRequest } from '../types'
import { Alert, Modal } from './Ui'

const DRINK_TYPES: DrinkType[] = ['MATCHA', 'ESPRESSO', 'COLD_BREW', 'LATTE']

export function DrinkForm({ cafeId, drink, onClose, onSaved }: { cafeId: number; drink: AdminDrink | null; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState(drink?.name ?? '')
  const [type, setType] = useState<DrinkType>(drink?.type ?? 'LATTE')
  const [description, setDescription] = useState(drink?.description ?? '')
  const [photoPath, setPhotoPath] = useState(drink?.photoPath ?? '')
  const [retailPrice, setRetailPrice] = useState(String(drink?.retailPrice ?? ''))
  const [creditPrice, setCreditPrice] = useState(String(drink?.creditPrice ?? ''))
  const [signature, setSignature] = useState(drink?.signature ?? false)
  const [active, setActive] = useState(drink?.active ?? true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const retail = Number(retailPrice)
    const credits = Number(creditPrice)
    if (!name.trim()) return setError('Drink name is required.')
    if (!Number.isFinite(retail) || retail < 0) return setError('Retail price must be zero or greater.')
    if (!Number.isInteger(credits) || credits <= 0) return setError('Credit price must be a positive whole number.')

    const request: DrinkWriteRequest = {
      name: name.trim(), type, description: description.trim() || null,
      photoPath: photoPath.trim() || null, retailPrice: retail,
      creditPrice: credits, signature, ...(drink ? { active } : {}),
    }
    setIsSubmitting(true); setError(null)
    try {
      if (drink) await updateDrink(drink.id, request)
      else await createDrink(cafeId, request)
      onSaved()
    } catch (requestError) { setError(getApiErrorMessage(requestError, `Unable to ${drink ? 'update' : 'create'} drink.`)) }
    finally { setIsSubmitting(false) }
  }

  return (
    <Modal onClose={onClose} title={drink ? 'Edit drink' : 'Add drink'}>
      <form className="admin-form" onSubmit={(event) => void submit(event)}>
        {error ? <Alert>{error}</Alert> : null}
        <div className="form-grid two">
          <label><span>Drink name</span><input maxLength={255} onChange={(event) => setName(event.target.value)} required value={name} /></label>
          <label><span>Type</span><select onChange={(event) => setType(event.target.value as DrinkType)} value={type}>{DRINK_TYPES.map((item) => <option key={item} value={item}>{item.replace('_', ' ')}</option>)}</select></label>
        </div>
        <label><span>Description</span><textarea onChange={(event) => setDescription(event.target.value)} rows={3} value={description} /></label>
        <label><span>Photo path</span><input maxLength={500} onChange={(event) => setPhotoPath(event.target.value)} placeholder="Optional URL or storage path" value={photoPath} /></label>
        <div className="form-grid two">
          <label><span>Retail price</span><input inputMode="decimal" min="0" onChange={(event) => setRetailPrice(event.target.value)} required step="0.01" type="number" value={retailPrice} /></label>
          <label><span>Credit price</span><input inputMode="numeric" min="1" onChange={(event) => setCreditPrice(event.target.value)} required step="1" type="number" value={creditPrice} /></label>
        </div>
        <div className="checkbox-row">
          <label><input checked={signature} onChange={(event) => setSignature(event.target.checked)} type="checkbox" />Signature drink</label>
          {drink ? <label><input checked={active} onChange={(event) => setActive(event.target.checked)} type="checkbox" />Active</label> : null}
        </div>
        <div className="modal-actions"><button className="button secondary" onClick={onClose} type="button">Cancel</button><button className="button primary" disabled={isSubmitting} type="submit">{isSubmitting ? 'Saving…' : 'Save drink'}</button></div>
      </form>
    </Modal>
  )
}
