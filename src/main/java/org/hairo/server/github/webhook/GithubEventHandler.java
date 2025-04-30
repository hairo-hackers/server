package org.hairo.server.github.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GithubEventHandler {

    private final static Logger log = LoggerFactory.getLogger(GithubEventHandler.class);

    public void handleDiscussionCreatedEvent(DiscussionCreatedEvent event) {
        log.info("Handling discussion created event: org={}, repo={}, discussionId={}",
                event.org(), event.repo(), event.discussionId());
    }
}
