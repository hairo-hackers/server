package org.hairo.server.discord;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DiscordBot {

    @Value("${discord.token}")
    private String token;

    private final AtomicReference<JDA> jdaReference = new AtomicReference<JDA>();

    public DiscordBot() {
        JDABuilder.createDefault(token)
                .addEventListeners(new ListenerAdapter() {
                    @Override
                    public void onReady(@NotNull ReadyEvent event) {
                        jdaReference.set(event.getJDA());
                    }
                })
                .build();
    }

    public void sendMessageToChannel(@NonNull final String channelId, String message) {
        Objects.requireNonNull(channelId, "Channel ID must not be null");
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