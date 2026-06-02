import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'
import { Sidebar } from '@/components/layout/Sidebar'
import { Login } from '@/modules/auth/Login'
import { KeycloakCallback } from '@/modules/auth/KeycloakCallback'
import { Dashboard } from '@/modules/dashboard/Dashboard'
import { Profile } from '@/modules/profile/Profile'
import { Admin } from '@/modules/admin/Admin'
import { PrivateRoute } from '@/components/router/PrivateRoute'
import { AdminRoute } from '@/components/router/AdminRoute'
import { Preferences } from '@/modules/preferences/Preferences'
import { MapExplorer } from '@/modules/map/MapExplorer'
import { RoutePlanner } from '@/modules/route-planner/RoutePlanner'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/auth/callback" element={<KeycloakCallback />} />
        <Route
          path="/*"
          element={
            <PrivateRoute>
              <div className="flex h-screen overflow-hidden antialiased">
                <Sidebar />
                <Routes>
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/profile" element={<Profile />} />
                  <Route path="/admin" element={<AdminRoute><Admin /></AdminRoute>} />
                  <Route path="/preferences" element={<Preferences />} />
                  <Route path="/map-explorer" element={<MapExplorer />} />
                  <Route path="/route-planner" element={<RoutePlanner />} />
                </Routes>
              </div>
            </PrivateRoute>
          }
        />
      </Routes>
      <Toaster position="top-right" richColors />
    </BrowserRouter>
  )
}

export default App
