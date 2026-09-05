import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { AdminLayout } from './components/AdminLayout'
import { CafeDrinksPage } from './pages/CafeDrinksPage'
import { CafesPage } from './pages/CafesPage'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { PayoutsPage } from './pages/PayoutsPage'
import { RedemptionsPage } from './pages/RedemptionsPage'
import './App.css'

function App() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return <main className="app-loading" aria-live="polite"><span className="spinner" />Verifying administrator access…</main>
  }

  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate replace to="/" /> : <LoginPage />} />
      <Route element={isAuthenticated ? <AdminLayout /> : <Navigate replace to="/login" />}>
        <Route index element={<DashboardPage />} />
        <Route path="cafes" element={<CafesPage />} />
        <Route path="cafes/:cafeId" element={<CafeDrinksPage />} />
        <Route path="cafes/:cafeId/drinks" element={<CafeDrinksPage />} />
        <Route path="redemptions" element={<RedemptionsPage />} />
        <Route path="payouts" element={<PayoutsPage />} />
      </Route>
      <Route path="*" element={<Navigate replace to={isAuthenticated ? '/' : '/login'} />} />
    </Routes>
  )
}

export default App
