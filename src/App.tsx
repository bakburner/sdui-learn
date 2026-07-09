import './App.css'
import { Nav } from './components/Nav'
import { Hero } from './sections/Hero'
import { BeforeAfter } from './sections/BeforeAfter'
import { HowItWorks } from './sections/HowItWorks'
import { DayInTheLife } from './sections/DayInTheLife'
import { Playground } from './sections/Playground'
import { FAQ } from './sections/FAQ'
import { useScrollSpy } from './hooks/useScrollSpy'

function App() {
  const activeSection = useScrollSpy()

  return (
    <div className="app">
      <Nav activeSection={activeSection} />
      <main>
        <Hero />
        <BeforeAfter />
        <HowItWorks />
        <DayInTheLife />
        <Playground />
        <FAQ />
      </main>
    </div>
  )
}

export default App
