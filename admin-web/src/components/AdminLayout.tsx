import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Alert } from './Ui'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: '▦' },
  { to: '/cafes', label: 'Cafes', icon: '⌂' },
  { to: '/redemptions', label: 'Redemptions', icon: '✓' },
  { to: '/payouts', label: 'Payouts', icon: '$' },
]

export function AdminLayout() {
  const { user, logout, authorizationMessage, clearAuthorizationMessage } = useAuth()
  return (
    <div className="admin-shell">
      <aside className="sidebar">
        <div className="brand"><span>SC</span><div><strong>Social Cup Admin</strong><small>Operations console</small></div></div>
        <nav aria-label="Admin navigation">
          {NAV_ITEMS.map((item) => (
            <NavLink className={({ isActive }) => isActive ? 'active' : ''} end={item.to === '/'} key={item.to} to={item.to}>
              <i aria-hidden="true">{item.icon}</i>{item.label}
            </NavLink>
          ))}
        </nav>
        <div className="admin-identity"><strong>{user?.displayName}</strong><span>{user?.email}</span></div>
        <button className="sidebar-logout" onClick={logout} type="button">Logout</button>
      </aside>
      <div className="admin-main">
        <header className="desktop-header">
          <strong>Social Cup Admin</strong>
          <div><span>{user?.displayName}</span><small>{user?.email}</small></div>
        </header>
        <header className="mobile-header">
          <div className="brand"><span>SC</span><div><strong>Social Cup Admin</strong><small>{user?.displayName || user?.email}</small></div></div>
          <button onClick={logout} type="button">Logout</button>
        </header>
        <div className="mobile-nav">
          {NAV_ITEMS.map((item) => <NavLink className={({ isActive }) => isActive ? 'active' : ''} end={item.to === '/'} key={item.to} to={item.to}>{item.label}</NavLink>)}
        </div>
        {authorizationMessage ? <div className="global-alert"><Alert>{authorizationMessage} <button onClick={clearAuthorizationMessage} type="button">Dismiss</button></Alert></div> : null}
        <Outlet />
      </div>
    </div>
  )
}
