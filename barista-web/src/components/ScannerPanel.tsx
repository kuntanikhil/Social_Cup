import QrScanner from 'qr-scanner'
import { useEffect, useRef, useState, type FormEvent } from 'react'

type ScannerPanelProps = {
  isValidating: boolean
  onValidate: (kind: 'qr' | 'backup', value: string) => Promise<void>
}

export function ScannerPanel({ isValidating, onValidate }: ScannerPanelProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const scannerRef = useRef<QrScanner | null>(null)
  const scanLocked = useRef(false)
  const [cameraState, setCameraState] = useState<'starting' | 'ready' | 'unavailable'>('starting')
  const [backupCode, setBackupCode] = useState('')
  const [manualError, setManualError] = useState<string | null>(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return
    let disposed = false

    const scanner = new QrScanner(
      video,
      (scanResult) => {
        if (scanLocked.current || !scanResult.data) return
        scanLocked.current = true
        scanner.stop()
        void onValidate('qr', scanResult.data)
      },
      {
        preferredCamera: 'environment',
        highlightCodeOutline: true,
        highlightScanRegion: true,
        maxScansPerSecond: 5,
        returnDetailedScanResult: true,
      },
    )
    scannerRef.current = scanner

    void QrScanner.hasCamera()
      .then((hasCamera) => {
        if (!hasCamera) throw new Error('No camera')
        return scanner.start()
      })
      .then(() => {
        if (!disposed) setCameraState('ready')
      })
      .catch(() => {
        if (!disposed) setCameraState('unavailable')
      })

    return () => {
      disposed = true
      scanner.stop()
      scanner.destroy()
      scannerRef.current = null
    }
  }, [onValidate])

  useEffect(() => {
    if (isValidating) scannerRef.current?.stop()
  }, [isValidating])

  const submitBackupCode = (event: FormEvent) => {
    event.preventDefault()
    if (!/^\d{6}$/.test(backupCode)) {
      setManualError('Enter exactly six digits.')
      return
    }
    setManualError(null)
    const code = backupCode
    setBackupCode('')
    void onValidate('backup', code)
  }

  return (
    <section className="scan-layout">
      <div className="panel scanner-panel">
        <div className="section-heading">
          <div><p className="eyebrow">Camera</p><h2>Scan member QR</h2></div>
          {cameraState === 'ready' ? <span className="status-chip live"><i /> Camera ready</span> : null}
        </div>
        <div className={`camera-frame ${cameraState}`}>
          <video aria-label="QR scanner camera preview" muted playsInline ref={videoRef} />
          {cameraState === 'starting' ? <div className="camera-message"><span className="spinner" />Starting camera…</div> : null}
          {cameraState === 'unavailable' ? <div className="camera-message"><strong>Camera unavailable.</strong><span>Enter the 6-digit code instead.</span></div> : null}
          {isValidating ? <div className="camera-message validating"><span className="spinner" />Validating code…</div> : null}
          {cameraState === 'ready' && !isValidating ? <div aria-hidden="true" className="scan-guide" /> : null}
        </div>
        <p className="helper-text">Hold the member’s QR code inside the frame. Validation starts automatically.</p>
      </div>

      <div className="panel manual-panel">
        <div className="section-heading"><div><p className="eyebrow">Fallback</p><h2>Enter backup code</h2></div></div>
        <p className="helper-text">Use the six digits shown below the member’s QR code.</p>
        <form onSubmit={submitBackupCode}>
          <label htmlFor="backup-code">6-digit code</label>
          <input
            autoComplete="one-time-code"
            className="code-input"
            id="backup-code"
            inputMode="numeric"
            maxLength={6}
            onChange={(event) => setBackupCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
            pattern="[0-9]*"
            placeholder="000000"
            value={backupCode}
          />
          {manualError ? <p aria-live="polite" className="form-error" role="alert">{manualError}</p> : null}
          <button className="primary-button" disabled={isValidating || backupCode.length !== 6} type="submit">
            {isValidating ? 'Validating…' : 'Validate Code'}
          </button>
        </form>
      </div>
    </section>
  )
}
