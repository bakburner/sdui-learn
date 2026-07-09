import { useEffect, useState } from 'react'

const SECTION_IDS = [
  'hero',
  'before-after',
  'how-it-works',
  'architecture',
  'deep-dive',
  'playground',
  'faq',
]

export function useScrollSpy() {
  const [activeSection, setActiveSection] = useState('hero')

  useEffect(() => {
    const observers: IntersectionObserver[] = []
    const visibleSections = new Map<string, number>()

    SECTION_IDS.forEach((id) => {
      const element = document.getElementById(id)
      if (!element) return

      const observer = new IntersectionObserver(
        ([entry]) => {
          if (entry.isIntersecting) {
            visibleSections.set(id, entry.intersectionRatio)
          } else {
            visibleSections.delete(id)
          }

          // Pick the first visible section in document order
          for (const sectionId of SECTION_IDS) {
            if (visibleSections.has(sectionId)) {
              setActiveSection(sectionId)
              break
            }
          }
        },
        {
          rootMargin: '-20% 0px -70% 0px',
          threshold: 0,
        }
      )

      observer.observe(element)
      observers.push(observer)
    })

    return () => {
      observers.forEach((obs) => obs.disconnect())
    }
  }, [])

  return activeSection
}
