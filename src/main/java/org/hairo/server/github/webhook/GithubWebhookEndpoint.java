package org.hairo.server.github.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Objects;
import org.hairo.server.comment.Comment;
import org.hairo.server.comment.CommentHandler;
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

    private final CommentHandler commentHandler;

    @Autowired
    public GithubWebhookEndpoint(CommentHandler commentHandler) {
        this.commentHandler = commentHandler;
    }

    @PostMapping("/github/webhook")
    public void onGitHubEvent(@RequestHeader(HUB_EVENT) String eventType, @RequestBody String body) {
        log.info("Received GitHub event of type '{}': {}", eventType, body);
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode jsonNode = objectMapper.readTree(body);
            final String action = getAction(jsonNode);
            final GitHubWebhookEventTypes eventTypeEnum = GitHubWebhookEventTypes.of(eventType, action)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + eventType));
            if (eventTypeEnum == GitHubWebhookEventTypes.DISCUSSION_CREATED) {
                final String title = getDiscussionTitle(jsonNode);
                final String content = title + "\n" + getDiscussionText(jsonNode);
                final URI url = getDiscussionUrl(jsonNode);
                final Comment titleComment = new Comment(content, url);
                commentHandler.handleComment(titleComment);
            } else if (eventTypeEnum == GitHubWebhookEventTypes.DISCUSSION_COMMENT_CREATED) {
                final String content = getComment(jsonNode);
                final URI url = getCommentUrl(jsonNode);
                final Comment titleComment = new Comment(content, url);
                commentHandler.handleComment(titleComment);
            } else if (eventTypeEnum == GitHubWebhookEventTypes.ISSUE_CREATED) {
                final String title = getIssueTitle(jsonNode);
                final String content = title + "\n" + getIssueText(jsonNode);
                final URI url = getIssueUrl(jsonNode);
                final Comment titleComment = new Comment(content, url);
                commentHandler.handleComment(titleComment);
            } else if (eventTypeEnum == GitHubWebhookEventTypes.ISSUE_COMMENT_CREATED) {
                final String content = getComment(jsonNode);
                final URI url = getCommentUrl(jsonNode);
                final Comment titleComment = new Comment(content, url);
                commentHandler.handleComment(titleComment);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in Github webhook", e);
        }
    }

    private URI getIssueUrl(JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("issue") && jsonNode.get("issue").has("html_url")) {
            try {
                return new URI(jsonNode.get("issue").get("html_url").asText());
            } catch (Exception e) {
                throw new RuntimeException("Error parsing URL", e);
            }
        }
        throw new IllegalArgumentException("url not found in JSON");
    }

    private String getIssueTitle(JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("issue") && jsonNode.get("issue").has("title")) {
            return jsonNode.get("issue").get("title").asText();
        }
        throw new IllegalArgumentException("title not found in JSON");
    }

    private String getIssueText(JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("issue") && jsonNode.get("issue").has("body")) {
            return jsonNode.get("issue").get("body").asText();
        }
        throw new IllegalArgumentException("title not found in JSON");
    }

    private String getAction(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("action")) {
            return jsonNode.get("action").asText();
        }
        throw new IllegalArgumentException("Action not found in JSON");
    }

    private String getDiscussionTitle(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("discussion") && jsonNode.get("discussion").has("title")) {
            return jsonNode.get("discussion").get("title").asText();
        }
        throw new IllegalArgumentException("title not found in JSON");
    }

    private String getDiscussionText(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("discussion") && jsonNode.get("discussion").has("body")) {
            return jsonNode.get("discussion").get("body").asText();
        }
        throw new IllegalArgumentException("comment not found in JSON");
    }

    private String getComment(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("comment") && jsonNode.get("comment").has("body")) {
            return jsonNode.get("comment").get("body").asText();
        }
        throw new IllegalArgumentException("comment not found in JSON");
    }

    private URI getDiscussionUrl(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("discussion") && jsonNode.get("discussion").has("html_url")) {
            try {
                return new URI(jsonNode.get("discussion").get("html_url").asText());
            } catch (Exception e) {
                throw new RuntimeException("Error parsing discussion comment URL", e);
            }
        }
        throw new IllegalArgumentException("url not found in JSON");
    }

    private URI getCommentUrl(final @NonNull JsonNode jsonNode) {
        Objects.requireNonNull(jsonNode, "jsonNode must not be null");
        if (jsonNode.has("comment") && jsonNode.get("comment").has("html_url")) {
            try {
                return new URI(jsonNode.get("comment").get("html_url").asText());
            } catch (Exception e) {
                throw new RuntimeException("Error parsing discussion comment URL", e);
            }
        }
        throw new IllegalArgumentException("url not found in JSON");
    }
}
