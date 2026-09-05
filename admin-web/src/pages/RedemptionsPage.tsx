import { useCallback, useEffect, useState } from 'react'
import { fetchRedemptions } from '../api/adminApi'
import { getApiErrorMessage } from '../api/apiClient'
import { Alert, LoadingPanel, PageHeader, StatePanel, StatusBadge } from '../components/Ui'
import type { AdminRedemption } from '../types'

export function RedemptionsPage() {
  const [items, setItems] = useState<AdminRedemption[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setIsLoading(true); setError(null)
    try { setItems(await fetchRedemptions(100)) }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to load redemptions.')) }
    finally { setIsLoading(false) }
  }, [])

  useEffect(() => {
    let active = true
    void fetchRedemptions(100).then((data) => { if (active) setItems(data) }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError, 'Unable to load redemptions.')) }).finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [])

  return (
    <main className="page-content">
      <PageHeader action={<button className="button secondary" disabled={isLoading} onClick={() => void load()} type="button">Refresh</button>} description="Up to 100 most recent completed redemptions." title="Redemptions" />
      {error ? <Alert>{error}</Alert> : null}
      {isLoading && !items.length ? <LoadingPanel label="Loading redemptions…" /> : null}
      {!isLoading && !items.length ? <StatePanel message="Successful barista validations will appear here." title="No redemptions found" /> : null}
      {items.length ? <section className="content-card table-card"><div className="table-scroll"><table><thead><tr><th>Date & time</th><th>Member</th><th>Cafe</th><th>Drink</th><th>Credits</th><th>Status</th></tr></thead><tbody>
        {items.map((item) => <tr key={item.redemptionId}><td>{formatDate(item.redeemedAt)}</td><td>{item.memberName}</td><td>{item.cafeName}</td><td>{item.drinkName}</td><td>{item.creditsSpent}</td><td><StatusBadge active activeLabel="Completed" /></td></tr>)}
      </tbody></table></div></section> : null}
    </main>
  )
}

function formatDate(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}
