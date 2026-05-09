import { describe, expect, test, vi } from 'vitest'
import { renderWithProviders, screen, userEvent } from '../../../test/render'
import ConfirmationButton from './ConfirmationButton'

// Reference test for the new Vitest + RTL setup. ConfirmationButton is a
// good first target because it exercises:
//   - Plain render of a Mantine Button
//   - Click interaction via @testing-library/user-event
//   - A Mantine Modal portal (validates jsdom polyfills in test/setup.ts)
//   - The Cancel / Confirm two-step flow used elsewhere in the app

describe('ConfirmationButton', () => {
  test('renders the trigger label', () => {
    renderWithProviders(
      <ConfirmationButton confirmationTitle='Delete?' confirmationText='Are you sure?'>
        Delete item
      </ConfirmationButton>,
    )

    expect(screen.getByRole('button', { name: /delete item/i })).toBeInTheDocument()
  })

  test('does not call onClick on the first click and opens the confirmation modal', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()

    renderWithProviders(
      <ConfirmationButton
        confirmationTitle='Delete?'
        confirmationText='Are you sure?'
        onClick={onConfirm}
      >
        Delete item
      </ConfirmationButton>,
    )

    await user.click(screen.getByRole('button', { name: /delete item/i }))

    expect(await screen.findByText('Are you sure?')).toBeInTheDocument()
    expect(onConfirm).not.toHaveBeenCalled()
  })

  test('Cancel closes the modal without invoking onClick', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()

    renderWithProviders(
      <ConfirmationButton
        confirmationTitle='Delete?'
        confirmationText='Are you sure?'
        onClick={onConfirm}
      >
        Delete item
      </ConfirmationButton>,
    )

    await user.click(screen.getByRole('button', { name: /delete item/i }))
    await user.click(await screen.findByRole('button', { name: /cancel/i }))

    expect(onConfirm).not.toHaveBeenCalled()
  })

  test('Confirm invokes onClick exactly once', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()

    renderWithProviders(
      <ConfirmationButton
        confirmationTitle='Delete?'
        confirmationText='Are you sure?'
        onClick={onConfirm}
      >
        Delete item
      </ConfirmationButton>,
    )

    await user.click(screen.getByRole('button', { name: /delete item/i }))
    await user.click(await screen.findByRole('button', { name: /confirm/i }))

    expect(onConfirm).toHaveBeenCalledTimes(1)
  })
})
