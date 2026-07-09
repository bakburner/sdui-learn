import { useState, useEffect } from 'react'
import './PasswordGate.css'

const STORAGE_KEY = 'sdui-learn-auth'
const PASS_HASH = 'adrian'

export function PasswordGate({ children }: { children: React.ReactNode }) {
  const [authenticated, setAuthenticated] = useState(() => {
    return sessionStorage.getItem(STORAGE_KEY) === 'true'
  })
  const [input, setInput] = useState('')
  const [error, setError] = useState(false)

  useEffect(() => {
    if (authenticated) sessionStorage.setItem(STORAGE_KEY, 'true')
  }, [authenticated])

  if (authenticated) return <>{children}</>

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (input.toLowerCase() === PASS_HASH) {
      setAuthenticated(true)
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
