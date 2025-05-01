package org.hairo.server.logging;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoggingEndpoint {

    @GetMapping("/logs")
    public String getLogs() {
        return InMemoryLogAppender.getLogDump();
    }
}
