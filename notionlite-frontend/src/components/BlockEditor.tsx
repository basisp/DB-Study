import { useEffect, useMemo, useState } from 'react'

import type { BlockDto, BlockType } from '../types/notion'

interface BlockEditorProps {
  blocks: BlockDto[]
  onCreateAfter: (index: number) => void
  onUpdate: (blockId: number, input: { type?: BlockType; content?: string }) => void
  onDelete: (blockId: number) => void
}

function blockPlaceholder(type: BlockType): string {
  if (type === 'todo') return 'Todo item...'
  if (type === 'heading1') return 'Heading 1'
  if (type === 'heading2') return 'Heading 2'
  if (type === 'heading3') return 'Heading 3'
  return 'Write something...'
}

export function BlockEditor({ blocks, onCreateAfter, onUpdate, onDelete }: BlockEditorProps) {
  const sortedBlocks = useMemo(
    () => [...blocks].sort((left, right) => left.positionIndex - right.positionIndex),
    [blocks],
  )

  if (sortedBlocks.length === 0) {
    return (
      <div className="blockEditorEmpty">
        <button type="button" className="newBlockButton" onClick={() => onCreateAfter(-1)}>
          + Add first block
        </button>
      </div>
    )
  }

  return (
    <div className="blockEditor">
      {sortedBlocks.map((block, index) => (
        <BlockRow
          key={block.id}
          block={block}
          onCreateAfter={() => onCreateAfter(index)}
          onUpdate={onUpdate}
          onDelete={onDelete}
        />
      ))}
    </div>
  )
}

interface BlockRowProps {
  block: BlockDto
  onCreateAfter: () => void
  onUpdate: (blockId: number, input: { type?: BlockType; content?: string }) => void
  onDelete: (blockId: number) => void
}

function BlockRow({ block, onCreateAfter, onUpdate, onDelete }: BlockRowProps) {
  const [draft, setDraft] = useState(block.content)

  useEffect(() => {
    setDraft(block.content)
  }, [block.content])

  return (
    <div className="blockRow">
      <select
        className="blockTypeSelect"
        value={block.type}
        onChange={(event) => {
          onUpdate(block.id, { type: event.target.value as BlockType })
        }}
      >
        <option value="paragraph">Paragraph</option>
        <option value="heading1">Heading 1</option>
        <option value="heading2">Heading 2</option>
        <option value="heading3">Heading 3</option>
        <option value="todo">Todo</option>
      </select>
      <textarea
        className="blockTextarea"
        value={draft}
        placeholder={blockPlaceholder(block.type)}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={() => {
          if (draft !== block.content) {
            onUpdate(block.id, { content: draft })
          }
        }}
        onKeyDown={(event) => {
          if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault()
            onCreateAfter()
            return
          }
          if (event.key === 'Backspace' && draft.length === 0) {
            event.preventDefault()
            onDelete(block.id)
          }
        }}
      />
      <div className="blockActionButtons">
        <button
          type="button"
          className="addBlockInlineButton"
          aria-label="Insert block below"
          title="Insert block below"
          onClick={onCreateAfter}
        >
          +
        </button>
        <button type="button" className="deleteBlockButton" onClick={() => onDelete(block.id)}>
          Delete
        </button>
      </div>
    </div>
  )
}
