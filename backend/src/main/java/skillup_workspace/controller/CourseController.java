package skillup_workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.service.CourseService;
import skillup_workspace.util.constants.CourseSwaggerConstants;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = CourseSwaggerConstants.TAG_NAME, description = CourseSwaggerConstants.TAG_DESCRIPTION)
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @Operation(
            summary = CourseSwaggerConstants.GET_ALL_SUMMARY,
            description = CourseSwaggerConstants.GET_ALL_DESCRIPTION
    )
    @ApiResponse(responseCode = "200", description = CourseSwaggerConstants.GET_ALL_200_MSG)
    public ResponseEntity<List<CourseResponseDTO>> getActiveCourses() {
        List<CourseResponseDTO> courses = courseService.getActiveCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = CourseSwaggerConstants.GET_BY_ID_SUMMARY,
            description = CourseSwaggerConstants.GET_BY_ID_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = CourseSwaggerConstants.GET_BY_ID_200_MSG),
            @ApiResponse(responseCode = "404", description = CourseSwaggerConstants.GET_BY_ID_404_MSG)
    })
    public ResponseEntity<CourseDetailResponseDTO> getCourseById(@PathVariable Integer id) {
        CourseDetailResponseDTO course = courseService.getCourseById(id);
        return ResponseEntity.ok(course);
    }

    @PostMapping
    @Operation(
            summary = CourseSwaggerConstants.CREATE_SUMMARY,
            description = CourseSwaggerConstants.CREATE_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = CourseSwaggerConstants.CREATE_201_MSG),
            @ApiResponse(responseCode = "400", description = CourseSwaggerConstants.CREATE_400_MSG)
    })
    public ResponseEntity<CourseDetailResponseDTO> createCourse(@Valid @RequestBody CourseRequestDTO requestDTO) {
        CourseDetailResponseDTO createdCourse = courseService.createCourse(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = CourseSwaggerConstants.UPDATE_SUMMARY,
            description = CourseSwaggerConstants.UPDATE_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = CourseSwaggerConstants.UPDATE_200_MSG),
            @ApiResponse(responseCode = "404", description = CourseSwaggerConstants.UPDATE_404_MSG)
    })
    public ResponseEntity<CourseDetailResponseDTO> updateCourse(
            @PathVariable Integer id,
            @Valid @RequestBody CourseRequestDTO requestDTO) {
        CourseDetailResponseDTO updatedCourse = courseService.updateCourse(id, requestDTO);
        return ResponseEntity.ok(updatedCourse);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = CourseSwaggerConstants.DELETE_SUMMARY,
            description = CourseSwaggerConstants.DELETE_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = CourseSwaggerConstants.DELETE_204_MSG),
            @ApiResponse(responseCode = "404", description = CourseSwaggerConstants.DELETE_404_MSG)
    })
    public ResponseEntity<Void> deleteCourse(@PathVariable Integer id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}