import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { toast } from 'react-toastify';

const MfaSetup: React.FC = () => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [step, setStep] = useState<'setup' | 'verify' | 'complete'>('setup');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [mfaSecret, setMfaSecret] = useState('');
  const [qrCodeUrl, setQrCodeUrl] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [backupCodes, setBackupCodes] = useState<string[]>([]);

  useEffect(() => {
    setupMfa();
  }, []);

  const setupMfa = async () => {
    setLoading(true);
    try {
      const response = await apiService.setupMfa();
      setMfaSecret(response.secret);
      setQrCodeUrl(response.qrUrl);
    } catch (err: any) {
      toast.error('Failed to setup MFA. Please try again.');
      console.error('MFA setup error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      await apiService.verifyMfa(verificationCode, mfaSecret);
      setBackupCodes(generateBackupCodes());
      setStep('complete');
      toast.success('MFA enabled successfully!');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Invalid verification code. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const generateBackupCodes = (): string[] => {
    const codes: string[] = [];
    for (let i = 0; i < 8; i++) {
      const code = Math.random().toString(36).substring(2, 8).toUpperCase();
      codes.push(`${code.slice(0, 4)}-${code.slice(4)}`);
    }
    return codes;
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    toast.success('Copied to clipboard!');
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
              <div className="hidden md:flex items-center ml-10 space-x-2">
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

      <main className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-gradient-to-br from-violet-500 to-purple-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg shadow-violet-500/30">
            <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Two-Factor Authentication</h1>
          <p className="text-gray-500 mt-2">Add an extra layer of security to your account</p>
        </div>

        {loading ? (
          <div className="card p-8 flex items-center justify-center">
            <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : step === 'setup' ? (
          <div className="card p-8">
            <div className="space-y-6">
              <div className="bg-blue-50 border border-blue-100 rounded-xl p-4">
                <h3 className="text-sm font-semibold text-blue-800 mb-2">Step 1: Scan QR Code</h3>
                <p className="text-sm text-blue-700 mb-4">Use your authenticator app (Google Authenticator, Authy, etc.) to scan this QR code.</p>

                <div className="bg-white p-4 rounded-xl flex items-center justify-center mb-4">
                  {qrCodeUrl ? (
                    <img src={qrCodeUrl} alt="MFA QR Code" className="w-48 h-48" />
                  ) : (
                    <div className="w-48 h-48 bg-gray-100 rounded-xl flex items-center justify-center">
                      <svg className="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
                      </svg>
                    </div>
                  )}
                </div>

                <div className="text-center">
                  <p className="text-xs text-blue-600 mb-2">Can't scan? Enter this code manually:</p>
                  <code className="bg-white px-3 py-2 rounded-lg text-sm font-mono text-blue-800 border border-blue-200">
                    {mfaSecret}
                  </code>
                </div>
              </div>

              <button
                onClick={() => setStep('verify')}
                className="w-full btn-primary"
              >
                I've scanned the QR code
              </button>

              <button
                onClick={() => window.history.back()}
                className="w-full btn-secondary"
              >
                Cancel
              </button>
            </div>
          </div>
        ) : step === 'verify' ? (
          <div className="card p-8">
            <form onSubmit={handleVerify} className="space-y-6">
              <div className="bg-amber-50 border border-amber-100 rounded-xl p-4">
                <h3 className="text-sm font-semibold text-amber-800 mb-2">Step 2: Verify Code</h3>
                <p className="text-sm text-amber-700">Enter the 6-digit code from your authenticator app to verify setup.</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Verification Code
                </label>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  value={verificationCode}
                  onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ''))}
                  className="w-full px-4 py-4 bg-gray-50 border border-gray-200 rounded-xl text-center text-2xl font-mono tracking-widest focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none"
                  placeholder="000000"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={submitting || verificationCode.length !== 6}
                className="w-full btn-primary"
              >
                {submitting ? 'Verifying...' : 'Enable MFA'}
              </button>

              <button
                type="button"
                onClick={() => setStep('setup')}
                className="w-full btn-secondary"
              >
                Back
              </button>
            </form>
          </div>
        ) : (
          <div className="card p-8">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-gradient-to-br from-emerald-500 to-green-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-gray-900">MFA Enabled Successfully!</h2>
              <p className="text-gray-500 mt-2">Your account is now protected with two-factor authentication.</p>
            </div>

            <div className="bg-red-50 border border-red-100 rounded-xl p-4 mb-6">
              <div className="flex items-start space-x-3">
                <svg className="w-5 h-5 text-red-500 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <div>
                  <h3 className="text-sm font-semibold text-red-800">Save your backup codes</h3>
                  <p className="text-sm text-red-700 mt-1">Use these codes if you lose access to your authenticator app. Each code can only be used once.</p>
                </div>
              </div>
            </div>

            <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 mb-6">
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-sm font-medium text-gray-700">Backup Codes</h4>
                <button
                  onClick={() => copyToClipboard(backupCodes.join('\n'))}
                  className="text-sm text-blue-600 hover:text-blue-700 font-medium"
                >
                  Copy All
                </button>
              </div>
              <div className="grid grid-cols-2 gap-2">
                {backupCodes.map((code, index) => (
                  <code key={index} className="block bg-white px-3 py-2 rounded-lg text-sm font-mono text-gray-700 border border-gray-200 text-center">
                    {code}
                  </code>
                ))}
              </div>
            </div>

            <Link to="/" className="block w-full btn-primary text-center">
              Go to Dashboard
            </Link>
          </div>
        )}
      </main>
    </div>
  );
};

export default MfaSetup;
