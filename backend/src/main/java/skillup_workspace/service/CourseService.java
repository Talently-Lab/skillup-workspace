package skillup_workspace.service;

import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;

import java.util.List;

public interface CourseService {

    List<CourseResponseDTO> getAllCourses();

    CourseDetailResponseDTO getCourseById(Long id);

    CourseDetailResponseDTO createCourse(CourseRequestDTO requestDTO);

    CourseDetailResponseDTO updateCourse(Long id, CourseRequestDTO requestDTO);

    void deleteCourse(Long id);
}