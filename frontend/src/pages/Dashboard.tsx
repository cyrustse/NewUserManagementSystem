import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';

interface DashboardStats {
  totalUsers: number;
  activeRoles: number;
  pendingApprovals: number;
  systemHealth: number;
}

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [stats, setStats] = useState<DashboardStats>({
    totalUsers: 0,
    activeRoles: 0,
    pendingApprovals: 0,
    systemHealth: 99.9,
  });
  const [loading, setLoading] = useState(true);

  const navItems = [
    { path: '/', label: 'Dashboard', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
    { path: '/users', label: 'Users', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z' },
    { path: '/roles', label: 'Roles', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z' },
  ];

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const usersResponse = await apiService.clientInstance.get<{ data: { totalElements: number } }>('/users?size=1');
      const totalUsers = usersResponse.data.data.totalElements || 0;

      const rolesResponse = await apiService.clientInstance.get<{ data: { totalElements: number } }>('/roles?size=1');
      const activeRoles = rolesResponse.data.data.totalElements || 0;

      setStats({
        totalUsers,
        activeRoles,
        pendingApprovals: 0,
        systemHealth: 99.9,
      });
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const recentActivity = [
    { action: 'New user registered', user: 'john.doe@example.com', time: '2 minutes ago', type: 'user' },
    { action: 'Role updated', user: 'Admin', time: '15 minutes ago', type: 'role' },
    { action: 'Permission granted', user: 'jane.smith@example.com', time: '1 hour ago', type: 'permission' },
    { action: 'User deactivated', user: 'bob@example.com', time: '3 hours ago', type: 'user' },
    { action: 'System backup completed', user: 'System', time: '5 hours ago', type: 'system' },
  ];

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'user': return 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z';
      case 'role': return 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z';
      case 'permission': return 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z';
      default: return 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4';
    }
  };

  const getActivityColor = (type: string) => {
    switch (type) {
      case 'user': return 'bg-blue-50 text-blue-600';
      case 'role': return 'bg-violet-50 text-violet-600';
      case 'permission': return 'bg-emerald-50 text-emerald-600';
      default: return 'bg-gray-50 text-gray-600';
    }
  };

  // Skeleton loader component
  const StatCardSkeleton: React.FC = () => (
    <div className="card p-6 animate-pulse">
      <div className="flex items-center justify-between mb-4">
        <div className="w-12 h-12 rounded-xl bg-gray-200" />
      </div>
      <div className="h-8 bg-gray-200 rounded mb-2 w-20" />
      <div className="h-4 bg-gray-200 rounded w-24" />
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navigation */}
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
                  <p className="text-xs text-gray-500">{user?.email}</p>
                </div>
              </div>
              <button
                onClick={() => logout()}
                className="p-2.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all duration-200"
                title="Logout"
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

      {/* Main content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-500 mt-1.5">Overview of your user management system</p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {loading ? (
            <>
              <StatCardSkeleton />
              <StatCardSkeleton />
              <StatCardSkeleton />
              <StatCardSkeleton />
            </>
          ) : (
            <>
              {/* Total Users */}
              <div className="card-hover p-6 group cursor-pointer">
                <div className="flex items-center justify-between mb-4">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-50 to-indigo-50 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                    <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                    </svg>
                  </div>
                  <span className="badge-info">Total</span>
                </div>
                <p className="text-3xl font-bold text-gray-900">{stats.totalUsers}</p>
                <p className="text-sm text-gray-500 mt-1.5 font-medium">Total Users</p>
              </div>

              {/* Active Roles */}
              <div className="card-hover p-6 group cursor-pointer">
                <div className="flex items-center justify-between mb-4">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-violet-50 to-purple-50 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                    <svg className="w-6 h-6 text-violet-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                  </div>
                  <span className="badge-neutral">Active</span>
                </div>
                <p className="text-3xl font-bold text-gray-900">{stats.activeRoles}</p>
                <p className="text-sm text-gray-500 mt-1.5 font-medium">Active Roles</p>
              </div>

              {/* Pending Approvals */}
              <div className="card-hover p-6 group cursor-pointer">
                <div className="flex items-center justify-between mb-4">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-amber-50 to-orange-50 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                    <svg className="w-6 h-6 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <span className="badge-warning">Pending</span>
                </div>
                <p className="text-3xl font-bold text-gray-900">{stats.pendingApprovals}</p>
                <p className="text-sm text-gray-500 mt-1.5 font-medium">Pending Approvals</p>
              </div>

              {/* System Health */}
              <div className="card-hover p-6 group cursor-pointer">
                <div className="flex items-center justify-between mb-4">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-50 to-green-50 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                    <svg className="w-6 h-6 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                    </svg>
                  </div>
                  <span className="badge-success">Healthy</span>
                </div>
                <p className="text-3xl font-bold text-gray-900">{stats.systemHealth}%</p>
                <p className="text-sm text-gray-500 mt-1.5 font-medium">System Health</p>
              </div>
            </>
          )}
        </div>

        {/* Activity & Quick Actions */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Recent Activity */}
          <div className="lg:col-span-2 card overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">Recent Activity</h2>
                <p className="text-sm text-gray-500 mt-0.5">Latest actions in your system</p>
              </div>
              <button className="text-sm font-medium text-blue-600 hover:text-blue-700 transition-colors">
                View all
              </button>
            </div>
            <div className="divide-y divide-gray-50">
              {recentActivity.map((activity, index) => (
                <div key={index} className="px-6 py-4 flex items-center justify-between hover:bg-gray-50/80 transition-colors">
                  <div className="flex items-center space-x-4">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${getActivityColor(activity.type)}`}>
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={getActivityIcon(activity.type)} />
                      </svg>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-900">{activity.action}</p>
                      <p className="text-xs text-gray-500 mt-0.5">{activity.user}</p>
                    </div>
                  </div>
                  <span className="text-xs text-gray-400 font-medium">{activity.time}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="space-y-6">
            <h2 className="text-lg font-semibold text-gray-900">Quick Actions</h2>

            <Link
              to="/users"
              className="block group card p-6 text-white bg-gradient-to-br from-blue-600 to-indigo-600 hover:shadow-xl hover:shadow-blue-500/25 transition-all duration-300 transform hover:-translate-gray-y-1"
            >
              <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold mb-1">Add New User</h3>
              <p className="text-blue-100 text-sm">Create a new user account</p>
            </Link>

            <Link
              to="/roles"
              className="block group card p-6 text-white bg-gradient-to-br from-violet-600 to-purple-600 hover:shadow-xl hover:shadow-violet-500/25 transition-all duration-300 transform hover:-translate-gray-y-1"
            >
              <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold mb-1">Manage Roles</h3>
              <p className="text-violet-100 text-sm">Configure roles and permissions</p>
            </Link>

            <div className="block group card p-6 text-white bg-gradient-to-br from-emerald-600 to-teal-600 hover:shadow-xl hover:shadow-emerald-500/25 transition-all duration-300 transform hover:-translate-gray-y-1 cursor-pointer">
              <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold mb-1">View Reports</h3>
              <p className="text-emerald-100 text-sm">System analytics and reports</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;
