package by.frozzel.springreviewer.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    public static final String LOG_GENERATION_EXECUTOR = "logGenerationTaskExecutor";

    @Bean(name = LOG_GENERATION_EXECUTOR)
    public Executor logGenerationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("LogGenAsync-");
        executor.setRejectedExecutionHandler((r, exec) ->
                log.warn("Task rejected from log generation executor: {}", r)
        );
        executor.initialize();
        log.info("Configured ThreadPoolTaskExecutor bean with name '{}'", LOG_GENERATION_EXECUTOR);
        return executor;
    }
}