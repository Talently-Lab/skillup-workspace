package skillup_workspace.service;

import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;

import java.util.List;

public interface CourseService {
    List<CourseResponseDTO> getActiveCourses();
    CourseDetailResponseDTO getCourseById(Integer id);
    CourseDetailResponseDTO createCourse(CourseRequestDTO requestDTO);
    CourseDetailResponseDTO updateCourse(Integer id, CourseRequestDTO requestDTO);
    void deleteCourse(Integer id);
}