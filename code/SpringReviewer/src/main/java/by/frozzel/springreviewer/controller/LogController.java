package by.frozzel.springreviewer.controller;

import by.frozzel.springreviewer.exception.ResourceNotFoundException;
import by.frozzel.springreviewer.model.LogGenerationTask;
import by.frozzel.springreviewer.model.enums.LogGenerationStatus;
import by.frozzel.springreviewer.service.LogGenerationTaskRegistry;
import by.frozzel.springreviewer.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Logs", description = "API для работы с логами приложения")
public class LogController {

    private final LogService logService;
    private final LogGenerationTaskRegistry taskRegistry;

    @PostMapping("/generate")
    @Operation(
            summary = "Запустить асинхронную генерацию лог-файла",
            description = "Запускает фоновую задачу генерации лог-файла для указанной даты и немедленно возвращает ID задачи."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Задача генерации принята в обработку",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LogGenerationTaskResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный параметр запроса (дата не указана или некорректный формат)",
                    content = @Content
            )
    })
    public ResponseEntity<LogGenerationTaskResponse> startLogGeneration(
            @Parameter(
                    description = "Дата для генерации логов (в формате YYYY-MM-DD)",
                    required = true,
                    example = "2025-04-22")
            @RequestParam("date")
            @NotNull(message = "Date parameter is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        LogGenerationTask task = taskRegistry.createTask();
        logService.initiateLogGeneration(task.getId(), date);

        String statusUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/logs/generate/{id}/status")
                .buildAndExpand(task.getId())
                .toUriString();

        return ResponseEntity.accepted()
                .header(HttpHeaders.LOCATION, statusUrl)
                .body(new LogGenerationTaskResponse(task.getId(), task.getStatus(), statusUrl));
    }

    @GetMapping("/generate/{id}/status")
    @Operation(
            summary = "Получить статус задачи генерации лог-файла",
            description = "Возвращает текущий статус задачи по её ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус задачи успешно получен",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LogGenerationTaskStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Задача с указанным ID не найдена",
                    content = @Content
            )
    })
    public ResponseEntity<LogGenerationTaskStatusResponse> getLogGenerationStatus(
            @Parameter(description = "ID задачи генерации", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            @PathVariable String id) {
        try {
            LogGenerationTask task = taskRegistry.getTask(id);
            String downloadUrl = null;
            if (task.getStatus() == LogGenerationStatus.COMPLETED) {
                downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/logs/generate/{id}/download")
                        .buildAndExpand(id)
                        .toUriString();
            }

            return ResponseEntity.ok(new LogGenerationTaskStatusResponse(task.getStatus(), task.getErrorMessage(), downloadUrl));
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping("/generate/{id}/download")
    @Operation(
            summary = "Скачать сгенерированный лог-файл",
            description = "Возвращает сгенерированный лог-файл, если задача завершена успешно."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Файл успешно найден и отправлен",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Задача не найдена",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Задача еще не завершена или завершилась с ошибкой",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при чтении файла",
                    content = @Content
            )
    })
    public ResponseEntity<Resource> downloadGeneratedLogFile(
            @Parameter(description = "ID задачи генерации", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            @PathVariable String id) {
        try {
            LogGenerationTask task = taskRegistry.getTask(id);

            if (task.getStatus() == LogGenerationStatus.FAILED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Log generation failed: " + task.getErrorMessage());
            }

            if (task.getStatus() != LogGenerationStatus.COMPLETED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Log generation is not yet complete. Status: " + task.getStatus());
            }

            if (task.getResultPath() == null) {
                log.error("Task {} is COMPLETED but result path is null!", id);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Log generation completed but result file is missing.");
            }

            Path logFilePath = task.getResultPath();
            Resource resource = new FileSystemResource(logFilePath);

            if (!resource.exists() || !resource.isReadable()) {
                log.error("Generated log resource not found or not readable for task {}: {}", id, logFilePath);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Generated log file became unreadable or was deleted.");
            }

            String filename = logFilePath.getFileName().toString();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            long contentLength = Files.size(logFilePath);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(contentLength)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error preparing generated log file for download for task {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading generated log file.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting generated log file for task {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", e);
        }
    }

    @GetMapping("/download")
    @Operation(
            summary = "Скачать стандартный файл логов за определенную дату",
            description = "Возвращает стандартный файл логов приложения (текущий или архивный) для указанной даты."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Файл логов успешно найден и отправлен",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный параметр запроса (дата не указана, некорректный формат или дата в будущем)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Файл логов на указанную дату не найден",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при чтении файла",
                    content = @Content
            )
    })
    public ResponseEntity<Resource> downloadStandardLogFile(
            @Parameter(
                    description = "Дата для скачивания логов (в формате YYYY-MM-DD)",
                    required = true,
                    example = "2025-04-15")
            @RequestParam("date")
            @NotNull(message = "Date parameter is required")
            @PastOrPresent(message = "Date must be in the past or present")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        try {
            Path logFilePath = logService.getLogFilePathForDate(date);
            Resource resource = new FileSystemResource(logFilePath);

            if (!resource.exists() || !resource.isReadable()) {
                log.error("Standard log resource not found or not readable after service check: {}", logFilePath);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Log file became unreadable or was deleted.");
            }

            String filename = logFilePath.getFileName().toString();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            long contentLength = Files.size(logFilePath);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(contentLength)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (ResourceNotFoundException e) {
            log.warn("Standard log file not found request for date {}: {}", date, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error preparing standard log file for download for date {}: {}", date, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading standard log file.", e);
        } catch (Exception e) {
            log.error("Unexpected error getting standard log file for date {}: {}", date, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", e);
        }
    }

    @Schema(description = "Ответ при запуске задачи генерации логов")
    private record LogGenerationTaskResponse(
            @Schema(description = "Уникальный ID задачи", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") String taskId,
            @Schema(description = "Начальный статус задачи", example = "PENDING") LogGenerationStatus status,
            @Schema(description = "URL для проверки статуса задачи", example = "http://localhost:8080/logs/generate/a1b2c3d4-e5f6-7890-1234-567890abcdef/status") String statusUrl
    ) {}

    @Schema(description = "Ответ со статусом задачи генерации логов")
    private record LogGenerationTaskStatusResponse(
            @Schema(description = "Текущий статус задачи", example = "RUNNING") LogGenerationStatus status,
            @Schema(description = "Сообщение об ошибке (если статус FAILED)", example = "Failed to write file", nullable = true) String errorMessage,
            @Schema(description = "URL для скачивания файла (если статус COMPLETED)", example = "http://localhost:8080/logs/generate/a1b2c3d4-e5f6-7890-1234-567890abcdef/download", nullable = true) String downloadUrl
    ) {}
}