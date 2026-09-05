import { useCallback, useEffect, useState } from 'react'
import { fetchTodayRedemptions, isNetworkError } from '../api/baristaApi'
import type { TodayRedemption } from '../types'

const DALLAS_TIME = new Intl.DateTimeFormat('en-US', {
  hour: 'numeric',
  minute: '2-digit',
  timeZone: 'America/Chicago',
})

export function TodayPanel() {
  const [redemptions, setRedemptions] = useState<TodayRedemption[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      setRedemptions(await fetchTodayRedemptions())
    } catch (requestError) {
      setError(
        isNetworkError(requestError)
          ? 'Unable to reach Social Cup. Check the backend connection.'
          : 'Today’s redemptions could not be loaded.',
      )
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    void fetchTodayRedemptions()
      .then((items) => {
        if (active) setRedemptions(items)
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(
            isNetworkError(requestError)
              ? 'Unable to reach Social Cup. Check the backend connection.'
              : 'Today’s redemptions could not be loaded.',
          )
        }
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return (
    <section className="today-panel">
      <div className="today-header">
        <div><p className="eyebrow">Dallas cafe date</p><h2>Today’s redemptions</h2></div>
        <button className="secondary-button" disabled={isLoading} onClick={() => void load()} type="button">{isLoading ? 'Refreshing…' : 'Refresh'}</button>
      </div>

      {error ? <div aria-live="polite" className="inline-alert" role="alert">{error}</div> : null}
      {isLoading && redemptions.length === 0 ? (
        <div className="loading-state"><span className="spinner dark" /><span>Loading today’s redemptions…</span></div>
      ) : redemptions.length === 0 ? (
        <div className="empty-state"><span aria-hidden="true">☕</span><strong>No redemptions yet</strong><p>Successful redemptions for this cafe will appear here.</p></div>
      ) : (
        <div className="redemption-list">
          {redemptions.map((redemption) => (
            <article className="redemption-row" key={redemption.redemptionId}>
              <time dateTime={redemption.redeemedAt}>{formatTime(redemption.redeemedAt)}</time>
              <div className="redemption-person"><strong>{redemption.memberFirstName || 'Member'}</strong><span>{redemption.drinkName}</span></div>
              <strong className="redemption-credits">{redemption.creditsSpent} credits</strong>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function formatTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : DALLAS_TIME.format(date)
}
