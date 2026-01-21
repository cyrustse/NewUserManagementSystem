import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { toast } from 'react-toastify';

interface RoleResponse {
  id: string;
  name: string;
  description: string;
  system: boolean;
  parentId: string | null;
  parentName: string | null;
  priority: number;
  permissions: string[] | null;
  userCount: number;
  createdAt: string;
}

interface Role {
  id: string;
  name: string;
  description: string;
  permissions: string[];
  users: number;
  system: boolean;
  priority: number;
}

const Roles: React.FC = () => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [searchQuery, setSearchQuery] = useState('');
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showPermissionsModal, setShowPermissionsModal] = useState(false);
  const [showViewModal, setShowViewModal] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [allPermissions, setAllPermissions] = useState<any[]>([]);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<string[]>([]);
  const [loadingPermissions, setLoadingPermissions] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    priority: 50,
  });
  const [editFormData, setEditFormData] = useState({
    name: '',
    description: '',
    priority: 50,
  });

  const fetchRoles = async () => {
    try {
      setLoading(true);
      const response = await apiService.clientInstance.get<{ data: { content: RoleResponse[] } }>('/roles');
      const roleResponses = response.data.data.content;

      const mappedRoles: Role[] = roleResponses.map((r: RoleResponse) => ({
        id: r.id,
        name: r.name,
        description: r.description,
        permissions: r.permissions || [],
        users: r.userCount || 0,
        system: r.system,
        priority: r.priority,
      }));

      setRoles(mappedRoles);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch roles:', err);
      setError('Failed to load roles');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRole = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await apiService.createRole({
        name: formData.name,
        description: formData.description,
        priority: formData.priority,
      });
      toast.success('Role created successfully');
      setShowCreateModal(false);
      setFormData({ name: '', description: '', priority: 50 });
      fetchRoles();
    } catch (err: any) {
      const message = err.response?.data?.message || 'Failed to create role';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (role: Role) => {
    setSelectedRole(role);
    setEditFormData({
      name: role.name,
      description: role.description,
      priority: role.priority,
    });
    setShowEditModal(true);
  };

  const handleDelete = (role: Role) => {
    setSelectedRole(role);
    setShowDeleteModal(true);
  };

  const handleView = (role: Role) => {
    setSelectedRole(role);
    setShowViewModal(true);
  };

  const handleUpdateRole = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRole) return;

    setSubmitting(true);
    try {
      await apiService.updateRole(selectedRole.id, editFormData);
      toast.success('Role updated successfully');
      setShowEditModal(false);
      setSelectedRole(null);
      fetchRoles();
    } catch (err: any) {
      const message = err.response?.data?.message || 'Failed to update role';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!selectedRole) return;

    setSubmitting(true);
    try {
      await apiService.deleteRole(selectedRole.id);
      toast.success('Role deleted successfully');
      setShowDeleteModal(false);
      setSelectedRole(null);
      fetchRoles();
    } catch (err: any) {
      const message = err.response?.data?.message || 'Failed to delete role';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleManagePermissions = async (role: Role) => {
    setSelectedRole(role);
    setLoadingPermissions(true);
    setShowPermissionsModal(true);

    try {
      // Fetch all permissions
      const permissionsResponse = await apiService.getPermissions(0, 100, '');
      const permissions = permissionsResponse.data?.content || [];

      // Fetch role's current permissions
      const rolePermissions = await apiService.getRolePermissions(role.id);

      setAllPermissions(permissions);
      setSelectedPermissionIds(rolePermissions);
    } catch (err: any) {
      console.error('Failed to load permissions:', err);
      toast.error('Failed to load permissions');
    } finally {
      setLoadingPermissions(false);
    }
  };

  const handleTogglePermission = (permissionId: string) => {
    setSelectedPermissionIds(prev =>
      prev.includes(permissionId)
        ? prev.filter(id => id !== permissionId)
        : [...prev, permissionId]
    );
  };

  const handleSavePermissions = async () => {
    if (!selectedRole) return;

    setSubmitting(true);
    try {
      await apiService.updateRolePermissions(selectedRole.id, selectedPermissionIds);
      toast.success('Permissions updated successfully');
      setShowPermissionsModal(false);
      setSelectedRole(null);
      fetchRoles();
    } catch (err: any) {
      const message = err.response?.data?.message || 'Failed to update permissions';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    fetchRoles();
  }, []);

  const navItems = [
    { path: '/', label: 'Dashboard', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
    { path: '/users', label: 'Users', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z' },
    { path: '/roles', label: 'Roles', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z' },
    { path: '/settings', label: 'Settings', icon: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z' },
  ];

  const getPriorityColor = (priority: number) => {
    if (priority >= 80) return 'from-red-500 to-orange-500';
    if (priority >= 60) return 'from-orange-500 to-amber-500';
    if (priority >= 40) return 'from-amber-500 to-yellow-500';
    return 'from-emerald-500 to-teal-500';
  };

  const getPriorityBadge = (priority: number) => {
    if (priority >= 80) return 'bg-red-50 text-red-700 border-red-200';
    if (priority >= 60) return 'bg-orange-50 text-orange-700 border-orange-200';
    if (priority >= 40) return 'bg-amber-50 text-amber-700 border-amber-200';
    return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  };

  const getPriorityText = (priority: number) => {
    if (priority >= 80) return 'Critical';
    if (priority >= 60) return 'High';
    if (priority >= 40) return 'Medium';
    return 'Low';
  };

  const filteredRoles = roles.filter(r =>
    r.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    r.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const RoleCardSkeleton: React.FC = () => (
    <div className="card p-6 animate-pulse">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-xl bg-gray-200" />
          <div>
            <div className="h-5 bg-gray-200 rounded w-32 mb-2" />
            <div className="h-4 bg-gray-200 rounded w-20" />
          </div>
        </div>
        <div className="h-6 bg-gray-200 rounded w-16" />
      </div>
      <div className="h-4 bg-gray-200 rounded w-full mb-4" />
      <div className="flex items-center justify-between mb-4">
        <div className="h-4 bg-gray-200 rounded w-20" />
        <div className="h-4 bg-gray-200 rounded w-20" />
      </div>
      <div className="flex flex-wrap gap-2">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-6 bg-gray-200 rounded w-16" />
        ))}
      </div>
    </div>
  );

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
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Roles</h1>
            <p className="text-gray-500 mt-1.5">Configure roles and permissions</p>
          </div>
          <button onClick={() => setShowCreateModal(true)} className="mt-4 sm:mt-0 btn-primary">
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
            Create Role
          </button>
        </div>

        <div className="card p-5 mb-6">
          <div className="relative group">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none transition-colors group-focus-within:text-blue-600">
              <svg className="h-5 w-5 text-gray-400 group-focus-within:text-blue-600 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              placeholder="Search roles by name or description..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="input-with-icon"
              aria-label="Search roles"
            />
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <RoleCardSkeleton key={i} />
            ))}
          </div>
        ) : error ? (
          <div className="flex items-center justify-center py-16">
            <div className="text-center">
              <svg className="w-12 h-12 text-red-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p className="text-red-600 font-medium">{error}</p>
            </div>
          </div>
        ) : filteredRoles.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <p className="text-gray-600 font-medium">No roles found</p>
            <p className="text-sm text-gray-400 mt-1">Try adjusting your search</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredRoles.map((role) => (
              <div key={role.id} className="card-hover overflow-hidden group">
                <div className="px-6 py-5 border-b border-gray-100 flex items-center justify-between">
                  <div className="flex items-center space-x-4">
                    <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${getPriorityColor(role.priority)} flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-300`}>
                      <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900">{role.name}</h3>
                      {role.system && (
                        <span className="inline-flex items-center px-2 py-0.5 text-xs font-medium bg-violet-50 text-violet-700 rounded-md mt-1">
                          System Role
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <span className={`inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-full border ${getPriorityBadge(role.priority)}`}>
                      {getPriorityText(role.priority)}
                    </span>
                  </div>
                </div>
                <div className="px-6 py-5">
                  <p className="text-sm text-gray-600 mb-5 line-clamp-2">{role.description}</p>

                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center space-x-2 text-gray-600">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                      </svg>
                      <span className="text-sm font-medium">{role.users} users</span>
                    </div>
                    <div className="flex items-center space-x-2 text-gray-600">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                      <span className="text-sm font-medium">{role.permissions.length} permissions</span>
                    </div>
                  </div>

                  {role.permissions.length > 0 && (
                    <div className="flex flex-wrap gap-1.5">
                      {role.permissions.slice(0, 4).map((perm, index) => (
                        <span key={index} className="badge-neutral">{perm}</span>
                      ))}
                      {role.permissions.length > 4 && (
                        <span className="inline-flex items-center px-2.5 py-1 text-xs font-medium bg-gray-50 text-gray-500 rounded-lg">
                          +{role.permissions.length - 4} more
                        </span>
                      )}
                    </div>
                  )}
                </div>

                <div className="px-6 py-3 bg-gray-50/50 border-t border-gray-100 flex items-center justify-end space-x-2">
                  <button
                    onClick={() => handleView(role)}
                    className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition-all duration-200"
                    title="View"
                    aria-label="View role"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  </button>
                  <button
                    onClick={() => handleManagePermissions(role)}
                    className="p-2 text-gray-400 hover:text-violet-600 hover:bg-violet-50 rounded-xl transition-all duration-200"
                    title="Manage Permissions"
                    aria-label="Manage permissions"
                    disabled={role.system}
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                  </button>
                  <button
                    onClick={() => handleEdit(role)}
                    className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition-all duration-200"
                    title="Edit"
                    aria-label="Edit role"
                    disabled={role.system}
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button
                    onClick={() => handleDelete(role)}
                    className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all duration-200"
                    title="Delete"
                    aria-label="Delete role"
                    disabled={role.system}
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-md animate-fade-in">
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h2 className="text-xl font-semibold text-gray-900">Create New Role</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <form onSubmit={handleCreateRole} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Role Name *</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none"
                  placeholder="Enter role name"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Description *</label>
                <textarea
                  required
                  rows={3}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none resize-none"
                  placeholder="Enter role description"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Priority (1-100) *</label>
                <input
                  type="number"
                  required
                  min={1}
                  max={100}
                  value={formData.priority}
                  onChange={(e) => setFormData({ ...formData, priority: parseInt(e.target.value) || 50 })}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none"
                />
                <p className="text-xs text-gray-500 mt-1">Higher priority roles take precedence</p>
              </div>
              <div className="flex items-center justify-end space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-5 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition-all"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2.5 text-sm font-semibold text-white bg-gradient-to-r from-blue-600 to-indigo-600 rounded-xl shadow-lg shadow-blue-500/30 hover:shadow-xl hover:shadow-blue-500/40 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {submitting ? 'Creating...' : 'Create Role'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showEditModal && selectedRole && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-md animate-fade-in">
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h2 className="text-xl font-semibold text-gray-900">Edit Role</h2>
              <button
                onClick={() => setShowEditModal(false)}
                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <form onSubmit={handleUpdateRole} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Role Name *</label>
                <input
                  type="text"
                  required
                  value={editFormData.name}
                  onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none"
                  placeholder="Enter role name"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Description *</label>
                <textarea
                  required
                  rows={3}
                  value={editFormData.description}
                  onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none resize-none"
                  placeholder="Enter role description"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Priority (1-100) *</label>
                <input
                  type="number"
                  required
                  min={1}
                  max={100}
                  value={editFormData.priority}
                  onChange={(e) => setEditFormData({ ...editFormData, priority: parseInt(e.target.value) || 50 })}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none"
                />
                <p className="text-xs text-gray-500 mt-1">Higher priority roles take precedence</p>
              </div>
              <div className="flex items-center justify-end space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="px-5 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition-all"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2.5 text-sm font-semibold text-white bg-gradient-to-r from-blue-600 to-indigo-600 rounded-xl shadow-lg shadow-blue-500/30 hover:shadow-xl hover:shadow-blue-500/40 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {submitting ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showDeleteModal && selectedRole && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-md animate-fade-in">
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h2 className="text-xl font-semibold text-gray-900">Delete Role</h2>
              <button
                onClick={() => setShowDeleteModal(false)}
                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="p-6">
              <div className="flex items-center justify-center w-16 h-16 bg-red-100 rounded-full mx-auto mb-4">
                <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <p className="text-center text-gray-600 mb-6">
                Are you sure you want to delete <span className="font-semibold text-gray-900">{selectedRole.name}</span>?
                This action cannot be undone.
              </p>
              <div className="flex items-center justify-end space-x-3">
                <button
                  onClick={() => setShowDeleteModal(false)}
                  className="px-5 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition-all"
                >
                  Cancel
                </button>
                <button
                  onClick={handleConfirmDelete}
                  disabled={submitting}
                  className="px-5 py-2.5 text-sm font-semibold text-white bg-gradient-to-r from-red-600 to-rose-600 rounded-xl shadow-lg shadow-red-500/30 hover:shadow-xl hover:shadow-red-500/40 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {submitting ? 'Deleting...' : 'Delete Role'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showPermissionsModal && selectedRole && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-2xl animate-fade-in max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <div>
                <h2 className="text-xl font-semibold text-gray-900">Manage Permissions</h2>
                <p className="text-sm text-gray-500 mt-1">Assign permissions to {selectedRole.name}</p>
              </div>
              <button
                onClick={() => setShowPermissionsModal(false)}
                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              {loadingPermissions ? (
                <div className="flex items-center justify-center py-12">
                  <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : allPermissions.length === 0 ? (
                <div className="text-center py-12">
                  <svg className="w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  <p className="text-gray-600 font-medium">No permissions available</p>
                  <p className="text-sm text-gray-400 mt-1">Create permissions in the Permissions page</p>
                </div>
              ) : (
                <div className="space-y-2 max-h-[400px] overflow-y-auto">
                  {allPermissions.map((permission: any) => (
                    <label
                      key={permission.id}
                      className={`flex items-center p-4 rounded-xl border cursor-pointer transition-all ${
                        selectedPermissionIds.includes(permission.id)
                          ? 'border-blue-200 bg-blue-50'
                          : 'border-gray-100 hover:border-gray-200 hover:bg-gray-50'
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={selectedPermissionIds.includes(permission.id)}
                        onChange={() => handleTogglePermission(permission.id)}
                        className="w-5 h-5 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                      />
                      <div className="ml-3 flex-1">
                        <p className="text-sm font-medium text-gray-900">{permission.name}</p>
                        <p className="text-xs text-gray-500">
                          {permission.action} {permission.resource ? `• ${permission.resource}` : ''}
                        </p>
                      </div>
                      {selectedPermissionIds.includes(permission.id) && (
                        <span className="badge-success">Assigned</span>
                      )}
                    </label>
                  ))}
                </div>
              )}
            </div>

            <div className="flex items-center justify-between p-6 border-t border-gray-100">
              <p className="text-sm text-gray-600">
                {selectedPermissionIds.length} permission(s) selected
              </p>
              <div className="flex items-center space-x-3">
                <button
                  type="button"
                  onClick={() => setShowPermissionsModal(false)}
                  className="px-5 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition-all"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSavePermissions}
                  disabled={submitting}
                  className="px-5 py-2.5 text-sm font-semibold text-white bg-gradient-to-r from-blue-600 to-indigo-600 rounded-xl shadow-lg shadow-blue-500/30 hover:shadow-xl hover:shadow-blue-500/40 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {submitting ? 'Saving...' : 'Save Permissions'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showViewModal && selectedRole && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-md animate-fade-in">
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h2 className="text-xl font-semibold text-gray-900">Role Details</h2>
              <button
                onClick={() => setShowViewModal(false)}
                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">Role Name</label>
                <p className="text-gray-900 font-medium">{selectedRole.name}</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">Description</label>
                <p className="text-gray-900">{selectedRole.description || 'No description'}</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">Priority</label>
                <p className="text-gray-900">{selectedRole.priority} - {
                  selectedRole.priority >= 80 ? 'Critical' :
                  selectedRole.priority >= 60 ? 'High' :
                  selectedRole.priority >= 40 ? 'Medium' : 'Low'
                }</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">Users Assigned</label>
                <p className="text-gray-900">{selectedRole.users} users</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">System Role</label>
                <p className="text-gray-900">{selectedRole.system ? 'Yes (cannot be deleted)' : 'No'}</p>
              </div>
              {selectedRole.permissions.length > 0 && (
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">Permissions ({selectedRole.permissions.length})</label>
                  <div className="flex flex-wrap gap-1.5">
                    {selectedRole.permissions.slice(0, 6).map((perm, index) => (
                      <span key={index} className="badge-neutral">{perm}</span>
                    ))}
                    {selectedRole.permissions.length > 6 && (
                      <span className="inline-flex items-center px-2.5 py-1 text-xs font-medium bg-gray-50 text-gray-500 rounded-lg">
                        +{selectedRole.permissions.length - 6} more
                      </span>
                    )}
                  </div>
                </div>
              )}
            </div>
            <div className="p-6 border-t border-gray-100 flex justify-end">
              <button
                onClick={() => setShowViewModal(false)}
                className="px-5 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition-all"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Roles;
