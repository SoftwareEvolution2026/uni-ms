export type Role = "ADMIN" | "ACADEMIC_MANAGER";
export type RegistryStatus = "ACTIVE" | "INACTIVE";
export type Semester = "SEMESTER_1" | "SEMESTER_2" | "SUMMER";

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: User;
}

export interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  code: string;
  detail: string;
  instance: string;
  timestamp: string;
  fieldErrors?: Record<string, string>;
}

export interface SortState {
  property: string;
  direction: "ASC" | "DESC";
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  sort: SortState[];
}

export interface Department {
  id: number;
  departmentName: string;
  departmentCode: string;
  faculty: string;
  description: string | null;
  status: RegistryStatus;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  version: number;
  courseCount: number;
}

export interface DepartmentInput {
  departmentName: string;
  departmentCode: string;
  faculty: string;
  description: string | null;
  status: RegistryStatus;
  version?: number;
}

export interface Course {
  id: number;
  departmentId: number;
  courseName: string;
  courseCode: string;
  creditUnits: number;
  semester: Semester;
  academicYear: string;
  description: string | null;
  status: RegistryStatus;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  version: number;
}

export interface CourseInput {
  departmentId: number;
  courseName: string;
  courseCode: string;
  creditUnits: number;
  semester: Semester;
  academicYear: string;
  description: string | null;
  status: RegistryStatus;
  version?: number;
}

export interface DashboardStatistics {
  totalDepartments: number;
  activeDepartments: number;
  inactiveDepartments: number;
  totalCourses: number;
  activeCourses: number;
  inactiveCourses: number;
}
