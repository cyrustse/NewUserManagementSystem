import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { toast } from 'react-toastify';

interface UserInfo {
  id: string;
  username: string;
  email: string;
  mfaEnabled: boolean;
}

const Settings: React.FC = () => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [mfaLoading, setMfaLoading] = useState(false);
  const [verifyLoading, setVerifyLoading] = useState(false);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [mfaEnabled, setMfaEnabled] = useState(false);
  const [mfaSecret, setMfaSecret] = useState('');
  const [mfaQrUrl, setMfaQrUrl] = useState('');
  const [showMfaSetup, setShowMfaSetup] = useState(false);
  const [verificationCode, setVerificationCode] = useState('');

  const fetchUserInfo = async () => {
    try {
      setLoading(true);
      const response = await apiService.clientInstance.get<{ data: UserInfo }>('/users/me');
      setUserInfo(response.data.data);
      setMfaEnabled(response.data.data.mfaEnabled || false);
    } catch (err) {
      console.error('Failed to fetch user info:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUserInfo();
  }, []);

  const handleSetupMfa = async () => {
    setMfaLoading(true);
    try {
      const response = await apiService.setupMfa();
      setMfaSecret(response.data.secret);
      setMfaQrUrl(response.qrUrl);
      setShowMfaSetup(true);
    } catch (err: any) {
      const message = err.response?.data?.message || 'Failed to setup MFA';
      toast.error(message);
    } finally {
      setMfaLoading(false);
    }
  };

  const handleVerifyMfa = async (e: React.FormEvent) => {
    e.preventDefault();
    setVerifyLoading(true);
    try {
      await apiService.verifyMfa(verificationCode, mfaSecret);
      toast.success('MFA enabled successfully!');
      setMfaEnabled(true);
      setShowMfaSetup(false);
      setVerificationCode('');
      setMfaSecret('');
      setMfaQrUrl('');
      fetchUserInfo();
    } catch (err: any) {
      const message = err.response?.data?.message || 'Invalid verification code';
      toast.error(message);
    } finally {
      setVerifyLoading(false);
    }
  };

  const navItems = [
    { path: '/', label: 'Dashboard', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
    { path: '/users', label: 'Users', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z' },
    { path: '/roles', label: 'Roles', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z' },
    { path: '/settings', label: 'Settings', icon: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z' },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white border-b border-gray-200 sticky top-0 z-50 backdrop-blur-lg bg-white/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <div className="flex-shrink-0 flex items-center">
                <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-xl flex items-center justify-center mr-3 shadow-lg shadow-blue-500/30">
                  <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <span className="text-xl font-bold text-gradient">UserHub</span>
              </div>
              <div className="hidden sm:ml-10 sm:flex sm:space-x-1">
                {navItems.map((item) => {
                  const isActive = location.pathname === item.path;
                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 flex items-center space-x-2 ${
                        isActive
                          ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-lg shadow-blue-500/30'
                          : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                      }`}
                    >
                      <svg className={`w-5 h-5 ${isActive ? 'text-white' : 'text-gray-400'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={item.icon} />
                      </svg>
                      <span>{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-3">
                <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500 to-indigo-500 flex items-center justify-center text-white font-semibold text-sm shadow-lg">
                  {user?.username?.charAt(0).toUpperCase()}
                </div>
                <div className="hidden sm:block">
                  <p className="text-sm font-semibold text-gray-900">{user?.username}</p>
                </div>
              </div>
              <button
                onClick={() => logout()}
                className="p-2.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all duration-200"
                aria-label="Logout"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
          <p className="text-gray-500 mt-1.5">Manage your account settings and security</p>
        </div>

        {loading ? (
          <div className="card p-8 animate-pulse">
            <div className="flex items-center space-x-4">
              <div className="w-16 h-16 bg-gray-200 rounded-full" />
              <div className="space-y-2">
                <div className="h-6 bg-gray-200 rounded w-48" />
                <div className="h-4 bg-gray-200 rounded w-32" />
              </div>
            </div>
          </div>
        ) : (
          <div className="space-y-6">
            <div className="card p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div className="w-16 h-16 rounded-full bg-gradient-to-br from-blue-500 to-indigo-500 flex items-center justify-center text-white text-xl font-bold">
                    {userInfo?.username?.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <h2 className="text-xl font-semibold text-gray-900">{userInfo?.username}</h2>
                    <p className="text-gray-500">{userInfo?.email}</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="card p-6">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">Two-Factor Authentication</h3>
                  <p className="text-sm text-gray-500 mt-1">Add an extra layer of security to your account</p>
                </div>
                <div className="flex items-center space-x-3">
                  {mfaEnabled ? (
                    <>
                      <span className="inline-flex items-center px-3 py-1 text-sm font-semibold rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                        <svg className="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        Enabled
                      </span>
                      <button
                        onClick={() => toast.info('MFA is already enabled. Contact support to disable.')}
                        className="px-4 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition-all"
                      >
                        Disable
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={handleSetupMfa}
                      disabled={mfaLoading || showMfaSetup}
                      className="px-5 py-2.5 text-sm font-semibold text-white bg-gradient-to-r from-blue-600 to-indigo-600 rounded-xl shadow-lg shadow-blue-500/30 hover:shadow-xl hover:shadow-blue-500/40 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                    >
                      {mfaLoading ? (
                        <>
                          <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                          </svg>
                          Setting up...
                        </>
                      ) : (
                        <>
                          <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                          </svg>
                          Enable MFA
                        </>
                      )}
                    </button>
                  )}
                </div>
              </div>

              {showMfaSetup && (
                <div className="border-t border-gray-100 pt-6 mt-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <div>
                      <h4 className="text-sm font-semibold text-gray-900 mb-4">Scan QR Code</h4>
                      <div className="bg-white p-4 rounded-xl border border-gray-200 inline-block">
                        {mfaQrUrl ? (
                          <img
                            src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(mfaQrUrl)}`}
                            alt="MFA QR Code"
                            className="w-48 h-48"
                          />
                        ) : (
                          <div className="w-48 h-48 bg-gray-100 rounded-xl flex items-center justify-center">
                            <svg className="w-8 h-8 text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
                              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                          </div>
                        )}
                      </div>
                      <p className="text-sm text-gray-500 mt-4">
                        Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.)
                      </p>
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-gray-900 mb-4">Enter Verification Code</h4>
                      <form onSubmit={handleVerifyMfa} className="space-y-4">
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1.5">
                            6-digit code from your authenticator app
                          </label>
                          <input
                            type="text"
                            maxLength={6}
                            minLength={6}
                            pattern="[0-9]{6}"
                            value={verificationCode}
                            onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ''))}
                            className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none text-center text-2xl tracking-widest font-mono"
                            placeholder="000000"
                            required
                          />
                        </div>
                        <div className="flex items-center space-x-3">
                          <button
                            type="submit"
                            disabled={verifyLoading || verificationCode.length !== 6}
                            className="flex-1 px-5 py-2.5 text-sm font-semibold text-white bg-gradient-to-r from-blue-600 to-indigo-600 rounded-xl shadow-lg shadow-blue-500/30 hover:shadow-xl hover:shadow-blue-500/40 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                          >
                            {verifyLoading ? 'Verifying...' : 'Verify & Enable'}
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setShowMfaSetup(false);
                              setVerificationCode('');
                              setMfaSecret('');
                              setMfaQrUrl('');
                            }}
                            className="px-5 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition-all"
                          >
                            Cancel
                          </button>
                        </div>
                      </form>
                      <div className="mt-6 p-4 bg-blue-50 rounded-xl">
                        <p className="text-sm text-blue-700">
                          <strong>Manual setup:</strong> If you can't scan the QR code, enter this secret manually in your authenticator app:
                        </p>
                        <code className="block mt-2 p-2 bg-white rounded-lg text-sm font-mono text-blue-600 border border-blue-200">
                          {mfaSecret}
                        </code>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="card p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Session Information</h3>
              <div className="space-y-3">
                <div className="flex items-center justify-between py-2 border-b border-gray-100">
                  <span className="text-sm text-gray-500">User ID</span>
                  <span className="text-sm font-mono text-gray-900">{userInfo?.id}</span>
                </div>
                <div className="flex items-center justify-between py-2">
                  <span className="text-sm text-gray-500">Email</span>
                  <span className="text-sm text-gray-900">{userInfo?.email}</span>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default Settings;
