package skillup_workspace.observability;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;

public final class CourseCreationPerformanceLog {

    private CourseCreationPerformanceLog() {
    }

    public static LoggingEventBuilder event(Logger logger, String eventName) {
        LoggingEventBuilder event = logger.atInfo().addKeyValue("event", eventName);
        addContext(event, "requestId");
        addContext(event, "imageBytes");
        addContext(event, "pdfBytes");
        return event;
    }

    public static LoggingEventBuilder warning(Logger logger, String eventName) {
        LoggingEventBuilder event = logger.atWarn().addKeyValue("event", eventName);
        addContext(event, "requestId");
        addContext(event, "imageBytes");
        addContext(event, "pdfBytes");
        return event;
    }

    private static void addContext(LoggingEventBuilder event, String key) {
        String value = MDC.get(key);
        if (value != null) {
            event.addKeyValue(key, value);
        }
    }
}
