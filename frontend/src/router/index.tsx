import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from '../components/Layout'
import Dashboard from '../pages/Dashboard'
import ApiKeys from '../pages/ApiKeys'
import Models from '../pages/Models'
import Logs from '../pages/Logs'
import Chat from '../pages/Chat'
import Channel from '../pages/Channel'
import Providers from '../pages/Providers'
import User from '../pages/User'
import TopUp from '../pages/TopUp'
import Setting from '../pages/Setting'
import Task from '../pages/Task'
import KeyApplications from '../pages/KeyApplications'
import Docs from '../pages/Docs'
import Redemption from '../pages/Redemption'
import Login from '../pages/Login'
import UserCenter from '../pages/UserCenter'

const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const token = localStorage.getItem('token')
  return token ? <>{children}</> : <Navigate to="/login" replace />
}

const AdminRoute = ({ children }: { children: React.ReactNode }) => {
  const role = (localStorage.getItem('role') as 'admin' | 'user') || 'admin'
  return role === 'admin' ? <>{children}</> : <Navigate to="/keys" replace />
}

const Router = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }
      >
        <Route index element={<AdminRoute><Dashboard /></AdminRoute>} />
        <Route path="providers" element={<AdminRoute><Providers /></AdminRoute>} />
        <Route path="channel" element={<AdminRoute><Channel /></AdminRoute>} />
        <Route path="keys" element={<ApiKeys />} />
        <Route path="key-applications" element={<KeyApplications />} />
        <Route path="user" element={<AdminRoute><User /></AdminRoute>} />
        <Route path="logs" element={<Logs />} />
        <Route path="topup" element={<AdminRoute><TopUp /></AdminRoute>} />
        <Route path="redemption" element={<AdminRoute><Redemption /></AdminRoute>} />
        <Route path="models" element={<Models />} />
        <Route path="model-subscriptions" element={<Navigate to="/models" replace />} />
        <Route path="setting" element={<AdminRoute><Setting /></AdminRoute>} />
        <Route path="task" element={<AdminRoute><Task /></AdminRoute>} />
        <Route path="docs" element={<Docs />} />
        <Route path="chat" element={<Chat />} />
        <Route path="account" element={<UserCenter />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default Router
