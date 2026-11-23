# SkillUp Campus | QA del MVP

**Testing funcional de una plataforma web de cursos online:** desde el análisis de historias de usuario y contratos API hasta la ejecución de pruebas, el seguimiento de bugs y el reporte de resultados.

## Resultados del proyecto

| Pruebas | Ejecución | Defectos |
|:--|:--|:--|
| **191 casos** (117 UI + 74 API) | **188 Pass · 0 Fail** | **10 registrados** |
| 2 pendientes · 1 bloqueado | **98,4 %** de ejecución | **9 cerrados · 1 abierto** |

*Estado documentado al 22/11/2026. El porcentaje refleja casos ejecutados, no cobertura total del producto.*

## Qué se probó

- **Usuarios y acceso:** registro, login, logout, sesiones y permisos de Alumno/Admin.
- **Experiencia del alumno:** catálogo, búsqueda, filtros, detalle e inscripción a cursos; visualización de cursos inscriptos.
- **Administración:** panel, gestión de cursos y consulta de inscriptos y usuarios.
- **API:** endpoints, códigos HTTP, validaciones, autenticación y autorización.

## Cómo se trabajó

Diseño de casos a partir de **historias de usuario, criterios de aceptación, brief y OpenAPI**. Ejecución de escenarios positivos, negativos y de borde; verificaciones responsive y de seguridad funcional. Tras cambios de infraestructura, se reejecutaron pruebas API y se realizaron **smoke testing y regresión** sobre la interfaz.

**Herramientas:** Postman, Newman, Google Sheets, Google Docs y Jam para evidencias de bugs.

## Documentación y evidencias

- 📋 [Plan de pruebas QA](https://docs.google.com/document/d/1S_e7DVmBRdd102RZi14CMXKA5C5mtkys0hb1cGma5fM/edit) — alcance, estrategia y evaluación del cierre.
- ✅ [Casos UI y API · Bugs · Dashboard](https://docs.google.com/spreadsheets/d/15LQKcp5c-Vuz8vAOClJITl8GWga4pgn-7vgwqfQf7sw/edit) — resultados y seguimiento de defectos.
- 📖 [Historias de usuario y criterios refinados](https://docs.google.com/document/d/1figWkco2glHcfS_CJV7UwsL54Qp2xQEdeJxmta9BTHw/edit) — trazabilidad y definiciones funcionales.
- 📊 [Reporte de ejecución Newman](https://drive.google.com/file/d/1xRrAWiz2LYpAqBLhR8672pCxRdwqXUVB/view?usp=drive_link) — evidencia complementaria de las pruebas API.

**Cómo visualizarlo:** descargá el archivo HTML desde Google Drive y abrilo en Chrome o Edge; la vista previa de Drive no muestra el reporte interactivo.

## Estado del cierre QA

La entrega documenta **2 casos pendientes**, **1 bloqueado** por falta de definición sobre sesiones entre pestañas y **1 bug abierto** relacionado con fechas pasadas en «Próxima fecha» (regla aún sin confirmación formal). No se ejecutó una auditoría de accesibilidad WCAG y el acceso al aula virtual quedó fuera del alcance validado.

**El cierre QA informa los resultados y riesgos conocidos; no equivale a certificar un producto libre de defectos.**
