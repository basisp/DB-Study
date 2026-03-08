import { requestJson } from './http'
import type { WorkspaceDto } from '../types/notion'

export const workspacesApi = {
  getWorkspaces(): Promise<WorkspaceDto[]> {
    return requestJson<WorkspaceDto[]>('/api/workspaces')
  },

  createWorkspace(name: string): Promise<WorkspaceDto> {
    return requestJson<WorkspaceDto>('/api/workspaces', {
      method: 'POST',
      body: JSON.stringify({ name }),
    })
  },

  getWorkspace(workspaceId: number): Promise<WorkspaceDto> {
    return requestJson<WorkspaceDto>(`/api/workspaces/${workspaceId}`)
  },
}
