package org.hairo.server.comment;

import java.util.Objects;
import org.hairo.server.discord.DiscordBot;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentHandler {

    private final static Logger log = LoggerFactory.getLogger(CommentHandler.class);

    private final DiscordBot discordBot;

    @Autowired
    public CommentHandler(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    public void handleComment(final @NonNull Comment comment) {
        Objects.requireNonNull(comment, "comment must not be null");
        log.info("Handling comment: '{}' from '{}'", comment.comment(), comment.source());
        final String message = "New comment :" + comment.comment() + "\nsee " + comment.source();
        discordBot.sendMessageToChannel(message);
    }
}
