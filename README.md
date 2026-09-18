# 🚀 SkillUp Campus - Backend API

Bienvenido al repositorio del Backend de SkillUp Campus, una plataforma web desarrollada en 8 semanas para centralizar la gestión de alumnos y cursos.

## 👥 Integrantes del Equipo
* **Backend Developers:** Alexis, Yus
* **Frontend Developers:** Sabrina, Joaquin
* **QA:** Jimena
* **Data Analyst:** Lorena
* **UX/UI:** Julieta
* **Project Manager:** Nicko

## 🛠 Decisiones Técnicas (Stack)
* **Lenguaje:** Java 17+
* **Framework:** Spring Boot
* **Base de Datos:** MySQL
* **Gestor de dependencias:** Maven

---

## ⚙️ Cómo levantar el proyecto localmente (Para el equipo)

Para que los equipos de Frontend y QA puedan consumir la API en sus computadoras para sus pruebas, sigan estos pasos:

### 1. Clonar el repositorio
```bash
git clone [https://github.com/Talently-Lab/skillup-workspace.git](https://github.com/Talently-Lab/skillup-workspace.git)
cd skillup-workspace/backend
```

### 2. Configurar la Base de Datos Local
Nuestro archivo `application.properties` está configurado para buscar variables de entorno, pero si no las tienen, utilizará credenciales por defecto. Para que funcione a la primera, asegúrense de tener MySQL instalado y creen una base de datos con estos datos exactos:

* **Nombre de la BD:** `skillup_db`
* **Usuario:** `root`
* **Contraseña:** `root`

### 3. Levantar el Servidor
Abran su terminal dentro de la carpeta `backend` y ejecuten el comando correspondiente a su sistema operativo:

**Para Windows:**
```cmd
mvnw.cmd spring-boot:run
```

**Para Mac / Linux:**
```bash
./mvnw spring-boot:run
```

### 4. Probar la API
Si todo salió bien, verán el logo de Spring Boot en la consola y la API estará corriendo localmente en:
`http://localhost:8080`
