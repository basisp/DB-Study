interface TopbarProps {
  searchQuery: string
  workspaceName: string
  onSearchQueryChange: (value: string) => void
  onToggleSidebar: () => void
}

export function Topbar({ searchQuery, workspaceName, onSearchQueryChange, onToggleSidebar }: TopbarProps) {
  return (
    <header className="topbar">
      <button type="button" className="sidebarToggleButton" onClick={onToggleSidebar}>
        Sidebar
      </button>
      <div className="topbarWorkspaceName">{workspaceName}</div>
      <div className="searchWrap">
        <input
          className="searchInput"
          type="text"
          placeholder="Search pages..."
          value={searchQuery}
          onChange={(event) => onSearchQueryChange(event.target.value)}
        />
      </div>
    </header>
  )
}
