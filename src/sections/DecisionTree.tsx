import { useState } from 'react'
import { useScrollReveal } from '../hooks/useScrollReveal'
import '../hooks/useScrollReveal.css'
import './DecisionTree.css'

interface TreeNode {
  id: string
  question: string
  yes: string | TreeNode
  no: string | TreeNode
}

const DECISION_TREE: TreeNode = {
  id: 'q1',
  question: 'Can the server fully compose it with existing atomic elements?',
  yes: 'use-atomic',
  no: {
    id: 'q2',
    question: 'Can a schema or action change solve it without new client code?',
    yes: 'schema-change',
    no: {
      id: 'q3',
      question: 'Can it be an AtomicComposite with a new atomic element type?',
      yes: 'new-element',
      no: {
        id: 'q4',
        question: 'Does the client need to own state? (sort, scroll position, form input, playback)',
        yes: 'use-semantic',
        no: 'use-atomic-composite',
      },
    },
  },
}

type Answer = 'yes' | 'no'

interface Step {
  nodeId: string
  answer: Answer
}

const OUTCOMES: Record<string, { title: string; description: string; color: string }> = {
  'use-atomic': {
    title: 'Use AtomicComposite',
    description: 'The server can compose this entirely with existing Container, Text, Image, Button, and other atomic primitives. No client work needed.',
    color: 'var(--positive)',
  },
  'schema-change': {
    title: 'Schema / Action Change',
    description: 'Add a new action type, token, or property to the schema. Run codegen. The server can then compose the new behavior without a new client renderer.',
    color: 'var(--link)',
  },
  'new-element': {
    title: 'New Atomic Element',
    description: 'Add a new atomic element type to the schema and implement its renderer in AtomicRouter on each platform. The server can then compose it freely.',
    color: 'var(--nba-tint)',
  },
  'use-semantic': {
    title: 'Add a Semantic Section',
    description: 'Client-owned state is required. Create a new semantic section type with its own renderer. The server sends data; the client owns interaction logic.',
    color: 'var(--negative)',
  },
  'use-atomic-composite': {
    title: 'Use AtomicComposite (with bridge)',
    description: 'Complex layout but no client state needed. Use AtomicComposite with SectionSlot bridge if you need to embed an existing semantic section inside an atomic tree.',
    color: 'var(--positive)',
  },
}

export function DecisionTree() {
  const [path, setPath] = useState<Step[]>([])
  const [currentNode, setCurrentNode] = useState<TreeNode | string>(DECISION_TREE)
  const header = useScrollReveal<HTMLDivElement>()
  const tree = useScrollReveal<HTMLDivElement>({ threshold: 0.05 })

  const handleAnswer = (answer: Answer) => {
    if (typeof currentNode === 'string') return
    const next = answer === 'yes' ? currentNode.yes : currentNode.no
    setPath([...path, { nodeId: currentNode.id, answer }])
    setCurrentNode(next)
  }

  const handleReset = () => {
    setPath([])
    setCurrentNode(DECISION_TREE)
  }

  const handleBack = () => {
    if (path.length === 0) return
    const newPath = path.slice(0, -1)
    setPath(newPath)
    // Replay to get the node
    let node: TreeNode | string = DECISION_TREE
    for (const step of newPath) {
      if (typeof node === 'string') break
      node = step.answer === 'yes' ? node.yes : node.no
    }
    setCurrentNode(node)
  }

  const isOutcome = typeof currentNode === 'string'
  const outcome = isOutcome ? OUTCOMES[currentNode as string] : null

  return (
    <section id="decision-tree">
      <div ref={header.ref} className={`reveal ${header.isVisible ? 'visible' : ''}`}>
        <div className="section-label">Decision Guide</div>
        <h2 className="section-title">Atomic or Semantic?</h2>
        <p className="section-subtitle">
          Follow this flowchart to determine the right approach for your new UI component. Default to atomic — only go semantic when you must.
        </p>
      </div>

      <div ref={tree.ref} className={`decision-container reveal-scale ${tree.isVisible ? 'visible' : ''}`}>
        <div className="decision-path">
          {path.map((step, i) => (
            <div key={i} className="path-step">
              <span className="path-dot" data-answer={step.answer}></span>
              <span className="path-label">{step.answer.toUpperCase()}</span>
            </div>
          ))}
        </div>

        {!isOutcome && (
          <div className="decision-question">
            <div className="question-number">Q{path.length + 1}</div>
            <h3 className="question-text">{(currentNode as TreeNode).question}</h3>
            <div className="question-actions">
              <button className="answer-btn answer-yes" onClick={() => handleAnswer('yes')}>
                Yes
              </button>
              <button className="answer-btn answer-no" onClick={() => handleAnswer('no')}>
                No
              </button>
            </div>
          </div>
        )}

        {isOutcome && outcome && (
          <div className="decision-outcome" style={{ borderColor: outcome.color }}>
            <div className="outcome-badge" style={{ color: outcome.color }}>Result</div>
            <h3 className="outcome-title" style={{ color: outcome.color }}>{outcome.title}</h3>
            <p className="outcome-description">{outcome.description}</p>
          </div>
        )}

        <div className="decision-controls">
          <button className="control-btn" onClick={handleBack} disabled={path.length === 0}>
            ← Back
          </button>
          <button className="control-btn" onClick={handleReset}>
            Start Over
          </button>
        </div>

        <div className="decision-principles">
          <h4 className="principles-title">Guiding Principles</h4>
          <div className="principles-list">
            <div className="principle">
              <span className="principle-icon" style={{ color: 'var(--positive)' }}>●</span>
              <span>Atomic is always preferred unless platform-specific complexity exists</span>
            </div>
            <div className="principle">
              <span className="principle-icon" style={{ color: 'var(--link)' }}>●</span>
              <span>Server controls more presentation instead of raw values</span>
            </div>
            <div className="principle">
              <span className="principle-icon" style={{ color: 'var(--nba-tint)' }}>●</span>
              <span>Only 10 semantic section types exist — adding one is a high bar</span>
            </div>
            <div className="principle">
              <span className="principle-icon" style={{ color: 'var(--negative)' }}>●</span>
              <span>If you need client state (sort, scroll, input, playback) → semantic</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
