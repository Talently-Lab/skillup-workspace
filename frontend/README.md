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
