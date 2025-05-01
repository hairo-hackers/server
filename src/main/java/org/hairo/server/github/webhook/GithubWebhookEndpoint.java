package org.hairo.server.github.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GithubWebhookEndpoint {

    private final static Logger log = LoggerFactory.getLogger(GithubWebhookEndpoint.class);
    public static final String HUB_EVENT = "X-GitHub-Event";

    private final GithubEventHandler githubEventHandler;

    @Autowired
    public GithubWebhookEndpoint(GithubEventHandler githubEventHandler) {
        this.githubEventHandler = githubEventHandler;
    }

    @PostMapping("/github/webhook")
    public void onGitHubEvent(@RequestHeader(HUB_EVENT) String eventType, @RequestBody String body) {
        log.info("Received GitHub event of type '{}': {}", eventType, body);
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode jsonNode = objectMapper.readTree(body);

            if (Objects.equals("discussion", eventType)) {
                final String action = getAction(jsonNode);
                if (Objects.equals(action, "created")) {
                    final String orgName = getOrgName(jsonNode);
                    final String repoName = getRepoName(jsonNode);
                    final String discussionId = getDiscussionId(jsonNode);
                    final DiscussionCreatedEvent event = new DiscussionCreatedEvent(orgName, repoName,
                            discussionId);
                    githubEventHandler.handleDiscussionCreatedEvent(event);
                } else {
                    log.info("Ignoring non-created action for discussion: {}", action);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in Github webhook", e);
        }
    }

    private String getAction(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("action")) {
            return jsonNode.get("action").asText();
        }
        throw new IllegalArgumentException("Action not found in JSON");
    }

    private String getOrgName(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("organization") && jsonNode.get("organization").has("login")) {
            return jsonNode.get("organization").get("login").asText();
        }
        throw new IllegalArgumentException("Repository full name not found in JSON");
    }

    private String getRepoName(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("repository") && jsonNode.get("repository").has("name")) {
            return jsonNode.get("repository").get("name").asText();
        }
        throw new IllegalArgumentException("Repository full name not found in JSON");
    }

    private String getDiscussionId(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("discussion") && jsonNode.get("discussion").has("number")) {
            return jsonNode.get("discussion").get("number").asText();
        }
        throw new IllegalArgumentException("Discussion ID not found in JSON");
    }
}
