package by.frozzel.springreviewer.model;

import by.frozzel.springreviewer.model.enums.LogGenerationStatus;
import java.nio.file.Path;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LogGenerationTask {
    private final String id;
    private volatile LogGenerationStatus status = LogGenerationStatus.PENDING;
    private volatile Path resultPath;
    private volatile String errorMessage;
}