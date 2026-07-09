import { useState, useEffect, createContext, useContext } from 'react'
import './PasswordGate.css'

const STORAGE_KEY = 'sdui-learn-auth'

type AuthLevel = 'none' | 'basic' | 'full'

const AuthContext = createContext<AuthLevel>('none')
export const useAuthLevel = () => useContext(AuthContext)

function getAuthLevel(password: string): AuthLevel {
  const p = password.toLowerCase()
  if (p === 'nbasdui') return 'full'
  if (p === 'adrian') return 'basic'
  return 'none'
}

export function PasswordGate({ children }: { children: React.ReactNode }) {
  const [authLevel, setAuthLevel] = useState<AuthLevel>(() => {
    return (sessionStorage.getItem(STORAGE_KEY) as AuthLevel) || 'none'
  })
  const [input, setInput] = useState('')
  const [error, setError] = useState(false)

  useEffect(() => {
    if (authLevel !== 'none') sessionStorage.setItem(STORAGE_KEY, authLevel)
  }, [authLevel])

  if (authLevel !== 'none') {
    return <AuthContext.Provider value={authLevel}>{children}</AuthContext.Provider>
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const level = getAuthLevel(input)
    if (level !== 'none') {
      setAuthLevel(level)
    } else {
      setError(true)
      setTimeout(() => setError(false), 1500)
    }
  }

  return (
    <div className="password-gate">
      <form className="password-form" onSubmit={handleSubmit}>
        <span className="password-brand">SDUI</span>
        <p className="password-label">Enter password to continue</p>
        <input
          type="password"
          className={`password-input ${error ? 'shake' : ''}`}
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="Password"
          autoFocus
        />
        <button type="submit" className="password-submit">Enter</button>
      </form>
    </div>
  )
}
