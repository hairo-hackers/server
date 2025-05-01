package org.hairo.server.github.webhook;

import static org.hairo.server.github.webhook.GitHubWebhookEventTypes.DISCUSSION_COMMENT_CREATED;
import static org.hairo.server.github.webhook.GitHubWebhookEventTypes.DISCUSSION_CREATED;
import static org.hairo.server.github.webhook.GitHubWebhookEventTypes.ISSUE_COMMENT_CREATED;
import static org.hairo.server.github.webhook.GitHubWebhookEventTypes.ISSUE_CREATED;
import static org.hairo.server.github.webhook.GitHubWebhookEventTypes.PR_CREATED;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getAction;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getComment;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getCommentUrl;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getDiscussionAuthor;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getDiscussionText;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getDiscussionTitle;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getDiscussionUrl;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getIssueAuthor;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getIssueText;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getIssueTitle;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getIssueUrl;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getPullRequestAuthor;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getPullRequestText;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getPullRequestTitle;
import static org.hairo.server.github.webhook.GithubWebhookJsonParser.getPullRequestUrl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Objects;
import org.hairo.server.comment.Comment;
import org.hairo.server.comment.CommentHandler;
import org.hairo.server.contribution.ContributionHandler;
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

    private final ContributionHandler contributionHandler;

    @Autowired
    public GithubWebhookEndpoint(final @NonNull CommentHandler commentHandler,
            final @NonNull ContributionHandler contributionHandler) {
        this.commentHandler = Objects.requireNonNull(commentHandler, "commentHandler must not be null");
        this.contributionHandler = Objects.requireNonNull(contributionHandler, "contributionHandler must not be null");
    }

    @PostMapping("/github/webhook")
    public void onGitHubEvent(@RequestHeader(HUB_EVENT) String eventType, @RequestBody String body) {
        log.info("Received GitHub event of type '{}': {}", eventType, body);
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode jsonNode = objectMapper.readTree(body);
            final String action = getAction(jsonNode);
            GitHubWebhookEventTypes.of(eventType, action).ifPresent(eventTypeEnum -> {
                if (eventTypeEnum == DISCUSSION_CREATED) {
                    final String content = getDiscussionTitle(jsonNode) + "\n" + getDiscussionText(jsonNode);
                    final URI url = getDiscussionUrl(jsonNode);
                    final Comment titleComment = new Comment(content, url);
                    commentHandler.handleComment(titleComment);
                    contributionHandler.handleContribution(getDiscussionTitle(jsonNode), getDiscussionAuthor(jsonNode),
                            url);
                } else if (eventTypeEnum == DISCUSSION_COMMENT_CREATED) {
                    final String content = getComment(jsonNode);
                    final URI url = getCommentUrl(jsonNode);
                    final Comment titleComment = new Comment(content, url);
                    commentHandler.handleComment(titleComment);
                } else if (eventTypeEnum == ISSUE_CREATED) {
                    final String content = getIssueTitle(jsonNode) + "\n" + getIssueText(jsonNode);
                    final URI url = getIssueUrl(jsonNode);
                    final Comment titleComment = new Comment(content, url);
                    commentHandler.handleComment(titleComment);
                    contributionHandler.handleContribution(getIssueTitle(jsonNode), getIssueAuthor(jsonNode), url);
                    contributionHandler.handleIssueComplexity(getIssueTitle(jsonNode), getIssueText(jsonNode), url);
                } else if (eventTypeEnum == ISSUE_COMMENT_CREATED) {
                    final String content = getComment(jsonNode);
                    final URI url = getCommentUrl(jsonNode);
                    final Comment titleComment = new Comment(content, url);
                    commentHandler.handleComment(titleComment);
                } else if (eventTypeEnum == PR_CREATED) {
                    final String content = getPullRequestTitle(jsonNode) + "\n" + getPullRequestText(jsonNode);
                    final URI url = getPullRequestUrl(jsonNode);
                    final Comment titleComment = new Comment(content, url);
                    commentHandler.handleComment(titleComment);
                    contributionHandler.handleContribution(getPullRequestTitle(jsonNode),
                            getPullRequestAuthor(jsonNode),
                            url);
                } else {
                    log.warn("Unhandled GitHub event type: {} - {}", eventType, action);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error in Github webhook", e);
        }
    }

}
