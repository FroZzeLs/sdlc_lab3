package by.frozzel.springreviewer.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class VisitCounterService {

    private final ConcurrentHashMap<String, AtomicLong> visitCountsPerUrl = new ConcurrentHashMap<>();

    public void incrementVisit(String urlPattern) {
        if (urlPattern == null) {
            return;
        }
        visitCountsPerUrl.computeIfAbsent(urlPattern, k -> new AtomicLong(0)).incrementAndGet();
    }

    public Map<String, Long> getAllVisitCounts() {
        return visitCountsPerUrl.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                ));
    }
}