import { useEffect, useMemo, useState } from 'react'

import { blocksApi } from '../api/blocksApi'
import { pagesApi } from '../api/pagesApi'
import { workspacesApi } from '../api/workspacesApi'
import { MainContent } from '../components/MainContent'
import { Sidebar } from '../components/Sidebar'
import { Topbar } from '../components/Topbar'
import type { BlockDto, BlockType, PageSummaryDto, WorkspaceDto } from '../types/notion'

const MOBILE_BREAKPOINT = 900
const SEARCH_DEBOUNCE_MS = 250

export function WorkspacePage() {
  const [workspaces, setWorkspaces] = useState<WorkspaceDto[]>([])
  const [currentWorkspaceId, setCurrentWorkspaceId] = useState<number | null>(null)
  const [pages, setPages] = useState<PageSummaryDto[]>([])
  const [searchResults, setSearchResults] = useState<PageSummaryDto[] | null>(null)
  const [selectedPageId, setSelectedPageId] = useState<number | null>(null)
  const [blocks, setBlocks] = useState<BlockDto[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [isSidebarOpen, setIsSidebarOpen] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    const mediaQuery = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT}px)`)

    const syncSidebarByViewport = () => {
      setIsSidebarOpen(!mediaQuery.matches)
    }

    syncSidebarByViewport()
    mediaQuery.addEventListener('change', syncSidebarByViewport)

    return () => {
      mediaQuery.removeEventListener('change', syncSidebarByViewport)
    }
  }, [])

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const loadedWorkspaces = await workspacesApi.getWorkspaces()
        if (loadedWorkspaces.length > 0) {
          setWorkspaces(loadedWorkspaces)
          setCurrentWorkspaceId(loadedWorkspaces[0].id)
          return
        }

        const created = await workspacesApi.createWorkspace('Default Workspace')
        setWorkspaces([created])
        setCurrentWorkspaceId(created.id)
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : 'Failed to load workspaces')
      }
    }

    void bootstrap()
  }, [])

  useEffect(() => {
    const loadPages = async () => {
      if (!currentWorkspaceId) {
        return
      }
      try {
        const loadedPages = await pagesApi.getPages(currentWorkspaceId)
        setPages(loadedPages)

        if (loadedPages.length === 0) {
          setSelectedPageId(null)
          setBlocks([])
          return
        }

        const firstPageId = loadedPages[0].id
        setSelectedPageId((previous) =>
          previous && loadedPages.some((page) => page.id === previous) ? previous : firstPageId,
        )
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : 'Failed to load pages')
      }
    }

    void loadPages()
  }, [currentWorkspaceId])

  useEffect(() => {
    const query = searchQuery.trim()
    if (!query || !currentWorkspaceId) {
      setSearchResults(null)
      return
    }

    const timeoutId = window.setTimeout(async () => {
      try {
        const results = await pagesApi.searchPages(currentWorkspaceId, query)
        setSearchResults(results)
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : 'Failed to search pages')
      }
    }, SEARCH_DEBOUNCE_MS)

    return () => window.clearTimeout(timeoutId)
  }, [searchQuery, currentWorkspaceId, pages])

  useEffect(() => {
    const loadBlocks = async () => {
      if (!selectedPageId) {
        setBlocks([])
        return
      }

      try {
        const loadedBlocks = await blocksApi.getBlocks(selectedPageId)
        setBlocks(loadedBlocks)
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : 'Failed to load blocks')
      }
    }

    void loadBlocks()
  }, [selectedPageId])

  const selectedPage = useMemo(
    () => pages.find((page) => page.id === selectedPageId) ?? null,
    [pages, selectedPageId],
  )

  const currentWorkspace = useMemo(
    () => workspaces.find((workspace) => workspace.id === currentWorkspaceId) ?? null,
    [workspaces, currentWorkspaceId],
  )

  const sidebarPages = useMemo(() => {
    if (searchResults) {
      return searchResults
    }
    return pages
  }, [pages, searchResults])

  const handleCreatePage = async () => {
    if (!currentWorkspaceId) {
      return
    }

    try {
      setErrorMessage(null)
      const created = await pagesApi.createPage({
        workspaceId: currentWorkspaceId,
        title: 'Untitled',
        parentId: null,
      })
      setPages((current) => [...current, created])
      setSelectedPageId(created.id)
      setBlocks([])

      if (window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT}px)`).matches) {
        setIsSidebarOpen(false)
      }
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to create page')
    }
  }

  const handleSelectPage = (pageId: number) => {
    setSelectedPageId(pageId)

    if (window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT}px)`).matches) {
      setIsSidebarOpen(false)
    }
  }

  const handleUpdatePageTitle = async (title: string) => {
    if (!selectedPageId) {
      return
    }

    try {
      setErrorMessage(null)
      const updated = await pagesApi.updatePage(selectedPageId, { title })
      setPages((current) => current.map((page) => (page.id === updated.id ? updated : page)))
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to update page title')
    }
  }

  const handleMovePage = async (parentId: number | null) => {
    if (!selectedPageId) {
      return
    }

    try {
      setErrorMessage(null)
      const updated = await pagesApi.updatePage(selectedPageId, { parentId })
      setPages((current) => current.map((page) => (page.id === updated.id ? updated : page)))
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to move page')
    }
  }

  const handleDeletePage = async () => {
    if (!selectedPageId) {
      return
    }

    const deletingPageId = selectedPageId

    try {
      setErrorMessage(null)
      await pagesApi.deletePage(deletingPageId)

      const refreshed = await pagesApi.getPages(currentWorkspaceId as number)
      setPages(refreshed)

      if (refreshed.length === 0) {
        setSelectedPageId(null)
        setBlocks([])
      } else {
        const nextSelected = refreshed[0].id
        setSelectedPageId(nextSelected)
      }
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to delete page')
    }
  }

  const handleCreateBlockAfter = async (index: number) => {
    if (!selectedPageId) {
      return
    }

    try {
      setErrorMessage(null)
      const created = await blocksApi.createBlock(selectedPageId, {
        type: 'paragraph',
        content: '',
        positionIndex: index + 1,
      })
      setBlocks((current) => [...current, created].sort((left, right) => left.positionIndex - right.positionIndex))
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to create block')
    }
  }

  const handleUpdateBlock = async (
    blockId: number,
    input: { type?: BlockType; content?: string },
  ) => {
    if (!selectedPageId) {
      return
    }

    try {
      setErrorMessage(null)
      const updated = await blocksApi.updateBlock(selectedPageId, blockId, input)
      setBlocks((current) => current.map((block) => (block.id === updated.id ? updated : block)))
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to update block')
    }
  }

  const handleDeleteBlock = async (blockId: number) => {
    if (!selectedPageId) {
      return
    }

    try {
      setErrorMessage(null)
      await blocksApi.deleteBlock(selectedPageId, blockId)
      const nextBlocks = await blocksApi.getBlocks(selectedPageId)
      setBlocks(nextBlocks)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to delete block')
    }
  }

  return (
    <div className={`workspaceLayout ${isSidebarOpen ? '' : 'sidebarCollapsed'}`}>
      <Sidebar
        pages={sidebarPages}
        selectedPageId={selectedPageId}
        onSelectPage={handleSelectPage}
        onCreatePage={handleCreatePage}
      />

      {!isSidebarOpen && <div className="sidebarCollapsedRail" aria-hidden="true" />}

      <div className="contentArea">
        <Topbar
          workspaceName={currentWorkspace?.name ?? 'Workspace'}
          searchQuery={searchQuery}
          onSearchQueryChange={setSearchQuery}
          onToggleSidebar={() => setIsSidebarOpen((current) => !current)}
        />
        {errorMessage && <div className="errorBanner">{errorMessage}</div>}
        <MainContent
          selectedPage={selectedPage}
          pages={pages}
          blocks={blocks}
          onUpdatePageTitle={handleUpdatePageTitle}
          onMovePage={handleMovePage}
          onDeletePage={handleDeletePage}
          onCreateBlockAfter={handleCreateBlockAfter}
          onUpdateBlock={handleUpdateBlock}
          onDeleteBlock={handleDeleteBlock}
        />
      </div>

      {isSidebarOpen && (
        <button
          type="button"
          className="sidebarBackdrop"
          aria-label="Close sidebar"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}
    </div>
  )
}
