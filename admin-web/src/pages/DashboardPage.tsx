import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchCafes, fetchRedemptions } from '../api/adminApi'
import { getApiErrorMessage } from '../api/apiClient'
import { Alert, LoadingPanel, PageHeader, StatePanel, StatusBadge } from '../components/Ui'
import type { AdminCafe, AdminRedemption } from '../types'

type DashboardData = { cafes: AdminCafe[]; redemptions: AdminRedemption[] }

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    void Promise.all([fetchCafes(), fetchRedemptions(100)])
      .then(([cafes, redemptions]) => {
        if (active) setData({ cafes, redemptions })
      })
      .catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError, 'Unable to load the dashboard.')) })
    return () => { active = false }
  }, [])

  return (
    <main className="page-content">
      <PageHeader description="Current operational data from Social Cup." title="Dashboard" />
      {error ? <Alert>{error}</Alert> : null}
      {!data && !error ? <LoadingPanel label="Loading dashboard…" /> : null}
      {data ? (
        <>
          <section className="metric-grid">
            <MetricCard label="Total cafes" value={data.cafes.length} detail="All configured cafes" to="/cafes" />
            <MetricCard label="Active cafes" value={data.cafes.filter((cafe) => cafe.active).length} detail="Visible to members" to="/cafes" />
            <MetricCard label="Recent redemptions" value={data.redemptions.length} detail="Latest 100 window" to="/redemptions" />
            <MetricCard label="Payout snapshots" value={formatMoney(data.redemptions.reduce((sum, item) => sum + Number(item.payoutAmount), 0))} detail="Latest 100 redemptions" to="/payouts" />
          </section>

          <section className="content-card">
            <div className="card-heading"><div><h2>Recent redemptions</h2><p>Latest successful member check-ins.</p></div><Link to="/redemptions">View all</Link></div>
            {data.redemptions.length ? (
              <div className="table-scroll"><table><thead><tr><th>Time</th><th>Member</th><th>Cafe</th><th>Drink</th><th>Credits</th><th>Status</th></tr></thead><tbody>
                {data.redemptions.slice(0, 10).map((item) => <tr key={item.redemptionId}><td>{formatDate(item.redeemedAt)}</td><td>{item.memberName}</td><td>{item.cafeName}</td><td>{item.drinkName}</td><td>{item.creditsSpent}</td><td><StatusBadge active activeLabel="Completed" /></td></tr>)}
              </tbody></table></div>
            ) : <StatePanel message="Completed redemptions will appear here." title="No redemptions yet" />}
          </section>
        </>
      ) : null}
    </main>
  )
}

function MetricCard({ label, value, detail, to }: { label: string; value: number | string; detail: string; to: string }) {
  return <Link className="metric-card" to={to}><span>{label}</span><strong>{value}</strong><small>{detail}</small></Link>
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

function formatDate(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}
