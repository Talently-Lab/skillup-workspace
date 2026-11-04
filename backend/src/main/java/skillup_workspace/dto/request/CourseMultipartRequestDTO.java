package skillup_workspace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CourseMultipartRequest", description = "Formulario multipart para crear o actualizar un curso")
public class CourseMultipartRequestDTO {

    @Schema(
            description = "Datos del curso en formato JSON",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private CourseRequestDTO course;

    @Schema(
            type = "string",
            format = "binary",
            description = "Imagen opcional del curso"
    )
    private String image;

    @Schema(
            type = "string",
            format = "binary",
            description = "Programa PDF opcional"
    )
    private String syllabus;

    public CourseRequestDTO getCourse() {
        return course;
    }

    public String getImage() {
        return image;
    }

    public String getSyllabus() {
        return syllabus;
    }
}
