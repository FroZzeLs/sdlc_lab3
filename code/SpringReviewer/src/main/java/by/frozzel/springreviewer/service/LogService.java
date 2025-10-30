package by.frozzel.springreviewer.service;

import by.frozzel.springreviewer.exception.ResourceNotFoundException;
import by.frozzel.springreviewer.model.enums.LogGenerationStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class LogService {

    @Getter
    private final String logFilePathString;
    private final String logFilePattern;
    private final Path generatedLogsDir;
    private final LogGenerationTaskRegistry taskRegistry;

    private LogService self;

    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd");
    private static final String LOG_RESOURCE = "Log file";
    private static final long SIMULATED_DELAY_SECONDS = 3;

    public LogService(@Value("${logging.file.name}") String logFileName,
                      @Value("${generated.logs.dir:./generated-logs}") String generatedLogsDirPath,
                      LogGenerationTaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;

        Path path = Paths.get(logFileName).toAbsolutePath();
        this.logFilePathString = path.toString();
        Path parentDir = path.getParent();
        Path archiveDir = parentDir != null ? parentDir.resolve("archived") : Paths.get("archived");
        String baseName = path.getFileName().toString();
        String archivedNamePattern = baseName.endsWith(".log")
                ? baseName.substring(0, baseName.length() - 4) + "-%s.log"
                : baseName + "-%s.log";
        this.logFilePattern = archiveDir.resolve(archivedNamePattern).toString();

        this.generatedLogsDir = Paths.get(generatedLogsDirPath).toAbsolutePath();
        try {
            Files.createDirectories(this.generatedLogsDir);
            log.info("Generated logs directory: {}", this.generatedLogsDir);
        } catch (IOException e) {
            log.error("Could not create generated logs directory: {}", this.generatedLogsDir, e);
            throw new RuntimeException("Failed to initialize generated logs directory", e);
        }

        log.info("Log service initialized.");
        log.info("Active log file path: {}", this.logFilePathString);
        log.info("Archived log file pattern: {}", this.logFilePattern);
    }

    @Autowired
    @Lazy
    public void setSelf(LogService self) {
        this.self = self;
    }


    public void initiateLogGeneration(String taskId, LocalDate date) {
        log.info("Initiating log generation for task ID {} via self-proxy.", taskId);
        if (self == null) {
            log.error("Self-proxy LogService is null! Async call will likely fail. Check Spring configuration/initialization.");
            throw new IllegalStateException("Self-proxy for LogService was not injected correctly via setter.");
        }
        self.generateLogFileAsync(taskId, date);
    }


    @Async("logGenerationTaskExecutor")
    public void generateLogFileAsync(String taskId, LocalDate date) {
        log.info(">>> ASYNC METHOD ENTERED for task ID {} on thread {}", taskId, Thread.currentThread().getName());
        Path sourceLogPath = null;
        try {
            if (this.taskRegistry == null) {
                log.error("!!! taskRegistry is NULL inside async method for task ID {} (should be final!)!!!", taskId);
                throw new IllegalStateException("taskRegistry is null within async execution for task " + taskId);
            }

            sourceLogPath = getLogFilePathForDate(date);
            log.info("Source log file found for task {}: {}", taskId, sourceLogPath);

            taskRegistry.updateStatus(taskId, LogGenerationStatus.RUNNING);
            log.info(">>> Status updated to RUNNING for task ID {} on thread {}", taskId, Thread.currentThread().getName());

            log.info("Task {} - Simulating work ({} seconds)... on thread {}", taskId, SIMULATED_DELAY_SECONDS, Thread.currentThread().getName());
            TimeUnit.SECONDS.sleep(SIMULATED_DELAY_SECONDS);
            log.info("Task {} - Simulation finished. on thread {}", taskId, Thread.currentThread().getName());

            String formattedDate = date.format(LOG_DATE_FORMATTER);
            String generatedFileName = String.format("generated_log_%s_%s.log", formattedDate, taskId.substring(0, 8));
            Path targetPath = generatedLogsDir.resolve(generatedFileName);

            Files.copy(sourceLogPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Task {} - Successfully copied log content from {} to: {}", taskId, sourceLogPath, targetPath);

            taskRegistry.setSuccessResult(taskId, targetPath);

        } catch (ResourceNotFoundException e) {
            log.warn("Task {} failed: Source log file not found or not accessible for date {}. Reason: {}", taskId, date, e.getMessage());
            taskRegistry.setFailureResult(taskId, "Source log file not found or not accessible for date " + date + ": " + e.getMessage());
        } catch (IOException e) {
            log.error("Task {} failed during log file copying from {} to generated file.", taskId, sourceLogPath != null ? sourceLogPath : "unknown source", e);
            taskRegistry.setFailureResult(taskId, "Failed to copy log file content: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Task {} was interrupted during simulation.", taskId, e);
            taskRegistry.setFailureResult(taskId, "Task was interrupted");
        } catch (Exception e) {
            log.error("Task {} failed with an unexpected error.", taskId, e);
            taskRegistry.setFailureResult(taskId, "An unexpected error occurred: " + e.getMessage());
        } finally {
            log.info(">>> ASYNC METHOD EXITING for task ID {} on thread {}", taskId, Thread.currentThread().getName());
        }
    }

    public Path getLogFilePathForDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        Path logPathToRead;
        String formattedDate = date.format(LOG_DATE_FORMATTER);

        if (date.isAfter(today)) {
            log.warn("Cannot request logs for a future date: {}", formattedDate);
            throw new ResourceNotFoundException(LOG_RESOURCE, "date", "Future date " + formattedDate + " requested");
        } else if (date.isEqual(today)) {
            logPathToRead = Paths.get(logFilePathString);
            log.info("Requested logs for today ({}), using active file: {}", formattedDate, logPathToRead);
        } else {
            String specificLogFilePath = String.format(logFilePattern, formattedDate);
            logPathToRead = Paths.get(specificLogFilePath);
            log.info("Requested logs for past date ({}), using archived file: {}", formattedDate, logPathToRead);
        }

        try {
            if (!Files.exists(logPathToRead)) {
                log.warn("Standard log file not found at path: {}", logPathToRead);
                throw new ResourceNotFoundException(LOG_RESOURCE, "path", logPathToRead.toString());
            }
            if (!Files.isReadable(logPathToRead)) {
                log.error("Permission denied while trying to read standard log file: {}", logPathToRead);
                throw new ResourceNotFoundException(LOG_RESOURCE, "path", logPathToRead + " (permission denied)");
            }
        } catch (SecurityException e) {
            log.error("Permission denied while trying to access standard log file path: {}", logPathToRead, e);
            throw new ResourceNotFoundException(LOG_RESOURCE, "path", logPathToRead + " (permission denied)");
        }

        return logPathToRead;
    }
}