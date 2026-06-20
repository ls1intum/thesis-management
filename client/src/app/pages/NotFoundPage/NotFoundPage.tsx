import { usePageTitle } from '@/core/hooks/theme'
import NotFound from '@/core/components/NotFound/NotFound'

const NotFoundPage = () => {
  usePageTitle('Not Found')

  return <NotFound />
}

export default NotFoundPage
