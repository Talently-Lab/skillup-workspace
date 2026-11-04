package skillup_workspace.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
import skillup_workspace.repository.EnrollmentRepository.CourseEnrollmentCount;
import skillup_workspace.service.impl.CourseServiceImpl;
import skillup_workspace.service.impl.CoursePersistenceService;
import skillup_workspace.service.impl.CourseAssetCleanupService;
import skillup_workspace.service.impl.CourseUploadBulkhead;
import skillup_workspace.storage.R2StorageService;
import skillup_workspace.storage.R2StorageService.PreparedUpload;
import skillup_workspace.storage.R2StorageService.StoredAsset;
import skillup_workspace.storage.ImageConversionService;
import skillup_workspace.storage.UploadPurpose;
import skillup_workspace.exception.InvalidUploadException;
import skillup_workspace.exception.R2StorageException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseFavoriteRepository courseFavoriteRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private R2StorageService r2StorageService;

    @Mock
    private ImageConversionService imageConversionService;

    @Mock
    private CoursePersistenceService coursePersistenceService;

    private CourseAssetCleanupService assetCleanupService;

    @Mock
    private CourseUploadBulkhead courseUploadBulkhead;

    private ThreadPoolTaskExecutor courseUploadExecutor;
    private CourseServiceImpl courseService;

    private Course mockCourse;
    private CourseRequestDTO requestDTO;
    private CourseDetailResponseDTO detailResponseDTO;
    private CourseResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        mockCourse = new Course();
        mockCourse.setId(1L);
        mockCourse.setTitle("Curso de Java");
        mockCourse.setIsActive(true);
        mockCourse.setImageUrl("https://assets.example.test/current-image.png");
        mockCourse.setSyllabusUrl("https://assets.example.test/current-syllabus.pdf");

        requestDTO = new CourseRequestDTO();
        detailResponseDTO = new CourseDetailResponseDTO();
        responseDTO = new CourseResponseDTO();
        courseUploadExecutor = new ThreadPoolTaskExecutor();
        courseUploadExecutor.setCorePoolSize(2);
        courseUploadExecutor.setMaxPoolSize(2);
        courseUploadExecutor.setQueueCapacity(8);
        courseUploadExecutor.initialize();
        assetCleanupService = new CourseAssetCleanupService(r2StorageService, courseRepository);
        courseService = new CourseServiceImpl(
                courseRepository,
                courseFavoriteRepository,
                enrollmentRepository,
                courseMapper,
                r2StorageService,
                imageConversionService,
                coursePersistenceService,
                assetCleanupService,
                courseUploadBulkhead,
                courseUploadExecutor
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        courseUploadExecutor.destroy();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void setupAuthentication(String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user",
                "password",
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("Pruebas para updateCourseStatus")
    class UpdateCourseStatusTests {

        @Test
        @DisplayName("Debería persistir estado y sincronizar isActive")
        void shouldPersistStatusAndActiveFlag() {
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);

            CourseDetailResponseDTO result = courseService.updateCourseStatus(1L, CourseStatus.BORRADOR);

            assertSame(detailResponseDTO, result);
            assertEquals(CourseStatus.BORRADOR, mockCourse.getStatus());
            assertFalse(mockCourse.getIsActive());
            verify(courseRepository).save(mockCourse);
        }

        @Test
        @DisplayName("Debería activar cursos publicados")
        void whenPublishedShouldSetCourseActive() {
            mockCourse.setIsActive(false);
            mockCourse.setStatus(CourseStatus.ARCHIVADO);
            mockCourse.setCreatedAt(LocalDateTime.now().minusDays(1));
            LocalDateTime previousCreatedAt = mockCourse.getCreatedAt();
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);

            courseService.updateCourseStatus(1L, CourseStatus.PUBLICADO);

            assertEquals(CourseStatus.PUBLICADO, mockCourse.getStatus());
            assertTrue(mockCourse.getIsActive());
            assertTrue(mockCourse.getCreatedAt().isAfter(previousCreatedAt));
            verify(courseRepository).save(mockCourse);
        }

        @Test
        @DisplayName("No debería actualizar createdAt si el curso ya estaba publicado")
        void whenAlreadyPublishedShouldPreserveCreatedAt() {
            LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
            mockCourse.setCreatedAt(originalCreatedAt);
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);

            courseService.updateCourseStatus(1L, CourseStatus.PUBLICADO);

            assertEquals(originalCreatedAt, mockCourse.getCreatedAt());
        }

        @Test
        @DisplayName("Debería dejar archivado el curso inactivo")
        void whenArchivedShouldSetCourseInactive() {
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);

            courseService.updateCourseStatus(1L, CourseStatus.ARCHIVADO);

            assertEquals(CourseStatus.ARCHIVADO, mockCourse.getStatus());
            assertFalse(mockCourse.getIsActive());
            verify(courseRepository).save(mockCourse);
        }

        @Test
        @DisplayName("Debería retornar error si el curso no existe")
        void whenCourseDoesNotExistShouldThrowNotFound() {
            given(courseRepository.findById(1L)).willReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> courseService.updateCourseStatus(1L, CourseStatus.PUBLICADO));

            verify(courseRepository, never()).save(any(Course.class));
        }
    }


    @Nested
    @DisplayName("Pruebas para getAllCourses")
    class GetAllCoursesTests {

        @Test
        @DisplayName("getAllCourses - Debería filtrar como ADMIN cuando el usuario tiene ROLE_ADMIN")
        void getAllCourses_WhenUserIsAdmin_ShouldPassTrueToRepository() {
            setupAuthentication("ROLE_ADMIN");
            given(courseRepository.findAllCoursesWithFilter(true)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));


            List<CourseResponseDTO> result = courseService.getAllCourses();


            assertNotNull(result);
            assertEquals(1, result.size());
            verify(courseRepository, times(1)).findAllCoursesWithFilter(true);
        }

        @Test
        @DisplayName("getAllCourses - Reconoce la autoridad ADMIN asignada por el filtro JWT")
        void getAllCourses_WhenUserHasAdminAuthority_ShouldIncludeInactiveCourses() {
            setupAuthentication("ADMIN");
            given(courseRepository.findAllCoursesWithFilter(true)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));

            List<CourseResponseDTO> result = courseService.getAllCourses();

            assertNotNull(result);
            verify(courseRepository).findAllCoursesWithFilter(true);
        }

        @Test
        @DisplayName("getAllCourses - Devuelve el número de inscripciones activas por curso")
        void getAllCourses_ShouldAddActiveEnrollmentCountToResponse() {
            setupAuthentication("ROLE_ALUMNO");
            responseDTO.setId(1L);
            CourseEnrollmentCount count = mock(CourseEnrollmentCount.class);
            given(count.getCourseId()).willReturn(1L);
            given(count.getActiveEnrollmentCount()).willReturn(7L);
            given(courseRepository.findAllCoursesWithFilter(false)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));
            given(enrollmentRepository.findActiveEnrollmentCountsByCourse()).willReturn(List.of(count));

            List<CourseResponseDTO> result = courseService.getAllCourses();

            assertEquals(7L, result.get(0).getActiveEnrollmentCount());
        }

        @Test
        @DisplayName("getAllCourses - Debería filtrar como NO ADMIN cuando el usuario es ALUMNO")
        void getAllCourses_WhenUserIsStudent_ShouldPassFalseToRepository() {

            setupAuthentication("ROLE_ALUMNO");
            given(courseRepository.findAllCoursesWithFilter(false)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));


            List<CourseResponseDTO> result = courseService.getAllCourses();


            assertNotNull(result);
            assertEquals(1, result.size());
            verify(courseRepository, times(1)).findAllCoursesWithFilter(false);
        }

        @Test
        @DisplayName("getAllCourses - Debería retornar false en checkIsAdmin cuando SecurityContext no tiene autenticación")
        void getAllCourses_WhenAuthenticationIsNull_ShouldPassFalseToRepository() {

            SecurityContextHolder.clearContext();
            given(courseRepository.findAllCoursesWithFilter(false)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));


            List<CourseResponseDTO> result = courseService.getAllCourses();


            assertNotNull(result);
            verify(courseRepository, times(1)).findAllCoursesWithFilter(false);
        }

        @Test
        @DisplayName("getAllCourses - Debería retornar false en checkIsAdmin cuando el usuario no está autenticado")
        void getAllCourses_WhenUserIsNotAuthenticated_ShouldPassFalseToRepository() {

            UsernamePasswordAuthenticationToken unauthenticatedToken =
                    new UsernamePasswordAuthenticationToken("user", "password");
            unauthenticatedToken.setAuthenticated(false);
            SecurityContextHolder.getContext().setAuthentication(unauthenticatedToken);

            given(courseRepository.findAllCoursesWithFilter(false)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));


            List<CourseResponseDTO> result = courseService.getAllCourses();


            assertNotNull(result);
            verify(courseRepository, times(1)).findAllCoursesWithFilter(false);
        }
    }


    @Nested
    @DisplayName("Pruebas para getCourseById")
    class GetCourseByIdTests {

        @Test
        @DisplayName("getCourseById - Debería retornar el detalle si el curso existe y está activo")
        void getCourseById_WhenCourseExistsAndIsActive_ShouldReturnDetail() {

            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);


            CourseDetailResponseDTO result = courseService.getCourseById(1L);


            assertNotNull(result);
            verify(courseRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("getCourseById - Debería lanzar ResourceNotFoundException si el curso no existe")
        void getCourseById_WhenCourseDoesNotExist_ShouldThrowException() {

            given(courseRepository.findById(1L)).willReturn(Optional.empty());


            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.getCourseById(1L)
            );

            assertEquals("Curso no encontrado con el id: 1", exception.getMessage());
        }

        @Test
        @DisplayName("getCourseById - Debería lanzar ResourceNotFoundException si el curso está inactivo y el usuario NO es ADMIN")
        void getCourseById_WhenCourseIsInactiveAndUserIsNotAdmin_ShouldThrowException() {

            mockCourse.setIsActive(false);
            setupAuthentication("ROLE_ALUMNO");
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));


            assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.getCourseById(1L)
            );
        }

        @Test
        @DisplayName("getCourseById - Debería retornar el detalle si el curso está inactivo pero el usuario ES ADMIN")
        void getCourseById_WhenCourseIsInactiveAndUserIsAdmin_ShouldReturnDetail() {

            mockCourse.setIsActive(false);
            setupAuthentication("ROLE_ADMIN");
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);


            CourseDetailResponseDTO result = courseService.getCourseById(1L);


            assertNotNull(result);
            verify(courseRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("getCourseById - Debería lanzar ResourceNotFoundException si getIsActive es null y el usuario NO es ADMIN")
        void getCourseById_WhenCourseIsActiveIsNullAndUserIsNotAdmin_ShouldThrowException() {

            mockCourse.setIsActive(null);
            setupAuthentication("ROLE_ALUMNO");
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));


            assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.getCourseById(1L)
            );
        }

        @Test
        @DisplayName("getCourseById - Debería retornar el detalle si getIsActive es null pero el usuario ES ADMIN")
        void getCourseById_WhenCourseIsActiveIsNullAndUserIsAdmin_ShouldReturnDetail() {

            mockCourse.setIsActive(null);
            setupAuthentication("ROLE_ADMIN");
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);


            CourseDetailResponseDTO result = courseService.getCourseById(1L);


            assertNotNull(result);
            verify(courseRepository, times(1)).findById(1L);
        }
    }



    @Test
    @DisplayName("createCourse - Debería mapear, guardar y retornar el nuevo curso")
    void createCourse_ShouldSaveAndReturnCourse() {

        given(courseMapper.toEntity(requestDTO)).willReturn(mockCourse);
        given(coursePersistenceService.persistCreatedCourse(
                any(Course.class),
                any(),
                any(AtomicBoolean.class)
        )).willReturn(detailResponseDTO);


        CourseDetailResponseDTO result = courseService.createCourse(requestDTO);

        assertNotNull(result);
        assertEquals(CourseStatus.PUBLICADO, mockCourse.getStatus());
        assertTrue(mockCourse.getIsActive());
        assertNull(mockCourse.getImageUrl());
        assertNull(mockCourse.getSyllabusUrl());
        verify(courseMapper, times(1)).toEntity(requestDTO);
        verify(coursePersistenceService).persistCreatedCourse(
                eq(mockCourse),
                any(),
                any(AtomicBoolean.class)
        );
    }

    @Test
    @DisplayName("createCourse - Debería crear borrador inactivo")
    void createCourse_AsDraft_ShouldSaveInactiveCourse() {
        requestDTO.setStatus(CourseStatus.BORRADOR);
        given(courseMapper.toEntity(requestDTO)).willReturn(mockCourse);
        given(coursePersistenceService.persistCreatedCourse(
                any(Course.class),
                any(),
                any(AtomicBoolean.class)
        )).willReturn(detailResponseDTO);

        CourseDetailResponseDTO result = courseService.createCourse(requestDTO);

        assertNotNull(result);
        assertEquals(CourseStatus.BORRADOR, mockCourse.getStatus());
        assertFalse(mockCourse.getIsActive());
        verify(coursePersistenceService).persistCreatedCourse(
                eq(mockCourse),
                any(),
                any(AtomicBoolean.class)
        );
    }

    @Test
    @DisplayName("createCourse - Debería asociar los archivos cargados por el backend")
    void createCourseWithAssets_ShouldStoreReturnedUrls() {
        StoredAsset imageAsset = new StoredAsset(
                "courses/images/12345678-1234-1234-1234-123456789012.png",
                "https://assets.example.test/courses/images/12345678-1234-1234-1234-123456789012.png"
        );
        StoredAsset syllabusAsset = new StoredAsset(
                "courses/syllabi/12345678-1234-1234-1234-123456789012.pdf",
                "https://assets.example.test/courses/syllabi/12345678-1234-1234-1234-123456789012.pdf"
        );
        MultipartFile image = new MockMultipartFile("image", "image.png", "image/png", new byte[]{1});
        MultipartFile syllabus = new MockMultipartFile("syllabus", "course.pdf", "application/pdf", new byte[]{2});
        PreparedUpload preparedSyllabus = new PreparedUpload(
                "courses/syllabi/12345678-1234-1234-1234-123456789012.pdf",
                "application/pdf",
                syllabus,
                syllabus.getSize(),
                UploadPurpose.COURSE_SYLLABUS
        );
        ImageConversionService.ConvertedImage convertedImage = new ImageConversionService.ConvertedImage(
                new byte[]{3, 4}
        );
        given(courseMapper.toEntity(requestDTO)).willReturn(mockCourse);
        given(imageConversionService.convertToWebp(image)).willReturn(convertedImage);
        given(r2StorageService.uploadWebp(convertedImage.content())).willReturn(imageAsset);
        given(r2StorageService.prepareUpload(syllabus, UploadPurpose.COURSE_SYLLABUS))
                .willReturn(preparedSyllabus);
        given(r2StorageService.upload(preparedSyllabus)).willReturn(syllabusAsset);
        given(coursePersistenceService.persistCreatedCourse(
                any(Course.class),
                any(),
                any(AtomicBoolean.class)
        )).willReturn(detailResponseDTO);
        TransactionSynchronizationManager.initSynchronization();

        CourseDetailResponseDTO result = courseService.createCourse(requestDTO, image, syllabus);

        assertNotNull(result);
        assertEquals(imageAsset.publicUrl(), mockCourse.getImageUrl());
        assertEquals(syllabusAsset.publicUrl(), mockCourse.getSyllabusUrl());
        verify(r2StorageService).uploadWebp(convertedImage.content());
        verify(r2StorageService).prepareUpload(syllabus, UploadPurpose.COURSE_SYLLABUS);
        verify(r2StorageService).upload(preparedSyllabus);
    }

    @Test
    @DisplayName("createCourse - Debería limpiar los archivos subidos cuando falla la persistencia")
    void createCourseWhenSaveFails_ShouldDeleteUploadedAssets() {
        StoredAsset imageAsset = new StoredAsset(
                "courses/images/12345678-1234-1234-1234-123456789012.png",
                "https://assets.example.test/courses/images/12345678-1234-1234-1234-123456789012.png"
        );
        MultipartFile image = new MockMultipartFile("image", "image.png", "image/png", new byte[]{1});
        ImageConversionService.ConvertedImage convertedImage = new ImageConversionService.ConvertedImage(
                new byte[]{3, 4}
        );
        given(courseMapper.toEntity(requestDTO)).willReturn(mockCourse);
        given(imageConversionService.convertToWebp(image)).willReturn(convertedImage);
        given(r2StorageService.uploadWebp(convertedImage.content())).willReturn(imageAsset);
        given(coursePersistenceService.persistCreatedCourse(
                any(Course.class),
                any(),
                any(AtomicBoolean.class)
        )).willThrow(new IllegalStateException("database failure"));

        assertThrows(
                IllegalStateException.class,
                () -> courseService.createCourse(requestDTO, image, null)
        );

        verify(r2StorageService).deleteAsset(imageAsset);
    }


    @Nested
    @DisplayName("Pruebas para updateCourse")
    class UpdateCourseTests {

        @Test
        @DisplayName("updateCourse - Debería actualizar un curso existente exitosamente")
        void updateCourse_WhenCourseExists_ShouldUpdateAndReturn() {

            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            doNothing().when(courseMapper).updateEntityFromDto(requestDTO, mockCourse);
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);

            CourseDetailResponseDTO result = courseService.updateCourse(1L, requestDTO);

            assertNotNull(result);
            assertEquals("https://assets.example.test/current-image.png", mockCourse.getImageUrl());
            assertEquals("https://assets.example.test/current-syllabus.pdf", mockCourse.getSyllabusUrl());
            verify(courseMapper, times(1)).updateEntityFromDto(requestDTO, mockCourse);
            verify(courseRepository, times(1)).save(mockCourse);
        }

        @Test
        @DisplayName("updateCourse - Debería eliminar el archivo anterior solo después del commit y si no se comparte")
        void updateCourseWithReplacement_ShouldDeleteOldAssetAfterCommitWhenUnused() {
            StoredAsset replacement = new StoredAsset(
                    "courses/images/12345678-1234-1234-1234-123456789012.png",
                    "https://assets.example.test/courses/images/12345678-1234-1234-1234-123456789012.png"
            );
            MultipartFile image = new MockMultipartFile("image", "new.png", "image/png", new byte[]{1});
            ImageConversionService.ConvertedImage convertedImage = new ImageConversionService.ConvertedImage(
                    new byte[]{3, 4}
            );
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            doNothing().when(courseMapper).updateEntityFromDto(requestDTO, mockCourse);
            given(imageConversionService.convertToWebp(image)).willReturn(convertedImage);
            given(r2StorageService.uploadWebp(convertedImage.content())).willReturn(replacement);
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);
            TransactionSynchronizationManager.initSynchronization();

            courseService.updateCourse(1L, requestDTO, image, null);

            verify(r2StorageService, never()).deleteManagedAsset("https://assets.example.test/current-image.png");
            TransactionSynchronization synchronization =
                    TransactionSynchronizationManager.getSynchronizations().get(0);
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

            verify(courseRepository).existsByImageUrl("https://assets.example.test/current-image.png");
            verify(courseRepository).existsBySyllabusUrl("https://assets.example.test/current-image.png");
            verify(r2StorageService).deleteManagedAsset("https://assets.example.test/current-image.png");
        }

        @Test
        @DisplayName("updateCourse - Debería conservar el archivo antiguo cuando otro curso lo utiliza")
        void updateCourseWithReplacement_ShouldKeepOldAssetWhenShared() {
            StoredAsset replacement = new StoredAsset(
                    "courses/images/12345678-1234-1234-1234-123456789012.png",
                    "https://assets.example.test/courses/images/12345678-1234-1234-1234-123456789012.png"
            );
            MultipartFile image = new MockMultipartFile("image", "new.png", "image/png", new byte[]{1});
            ImageConversionService.ConvertedImage convertedImage = new ImageConversionService.ConvertedImage(
                    new byte[]{3, 4}
            );
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            doNothing().when(courseMapper).updateEntityFromDto(requestDTO, mockCourse);
            given(imageConversionService.convertToWebp(image)).willReturn(convertedImage);
            given(r2StorageService.uploadWebp(convertedImage.content())).willReturn(replacement);
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);
            given(courseRepository.existsByImageUrl("https://assets.example.test/current-image.png"))
                    .willReturn(true);
            TransactionSynchronizationManager.initSynchronization();

            courseService.updateCourse(1L, requestDTO, image, null);
            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

            verify(r2StorageService, never()).deleteManagedAsset("https://assets.example.test/current-image.png");
        }

        @Test
        @DisplayName("updateCourse - Debería eliminar el archivo nuevo si la transacción termina en rollback")
        void updateCourseWithReplacement_ShouldCleanupNewAssetAfterRollback() {
            StoredAsset replacement = new StoredAsset(
                    "courses/images/12345678-1234-1234-1234-123456789012.png",
                    "https://assets.example.test/courses/images/12345678-1234-1234-1234-123456789012.png"
            );
            MultipartFile image = new MockMultipartFile("image", "new.png", "image/png", new byte[]{1});
            ImageConversionService.ConvertedImage convertedImage = new ImageConversionService.ConvertedImage(
                    new byte[]{3, 4}
            );
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            doNothing().when(courseMapper).updateEntityFromDto(requestDTO, mockCourse);
            given(imageConversionService.convertToWebp(image)).willReturn(convertedImage);
            given(r2StorageService.uploadWebp(convertedImage.content())).willReturn(replacement);
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);
            TransactionSynchronizationManager.initSynchronization();

            courseService.updateCourse(1L, requestDTO, image, null);
            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(r2StorageService).deleteAsset(replacement);
            verify(r2StorageService, never()).deleteManagedAsset("https://assets.example.test/current-image.png");
        }

        @Test
        @DisplayName("updateCourse - Debería conservar la imagen anterior si falla la conversión")
        void updateCourseWhenImageConversionFails_ShouldKeepExistingImage() {
            MultipartFile image = new MockMultipartFile("image", "bad.png", "image/png", new byte[]{1});
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            doNothing().when(courseMapper).updateEntityFromDto(requestDTO, mockCourse);
            given(imageConversionService.convertToWebp(image))
                    .willThrow(new InvalidUploadException("Imagen inválida"));

            assertThrows(
                    InvalidUploadException.class,
                    () -> courseService.updateCourse(1L, requestDTO, image, null)
            );

            assertEquals("https://assets.example.test/current-image.png", mockCourse.getImageUrl());
            verify(r2StorageService, never()).uploadWebp(any());
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateCourse - Debería conservar la imagen anterior si falla la carga en R2")
        void updateCourseWhenR2UploadFails_ShouldKeepExistingImage() {
            MultipartFile image = new MockMultipartFile("image", "new.png", "image/png", new byte[]{1});
            ImageConversionService.ConvertedImage convertedImage = new ImageConversionService.ConvertedImage(
                    new byte[]{3, 4}
            );
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            doNothing().when(courseMapper).updateEntityFromDto(requestDTO, mockCourse);
            given(imageConversionService.convertToWebp(image)).willReturn(convertedImage);
            given(r2StorageService.uploadWebp(convertedImage.content()))
                    .willThrow(new R2StorageException("R2 no disponible"));

            assertThrows(
                    R2StorageException.class,
                    () -> courseService.updateCourse(1L, requestDTO, image, null)
            );

            assertEquals("https://assets.example.test/current-image.png", mockCourse.getImageUrl());
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateCourse - Debería lanzar ResourceNotFoundException cuando el curso no existe")
        void updateCourse_WhenCourseDoesNotExist_ShouldThrowException() {
            given(courseRepository.findById(1L)).willReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.updateCourse(1L, requestDTO)
            );

            verify(courseRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("Pruebas para deleteCourse")
    class DeleteCourseTests {

        @Test
        @DisplayName("deleteCourse - Debería eliminar físicamente el curso existente")
        void deleteCourse_WhenCourseExists_ShouldDeleteCourse() {

            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(enrollmentRepository.existsByCourse_Id(1L)).willReturn(false);

            courseService.deleteCourse(1L);

            assertTrue(mockCourse.getIsActive());
            verify(courseFavoriteRepository).deleteByCourse_Id(1L);
            verify(courseRepository).delete(mockCourse);
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("deleteCourse - Debería lanzar ResourceNotFoundException si el curso a eliminar no existe")
        void deleteCourse_WhenCourseDoesNotExist_ShouldThrowException() {

            given(courseRepository.findById(1L)).willReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.deleteCourse(1L)
            );

            verify(courseRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteCourse - No debería eliminar un curso con inscripciones asociadas")
        void deleteCourse_WhenEnrollmentsExist_ShouldThrowConflict() {
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(enrollmentRepository.existsByCourse_Id(1L)).willReturn(true);

            assertThrows(IllegalStateException.class, () -> courseService.deleteCourse(1L));

            verify(courseRepository, never()).delete(any());
        }
    }
}