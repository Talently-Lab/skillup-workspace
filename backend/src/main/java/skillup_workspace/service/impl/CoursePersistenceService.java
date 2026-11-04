package skillup_workspace.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.mapper.CourseMapper;
import skillup_workspace.observability.CourseCreationPerformanceLog;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.storage.R2StorageService.StoredAsset;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class CoursePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(CoursePersistenceService.class);

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final CourseAssetCleanupService assetCleanupService;

    @Transactional
    public CourseDetailResponseDTO persistCreatedCourse(
            Course course,
            List<StoredAsset> uploadedAssets,
            AtomicBoolean rollbackCleanupRegistered
    ) {
        long transactionStartedAt = System.nanoTime();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("No se pudo asegurar la consistencia de los archivos del curso.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                long transactionDurationMs = (System.nanoTime() - transactionStartedAt) / 1_000_000;
                if (status != STATUS_COMMITTED && !uploadedAssets.isEmpty()) {
                    rollbackCleanupRegistered.set(true);
                    assetCleanupService.cleanupUploadedAssets(uploadedAssets, null);
                }
                CourseCreationPerformanceLog.event(log, "course_creation_stage")
                        .addKeyValue("stage", "transaction_completion")
                        .addKeyValue("durationMs", transactionDurationMs)
                        .addKeyValue("result", status == STATUS_COMMITTED ? "committed" : "rolled_back")
                        .log("Course creation transaction completed");
            }
        });
        if (!uploadedAssets.isEmpty()) {
            rollbackCleanupRegistered.set(true);
        }

        long persistenceStartedAt = System.nanoTime();
        RuntimeException persistenceFailure = null;
        try {
            Course savedCourse = courseRepository.save(course);
            return courseMapper.toDetailResponseDTO(savedCourse);
        } catch (RuntimeException exception) {
            persistenceFailure = exception;
            throw exception;
        } finally {
            var event = persistenceFailure == null
                    ? CourseCreationPerformanceLog.event(log, "course_creation_stage")
                    : CourseCreationPerformanceLog.warning(log, "course_creation_stage");
            event
                    .addKeyValue("stage", "persistence")
                    .addKeyValue("durationMs", (System.nanoTime() - persistenceStartedAt) / 1_000_000)
                    .addKeyValue("result", persistenceFailure == null ? "success" : "failure");
            if (persistenceFailure != null) {
                event.addKeyValue("errorType", persistenceFailure.getClass().getSimpleName());
            }
            event.log("Course persistence completed");
        }
    }
}
