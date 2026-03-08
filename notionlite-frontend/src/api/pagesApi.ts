import { requestJson } from './http'
import type { PageSummaryDto } from '../types/notion'

interface CreatePageRequest {
  workspaceId: number
  title: string
  parentId: number | null
}

interface UpdatePageRequest {
  title?: string
  parentId?: number | null
}

export const pagesApi = {
  getPages(workspaceId: number): Promise<PageSummaryDto[]> {
    return requestJson<PageSummaryDto[]>(`/api/pages?workspaceId=${workspaceId}`)
  },

  searchPages(workspaceId: number, query: string): Promise<PageSummaryDto[]> {
    const encodedQuery = encodeURIComponent(query)
    return requestJson<PageSummaryDto[]>(`/api/search/pages?workspaceId=${workspaceId}&query=${encodedQuery}`)
  },

  createPage(input: CreatePageRequest): Promise<PageSummaryDto> {
    return requestJson<PageSummaryDto>('/api/pages', {
      method: 'POST',
      body: JSON.stringify(input),
    })
  },

  getPage(pageId: number): Promise<PageSummaryDto> {
    return requestJson<PageSummaryDto>(`/api/pages/${pageId}`)
  },

  updatePage(pageId: number, input: UpdatePageRequest): Promise<PageSummaryDto> {
    return requestJson<PageSummaryDto>(`/api/pages/${pageId}`, {
      method: 'PATCH',
      body: JSON.stringify(input),
    })
  },

  deletePage(pageId: number): Promise<void> {
    return requestJson<void>(`/api/pages/${pageId}`, {
      method: 'DELETE',
    })
  },
}
