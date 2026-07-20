import { AxiosError } from "axios";
import { api } from "./client";
import type {
  Course,
  CourseInput,
  DashboardStatistics,
  Department,
  DepartmentInput,
  PageResponse,
  ProblemDetails,
} from "../types";

export interface ListQuery {
  search?: string;
  status?: string;
  faculty?: string;
  page: number;
  size: number;
  sort: string;
  trash?: boolean;
}

export interface CourseListQuery extends ListQuery {
  departmentId?: number;
  semester?: string;
  academicYear?: string;
}

export const dashboardApi = {
  get: async () => (await api.get<DashboardStatistics>("/dashboard")).data,
};

export const departmentApi = {
  list: async (query: ListQuery) =>
    (await api.get<PageResponse<Department>>(
      `/departments${query.trash ? "/trash" : ""}`,
      { params: cleanParams(query) },
    )).data,
  get: async (id: number) => (await api.get<Department>(`/departments/${id}`)).data,
  create: async (body: DepartmentInput) =>
    (await api.post<Department>("/departments", body)).data,
  update: async (id: number, body: DepartmentInput) =>
    (await api.put<Department>(`/departments/${id}`, body)).data,
  remove: async (id: number) => api.delete(`/departments/${id}`),
  restore: async (id: number) =>
    (await api.post<Department>(`/departments/${id}/restore`)).data,
  permanentlyRemove: async (id: number) => api.delete(`/departments/${id}/permanent`),
};

export const courseApi = {
  list: async (query: CourseListQuery) =>
    (await api.get<PageResponse<Course>>(`/courses${query.trash ? "/trash" : ""}`, {
      params: cleanParams(query),
    })).data,
  get: async (id: number) => (await api.get<Course>(`/courses/${id}`)).data,
  create: async (body: CourseInput) => (await api.post<Course>("/courses", body)).data,
  update: async (id: number, body: CourseInput) =>
    (await api.put<Course>(`/courses/${id}`, body)).data,
  remove: async (id: number) => api.delete(`/courses/${id}`),
  restore: async (id: number) => (await api.post<Course>(`/courses/${id}/restore`)).data,
  permanentlyRemove: async (id: number) => api.delete(`/courses/${id}/permanent`),
};

export function problemFrom(error: unknown): ProblemDetails | undefined {
  return (error as AxiosError<ProblemDetails>)?.response?.data;
}

export function errorMessage(error: unknown, fallback: string): string {
  const problem = problemFrom(error);
  if (problem?.fieldErrors) return Object.values(problem.fieldErrors)[0] ?? problem.detail;
  return problem?.detail ?? fallback;
}

function cleanParams(query: ListQuery | CourseListQuery) {
  const { trash: _trash, ...params } = query;
  return Object.fromEntries(Object.entries(params).filter(([, value]) =>
    value !== undefined && value !== ""));
}
