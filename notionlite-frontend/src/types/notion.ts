export type BlockType = 'paragraph' | 'heading1' | 'heading2' | 'heading3' | 'todo'

export interface WorkspaceDto {
  id: number
  name: string
  createdAt: string
  updatedAt: string
}

export interface PageSummaryDto {
  id: number
  workspaceId: number
  title: string
  parentId: number | null
  createdAt: string
  updatedAt: string
}

export interface BlockDto {
  id: number
  pageId: number
  type: BlockType
  content: string
  positionIndex: number
  createdAt: string
  updatedAt: string
}
