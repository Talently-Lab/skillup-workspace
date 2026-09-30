package skillup_workspace.util.constants;

public final class CourseSwaggerConstants {

    private CourseSwaggerConstants() {
    }

    // Tag
    public static final String TAG_NAME = "Cursos";
    public static final String TAG_DESCRIPTION = "Endpoints para la consulta pública y administración (CRUD) del catálogo de cursos";

    // GET /api/v1/courses
    public static final String GET_ALL_SUMMARY = "Obtener catálogo general de cursos activos";
    public static final String GET_ALL_DESCRIPTION = "Retorna una lista simplificada de los cursos activos para renderizar las tarjetas del catálogo.";
    public static final String GET_ALL_200_MSG = "Lista de cursos recuperada exitosamente";

    // GET /api/v1/courses/{id}
    public static final String GET_BY_ID_SUMMARY = "Obtener información detallada de un curso";
    public static final String GET_BY_ID_DESCRIPTION = "Retorna todos los detalles completos de un curso específico a partir de su ID.";
    public static final String GET_BY_ID_200_MSG = "Curso encontrado";
    public static final String GET_BY_ID_404_MSG = "Curso no encontrado";

    // POST /api/v1/courses
    public static final String CREATE_SUMMARY = "Crear un nuevo curso";
    public static final String CREATE_DESCRIPTION = "Permite al Administrador registrar un curso completo en el catálogo.";
    public static final String CREATE_201_MSG = "Curso creado exitosamente";
    public static final String CREATE_400_MSG = "Datos de entrada inválidos";

    // PUT /api/v1/courses/{id}
    public static final String UPDATE_SUMMARY = "Actualizar un curso";
    public static final String UPDATE_DESCRIPTION = "Permite al Administrador actualizar toda la información de un curso previamente registrado.";
    public static final String UPDATE_200_MSG = "Curso actualizado exitosamente";
    public static final String UPDATE_404_MSG = "El curso no fue encontrado";

    // DELETE /api/v1/courses/{id}
    public static final String DELETE_SUMMARY = "Desactivar o eliminar un curso";
    public static final String DELETE_DESCRIPTION = "Permite al Administrador desactivar un curso existente.";
    public static final String DELETE_204_MSG = "Curso eliminado/desactivado correctamente";
    public static final String DELETE_404_MSG = "El curso no fue encontrado";
}