import type { PageSummaryDto } from '../types/notion'

interface PageTreeItemProps {
  page: PageSummaryDto
  pages: PageSummaryDto[]
  depth: number
  selectedPageId: number | null
  onSelectPage: (pageId: number) => void
}

const INDENT_SIZE = 12

export function PageTreeItem({ page, pages, depth, selectedPageId, onSelectPage }: PageTreeItemProps) {
  const childPages = pages
    .filter((candidate) => candidate.parentId === page.id)
    .sort((left, right) => left.createdAt.localeCompare(right.createdAt))

  return (
    <li className="treeNode">
      <button
        type="button"
        className={`pageItem ${selectedPageId === page.id ? 'selected' : ''}`}
        style={{ paddingLeft: `${8 + depth * INDENT_SIZE}px` }}
        onClick={() => onSelectPage(page.id)}
      >
        <span className="pageTitle">{page.title}</span>
      </button>
      {childPages.length > 0 && (
        <ul className="pageTreeList">
          {childPages.map((child) => (
            <PageTreeItem
              key={child.id}
              page={child}
              pages={pages}
              depth={depth + 1}
              selectedPageId={selectedPageId}
              onSelectPage={onSelectPage}
            />
          ))}
        </ul>
      )}
    </li>
  )
}
