package org.hairo.server.discord;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DiscordBot {

    private final static Logger log = LoggerFactory.getLogger(DiscordBot.class);

    @Value("${discord.token}")
    private String token;

    @Value("${discord.channelId}")
    private String channelId;

    private final AtomicReference<JDA> jdaReference = new AtomicReference<JDA>();

    @PostConstruct
    private void init() {
        log.info("Initializing Discord bot");
        JDABuilder.createDefault(token)
                .addEventListeners(new ListenerAdapter() {
                    @Override
                    public void onReady(@NotNull ReadyEvent event) {
                        log.info("Discord bot is ready!");
                        jdaReference.set(event.getJDA());
                        event.getJDA().getTextChannels().forEach(ch -> {
                            log.info("Available channel: " + ch.getName() + " → " + ch.getId());
                        });
                    }
                })
                .build();
    }

    public void sendMessageToChannel(String channelId, String message) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final JDA jda = jdaReference.get();
        if (jda != null) {
            final TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue(m -> future.complete(null),
                        throwable -> future.completeExceptionally(throwable));
            } else {
                throw new IllegalArgumentException("Channel not found: " + channelId);
            }
        } else {
            throw new IllegalStateException("JDA is not initialized yet. Please wait for the bot to be ready.");
        }
        try {
            future.get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message to Discord channel", e);
        }
    }
}