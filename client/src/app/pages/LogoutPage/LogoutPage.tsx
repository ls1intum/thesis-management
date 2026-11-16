import { useEffect } from 'react'
import { useNavigate } from 'react-router'
import { useAuthenticationContext } from '@/core/hooks/authentication'
import PageLoader from '@/core/components/PageLoader/PageLoader'

const LogoutPage = () => {
  const auth = useAuthenticationContext()
  const navigate = useNavigate()

  useEffect(() => {
    // logout resolves to false when the server could not end the session; the user is still logged in then
    // (the provider has told them), so leave the logout page instead of showing a loader forever
    void Promise.resolve(auth.logout('/')).then((loggedOut) => {
      if (loggedOut === false) {
        void navigate('/dashboard', { replace: true })
      }
    })
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- mount-only: logout fires exactly once when the page renders
  }, [])

  return <PageLoader />
}

export default LogoutPage
