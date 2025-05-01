package org.hairo.server.github.webhook;

import org.jspecify.annotations.NonNull;

public record DiscussionCreatedEvent(@NonNull String org, @NonNull String repo, @NonNull String discussionId) {
}
