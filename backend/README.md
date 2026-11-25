# ⚙️ SkillUp Campus - Backend API

API REST desarrollada en **Java 21** y **Spring Boot** para gestionar la autenticación, el catálogo de cursos, las inscripciones de alumnos y el almacenamiento multimedia de la plataforma **SkillUp Campus**.

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** Java 21
* **Framework:** Spring Boot 3.2.5 (Web, Data JPA, Security, Validation)
* **Autenticación y Seguridad:** JWT (`jjwt` 0.11.5) con control de acceso por roles (`ALUMNO`, `ADMIN`)
* **Base de Datos:** MySQL (producción/local) y H2 (entorno de pruebas)
* **Mapeo y Utilidades:** MapStruct 1.5.5 + Lombok
* **Almacenamiento Multimedia:** Cloudflare R2 (vía AWS SDK S3 2.25.60) + compresión automática a WebP (`webp-imageio`)
* **Documentación API:** OpenAPI 3 / Swagger UI (`springdoc-openapi` 2.5.0)
* **Testing y Cobertura:** JUnit 5, Mockito, Spring Security Test y JaCoCo 0.8.11
* **Contenedores y Build:** Maven Wrapper (`mvnw`) y Docker

---

## 📂 Estructura del Proyecto

```text
backend/src/main/java/skillup_workspace/
├── config/         # Configuración de seguridad (SecurityConfig), CORS y beans
├── controller/     # Endpoints REST y anotaciones OpenAPI/Swagger
├── dto/            # Objetos de entrada (request) y salida (response) con validaciones
├── entity/         # Entidades JPA (User, Course, Enrollment)
├── exception/      # Excepciones personalizadas y GlobalExceptionHandler
├── mapper/         # Mappers con MapStruct
├── observability/  # Logging estructurado y trazabilidad
├── repository/     # Interfaces Spring Data JPA
├── security/       # Filtro JWT y utilidades de tokens
├── service/        # Lógica de negocio e implementaciones
└── storage/        # Integración con Cloudflare R2 y procesamiento de imágenes WebP
```

---

## 📋 Requisitos Previos

* **Opción local:** JDK 21 y MySQL 8+ (no es necesario tener Maven instalado gracias al wrapper `mvnw`).
* **Opción contenedores:** Docker instalado.

---

## 🔐 Variables de Entorno

El archivo `application.properties` lee las siguientes variables de entorno. La única estrictamente obligatoria para iniciar el servidor en local es `JWT_SECRET`.

| Variable | Descripción | Obligatoria | Valor por defecto |
| :--- | :--- | :---: | :--- |
| `JWT_SECRET` | Clave secreta en Base64 para firmar los tokens JWT | **Sí** | *(sin valor por defecto)* |
| `DB_URL` | URL JDBC de conexión a MySQL | No | `jdbc:mysql://localhost:3306/skillup_db` |
| `DB_USER` | Usuario de la base de datos | No | `root` |
| `DB_PASSWORD` | Contraseña de la base de datos | No | `root` |
| `R2_ENABLED` | Habilita o deshabilita la subida de archivos a Cloudflare R2 | No | `true` |
| `R2_ACCOUNT_ID` | ID de cuenta de Cloudflare R2 | Solo si `R2_ENABLED=true` | *(vacío)* |
| `R2_ACCESS_KEY_ID` | Access Key de R2 | Solo si `R2_ENABLED=true` | *(vacío)* |
| `R2_SECRET_ACCESS_KEY` | Secret Access Key de R2 | Solo si `R2_ENABLED=true` | *(vacío)* |
| `R2_BUCKET_NAME` | Nombre del bucket en R2 | No | `skillupcampus` |
| `R2_PUBLIC_URL` | URL pública base del bucket | Solo si `R2_ENABLED=true` | *(vacío)* |
| `IMAGE_WEBP_ENABLED` | Convierte imágenes subidas a formato WebP | No | `true` |

> **Tip para desarrollo local:** Si no cuentas con credenciales de Cloudflare R2, puedes levantar la API igualmente configurando `R2_ENABLED=false`.

---

## 🚀 Cómo levantar el proyecto localmente

### 1. Clonar el repositorio

```bash
git clone https://github.com/Talently-Lab/skillup-workspace.git
cd skillup-workspace/backend
```

### 2. Preparar la base de datos local

Crea una base de datos vacía en MySQL llamada `skillup_db` (las tablas se generan y actualizan automáticamente al iniciar el servidor mediante Hibernate):

```sql
CREATE DATABASE skillup_db;
```

### 3. Ejecutar el servidor

#### Opción A: Con Maven Wrapper (Local)

Configura al menos la variable `JWT_SECRET` en tu terminal (o en las *Run Configurations* de IntelliJ IDEA) y ejecuta el servidor según tu sistema operativo:

**Windows (PowerShell):**
```powershell
$env:JWT_SECRET="tu_clave_secreta_en_base64_de_al_menos_32_bytes"
$env:R2_ENABLED="false"
.\mvnw.cmd spring-boot:run
```

**Mac / Linux:**
```bash
export JWT_SECRET="tu_clave_secreta_en_base64_de_al_menos_32_bytes"
export R2_ENABLED="false"
./mvnw spring-boot:run
```

#### Opción B: Con Docker

Desde la carpeta `backend/`, construye la imagen y levanta el contenedor pasando tus variables de entorno:

```bash
docker build -t skillup-backend .

docker run -p 8080:8080 \
  -e JWT_SECRET="tu_clave_secreta_en_base64_de_al_menos_32_bytes" \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/skillup_db" \
  -e DB_USER="root" \
  -e DB_PASSWORD="root" \
  -e R2_ENABLED="false" \
  skillup-backend
```

---

## 📖 Documentación de la API (Swagger / OpenAPI)

Una vez levantado el servidor, la API estará disponible en `http://localhost:8080` y podrás explorar y probar todos los endpoints desde Swagger UI:

* **Swagger Local:** `http://localhost:8080/swagger-ui/index.html`
* **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Resumen de Endpoints Principales (`/api/v1`)

| Módulo | Método | Ruta | Acceso | Descripción |
| :--- | :---: | :--- | :---: | :--- |
| **Auth** | `POST` | `/api/v1/auth/register` | Público | Registro de nuevos usuarios (rol `ALUMNO`) |
| **Auth** | `POST` | `/api/v1/auth/login` | Público | Autenticación y obtención de Token JWT |
| **Cursos** | `GET` | `/api/v1/courses` | Público | Listado de cursos activos del catálogo |
| **Cursos** | `GET` | `/api/v1/courses/{id}` | Público | Detalle completo de un curso |
| **Cursos** | `POST` | `/api/v1/courses` | `ADMIN` | Creación de un nuevo curso |
| **Cursos** | `PUT` | `/api/v1/courses/{id}` | `ADMIN` | Edición de un curso existente |
| **Cursos** | `DELETE` | `/api/v1/courses/{id}` | `ADMIN` | Baja lógica de un curso |
| **Inscripciones** | `POST` | `/api/v1/enrollments` | `ALUMNO` | Inscripción del usuario autenticado a un curso activo |

---

## 🧪 Tests y Cobertura

Las pruebas unitarias y de integración utilizan JUnit 5, Mockito y una base de datos H2 en memoria. Para ejecutarlas y generar el reporte de cobertura con JaCoCo:

```bash
# Mac / Linux
./mvnw test

# Windows
.\mvnw.cmd test
```

El reporte HTML de cobertura se genera en `target/site/jacoco/index.html`.