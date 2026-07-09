import './Nav.css'

const NAV_ITEMS = [
  { id: 'hero', label: 'Intro' },
  { id: 'before-after', label: 'Why SDUI' },
  { id: 'how-it-works', label: 'How It Works' },
  { id: 'architecture', label: 'Architecture' },
  { id: 'deep-dive', label: 'Deep Dive' },
  { id: 'day-in-the-life', label: 'Game Day' },
  { id: 'playground', label: 'Playground' },
  { id: 'faq', label: 'FAQ' },
]

interface NavProps {
  activeSection: string
}

export function Nav({ activeSection }: NavProps) {
  return (
    <nav className="nav">
      <div className="nav-inner">
        <span className="nav-brand">SDUI</span>
        <ul className="nav-links">
          {NAV_ITEMS.map((item) => (
            <li key={item.id}>
              <a
                href={`#${item.id}`}
                className={activeSection === item.id ? 'active' : ''}
                onClick={(e) => {
                  e.preventDefault()
                  document.getElementById(item.id)?.scrollIntoView({ behavior: 'smooth' })
                }}
              >
                {item.label}
              </a>
            </li>
          ))}
        </ul>
        <button
          className="nav-launch-btn"
          onClick={() => window.dispatchEvent(new CustomEvent('launch-editor'))}
        >
          Launch Editor
        </button>
      </div>
    </nav>
  )
}
