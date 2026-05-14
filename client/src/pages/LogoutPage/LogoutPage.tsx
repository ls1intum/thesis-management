import { useEffect } from 'react'
import { useAuthenticationContext } from '../../hooks/authentication'
import PageLoader from '../../components/PageLoader/PageLoader'

const LogoutPage = () => {
  const auth = useAuthenticationContext()

  useEffect(() => {
    auth.logout('/')
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- mount-only: logout fires exactly once when the page renders
  }, [])

  return <PageLoader />
}

export default LogoutPage
