package org.hairo.server.github.webhook;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GithubEventHandler {

    private final static Logger log = LoggerFactory.getLogger(GithubEventHandler.class);

    private final GitHubClient gitHubClient;

    @Autowired
    public GithubEventHandler(final @NonNull GitHubClient gitHubClient) {
        this.gitHubClient = Objects.requireNonNull(gitHubClient, "gitHubClient must not be null");
    }

    public void handleDiscussionCreatedEvent(final @NonNull DiscussionCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        log.info("Handling discussion created event: org={}, repo={}, discussionId={}",
                event.org(), event.repo(), event.discussionId());
        final String discussionContent = gitHubClient.getDiscussionContent(event);
        log.info("Discussion content for org: {}, repo: {}, discussionId: {} is: {}",
                event.org(), event.repo(), event.discussionId(), discussionContent);
    }
}
