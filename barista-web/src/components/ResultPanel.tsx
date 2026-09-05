import type { BaristaFailureReason, ValidationResult } from '../types'
import { resolveMediaUrl } from '../utils/media'

const FAILURE_MESSAGES: Record<BaristaFailureReason, { title: string; detail: string }> = {
  INVALID_CODE: { title: 'Code not recognised', detail: 'Check the member’s code and try again.' },
  CODE_EXPIRED: { title: 'This code has expired', detail: 'Ask the member to generate a new redemption code.' },
  CODE_ALREADY_USED: { title: 'This code was already redeemed', detail: 'No additional credits were deducted.' },
  WRONG_CAFE: { title: 'Code belongs to another cafe', detail: 'This device can redeem only for its authenticated cafe.' },
  MEMBERSHIP_INACTIVE: { title: 'Membership is inactive', detail: 'The member needs an active paid membership.' },
  INSUFFICIENT_CREDITS: { title: 'Not enough credits', detail: 'The member does not have enough credits for this drink.' },
  DEVICE_UNAUTHORIZED: { title: 'Device authorization expired', detail: 'Enter the cafe PIN to authorize this device again.' },
}

export function ResultPanel({ result, onReset }: { result: ValidationResult; onReset: () => void }) {
  if (result.kind === 'success') {
    const { data } = result
    const photo = resolveMediaUrl(data.member.profilePhoto)
    return (
      <section aria-live="assertive" className="result-panel success" role="status">
        <div aria-hidden="true" className="result-icon">✓</div>
        <div><p className="result-kicker">Validation successful</p><h2>Redeemed</h2></div>
        <div className="member-summary">
          {photo ? <img alt={`${data.member.firstName} profile`} src={photo} /> : <span aria-hidden="true" className="member-avatar">{data.member.firstName.charAt(0).toUpperCase() || 'M'}</span>}
          <div><strong>{data.member.firstName || 'Member'}</strong><span>{data.drink.name}</span></div>
        </div>
        <div className="credit-result">
          <div><span>Deducted</span><strong>{data.creditsDeducted} credits</strong></div>
          <div><span>Remaining</span><strong>{data.creditsRemaining} credits</strong></div>
        </div>
        <button className="result-button" onClick={onReset} type="button">Scan Next</button>
      </section>
    )
  }

  const message = result.kind === 'network-error'
    ? { title: 'Social Cup is unavailable', detail: 'Check the backend connection, then try this code again.' }
    : FAILURE_MESSAGES[result.reason]

  return (
    <section aria-live="assertive" className="result-panel failure" role="alert">
      <div aria-hidden="true" className="result-icon">×</div>
      <div><p className="result-kicker">Redemption not completed</p><h2>{message.title}</h2></div>
      <p className="result-detail">{message.detail}</p>
      <button className="result-button" onClick={onReset} type="button">Try Again</button>
    </section>
  )
}
