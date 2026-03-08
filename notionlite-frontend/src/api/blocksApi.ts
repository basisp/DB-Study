import { requestJson } from './http'
import type { BlockDto, BlockType } from '../types/notion'

interface CreateBlockRequest {
  type: BlockType
  content: string
  positionIndex?: number
}

interface UpdateBlockRequest {
  type?: BlockType
  content?: string
  positionIndex?: number
}

export const blocksApi = {
  getBlocks(pageId: number): Promise<BlockDto[]> {
    return requestJson<BlockDto[]>(`/api/pages/${pageId}/blocks`)
  },

  createBlock(pageId: number, input: CreateBlockRequest): Promise<BlockDto> {
    return requestJson<BlockDto>(`/api/pages/${pageId}/blocks`, {
      method: 'POST',
      body: JSON.stringify(input),
    })
  },

  updateBlock(pageId: number, blockId: number, input: UpdateBlockRequest): Promise<BlockDto> {
    return requestJson<BlockDto>(`/api/pages/${pageId}/blocks/${blockId}`, {
      method: 'PATCH',
      body: JSON.stringify(input),
    })
  },

  deleteBlock(pageId: number, blockId: number): Promise<void> {
    return requestJson<void>(`/api/pages/${pageId}/blocks/${blockId}`, {
      method: 'DELETE',
    })
  },

  reorderBlocks(pageId: number, blockIds: number[]): Promise<BlockDto[]> {
    return requestJson<BlockDto[]>(`/api/pages/${pageId}/blocks/reorder`, {
      method: 'PUT',
      body: JSON.stringify({ blockIds }),
    })
  },
}
