import { beforeEach, describe, expect, test, vi } from 'vitest'
import type { Ref } from 'react'
import { renderWithProviders, screen, userEvent, waitFor } from '@/../test/render'
import AvatarCropModal from '@/core/user/components/AvatarCropModal/AvatarCropModal'

const showSimpleError = vi.hoisted(() => vi.fn())
const state = vi.hoisted(() => ({ dataUrl: 'data:image/png;base64,iVBORw0KGgo=' }))

vi.mock('@/core/utils/notification', () => ({ showSimpleError, showSimpleSuccess: vi.fn() }))

// jsdom has no canvas: the editor is replaced by a stub that exports whatever the test wants
vi.mock('react-avatar-editor', async () => {
  const { useImperativeHandle } = await import('react')

  const EditorStub = ({ ref }: { ref?: Ref<unknown> }) => {
    useImperativeHandle(ref, () => ({
      getImageScaledToCanvas: () => ({ toDataURL: () => state.dataUrl }),
    }))
    return <div data-testid='editor' />
  }

  return { default: EditorStub }
})

const renderModal = (onSave = vi.fn()) => {
  renderWithProviders(
    <AvatarCropModal
      file={new File(['x'], 'photo.png', { type: 'image/png' })}
      onClose={() => undefined}
      onSave={onSave}
    />,
  )
  return onSave
}

describe('AvatarCropModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    state.dataUrl = 'data:image/png;base64,iVBORw0KGgo='
  })

  test('hands the cropped picture over', async () => {
    const onSave = renderModal()

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Save Avatar' }))

    await waitFor(() => expect(onSave).toHaveBeenCalled())
    const saved = onSave.mock.calls[0][0] as File
    expect(saved.name).toBe('avatar.png')
    expect(saved.size).toBeGreaterThan(0)
  })

  test('does not save an empty picture, which would delete the current one', async () => {
    // what browsers return for pictures that are too large for a canvas
    state.dataUrl = 'data:,'
    const onSave = renderModal()

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Save Avatar' }))

    await waitFor(() =>
      expect(showSimpleError).toHaveBeenCalledWith(
        expect.stringContaining('could not be processed'),
      ),
    )
    expect(onSave).not.toHaveBeenCalled()
  })
})
