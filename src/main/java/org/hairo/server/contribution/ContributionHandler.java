package org.hairo.server.contribution;

import java.net.URI;
import java.util.Objects;
import org.hairo.server.discord.DiscordBot;
import org.hairo.server.github.webhook.GitHubClient;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContributionHandler {

    private final GitHubClient gitHubClient;

    private final DiscordBot discordBot;

    @Autowired
    public ContributionHandler(final @NonNull GitHubClient gitHubClient, final @NonNull DiscordBot discordBot) {
        this.gitHubClient = Objects.requireNonNull(gitHubClient, "gitHubClient must not be null");
        this.discordBot = Objects.requireNonNull(discordBot, "discordBot must not be null");
    }

    public void handleContribution(String title, String author, URI contributionUri) {
        final boolean contributor = !gitHubClient.getAllUsersForOrg("hairo-hackers").contains(author);
        if (contributor) {
            final String message = "New contribution by " + author + ": " + title + "\n" +
                    "View it here: " + contributionUri.toString();
            discordBot.sendMessageToChannel(message);
        }
    }
}
