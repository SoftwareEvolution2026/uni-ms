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

export interface Result {
  id: number;
  studentId: number;
  courseCode: string;
  term: string;
  grade: string;
  score: number;
  credits: number;
}
