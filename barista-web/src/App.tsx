import { useCallback, useEffect, useRef, useState } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { getFailureReason, isNetworkError, validateBackupCode, validateQrToken } from './api/baristaApi'
import { DEVICE_UNAUTHORIZED_EVENT } from './api/apiClient'
import { clearDeviceSession, getDeviceSession, storeDeviceSession } from './auth/deviceSession'
import { Brand } from './components/Brand'
import { PinGate } from './components/PinGate'
import { ResultPanel } from './components/ResultPanel'
import { ScannerPanel } from './components/ScannerPanel'
import { TodayPanel } from './components/TodayPanel'
import type { DeviceSession, ValidationResult } from './types'
import './App.css'

function App() {
  return (
    <Routes>
      <Route element={<CafePage />} path="/cafe/:cafeId" />
      <Route element={<Landing />} path="*" />
    </Routes>
  )
}

function Landing() {
  return (
    <main className="centered-page">
      <section className="invalid-card">
        <Brand />
        <h1>Open your cafe check-in link</h1>
        <p>Use a cafe-specific address such as <code>/cafe/1</code>.</p>
      </section>
    </main>
  )
}

function CafePage() {
  const { cafeId: cafeIdParam } = useParams()
  const cafeId = Number(cafeIdParam)
  const validCafeId = Number.isSafeInteger(cafeId) && cafeId > 0
  const [deviceSession, setDeviceSession] = useState<DeviceSession | null>(() => {
    const stored = getDeviceSession()
    if (stored && stored.cafeId !== cafeId) clearDeviceSession()
    return stored?.cafeId === cafeId ? stored : null
  })
  const [activeTab, setActiveTab] = useState<'scan' | 'today'>('scan')
  const [result, setResult] = useState<ValidationResult | null>(null)
  const [isValidating, setIsValidating] = useState(false)
  const validationInFlight = useRef(false)

  useEffect(() => {
    const handleUnauthorized = () => {
      setDeviceSession(null)
      setResult(null)
      setActiveTab('scan')
    }
    window.addEventListener(DEVICE_UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => window.removeEventListener(DEVICE_UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [])

  const handleAuthenticated = (session: DeviceSession) => {
    storeDeviceSession(session)
    setDeviceSession(session)
  }

  const validate = useCallback(async (kind: 'qr' | 'backup', value: string) => {
    if (validationInFlight.current) return
    validationInFlight.current = true
    setIsValidating(true)
    try {
      const data = kind === 'qr' ? await validateQrToken(value) : await validateBackupCode(value)
      setResult({ kind: 'success', data })
    } catch (error) {
      setResult(isNetworkError(error)
        ? { kind: 'network-error' }
        : { kind: 'failure', reason: getFailureReason(error) })
    } finally {
      validationInFlight.current = false
      setIsValidating(false)
    }
  }, [])

  const closeDevice = () => {
    clearDeviceSession()
    setDeviceSession(null)
    setResult(null)
    setActiveTab('scan')
  }

  if (!validCafeId) return <Navigate replace to="/" />

  if (!deviceSession) {
    return <PinGate cafeId={cafeId} onAuthenticated={handleAuthenticated} />
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <Brand compact />
          <h1>{deviceSession.cafeName}</h1>
        </div>
        <button className="text-button" onClick={closeDevice} type="button">End device session</button>
      </header>

      <nav aria-label="Barista tools" className="tabs">
        <button aria-selected={activeTab === 'scan'} className={activeTab === 'scan' ? 'active' : ''} onClick={() => { setActiveTab('scan'); setResult(null) }} role="tab" type="button">Scan</button>
        <button aria-selected={activeTab === 'today'} className={activeTab === 'today' ? 'active' : ''} onClick={() => setActiveTab('today')} role="tab" type="button">Today</button>
      </nav>

      <main className="workspace">
        {activeTab === 'today' ? (
          <TodayPanel />
        ) : result ? (
          <ResultPanel result={result} onReset={() => setResult(null)} />
        ) : (
          <ScannerPanel isValidating={isValidating} onValidate={validate} />
        )}
      </main>
    </div>
  )
}

export default App
