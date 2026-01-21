import axios, { AxiosInstance, AxiosError } from 'axios';

class ApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: import.meta.env.VITE_API_URL || 'http://localhost/api/v1',
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 30000,
      withCredentials: true, // Enable sending cookies
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // Request interceptor - not needed for cookie auth since cookies are automatic
    // But we keep it for potential header additions
    this.client.interceptors.request.use((config) => {
      // Cookies are automatically sent with withCredentials: true
      return config;
    });

    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as any;

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            await this.refreshAccessToken();
            return this.client(originalRequest);
          } catch (refreshError) {
            this.clearTokens();
            window.location.href = '/login';
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  /**
   * Tokens are now stored in httpOnly cookies by the backend
   * This method is kept for backwards compatibility but is a no-op
   */
  public setTokens(_accessToken: string, _refreshToken: string): void {
    // Tokens are set via httpOnly cookies from backend response
    // This is a no-op for cookie-based auth
  }

  public clearTokens(): void {
    // Tokens are cleared via logout endpoint which sets expired cookies
  }

  public hasToken(): boolean {
    // Check if user is authenticated by trying to get current user
    return true; // Will be determined by API call
  }

  /**
   * Refresh access token - backend reads refresh token from cookie
   */
  private async refreshAccessToken(): Promise<void> {
    // Backend will read refresh token from cookie automatically
    await axios.post(
      `${import.meta.env.VITE_API_URL}/auth/refresh`,
      {},
      { withCredentials: true }
    );
    // New access token is set by backend via cookie
  }

  public get clientInstance(): AxiosInstance {
    return this.client;
  }

  public async createUser(userData: {
    username: string;
    email: string;
    password: string;
    phone?: string;
    roleIds?: string[];
  }): Promise<any> {
    const response = await this.client.post('/users', userData);
    return response.data;
  }

  public async updateUser(userId: string, userData: {
    username?: string;
    email?: string;
    phone?: string;
    status?: string;
    password?: string;
  }): Promise<any> {
    const response = await this.client.put(`/users/${userId}`, userData);
    return response.data;
  }

  public async deleteUser(userId: string): Promise<any> {
    const response = await this.client.delete(`/users/${userId}`);
    return response.data;
  }

  public async createRole(roleData: {
    name: string;
    description: string;
    priority: number;
    permissionIds?: string[];
  }): Promise<any> {
    const response = await this.client.post('/roles', roleData);
    return response.data;
  }

  public async updateRole(roleId: string, roleData: {
    name?: string;
    description?: string;
    priority?: number;
  }): Promise<any> {
    const response = await this.client.put(`/roles/${roleId}`, roleData);
    return response.data;
  }

  public async deleteRole(roleId: string): Promise<any> {
    const response = await this.client.delete(`/roles/${roleId}`);
    return response.data;
  }

  public async getRoles(page = 0, size = 50, search = ''): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (search) params.append('search', search);
    const response = await this.client.get(`/roles?${params.toString()}`);
    return response.data;
  }

  public async assignUserRole(userId: string, roleId: string): Promise<any> {
    const response = await this.client.post(`/users/${userId}/roles`, { roleId });
    return response.data;
  }

  public async removeUserRole(userId: string, roleId: string): Promise<any> {
    const response = await this.client.delete(`/users/${userId}/roles/${roleId}`);
    return response.data;
  }

  public async activateUser(userId: string): Promise<any> {
    const response = await this.client.post(`/users/${userId}/activate`);
    return response.data;
  }

  public async deactivateUser(userId: string): Promise<any> {
    const response = await this.client.post(`/users/${userId}/deactivate`);
    return response.data;
  }

  public async setupMfa(): Promise<any> {
    const response = await this.client.post('/auth/mfa/setup');
    return response.data.data;
  }

  public async verifyMfa(code: string, secret: string): Promise<any> {
    const response = await this.client.post('/auth/mfa/verify', { code, secret });
    return response.data;
  }

  // Role Permissions
  public async getRolePermissions(roleId: string): Promise<string[]> {
    const response = await this.client.get<{ data: string[] }>(`/roles/${roleId}/permissions`);
    return response.data.data;
  }

  public async updateRolePermissions(roleId: string, permissionIds: string[]): Promise<any> {
    const response = await this.client.put(`/roles/${roleId}/permissions`, permissionIds);
    return response.data;
  }

  // Permissions
  public async getPermissions(page = 0, size = 50, search = ''): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (search) params.append('search', search);
    const response = await this.client.get(`/permissions?${params.toString()}`);
    return response.data;
  }

  public async createPermission(permissionData: {
    name: string;
    resourceId?: string;
    action: string;
    conditions?: string;
  }): Promise<any> {
    const response = await this.client.post('/permissions', permissionData);
    return response.data;
  }

  public async deletePermission(permissionId: string): Promise<any> {
    const response = await this.client.delete(`/permissions/${permissionId}`);
    return response.data;
  }
}

export const apiService = new ApiService();
