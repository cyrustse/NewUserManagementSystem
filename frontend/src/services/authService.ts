import { apiService } from './api';

export const authService = {
  login: async (credentials: { usernameOrEmail: string; password: string }) => {
    // Tokens are set via httpOnly cookies by the backend
    const response = await apiService.clientInstance.post('/auth/login', credentials);
    return response;
  },

  refreshToken: async () => {
    // Refresh token is read from cookie by backend
    return apiService.clientInstance.post('/auth/refresh', {});
  },

  logout: async () => {
    return apiService.clientInstance.post('/auth/logout');
  },

  getCurrentUser: async () => {
    // Cookies are automatically sent with requests (withCredentials: true)
    return apiService.clientInstance.get('/users/me');
  },

  verifyMfa: async (code: string, tempToken: string) => {
    return apiService.clientInstance.post('/auth/mfa/verify', { code }, {
      headers: { 'X-Auth-Token': tempToken }
    });
  },
};
