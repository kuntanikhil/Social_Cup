import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchCafes, fetchNeighbourhoods, updateCafe } from '../api/adminApi'
import { getApiErrorMessage } from '../api/apiClient'
import { CafeForm } from '../components/CafeForm'
import { PinModal } from '../components/PinModal'
import { Alert, LoadingPanel, PageHeader, StatePanel, StatusBadge } from '../components/Ui'
import type { AdminCafe, CafeWriteRequest, Neighbourhood } from '../types'

export function CafesPage() {
  const [cafes, setCafes] = useState<AdminCafe[]>([])
  const [neighbourhoods, setNeighbourhoods] = useState<Neighbourhood[]>([])
  const [editingCafe, setEditingCafe] = useState<AdminCafe | null | undefined>(undefined)
  const [pinCafe, setPinCafe] = useState<AdminCafe | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setIsLoading(true); setError(null)
    try { const [nextCafes, nextNeighbourhoods] = await Promise.all([fetchCafes(), fetchNeighbourhoods()]); setCafes(nextCafes); setNeighbourhoods(nextNeighbourhoods) }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to load cafes.')) }
    finally { setIsLoading(false) }
  }, [])

  useEffect(() => {
    let active = true
    void Promise.all([fetchCafes(), fetchNeighbourhoods()]).then(([nextCafes, nextNeighbourhoods]) => { if (active) { setCafes(nextCafes); setNeighbourhoods(nextNeighbourhoods) } }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError, 'Unable to load cafes.')) }).finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [])

  const toggle = async (cafe: AdminCafe, field: 'active' | 'featured') => {
    const nextValue = !cafe[field]
    if (field === 'active' && !nextValue && !window.confirm(`Deactivate ${cafe.name}? It will disappear from public cafe APIs.`)) return
    setBusyId(cafe.id); setError(null)
    try {
      const request: CafeWriteRequest = {
        name: cafe.name, address: cafe.address, neighbourhoodId: cafe.neighbourhoodId,
        latitude: cafe.latitude, longitude: cafe.longitude, perkLine: cafe.perkLine,
        payoutRatePerCredit: Number(cafe.payoutRatePerCredit), featured: field === 'featured' ? nextValue : cafe.featured,
        active: field === 'active' ? nextValue : cafe.active,
      }
      const updated = await updateCafe(cafe.id, request)
      setCafes((items) => items.map((item) => item.id === updated.id ? updated : item))
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to update cafe.')) }
    finally { setBusyId(null) }
  }

  return (
    <main className="page-content">
      <PageHeader action={<button className="button primary" onClick={() => setEditingCafe(null)} type="button">Add cafe</button>} description="Manage cafe visibility, featured placement, payout rate, menus, and barista access." title="Cafes" />
      {error ? <Alert>{error}</Alert> : null}
      {isLoading && !cafes.length ? <LoadingPanel label="Loading cafes…" /> : null}
      {!isLoading && !cafes.length ? <StatePanel action={<button className="button primary" onClick={() => setEditingCafe(null)} type="button">Add cafe</button>} message="Create the first cafe to begin managing its menu." title="No cafes found" /> : null}
      {cafes.length ? <section className="content-card table-card"><div className="table-scroll"><table><thead><tr><th>Cafe</th><th>Neighbourhood</th><th>Status</th><th>Featured</th><th>Payout / credit</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
        {cafes.map((cafe) => <tr key={cafe.id}><td><strong>{cafe.name}</strong><small>{cafe.address}</small></td><td>{cafe.neighbourhoodName}</td><td><button aria-label={`${cafe.active ? 'Deactivate' : 'Activate'} ${cafe.name}`} className="bare-button" disabled={busyId === cafe.id} onClick={() => void toggle(cafe, 'active')} type="button"><StatusBadge active={cafe.active} /></button></td><td><button aria-label={`${cafe.featured ? 'Remove' : 'Add'} featured status for ${cafe.name}`} className={`feature-toggle ${cafe.featured ? 'on' : ''}`} disabled={busyId === cafe.id} onClick={() => void toggle(cafe, 'featured')} type="button">{cafe.featured ? '★ Featured' : '☆ Standard'}</button></td><td className="money">${Number(cafe.payoutRatePerCredit).toFixed(2)}</td><td><div className="row-actions"><Link className="table-link" to={`/cafes/${cafe.id}`}>Drinks</Link><button onClick={() => setEditingCafe(cafe)} type="button">Edit</button><button onClick={() => setPinCafe(cafe)} type="button">Set PIN</button></div></td></tr>)}
      </tbody></table></div></section> : null}
      {editingCafe !== undefined ? <CafeForm cafe={editingCafe} neighbourhoods={neighbourhoods} onClose={() => setEditingCafe(undefined)} onSaved={() => { setEditingCafe(undefined); void load() }} /> : null}
      {pinCafe ? <PinModal cafe={pinCafe} onClose={() => setPinCafe(null)} /> : null}
    </main>
  )
}
