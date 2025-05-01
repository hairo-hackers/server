package org.hairo.server.comment;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommentHandler {

    private final static Logger log = LoggerFactory.getLogger(CommentHandler.class);
    
    public void handleComment(final @NonNull Comment comment) {
        Objects.requireNonNull(comment, "comment must not be null");
        log.info("Handling comment: '{}' from '{}'", comment.comment(), comment.source());
    }
}
