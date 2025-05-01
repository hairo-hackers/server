package org.hairo.server;

import ch.qos.logback.classic.Logger;
import jakarta.annotation.PostConstruct;
import org.hairo.server.logging.InMemoryLogAppender;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HairoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HairoServerApplication.class, args);
    }

    @PostConstruct
    public void initLogBuffer() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        InMemoryLogAppender memoryAppender = new InMemoryLogAppender();
        memoryAppender.setContext(rootLogger.getLoggerContext());
        memoryAppender.start();
        rootLogger.addAppender(memoryAppender);
    }
}
