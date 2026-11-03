import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom' // NUEVO
import AuthProvider from './context/AuthProvider'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>   {/* NUEVO */}
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>  {/* NUEVO */}
  </StrictMode>,
)
