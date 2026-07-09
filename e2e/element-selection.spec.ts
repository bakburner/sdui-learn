import { test, expect } from '@playwright/test'

test.describe('Playground element selection and code highlighting', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')

    // Authenticate with basic password
    await page.locator('.password-input').fill('adrian')
    await page.locator('.password-submit').click()

    // Open the playground in fullscreen
    await page.locator('.btn-playground').click()
    await expect(page.locator('.playground-fullscreen')).toBeVisible()

    // Dismiss the onboarding overlay
    const gotIt = page.locator('button', { hasText: 'Got it' })
    if (await gotIt.isVisible()) {
      await gotIt.click()
      await expect(gotIt).not.toBeVisible()
    }
  })

  test('clicking an element in preview highlights the corresponding code block', async ({ page }) => {
    // The default JSON has Text elements like "LAL", "108", "BOS", "112"
    // Click on a Text element showing "LAL"
    const lalText = page.locator('.atomic-hoverable', { hasText: 'LAL' }).first()
    await lalText.click()

    // Should create highlight overlay with active lines
    const activeLines = page.locator('.highlight-line.active')
    await expect(activeLines.first()).toBeVisible()

    // Count highlighted lines — a Text element block should be at least 1 line
    const count = await activeLines.count()
    expect(count).toBeGreaterThan(0)
  })

  test('highlighted code block contains the selected element type and properties', async ({ page }) => {
    // Click on a Text element that shows "LAL"
    const lalText = page.locator('.atomic-text', { hasText: 'LAL' }).first()
    await lalText.click()

    // Wait for highlight to appear
    await expect(page.locator('.highlight-line.active').first()).toBeVisible()

    // Get the textarea content and the highlighted line range
    const textareaValue = await page.locator('.editor-textarea').inputValue()
    const lines = textareaValue.split('\n')

    // Find which lines are highlighted by checking the overlay
    const allHighlightLines = page.locator('.highlight-line')
    const totalLines = await allHighlightLines.count()

    let highlightStart = -1
    let highlightEnd = -1
    for (let i = 0; i < totalLines; i++) {
      const hasActive = await allHighlightLines.nth(i).evaluate(el => el.classList.contains('active'))
      if (hasActive && highlightStart === -1) highlightStart = i
      if (hasActive) highlightEnd = i
    }

    // The highlighted block should contain "type": "Text"
    const highlightedBlock = lines.slice(highlightStart, highlightEnd + 1).join('\n')
    expect(highlightedBlock).toContain('"type"')
    expect(highlightedBlock).toContain('"Text"')
    // Should contain the "LAL" content since we clicked on that specific text
    expect(highlightedBlock).toContain('"LAL"')
  })

  test('clicking a Container element highlights its full block including children', async ({ page }) => {
    // Click on a Container — the outermost one wraps the whole card
    const container = page.locator('.atomic-container').first()
    await container.click()

    await expect(page.locator('.highlight-line.active').first()).toBeVisible()

    // A Container with children should highlight multiple lines (braces + children)
    const activeCount = await page.locator('.highlight-line.active').count()
    expect(activeCount).toBeGreaterThan(3)
  })

  test('clicking different elements updates the highlight to the new element', async ({ page }) => {
    // Click "LAL" text first
    const lalText = page.locator('.atomic-text', { hasText: 'LAL' }).first()
    await lalText.click()
    await expect(page.locator('.highlight-line.active').first()).toBeVisible()

    // Record which lines are highlighted
    const firstHighlightLines = await page.locator('.highlight-line.active').count()

    // Now click "BOS" text
    const bosText = page.locator('.atomic-text', { hasText: 'BOS' }).first()
    await bosText.click()

    // Wait for highlight to update — small delay for the setTimeout
    await page.waitForTimeout(100)
    await expect(page.locator('.highlight-line.active').first()).toBeVisible()

    // Get the textarea and verify the highlighted block now contains "BOS"
    const textareaValue = await page.locator('.editor-textarea').inputValue()
    const lines = textareaValue.split('\n')

    const allHighlightLines = page.locator('.highlight-line')
    const totalLines = await allHighlightLines.count()

    let highlightStart = -1
    let highlightEnd = -1
    for (let i = 0; i < totalLines; i++) {
      const hasActive = await allHighlightLines.nth(i).evaluate(el => el.classList.contains('active'))
      if (hasActive && highlightStart === -1) highlightStart = i
      if (hasActive) highlightEnd = i
    }

    const highlightedBlock = lines.slice(highlightStart, highlightEnd + 1).join('\n')
    expect(highlightedBlock).toContain('"BOS"')
    expect(highlightedBlock).not.toContain('"LAL"')
  })

  test('selected element gets the atomic-selected CSS class', async ({ page }) => {
    const lalText = page.locator('.atomic-text', { hasText: 'LAL' }).first()
    await lalText.click()

    // The clicked element should have the atomic-selected class
    await expect(lalText).toHaveClass(/atomic-selected/)
  })

  test('breadcrumb path updates when element is selected', async ({ page }) => {
    // Click on an element in the preview — any clickable element
    const previewElement = page.locator('[data-type]').first()
    await previewElement.click()

    // The breadcrumb in the toolbar should show a path with at least one element type
    const breadcrumb = page.locator('.selected-indicator')
    const text = await breadcrumb.textContent()
    // Should contain at least "Container" (the top-level element)
    expect(text).toContain('Container')
  })

  test('switching samples clears highlight and resets selection', async ({ page }) => {
    // Click an element to create a highlight
    const lalText = page.locator('.atomic-text', { hasText: 'LAL' }).first()
    await lalText.click()
    await expect(page.locator('.highlight-line.active').first()).toBeVisible()

    // Switch to a different sample
    await page.locator('.example-btn', { hasText: 'News Card' }).click()

    // Highlight should be cleared (new content loaded)
    // The old highlight lines won't be active since inspectedElement is cleared
    await expect(page.locator('.highlight-line.active')).toHaveCount(0)
  })

  test('highlight correctly identifies element with unique properties among duplicates', async ({ page }) => {
    // The default JSON has multiple Text elements. "108" and "112" are scores.
    // Click on "108" — its highlight should contain "108" but NOT "112"
    const scoreText = page.locator('.atomic-text', { hasText: '108' }).first()
    await scoreText.click()

    await expect(page.locator('.highlight-line.active').first()).toBeVisible()

    const textareaValue = await page.locator('.editor-textarea').inputValue()
    const lines = textareaValue.split('\n')

    const allHighlightLines = page.locator('.highlight-line')
    const totalLines = await allHighlightLines.count()

    let highlightStart = -1
    let highlightEnd = -1
    for (let i = 0; i < totalLines; i++) {
      const hasActive = await allHighlightLines.nth(i).evaluate(el => el.classList.contains('active'))
      if (hasActive && highlightStart === -1) highlightStart = i
      if (hasActive) highlightEnd = i
    }

    const highlightedBlock = lines.slice(highlightStart, highlightEnd + 1).join('\n')
    expect(highlightedBlock).toContain('"108"')
    expect(highlightedBlock).not.toContain('"112"')
  })

  test('Image element selection highlights the correct image block', async ({ page }) => {
    // Click on an Image element (team logo)
    const image = page.locator('.atomic-image').first()
    await image.click()

    await expect(page.locator('.highlight-line.active').first()).toBeVisible()

    const textareaValue = await page.locator('.editor-textarea').inputValue()
    const lines = textareaValue.split('\n')

    const allHighlightLines = page.locator('.highlight-line')
    const totalLines = await allHighlightLines.count()

    let highlightStart = -1
    let highlightEnd = -1
    for (let i = 0; i < totalLines; i++) {
      const hasActive = await allHighlightLines.nth(i).evaluate(el => el.classList.contains('active'))
      if (hasActive && highlightStart === -1) highlightStart = i
      if (hasActive) highlightEnd = i
    }

    const highlightedBlock = lines.slice(highlightStart, highlightEnd + 1).join('\n')
    expect(highlightedBlock).toContain('"type"')
    expect(highlightedBlock).toContain('"Image"')
    expect(highlightedBlock).toContain('"src"')
  })
})
