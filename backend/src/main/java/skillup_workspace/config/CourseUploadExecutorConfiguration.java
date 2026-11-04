package skillup_workspace.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class CourseUploadExecutorConfiguration {

    @Bean
    public ThreadPoolTaskExecutor courseUploadExecutor(CourseUploadProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getMaxConcurrentTasks());
        executor.setMaxPoolSize(properties.getMaxConcurrentTasks());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("course-upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(mdcTaskDecorator());
        return executor;
    }

    private TaskDecorator mdcTaskDecorator() {
        return task -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previousContext = MDC.getCopyOfContextMap();
                try {
                    if (context == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(context);
                    }
                    task.run();
                } finally {
                    if (previousContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previousContext);
                    }
                }
            };
        };
    }
}
