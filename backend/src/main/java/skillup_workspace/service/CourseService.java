package skillup_workspace.service;

import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.CourseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CourseService {

    List<CourseResponseDTO> getAllCourses();

    CourseDetailResponseDTO getCourseById(Long id);

    CourseDetailResponseDTO createCourse(CourseRequestDTO requestDTO);

    CourseDetailResponseDTO createCourse(
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus
    );

    CourseDetailResponseDTO createCourse(
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus,
            Map<String, MultipartFile> moduleVideos
    );

    CourseDetailResponseDTO updateCourse(Long id, CourseRequestDTO requestDTO);

    CourseDetailResponseDTO updateCourseStatus(Long id, CourseStatus status);

    CourseDetailResponseDTO updateCourse(
            Long id,
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus
    );

    CourseDetailResponseDTO updateCourse(
            Long id,
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus,
            Map<String, MultipartFile> moduleVideos
    );

    void deleteCourse(Long id);
}