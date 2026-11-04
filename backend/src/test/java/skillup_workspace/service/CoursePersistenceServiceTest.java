package skillup_workspace.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.mapper.CourseMapper;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.service.impl.CourseAssetCleanupService;
import skillup_workspace.service.impl.CoursePersistenceService;
import skillup_workspace.storage.R2StorageService;
import skillup_workspace.storage.R2StorageService.StoredAsset;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CoursePersistenceServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private R2StorageService r2StorageService;

    private CoursePersistenceService persistenceService;
    private StoredAsset uploadedAsset;

    @BeforeEach
    void setUp() {
        persistenceService = new CoursePersistenceService(
                courseRepository,
                courseMapper,
                new CourseAssetCleanupService(r2StorageService, courseRepository)
        );
        uploadedAsset = new StoredAsset(
                "courses/images/12345678-1234-1234-1234-123456789012.webp",
                "https://assets.example.test/courses/images/12345678-1234-1234-1234-123456789012.webp"
        );
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackCompensatesUploadedAssetsAfterDatabaseCompletion() {
        Course course = new Course();
        given(courseRepository.save(course)).willReturn(course);
        given(courseMapper.toDetailResponseDTO(course)).willReturn(new CourseDetailResponseDTO());
        AtomicBoolean cleanupRegistered = new AtomicBoolean();

        assertNotNull(persistenceService.persistCreatedCourse(
                course,
                List.of(uploadedAsset),
                cleanupRegistered
        ));
        assertNotNull(TransactionSynchronizationManager.getSynchronizations().get(0));
        TransactionSynchronizationManager.getSynchronizations().get(0)
                .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(r2StorageService).deleteAsset(uploadedAsset);
        verify(courseRepository).save(course);
    }

    @Test
    void committedDatabaseTransactionKeepsUploadedAssets() {
        Course course = new Course();
        given(courseRepository.save(course)).willReturn(course);
        given(courseMapper.toDetailResponseDTO(course)).willReturn(new CourseDetailResponseDTO());

        persistenceService.persistCreatedCourse(course, List.of(uploadedAsset), new AtomicBoolean());
        TransactionSynchronizationManager.getSynchronizations().get(0)
                .afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(r2StorageService, never()).deleteAsset(any());
    }

    @Test
    void databaseFailureKeepsRollbackCleanupRegistered() {
        Course course = new Course();
        given(courseRepository.save(course)).willThrow(new IllegalStateException("simulated database failure"));
        AtomicBoolean cleanupRegistered = new AtomicBoolean();

        assertThrows(
                IllegalStateException.class,
                () -> persistenceService.persistCreatedCourse(course, List.of(uploadedAsset), cleanupRegistered)
        );
        TransactionSynchronizationManager.getSynchronizations().get(0)
                .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(r2StorageService).deleteAsset(uploadedAsset);
        org.junit.jupiter.api.Assertions.assertTrue(cleanupRegistered.get());
    }
}
