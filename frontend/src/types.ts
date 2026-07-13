export interface User {
  id: number;
  fullName: string;
  email: string;
  enabled: boolean;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: User;
}
