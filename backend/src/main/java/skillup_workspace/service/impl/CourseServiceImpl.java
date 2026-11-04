package skillup_workspace.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.CourseStatus;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.mapper.CourseMapper;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.CourseFavoriteRepository;
import skillup_workspace.repository.EnrollmentRepository;
import skillup_workspace.service.CourseService;
import skillup_workspace.storage.R2StorageService;
import skillup_workspace.storage.R2StorageService.StoredAsset;
import skillup_workspace.storage.ImageConversionService;
import skillup_workspace.storage.UploadPurpose;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final CourseFavoriteRepository courseFavoriteRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseMapper courseMapper;
    private final R2StorageService r2StorageService;
    private final ImageConversionService imageConversionService;

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getAllCourses() {
        boolean isAdmin = checkIsAdmin();
        List<Course> courses = courseRepository.findAllCoursesWithFilter(isAdmin);
        List<CourseResponseDTO> response = courseMapper.toResponseDTOList(courses);
        Map<Long, Long> activeEnrollmentCounts = new HashMap<>();
        enrollmentRepository.findActiveEnrollmentCountsByCourse().forEach(count ->
                activeEnrollmentCounts.put(count.getCourseId(), count.getActiveEnrollmentCount())
        );
        response.forEach(course ->
                course.setActiveEnrollmentCount(activeEnrollmentCounts.getOrDefault(course.getId(), 0L))
        );
        return response;
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
        return createCourse(requestDTO, null, null);
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO createCourse(
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus
    ) {
        List<StoredAsset> uploadedAssets = new ArrayList<>();
        try {
            Course course = courseMapper.toEntity(requestDTO);
            CourseStatus initialStatus = requestDTO.getStatus() != null
                    ? requestDTO.getStatus()
                    : CourseStatus.PUBLICADO;
            course.setStatus(initialStatus);
            course.setIsActive(initialStatus == CourseStatus.PUBLICADO);
            course.setImageUrl(null);
            course.setSyllabusUrl(null);

            if (image != null) {
                byte[] webpImage = imageConversionService.convertToWebp(image).content();
                StoredAsset asset = r2StorageService.uploadWebp(webpImage);
                uploadedAssets.add(asset);
                course.setImageUrl(asset.publicUrl());
            }
            if (syllabus != null) {
                StoredAsset asset = r2StorageService.upload(syllabus, UploadPurpose.COURSE_SYLLABUS);
                uploadedAssets.add(asset);
                course.setSyllabusUrl(asset.publicUrl());
            }

            Course savedCourse = courseRepository.save(course);
            CourseDetailResponseDTO response = courseMapper.toDetailResponseDTO(savedCourse);
            registerAssetCleanup(uploadedAssets, List.of());
            return response;
        } catch (RuntimeException exception) {
            cleanupUploadedAssets(uploadedAssets, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO updateCourse(Long id, CourseRequestDTO requestDTO) {
        return updateCourse(id, requestDTO, null, null);
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO updateCourseStatus(Long id, CourseStatus status) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con el id: " + id));

        if (status == CourseStatus.PUBLICADO && course.getStatus() != CourseStatus.PUBLICADO) {
            course.setCreatedAt(LocalDateTime.now());
        }
        course.setStatus(status);
        course.setIsActive(status == CourseStatus.PUBLICADO);
        return courseMapper.toDetailResponseDTO(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO updateCourse(
            Long id,
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus
    ) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con el id: " + id));

        String oldImageUrl = existingCourse.getImageUrl();
        String oldSyllabusUrl = existingCourse.getSyllabusUrl();
        List<StoredAsset> uploadedAssets = new ArrayList<>();
        List<String> replacedUrls = new ArrayList<>();

        try {
            courseMapper.updateEntityFromDto(requestDTO, existingCourse);
            existingCourse.setImageUrl(oldImageUrl);
            existingCourse.setSyllabusUrl(oldSyllabusUrl);

            if (image != null) {
                byte[] webpImage = imageConversionService.convertToWebp(image).content();
                StoredAsset asset = r2StorageService.uploadWebp(webpImage);
                uploadedAssets.add(asset);
                existingCourse.setImageUrl(asset.publicUrl());
                if (oldImageUrl != null && !oldImageUrl.equals(asset.publicUrl())) {
                    replacedUrls.add(oldImageUrl);
                }
            }
            if (syllabus != null) {
                StoredAsset asset = r2StorageService.upload(syllabus, UploadPurpose.COURSE_SYLLABUS);
                uploadedAssets.add(asset);
                existingCourse.setSyllabusUrl(asset.publicUrl());
                if (oldSyllabusUrl != null && !oldSyllabusUrl.equals(asset.publicUrl())) {
                    replacedUrls.add(oldSyllabusUrl);
                }
            }

            Course updatedCourse = courseRepository.save(existingCourse);
            CourseDetailResponseDTO response = courseMapper.toDetailResponseDTO(updatedCourse);
            registerAssetCleanup(uploadedAssets, replacedUrls);
            return response;
        } catch (RuntimeException exception) {
            cleanupUploadedAssets(uploadedAssets, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con el id: " + id));

        if (enrollmentRepository.existsByCourse_Id(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar el curso porque tiene inscripciones asociadas."
            );
        }

        courseFavoriteRepository.deleteByCourse_Id(id);
        courseRepository.delete(course);
    }
    private boolean checkIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority ->
                        Objects.equals(grantedAuthority.getAuthority(), "ADMIN")
                                || Objects.equals(grantedAuthority.getAuthority(), "ROLE_ADMIN")
                );
    }

    private void registerAssetCleanup(List<StoredAsset> uploadedAssets, List<String> replacedUrls) {
        if (uploadedAssets.isEmpty() && replacedUrls.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("No se pudo asegurar la consistencia de los archivos del curso.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    deleteReplacedAssetsWhenUnused(replacedUrls);
                } else {
                    cleanupUploadedAssets(uploadedAssets, null);
                }
            }
        });
    }

    private void deleteReplacedAssetsWhenUnused(List<String> replacedUrls) {
        for (String url : replacedUrls) {
            try {
                if (!courseRepository.existsByImageUrl(url) && !courseRepository.existsBySyllabusUrl(url)) {
                    r2StorageService.deleteManagedAsset(url);
                }
            } catch (RuntimeException exception) {
                log.warn("No se pudo limpiar el archivo antiguo del curso después de actualizarlo.", exception);
            }
        }
    }

    private void cleanupUploadedAssets(List<StoredAsset> uploadedAssets, RuntimeException cause) {
        for (StoredAsset asset : uploadedAssets) {
            try {
                r2StorageService.deleteAsset(asset);
            } catch (RuntimeException cleanupException) {
                log.warn("No se pudo limpiar un archivo R2 que no quedó asociado a un curso.", cleanupException);
                if (cause != null) {
                    cause.addSuppressed(cleanupException);
                }
            }
        }
    }
}