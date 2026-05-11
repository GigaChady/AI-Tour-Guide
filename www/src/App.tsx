import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Sidebar } from '@/components/layout/Sidebar'
import { Login } from '@/modules/auth/Login'
import { Dashboard } from '@/modules/dashboard/Dashboard'
import { Profile } from '@/modules/profile/Profile'
import { Admin } from '@/modules/admin/Admin'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/*"
          element={
            <div className="flex h-screen overflow-hidden antialiased">
              <Sidebar />
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/admin" element={<Admin />} />
              </Routes>
            </div>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
