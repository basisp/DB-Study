import { useMemo } from 'react'

import { BlockEditor } from './BlockEditor'
import type { BlockDto, BlockType, PageSummaryDto } from '../types/notion'

interface MainContentProps {
  selectedPage: PageSummaryDto | null
  pages: PageSummaryDto[]
  blocks: BlockDto[]
  onUpdatePageTitle: (title: string) => void
  onMovePage: (parentId: number | null) => void
  onDeletePage: () => void
  onCreateBlockAfter: (index: number) => void
  onUpdateBlock: (blockId: number, input: { type?: BlockType; content?: string }) => void
  onDeleteBlock: (blockId: number) => void
}

export function MainContent({
  selectedPage,
  pages,
  blocks,
  onUpdatePageTitle,
  onMovePage,
  onDeletePage,
  onCreateBlockAfter,
  onUpdateBlock,
  onDeleteBlock,
}: MainContentProps) {
  const moveCandidates = useMemo(() => {
    if (!selectedPage) {
      return []
    }
    return pages
      .filter((page) => page.id !== selectedPage.id)
      .sort((left, right) => left.title.localeCompare(right.title))
  }, [pages, selectedPage])

  if (!selectedPage) {
    return <main className="mainContent emptyState">No page selected.</main>
  }

  return (
    <main className="mainContent">
      <div className="mainContentInner">
        <header className="mainHeader">
          <input
            className="pageTitleInput"
            defaultValue={selectedPage.title}
            onBlur={(event) => {
              const title = event.target.value.trim()
              if (title.length > 0 && title !== selectedPage.title) {
                onUpdatePageTitle(title)
              }
            }}
          />
        </header>

        <section className="pageActionsRow">
          <label className="pageMoveLabel">
            Move under
            <select
              className="pageMoveSelect"
              value={selectedPage.parentId ?? ''}
              onChange={(event) => {
                const rawValue = event.target.value
                onMovePage(rawValue === '' ? null : Number(rawValue))
              }}
            >
              <option value="">Root</option>
              {moveCandidates.map((candidate) => (
                <option key={candidate.id} value={candidate.id}>
                  {candidate.title}
                </option>
              ))}
            </select>
          </label>

          <button
            type="button"
            className="pageDeleteButton"
            onClick={() => {
              if (window.confirm('Delete this page and all its child pages/blocks?')) {
                onDeletePage()
              }
            }}
          >
            Delete Page
          </button>
        </section>

        <section className="editorSection">
          <BlockEditor
            blocks={blocks}
            onCreateAfter={onCreateBlockAfter}
            onUpdate={onUpdateBlock}
            onDelete={onDeleteBlock}
          />
        </section>
      </div>
    </main>
  )
}
