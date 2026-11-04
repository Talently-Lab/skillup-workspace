package skillup_workspace.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class CourseCreationPerformanceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CourseCreationPerformanceFilter.class);
    private static final String COURSE_COLLECTION_PATH = "/api/v1/courses";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !COURSE_COLLECTION_PATH.equals(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put("requestId", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            int status = response.getStatus();
            CourseCreationPerformanceLog.event(log, "course_creation_http")
                    .addKeyValue("httpStatus", status)
                    .addKeyValue("requestBytes", request.getContentLengthLong())
                    .addKeyValue("durationMs", durationMs)
                    .addKeyValue("result", status < 400 ? "success" : "failure")
                    .log("Course creation HTTP request completed");
            if (previousContext == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousContext);
            }
        }
    }
}
