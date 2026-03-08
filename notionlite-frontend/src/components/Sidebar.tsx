import type { PageSummaryDto } from '../types/notion'
import { PageTreeItem } from './PageTreeItem'

interface SidebarProps {
  pages: PageSummaryDto[]
  selectedPageId: number | null
  onSelectPage: (pageId: number) => void
  onCreatePage: () => void
}

export function Sidebar({ pages, selectedPageId, onSelectPage, onCreatePage }: SidebarProps) {
  const rootPages = pages
    .filter((page) => page.parentId === null)
    .sort((left, right) => left.createdAt.localeCompare(right.createdAt))

  return (
    <aside className="sidebar">
      <div className="sidebarHeader">
        <p className="workspaceName">Notion-lite</p>
      </div>

      <button type="button" className="newPageButton" onClick={onCreatePage}>
        + New Page
      </button>

      <p className="treeSectionLabel">Pages</p>
      <ul className="pageTreeList">
        {rootPages.map((page) => (
          <PageTreeItem
            key={page.id}
            page={page}
            pages={pages}
            depth={0}
            selectedPageId={selectedPageId}
            onSelectPage={onSelectPage}
          />
        ))}
      </ul>
    </aside>
  )
}
