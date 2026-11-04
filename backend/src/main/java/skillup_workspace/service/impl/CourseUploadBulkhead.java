package skillup_workspace.service.impl;

import org.springframework.stereotype.Component;
import skillup_workspace.config.CourseUploadProperties;
import skillup_workspace.exception.R2StorageException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class CourseUploadBulkhead {

    private final CourseUploadProperties properties;
    private final Semaphore permits;

    public CourseUploadBulkhead(CourseUploadProperties properties) {
        this.properties = properties;
        this.permits = new Semaphore(properties.getMaxConcurrentTasks(), true);
    }

    public void acquire() {
        try {
            if (!permits.tryAcquire(properties.getRequestWaitTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new R2StorageException("El servicio de carga de archivos está ocupado. Intenta nuevamente.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new R2StorageException("Se interrumpió la espera para procesar los archivos del curso.", exception);
        }
    }

    public void release() {
        permits.release();
    }
}
