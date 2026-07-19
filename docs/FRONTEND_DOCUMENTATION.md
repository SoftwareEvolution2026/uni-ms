# Frontend Documentation

**Project:** University Student Management System (uni-ms)

**Author:** Umar-Faruq Salihu, 23/378/BSSE-M

---

## Overview

This document describes the frontend implementation of the uni-ms project.
The frontend is built with React, Vite, and TypeScript.

## Architecture

- **Framework:** React 18
- **Build:** Vite
- **Language:** TypeScript
- **Router:** React Router
- **HTTP client:** Axios
- **State:** React context for authentication and simple component state

## Folder structure

- `src/`
  - `api/`
  - `auth/`
  - `components/`
  - `pages/`
  - `types.ts`

## Key frontend features

- Login page with JWT-based authentication
- Protected routes for authenticated users
- Role-based access control for admin and lecturer
- Dashboard page with account actions
- User management page for admins
- Results page for Team 4 result management

## Important files

- `src/App.tsx` — application routes and protected route configuration
- `src/auth/AuthContext.tsx` — authentication state, login, logout handling
- `src/auth/ProtectedRoute.tsx` — route guard logic based on user roles
- `src/pages/Dashboard.tsx` — main dashboard page
- `src/pages/Users.tsx` — admin user management page
- `src/pages/Results.tsx` — Team 4 results page
- `src/api/client.ts` — Axios client with token refresh handling
- `src/types.ts` — shared frontend data types

## Configuration

- Environment variable: `VITE_API_BASE_URL`

## How to run

```bash
cd frontend
pnpm install
pnpm dev
```

## Notes

- Add any local environment instructions here.
- Add deployment or build notes here.

## Additional details

(Leave this section blank for later completion.)

- Name:
- Role:
- Date:
- Comments:
