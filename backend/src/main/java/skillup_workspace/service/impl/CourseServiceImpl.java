package skillup_workspace.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.mapper.CourseMapper;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.service.CourseService;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getAllCourses() {
        boolean isAdmin = checkIsAdmin();
        List<Course> courses = courseRepository.findAllCoursesWithFilter(isAdmin);
        return courseMapper.toResponseDTOList(courses);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con el id: " + id));

        if (!Boolean.TRUE.equals(course.getIsActive()) && !checkIsAdmin()) {
            throw new ResourceNotFoundException("Curso no encontrado con el id: " + id);
        }

        return courseMapper.toDetailResponseDTO(course);
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO createCourse(CourseRequestDTO requestDTO) {
        Course course = courseMapper.toEntity(requestDTO);
        Course savedCourse = courseRepository.save(course);
        return courseMapper.toDetailResponseDTO(savedCourse);
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO updateCourse(Long id, CourseRequestDTO requestDTO) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con el id: " + id));

        courseMapper.updateEntityFromDto(requestDTO, existingCourse);
        Course updatedCourse = courseRepository.save(existingCourse);
        return courseMapper.toDetailResponseDTO(updatedCourse);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con el id: " + id));

        course.setIsActive(false);
        courseRepository.save(course);
    }
    private boolean checkIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> Objects.equals(grantedAuthority.getAuthority(), "ROLE_ADMIN"));
    }
}