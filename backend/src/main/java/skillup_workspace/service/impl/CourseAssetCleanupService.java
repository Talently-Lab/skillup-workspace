package skillup_workspace.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import skillup_workspace.observability.CourseCreationPerformanceLog;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.storage.R2StorageService;
import skillup_workspace.storage.R2StorageService.StoredAsset;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseAssetCleanupService {

    private static final Logger log = LoggerFactory.getLogger(CourseAssetCleanupService.class);

    private final R2StorageService r2StorageService;
    private final CourseRepository courseRepository;

    public void cleanupUploadedAssets(List<StoredAsset> uploadedAssets, RuntimeException cause) {
        for (StoredAsset asset : uploadedAssets) {
            try {
                r2StorageService.deleteAsset(asset);
                CourseCreationPerformanceLog.event(log, "course_asset_compensation")
                        .addKeyValue("operation", "delete_uploaded_asset")
                        .addKeyValue("result", "success")
                        .log("Compensating uploaded course asset");
            } catch (RuntimeException cleanupException) {
                CourseCreationPerformanceLog.warning(log, "course_asset_compensation")
                        .addKeyValue("operation", "delete_uploaded_asset")
                        .addKeyValue("result", "failure")
                        .addKeyValue("errorType", cleanupException.getClass().getSimpleName())
                        .log("Could not clean up an uploaded course asset");
                if (cause != null) {
                    cause.addSuppressed(cleanupException);
                }
            }
        }
    }

    public void deleteReplacedAssetsWhenUnused(List<String> replacedUrls) {
        for (String url : replacedUrls) {
            try {
                if (!courseRepository.existsByImageUrl(url) && !courseRepository.existsBySyllabusUrl(url)) {
                    r2StorageService.deleteManagedAsset(url);
                }
            } catch (RuntimeException exception) {
                CourseCreationPerformanceLog.warning(log, "course_asset_compensation")
                        .addKeyValue("operation", "delete_replaced_asset")
                        .addKeyValue("result", "failure")
                        .addKeyValue("errorType", exception.getClass().getSimpleName())
                        .log("Could not clean up a replaced course asset");
            }
        }
    }
}
