import { createRoot } from 'react-dom/client'
import App from './app/App'

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('Root element with id "root" not found in the document')
}

const root = createRoot(rootElement)

root.render(<App />)
