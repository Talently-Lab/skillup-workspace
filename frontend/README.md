# SkillUp Campus Frontend

Aplicacion frontend de SkillUp Campus creada con React + Vite usando JavaScript.

## Requisitos

- Node.js 20 o superior
- npm 10 o superior

## Configuracion

1. Instalar dependencias:

   ```bash
   npm install
   ```

2. Crear `.env.local` en la raiz de `frontend/` a partir de `.env.example` si
   necesitás configurar variables locales:

   ```bash
   cp .env.example .env.local
   ```

   Vite carga las variables desde la raiz del proyecto; `.env.example` es solo
   una plantilla y no se carga directamente. `.env.local` no se versiona.
   La variable `VITE_API_BASE_URL` debe contener la URL base del backend,
   incluyendo `/api/v1`. Para desarrollar contra un backend local, usar
   `http://localhost:8080/api/v1`.

   En Netlify, configurar `VITE_API_BASE_URL` con
   `https://skillup-workspace.onrender.com/api/v1` en las variables de entorno
   del sitio y volver a desplegar.

## Comandos

- Ejecutar en modo desarrollo:

  ```bash
  npm run dev
  ```

- Generar build de produccion:

  ```bash
  npm run build
  ```

- Previsualizar el build:

  ```bash
  npm run preview
  ```

- Ejecutar lint:

  ```bash
  npm run lint
  ```


## Stack

React + Vite + Tailwind CSS + React Router + Axios

## Estructura de carpetas (`src/`)

- `components/`: piezas reutilizables (`ui`, `layout`, `courses`, `home`)
- `pages/`: pantallas de la aplicación
- `routes/`: definición de rutas
- `services/`: llamadas a la API
- `context/`: estado compartido (por ejemplo, la sesión)
- `data/`: datos de prueba mientras la API no esté disponible

## Variables de entorno

`src/services/api.js` usa `VITE_API_BASE_URL` como base de todas las peticiones
HTTP. Los archivos `.env`, `.env.local` y `.env.example` deben estar en la raiz
de `frontend/`, no dentro de `src/`.