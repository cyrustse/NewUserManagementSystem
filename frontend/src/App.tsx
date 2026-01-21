import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ToastProvider } from './contexts/ToastContext';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import Roles from './pages/Roles';
import Settings from './pages/Settings';
import MfaSetup from './pages/MfaSetup';
import Permissions from './pages/Permissions';

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  
  if (isLoading) {
    return <div>Loading...</div>;
  }
  
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
};

const App: React.FC = () => {
  return (
    <ToastProvider>
      <AuthProvider>
        <Router>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={
              <PrivateRoute>
                <Dashboard />
              </PrivateRoute>
            } />
            <Route path="/users" element={
              <PrivateRoute>
                <Users />
              </PrivateRoute>
            } />
            <Route path="/roles" element={
              <PrivateRoute>
                <Roles />
              </PrivateRoute>
            } />
            <Route path="/settings" element={
              <PrivateRoute>
                <Settings />
              </PrivateRoute>
            } />
            <Route path="/mfa-setup" element={
              <PrivateRoute>
                <MfaSetup />
              </PrivateRoute>
            } />
            <Route path="/permissions" element={
              <PrivateRoute>
                <Permissions />
              </PrivateRoute>
            } />
          </Routes>
        </Router>
        <ToastContainer />
      </AuthProvider>
    </ToastProvider>
  );
};

export default App;
