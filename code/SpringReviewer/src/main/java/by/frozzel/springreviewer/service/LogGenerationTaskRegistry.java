package by.frozzel.springreviewer.service;

import by.frozzel.springreviewer.exception.ResourceNotFoundException;
import by.frozzel.springreviewer.model.LogGenerationTask;
import by.frozzel.springreviewer.model.enums.LogGenerationStatus;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogGenerationTaskRegistry {

    private final Map<String, LogGenerationTask> tasks = new ConcurrentHashMap<>();

    public LogGenerationTask createTask() {
        String taskId = UUID.randomUUID().toString();
        LogGenerationTask task = new LogGenerationTask(taskId);
        tasks.put(taskId, task);
        log.info("Created log generation task with ID: {}", taskId);
        return task;
    }

    public LogGenerationTask getTask(String taskId) {
        LogGenerationTask task = tasks.get(taskId);
        if (task == null) {
            log.warn("Attempted to access non-existent task with ID: {}", taskId);
            throw new ResourceNotFoundException("LogGenerationTask", "ID", taskId);
        }
        return task;
    }

    public void updateStatus(String taskId, LogGenerationStatus status) {
        LogGenerationTask task = getTask(taskId);
        task.setStatus(status);
        log.info("Updated status for task ID {}: {}", taskId, status);
    }

    public void setSuccessResult(String taskId, Path resultPath) {
        LogGenerationTask task = getTask(taskId);
        task.setResultPath(resultPath);
        task.setStatus(LogGenerationStatus.COMPLETED);
        log.info("Task ID {} completed successfully. Result path: {}", taskId, resultPath);
    }

    public void setFailureResult(String taskId, String errorMessage) {
        LogGenerationTask task = getTask(taskId);
        task.setErrorMessage(errorMessage);
        task.setStatus(LogGenerationStatus.FAILED);
        log.error("Task ID {} failed: {}", taskId, errorMessage);
    }
}