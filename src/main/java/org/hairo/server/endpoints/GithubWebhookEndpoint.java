package org.hairo.server.endpoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GithubWebhookEndpoint {

    private final static Logger log = LoggerFactory.getLogger(GithubWebhookEndpoint.class);
    public static final String GITHUB_EVENT_HEADER = "X-GitHub-Event";

    @PostMapping("/github/webhook")
    public void onGitHubEvent(@RequestHeader(GITHUB_EVENT_HEADER) String eventType, @RequestBody String payload) {
        log.info("Received GitHub event of tyoe '{}': {}", eventType, payload);
    }
}
