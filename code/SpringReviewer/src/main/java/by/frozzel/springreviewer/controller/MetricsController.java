package by.frozzel.springreviewer.controller;

import by.frozzel.springreviewer.service.VisitCounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema; // Убедитесь, что импорт правильный
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "API для получения метрик приложения")
public class MetricsController {

    private final VisitCounterService visitCounterService;

    @GetMapping("/visits/by-url")
    @Operation(summary = "Получить статистику посещений (GET запросов) по каждому URL")
    @ApiResponse(responseCode = "200", description = "Карта URL-паттернов и количества их посещений",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            type = "object",
                            additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
                            example = "{\"/reviews/{id}\": 120, \"/users\": 55, \"/teachers/{teacherId}/subjects/{subjectId}\": 10}"
                    )
            )
    )
    public Map<String, Long> getUrlVisitCounts() {
        return visitCounterService.getAllVisitCounts();
    }
}