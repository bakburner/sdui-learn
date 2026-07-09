import './Nav.css'

const NAV_ITEMS = [
  { id: 'hero', label: 'Intro' },
  { id: 'before-after', label: 'Why SDUI' },
  { id: 'how-it-works', label: 'How It Works' },
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
      </div>
    </nav>
  )
}
