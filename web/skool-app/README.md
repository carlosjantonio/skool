# Skool web app

React + Vite + TypeScript PWA. Default locale is pt-AO.

## Dev

```sh
cd web/skool-app
npm install
npm run dev
```

Runs on http://localhost:5173 and proxies `/api/*` and `/actuator/*` to the Spring backend at `http://localhost:8080`.

Log in with the seeded demo admin: `admin@skool.demo` / `admin123`.

## Build

```sh
npm run build      # → dist/
npm run preview    # serves the built app
```

## Structure

```
src/
├── api/          fetch wrapper with auth + refresh
├── auth/         AuthContext + ProtectedRoute
├── components/   Layout
├── i18n/         pt-AO (default) + en resource bundles
└── pages/        LoginPage, DashboardPage, UnauthorizedPage
```

## Adding a locale

Drop a JSON bundle beside `src/i18n/pt-AO.json`, register it in `src/i18n/index.ts`, then set `lng` per user preference.
