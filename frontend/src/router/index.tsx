import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from '../components/Layout'
import Dashboard from '../pages/Dashboard'
import ApiKeys from '../pages/ApiKeys'
import Models from '../pages/Models'
import Logs from '../pages/Logs'
import Chat from '../pages/Chat'
import Channel from '../pages/Channel'
import User from '../pages/User'
import TopUp from '../pages/TopUp'
import Setting from '../pages/Setting'
import Task from '../pages/Task'
import KeyApplications from '../pages/KeyApplications'
import ModelSubscriptions from '../pages/ModelSubscriptions'
import Docs from '../pages/Docs'
import Login from '../pages/Login'

const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const token = localStorage.getItem('token')
  return token ? <>{children}</> : <Navigate to="/login" replace />
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
        <Route index element={<Dashboard />} />
        <Route path="channel" element={<Channel />} />
        <Route path="keys" element={<ApiKeys />} />
        <Route path="key-applications" element={<KeyApplications />} />
        <Route path="user" element={<User />} />
        <Route path="logs" element={<Logs />} />
        <Route path="topup" element={<TopUp />} />
        <Route path="models" element={<Models />} />
        <Route path="model-subscriptions" element={<ModelSubscriptions />} />
        <Route path="setting" element={<Setting />} />
        <Route path="task" element={<Task />} />
        <Route path="docs" element={<Docs />} />
        <Route path="chat" element={<Chat />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default Router
