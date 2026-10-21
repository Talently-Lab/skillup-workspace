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

2. Crear un archivo `.env` a partir de `.env.example` si necesitás configurar variables locales:

   ```bash
   cp .env.example .env
   ```

   Variable disponible:

   ```env
   VITE_API_BASE_URL=http://localhost:8080
   ```

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

Copiar `.env.example` a `.env` y completar `VITE_API_BASE_URL` con la URL del backend (por defecto `http://localhost:8080`).