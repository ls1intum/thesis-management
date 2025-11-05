import { Modal, ScrollArea, Stack, Title } from '@mantine/core'

interface CreateInterviewProcessProps {
  opened: boolean
  onClose: () => void
}

const CreateInterviewProcess = ({ opened, onClose }: CreateInterviewProcessProps) => {
  return (
    <Modal
      opened={opened}
      onClose={onClose}
      centered
      size='xl'
      title={<Title order={3}>Create Interview Process</Title>}
    >
      <Stack>
        <ScrollArea h={'30vh'} w={'100%'} type='hover' offsetScrollbars>
          {/* Content for creating an interview process goes here */}
          <div>Test1</div>
          <div>Test2</div>
          <div>Test3</div>
          <div>Test4</div>
          <div>Test5</div>
          <div>Test6</div>
          <div>Test7</div>
          <div>Test8</div>
          <div>Test9</div>
          <div>Test10</div>
          <div>Test11</div>
          <div>Test12</div>
          <div>Test13</div>
          <div>Test14</div>
          <div>Test15</div>
          <div>Test16</div>
        </ScrollArea>
      </Stack>
    </Modal>
  )
}

export default CreateInterviewProcess
