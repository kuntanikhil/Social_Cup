import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchRedemptions } from '../api/adminApi'
import { getApiErrorMessage } from '../api/apiClient'
import { Alert, LoadingPanel, PageHeader, StatePanel } from '../components/Ui'
import type { AdminRedemption, PayoutSummary } from '../types'

export function PayoutsPage() {
  const [redemptions, setRedemptions] = useState<AdminRedemption[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const summaries = useMemo(() => summarize(redemptions), [redemptions])

  const load = useCallback(async () => {
    setIsLoading(true); setError(null)
    try { setRedemptions(await fetchRedemptions(200)) }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to load payout visibility.')) }
    finally { setIsLoading(false) }
  }, [])

  useEffect(() => {
    let active = true
    void fetchRedemptions(200).then((data) => { if (active) setRedemptions(data) }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError, 'Unable to load payout visibility.')) }).finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [])

  return (
    <main className="page-content">
      <PageHeader action={<button className="button secondary" disabled={isLoading} onClick={() => void load()} type="button">Refresh</button>} description="Snapshot payout visibility across up to 200 recently loaded redemptions. No transfers are initiated here." title="Payouts" />
      <Alert tone="info">Totals below are calculated only from the recent redemption window returned by the admin API.</Alert>
      {error ? <Alert>{error}</Alert> : null}
      {isLoading && !summaries.length ? <LoadingPanel label="Calculating payout visibility…" /> : null}
      {!isLoading && !summaries.length ? <StatePanel message="Completed redemptions with payout snapshots will appear here." title="No payout data" /> : null}
      {summaries.length ? <section className="content-card table-card"><div className="table-scroll"><table><thead><tr><th>Cafe</th><th>Eligible redemptions</th><th>Credits redeemed</th><th>Payout amount</th></tr></thead><tbody>
        {summaries.map((item) => <tr key={item.cafeId}><td><strong>{item.cafeName}</strong></td><td>{item.eligibleRedemptions}</td><td>{item.creditsRedeemed}</td><td className="money">{formatMoney(item.payoutAmount)}</td></tr>)}
      </tbody></table></div></section> : null}
    </main>
  )
}

function summarize(items: AdminRedemption[]): PayoutSummary[] {
  const byCafe = new Map<number, PayoutSummary & { payoutCents: number }>()
  for (const item of items) {
    if (item.status !== 'COMPLETED') continue
    const existing = byCafe.get(item.cafeId) ?? { cafeId: item.cafeId, cafeName: item.cafeName, eligibleRedemptions: 0, creditsRedeemed: 0, payoutAmount: 0, payoutCents: 0 }
    existing.eligibleRedemptions += 1
    existing.creditsRedeemed += item.creditsSpent
    existing.payoutCents += Math.round(Number(item.payoutAmount) * 100)
    existing.payoutAmount = existing.payoutCents / 100
    byCafe.set(item.cafeId, existing)
  }
  return [...byCafe.values()].sort((a, b) => b.payoutAmount - a.payoutAmount)
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}
