package org.hairo.server.github.webhook;

public record DiscussionCreatedEvent(String org, String repo, String discussionId) {
}
