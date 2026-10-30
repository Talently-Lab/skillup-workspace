package skillup_workspace.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skillup_workspace.dto.request.EnrollmentRequestDTO;
import skillup_workspace.dto.response.EnrollmentResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.Enrollment;
import skillup_workspace.entity.User;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.EnrollmentRepository;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.service.EnrollmentService;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public EnrollmentResponseDTO enrollUser(EnrollmentRequestDTO request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = "";

        if (auth.getPrincipal() instanceof User) {
            email = ((User) auth.getPrincipal()).getEmail();
        } else {
            email = auth.getName();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con el id: " + request.getCourseId()));

        if (!course.getIsActive()) {
            throw new IllegalStateException("No puedes inscribirte a un curso inactivo.");
        }

        if (enrollmentRepository.existsByUserAndCourse(user, course)) {
            throw new IllegalStateException("El usuario ya se encuentra inscripto en este curso.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus("ACTIVE");

        enrollment = enrollmentRepository.save(enrollment);

        EnrollmentResponseDTO response = new EnrollmentResponseDTO();
        response.setId(enrollment.getId());
        response.setCourseId(course.getId());
        response.setCourseTitle(course.getTitle());
        response.setStatus(enrollment.getStatus());

        return response;
    }
}