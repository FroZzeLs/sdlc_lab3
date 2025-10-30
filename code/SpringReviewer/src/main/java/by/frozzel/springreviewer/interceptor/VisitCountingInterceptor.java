package by.frozzel.springreviewer.interceptor;

import by.frozzel.springreviewer.service.VisitCounterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
@Slf4j
public class VisitCountingInterceptor implements HandlerInterceptor {

    private final VisitCounterService visitCounterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Object patternAttribute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        if (patternAttribute instanceof String pattern && !pattern.isBlank()) {
            visitCounterService.incrementVisit(pattern);
            log.trace("Incremented visit count for URL pattern [{}]: {}", request.getMethod(), pattern);
        } else {
            log.trace("Could not determine URL pattern for request: {} {}", request.getMethod(), request.getRequestURI());
        }

        return true;
    }
}