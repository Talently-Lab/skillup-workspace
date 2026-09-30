package skillup_workspace.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.Instructor;
import skillup_workspace.exception.ExceptionConstants;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.mapper.CourseMapper;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.InstructorRepository;
import skillup_workspace.service.CourseService;

import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseRepository courseRepository,
                             InstructorRepository instructorRepository,
                             CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.instructorRepository = instructorRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getActiveCourses() {
        return courseRepository.findByActiveTrue()
                .stream()
                .map(courseMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponseDTO getCourseById(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ExceptionConstants.COURSE_NOT_FOUND, id)
                ));
        return courseMapper.toDetailResponseDTO(course);
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO createCourse(CourseRequestDTO requestDTO) {
        Course course = courseMapper.toEntity(requestDTO);

        if (course.getInstructor() != null) {
            Instructor savedInstructor = instructorRepository.save(course.getInstructor());
            course.setInstructor(savedInstructor);
        }
        Course savedCourse = courseRepository.save(course);
        return courseMapper.toDetailResponseDTO(savedCourse);
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO updateCourse(Integer id, CourseRequestDTO requestDTO) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ExceptionConstants.COURSE_NOT_FOUND, id)
                ));

        courseMapper.updateEntityFromDto(requestDTO, existingCourse);

        if (existingCourse.getInstructor() != null) {
            Instructor savedInstructor = instructorRepository.save(existingCourse.getInstructor());
            existingCourse.setInstructor(savedInstructor);
        }

        Course updatedCourse = courseRepository.save(existingCourse);
        return courseMapper.toDetailResponseDTO(updatedCourse);
    }

    @Override
    @Transactional
    public void deleteCourse(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ExceptionConstants.COURSE_NOT_FOUND, id)
                ));

        course.setActive(false);
        courseRepository.save(course);
    }
}