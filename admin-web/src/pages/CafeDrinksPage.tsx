import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchCafes, fetchDrinks, updateDrink } from '../api/adminApi'
import { getApiErrorMessage } from '../api/apiClient'
import { DrinkForm } from '../components/DrinkForm'
import { Alert, LoadingPanel, PageHeader, StatePanel, StatusBadge } from '../components/Ui'
import type { AdminCafe, AdminDrink, DrinkWriteRequest } from '../types'

export function CafeDrinksPage() {
  const cafeId = Number(useParams().cafeId)
  const [cafe, setCafe] = useState<AdminCafe | null>(null)
  const [drinks, setDrinks] = useState<AdminDrink[]>([])
  const [editingDrink, setEditingDrink] = useState<AdminDrink | null | undefined>(undefined)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setIsLoading(true); setError(null)
    try {
      const [cafes, nextDrinks] = await Promise.all([fetchCafes(), fetchDrinks(cafeId)])
      setCafe(cafes.find((item) => item.id === cafeId) ?? null); setDrinks(nextDrinks)
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to load this menu.')) }
    finally { setIsLoading(false) }
  }, [cafeId])

  useEffect(() => {
    let active = true
    void Promise.all([fetchCafes(), fetchDrinks(cafeId)]).then(([cafes, nextDrinks]) => { if (active) { setCafe(cafes.find((item) => item.id === cafeId) ?? null); setDrinks(nextDrinks) } }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError, 'Unable to load this menu.')) }).finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [cafeId])

  const toggle = async (drink: AdminDrink, field: 'active' | 'signature') => {
    if (field === 'active' && drink.active && !window.confirm(`Deactivate ${drink.name}? Members will no longer see it on the active menu.`)) return
    const request: DrinkWriteRequest = {
      name: drink.name, type: drink.type, description: drink.description,
      photoPath: drink.photoPath, retailPrice: Number(drink.retailPrice), creditPrice: drink.creditPrice,
      signature: field === 'signature' ? !drink.signature : drink.signature,
      active: field === 'active' ? !drink.active : drink.active,
    }
    setBusyId(drink.id); setError(null)
    try { const updated = await updateDrink(drink.id, request); setDrinks((items) => items.map((item) => item.id === updated.id ? updated : item)) }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to update drink.')) }
    finally { setBusyId(null) }
  }

  return (
    <main className="page-content">
      <Link className="back-link" to="/cafes">← Back to cafes</Link>
      <PageHeader action={<button className="button primary" disabled={!cafe} onClick={() => setEditingDrink(null)} type="button">Add drink</button>} description="Manage prices, availability, and signature placement." title={cafe ? `${cafe.name} · Drinks` : 'Cafe drinks'} />
      {error ? <Alert>{error}</Alert> : null}
      {isLoading && !drinks.length ? <LoadingPanel label="Loading drinks…" /> : null}
      {!isLoading && !cafe ? <StatePanel message="The selected cafe could not be found." title="Cafe unavailable" /> : null}
      {!isLoading && cafe && !drinks.length ? <StatePanel action={<button className="button primary" onClick={() => setEditingDrink(null)} type="button">Add drink</button>} message="Add the cafe’s first menu item." title="No drinks found" /> : null}
      {drinks.length ? <section className="content-card table-card"><div className="table-scroll"><table><thead><tr><th>Drink</th><th>Type</th><th>Retail</th><th>Credits</th><th>Status</th><th>Signature</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
        {drinks.map((drink) => <tr key={drink.id}><td><strong>{drink.name}</strong>{drink.description ? <small>{drink.description}</small> : null}</td><td>{drink.type.replace('_', ' ')}</td><td className="money">${Number(drink.retailPrice).toFixed(2)}</td><td><strong>{drink.creditPrice}</strong></td><td><button className="bare-button" disabled={busyId === drink.id} onClick={() => void toggle(drink, 'active')} type="button"><StatusBadge active={drink.active} /></button></td><td><button className={`feature-toggle ${drink.signature ? 'on' : ''}`} disabled={busyId === drink.id} onClick={() => void toggle(drink, 'signature')} type="button">{drink.signature ? '★ Signature' : '☆ Standard'}</button></td><td><div className="row-actions"><button onClick={() => setEditingDrink(drink)} type="button">Edit</button></div></td></tr>)}
      </tbody></table></div></section> : null}
      {editingDrink !== undefined && cafe ? <DrinkForm cafeId={cafe.id} drink={editingDrink} onClose={() => setEditingDrink(undefined)} onSaved={() => { setEditingDrink(undefined); void load() }} /> : null}
    </main>
  )
}
