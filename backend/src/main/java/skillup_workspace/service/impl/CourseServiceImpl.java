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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.multipart.MultipartFile;
import skillup_workspace.dto.request.ContentModuleRequestDTO;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.CourseStatus;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.exception.R2StorageException;
import skillup_workspace.mapper.CourseMapper;
import skillup_workspace.observability.CourseCreationPerformanceLog;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.CourseFavoriteRepository;
import skillup_workspace.repository.EnrollmentRepository;
import skillup_workspace.service.CourseService;
import skillup_workspace.storage.R2StorageService;
import skillup_workspace.storage.R2StorageService.StoredAsset;
import skillup_workspace.storage.ImageConversionService;
import skillup_workspace.storage.R2StorageService.PreparedUpload;
import skillup_workspace.storage.UploadPurpose;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.net.URI;

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
    private final CoursePersistenceService coursePersistenceService;
    private final CourseAssetCleanupService assetCleanupService;
    private final CourseUploadBulkhead courseUploadBulkhead;
    private final ThreadPoolTaskExecutor courseUploadExecutor;

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
    public CourseDetailResponseDTO createCourse(CourseRequestDTO requestDTO) {
        return createCourse(requestDTO, null, null);
    }

    @Override
    public CourseDetailResponseDTO createCourse(
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus
    ) {
        return createCourse(requestDTO, image, syllabus, Map.of());
    }

    @Override
    public CourseDetailResponseDTO createCourse(
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus,
            Map<String, MultipartFile> moduleVideos
    ) {
        long processStartedAt = System.nanoTime();
        List<StoredAsset> uploadedAssets = new ArrayList<>();
        Map<Integer, PreparedUpload> preparedModuleVideos = new HashMap<>();
        AtomicBoolean rollbackCleanupRegistered = new AtomicBoolean();
        String result = "failure";
        boolean uploadPermitAcquired = false;
        try {
            validateModuleVideoRequest(requestDTO, moduleVideos);
            if (image != null || syllabus != null || hasModuleVideoFiles(moduleVideos)) {
                long waitStartedAt = System.nanoTime();
                try {
                    courseUploadBulkhead.acquire();
                } catch (RuntimeException exception) {
                    CourseCreationPerformanceLog.warning(log, "course_creation_stage")
                            .addKeyValue("stage", "upload_capacity_wait")
                            .addKeyValue("durationMs", (System.nanoTime() - waitStartedAt) / 1_000_000)
                            .addKeyValue("result", "failure")
                            .addKeyValue("errorType", exception.getClass().getSimpleName())
                            .log("Course upload capacity wait failed");
                    throw exception;
                }
                uploadPermitAcquired = true;
                CourseCreationPerformanceLog.event(log, "course_creation_stage")
                        .addKeyValue("stage", "upload_capacity_wait")
                        .addKeyValue("durationMs", (System.nanoTime() - waitStartedAt) / 1_000_000)
                        .addKeyValue("result", "success")
                        .log("Course upload capacity acquired");
            }
            Course course = courseMapper.toEntity(requestDTO);
            CourseStatus initialStatus = requestDTO.getStatus() != null
                    ? requestDTO.getStatus()
                    : CourseStatus.PUBLICADO;
            course.setStatus(initialStatus);
            course.setIsActive(initialStatus == CourseStatus.PUBLICADO);
            course.setImageUrl(null);
            course.setSyllabusUrl(null);

            byte[] webpImage = null;
            PreparedUpload preparedSyllabus = null;
            if (image != null) {
                long conversionStartedAt = System.nanoTime();
                try {
                    webpImage = imageConversionService.convertToWebp(image).content();
                    CourseCreationPerformanceLog.event(log, "course_creation_stage")
                            .addKeyValue("stage", "image_conversion")
                            .addKeyValue("durationMs", (System.nanoTime() - conversionStartedAt) / 1_000_000)
                            .addKeyValue("result", "success")
                            .log("Course image conversion completed");
                } catch (RuntimeException exception) {
                    CourseCreationPerformanceLog.warning(log, "course_creation_stage")
                            .addKeyValue("stage", "image_conversion")
                            .addKeyValue("durationMs", (System.nanoTime() - conversionStartedAt) / 1_000_000)
                            .addKeyValue("result", "failure")
                            .addKeyValue("errorType", exception.getClass().getSimpleName())
                            .log("Course image conversion failed");
                    throw exception;
                }
            }

            long fileValidationStartedAt = System.nanoTime();
            try {
                if (webpImage != null) {
                    r2StorageService.validateWebp(webpImage);
                }
                if (syllabus != null) {
                    preparedSyllabus = r2StorageService.prepareUpload(syllabus, UploadPurpose.COURSE_SYLLABUS);
                }
                for (ContentModuleRequestDTO module : modulesOrEmpty(requestDTO)) {
                    MultipartFile video = moduleVideoFor(module, moduleVideos);
                    if (video != null && !video.isEmpty()) {
                        preparedModuleVideos.put(
                                module.getModuleNumber(),
                                r2StorageService.prepareUpload(video, UploadPurpose.COURSE_MODULE_VIDEO)
                        );
                    }
                }
            } catch (RuntimeException exception) {
                CourseCreationPerformanceLog.warning(log, "course_creation_stage")
                        .addKeyValue("stage", "file_validation")
                        .addKeyValue("durationMs", (System.nanoTime() - fileValidationStartedAt) / 1_000_000)
                        .addKeyValue("result", "failure")
                        .addKeyValue("errorType", exception.getClass().getSimpleName())
                        .log("Course upload input validation failed");
                throw exception;
            }

            CourseCreationPerformanceLog.event(log, "course_creation_stage")
                    .addKeyValue("stage", "file_validation")
                    .addKeyValue("durationMs", (System.nanoTime() - fileValidationStartedAt) / 1_000_000)
                    .addKeyValue("result", "success")
                    .log("Course upload inputs validated");

            List<UploadTask> uploadTasks = new ArrayList<>();
            if (webpImage != null) {
                byte[] imageContent = webpImage;
                uploadTasks.add(new UploadTask(
                        "image",
                        imageContent.length,
                        () -> r2StorageService.uploadWebp(imageContent)
                ));
            }
            if (preparedSyllabus != null) {
                PreparedUpload syllabusContent = preparedSyllabus;
                uploadTasks.add(new UploadTask(
                        "pdf",
                        syllabusContent.fileSize(),
                        () -> r2StorageService.upload(syllabusContent)
                ));
            }
            preparedModuleVideos.forEach((moduleNumber, upload) -> uploadTasks.add(new UploadTask(
                    "module-video-" + moduleNumber,
                    upload.fileSize(),
                    () -> r2StorageService.upload(upload)
            )));

            uploadAssets(uploadTasks, uploadedAssets);
            for (StoredAsset asset : uploadedAssets) {
                if (asset.objectKey().startsWith("courses/images/")) {
                    course.setImageUrl(asset.publicUrl());
                } else if (asset.objectKey().startsWith("courses/syllabi/")) {
                    course.setSyllabusUrl(asset.publicUrl());
                }
            }
            setModuleVideoUrls(requestDTO, preparedModuleVideos, uploadedAssets);
            course.setContentModules(requestDTO.getContentModules());

            CourseDetailResponseDTO response = coursePersistenceService.persistCreatedCourse(
                    course,
                    uploadedAssets,
                    rollbackCleanupRegistered
            );
            result = "success";
            return response;
        } catch (RuntimeException exception) {
            if (!rollbackCleanupRegistered.get()) {
                assetCleanupService.cleanupUploadedAssets(uploadedAssets, exception);
            }
            throw exception;
        } finally {
            if (uploadPermitAcquired) {
                courseUploadBulkhead.release();
            }
            CourseCreationPerformanceLog.event(log, "course_creation_stage")
                    .addKeyValue("stage", "service_process")
                    .addKeyValue("durationMs", (System.nanoTime() - processStartedAt) / 1_000_000)
                    .addKeyValue("result", result)
                    .log("Course creation service process completed");
        }
    }

    private void uploadAssets(List<UploadTask> uploadTasks, List<StoredAsset> uploadedAssets) {
        List<CompletableFuture<StoredAsset>> uploads = new ArrayList<>();
        RuntimeException failure = null;
        for (UploadTask uploadTask : uploadTasks) {
            try {
                UploadTask submittedTask = new UploadTask(
                        uploadTask.name(),
                        uploadTask.fileBytes(),
                        uploadTask.upload(),
                        System.nanoTime()
                );
                uploads.add(CompletableFuture.supplyAsync(
                        () -> uploadAsset(submittedTask),
                        courseUploadExecutor
                ));
            } catch (RejectedExecutionException exception) {
                CourseCreationPerformanceLog.warning(log, "course_creation_storage")
                        .addKeyValue("operation", uploadTask.name())
                        .addKeyValue("durationMs", 0)
                        .addKeyValue("fileBytes", uploadTask.fileBytes())
                        .addKeyValue("result", "rejected")
                        .addKeyValue("errorType", exception.getClass().getSimpleName())
                        .log("Course upload task was rejected by the bounded executor");
                failure = new R2StorageException(
                        "El servicio de carga de archivos está ocupado. Intenta nuevamente.",
                        exception
                );
                break;
            }
        }

        for (CompletableFuture<StoredAsset> upload : uploads) {
            try {
                uploadedAssets.add(upload.join());
            } catch (CompletionException exception) {
                Throwable cause = exception.getCause();
                if (failure == null) {
                    failure = cause instanceof RuntimeException runtimeException
                            ? runtimeException
                            : new R2StorageException("No se pudo completar la carga de archivos.", cause);
                } else if (cause != null) {
                    failure.addSuppressed(cause);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private StoredAsset uploadAsset(UploadTask uploadTask) {
        long startedAt = System.nanoTime();
        long queueWaitMs = (startedAt - uploadTask.submittedAtNanos()) / 1_000_000;
        try {
            StoredAsset asset = uploadTask.upload().get();
            CourseCreationPerformanceLog.event(log, "course_creation_storage")
                    .addKeyValue("operation", uploadTask.name())
                    .addKeyValue("queueWaitMs", queueWaitMs)
                    .addKeyValue("durationMs", (System.nanoTime() - startedAt) / 1_000_000)
                    .addKeyValue("fileBytes", uploadTask.fileBytes())
                    .addKeyValue("result", "success")
                    .log("Course asset uploaded to R2");
            return asset;
        } catch (RuntimeException exception) {
            CourseCreationPerformanceLog.warning(log, "course_creation_storage")
                    .addKeyValue("operation", uploadTask.name())
                    .addKeyValue("queueWaitMs", queueWaitMs)
                    .addKeyValue("durationMs", (System.nanoTime() - startedAt) / 1_000_000)
                    .addKeyValue("fileBytes", uploadTask.fileBytes())
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Course asset upload to R2 failed");
            throw exception;
        }
    }

    private record UploadTask(
            String name,
            long fileBytes,
            Supplier<StoredAsset> upload,
            long submittedAtNanos
    ) {
        private UploadTask(String name, long fileBytes, Supplier<StoredAsset> upload) {
            this(name, fileBytes, upload, System.nanoTime());
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
        return updateCourse(id, requestDTO, image, syllabus, Map.of());
    }

    @Override
    @Transactional
    public CourseDetailResponseDTO updateCourse(
            Long id,
            CourseRequestDTO requestDTO,
            MultipartFile image,
            MultipartFile syllabus,
            Map<String, MultipartFile> moduleVideos
    ) {
        validateModuleVideoRequest(requestDTO, moduleVideos);
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con el id: " + id));

        String oldImageUrl = existingCourse.getImageUrl();
        String oldSyllabusUrl = existingCourse.getSyllabusUrl();
        Set<String> oldModuleVideoUrls = moduleVideoUrls(existingCourse.getContentModules());
        List<StoredAsset> uploadedAssets = new ArrayList<>();
        List<String> replacedUrls = new ArrayList<>();
        Map<Integer, PreparedUpload> preparedModuleVideos = new HashMap<>();

        try {
            courseMapper.updateEntityFromDto(requestDTO, existingCourse);
            existingCourse.setImageUrl(oldImageUrl);
            existingCourse.setSyllabusUrl(oldSyllabusUrl);
            for (ContentModuleRequestDTO module : modulesOrEmpty(requestDTO)) {
                MultipartFile video = moduleVideoFor(module, moduleVideos);
                if (video != null && !video.isEmpty()) {
                    preparedModuleVideos.put(
                            module.getModuleNumber(),
                            r2StorageService.prepareUpload(video, UploadPurpose.COURSE_MODULE_VIDEO)
                    );
                }
            }

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
            for (Map.Entry<Integer, PreparedUpload> entry : preparedModuleVideos.entrySet()) {
                StoredAsset asset = r2StorageService.upload(entry.getValue());
                uploadedAssets.add(asset);
                requestDTO.getContentModules().stream()
                        .filter(module -> Objects.equals(module.getModuleNumber(), entry.getKey()))
                        .findFirst()
                        .orElseThrow()
                        .setVideoUrl(asset.publicUrl());
            }
            existingCourse.setContentModules(requestDTO.getContentModules());

            Course updatedCourse = courseRepository.save(existingCourse);
            CourseDetailResponseDTO response = courseMapper.toDetailResponseDTO(updatedCourse);
            Set<String> newModuleVideoUrls = moduleVideoUrls(updatedCourse.getContentModules());
            oldModuleVideoUrls.stream()
                    .filter(url -> !newModuleVideoUrls.contains(url))
                    .forEach(replacedUrls::add);
            registerAssetCleanup(uploadedAssets, replacedUrls);
            return response;
        } catch (RuntimeException exception) {
            assetCleanupService.cleanupUploadedAssets(uploadedAssets, exception);
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

    private void validateModuleVideoRequest(
            CourseRequestDTO requestDTO,
            Map<String, MultipartFile> moduleVideos
    ) {
        Map<String, MultipartFile> files = moduleVideos == null ? Map.of() : moduleVideos;
        boolean liveCourse = requestDTO.getModality() != null
                && requestDTO.getModality().getType() != null
                && requestDTO.getModality().getType().trim().equalsIgnoreCase("EN VIVO");
        if (requestDTO.getSchedule() != null) {
            validateHttpUrl(requestDTO.getSchedule().getLiveClassUrl(), "La URL de la clase en vivo no es válida.");
        }
        Map<Integer, ContentModuleRequestDTO> modulesByNumber = new HashMap<>();
        for (ContentModuleRequestDTO module : modulesOrEmpty(requestDTO)) {
            if (liveCourse && module.getVideoUrl() != null && !module.getVideoUrl().isBlank()) {
                throw new IllegalArgumentException("Los videos por módulo solo se permiten para cursos grabados.");
            }
            validateHttpUrl(module.getVideoUrl(), "La URL del video del módulo no es válida.");
            if (module.getModuleNumber() != null) {
                if (modulesByNumber.put(module.getModuleNumber(), module) != null) {
                    throw new IllegalArgumentException("Los números de módulo no pueden repetirse.");
                }
            }
        }

        if (liveCourse && hasModuleVideoFiles(files)) {
            throw new IllegalArgumentException("Los videos por módulo solo se permiten para cursos grabados.");
        }

        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
            if (isCourseAssetPart(entry.getKey())) continue;
            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
            Integer moduleNumber = modulesByNumber.keySet().stream()
                    .filter(number -> entry.getKey().equals(moduleVideoPartName(number)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El archivo de video no corresponde a ningún módulo."
                    ));
            ContentModuleRequestDTO module = modulesByNumber.get(moduleNumber);
            if (module.getVideoUrl() != null && !module.getVideoUrl().isBlank()) {
                throw new IllegalArgumentException(
                        "Cada módulo puede tener un archivo de video o una URL, pero no ambos."
                );
            }
        }
    }

    private void validateHttpUrl(String value, String errorMessage) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private boolean hasModuleVideoFiles(Map<String, MultipartFile> moduleVideos) {
        return moduleVideos != null && moduleVideos.entrySet().stream()
                .filter(entry -> !isCourseAssetPart(entry.getKey()))
                .map(Map.Entry::getValue)
                .anyMatch(file -> file != null && !file.isEmpty());
    }

    private boolean isCourseAssetPart(String name) {
        return name.equals("course") || name.equals("image") || name.equals("syllabus");
    }

    private MultipartFile moduleVideoFor(
            ContentModuleRequestDTO module,
            Map<String, MultipartFile> moduleVideos
    ) {
        if (moduleVideos == null || module.getModuleNumber() == null) return null;
        return moduleVideos.get(moduleVideoPartName(module.getModuleNumber()));
    }

    private String moduleVideoPartName(Integer moduleNumber) {
        return "moduleVideo-" + moduleNumber;
    }

    private List<ContentModuleRequestDTO> modulesOrEmpty(CourseRequestDTO requestDTO) {
        return requestDTO.getContentModules() == null ? List.of() : requestDTO.getContentModules();
    }

    private void setModuleVideoUrls(
            CourseRequestDTO requestDTO,
            Map<Integer, PreparedUpload> preparedModuleVideos,
            List<StoredAsset> uploadedAssets
    ) {
        Map<String, String> uploadedUrlsByKey = new HashMap<>();
        uploadedAssets.forEach(asset -> uploadedUrlsByKey.put(asset.objectKey(), asset.publicUrl()));
        for (ContentModuleRequestDTO module : modulesOrEmpty(requestDTO)) {
            PreparedUpload upload = preparedModuleVideos.get(module.getModuleNumber());
            if (upload != null) {
                String videoUrl = uploadedUrlsByKey.get(upload.objectKey());
                if (videoUrl == null) {
                    throw new R2StorageException("No se pudo confirmar la carga de video del módulo.");
                }
                module.setVideoUrl(videoUrl);
            }
        }
    }

    private Set<String> moduleVideoUrls(List<ContentModuleRequestDTO> modules) {
        if (modules == null) return Set.of();
        return modules.stream()
                .map(ContentModuleRequestDTO::getVideoUrl)
                .filter(url -> url != null && !url.isBlank())
                .collect(java.util.stream.Collectors.toSet());
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
                    assetCleanupService.deleteReplacedAssetsWhenUnused(replacedUrls);
                } else {
                    assetCleanupService.cleanupUploadedAssets(uploadedAssets, null);
                }
            }
        });
    }
}