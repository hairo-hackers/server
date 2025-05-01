package org.hairo.server.contribution;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.hairo.server.discord.DiscordBot;
import org.hairo.server.github.webhook.GitHubClient;
import org.hairo.server.vertesia.VertesiaClient;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ContributionHandler {

    private final static Logger log = LoggerFactory.getLogger(ContributionHandler.class);

    public static final String HAIRO_HACKERS_ORG = "hairo-hackers";

    @Value("${discord.channel.maintainers}")
    private String discordChannelId;

    private final GitHubClient gitHubClient;

    private final DiscordBot discordBot;

    private final VertesiaClient vertesiaClient;

    @Autowired
    public ContributionHandler(final @NonNull GitHubClient gitHubClient, final @NonNull DiscordBot discordBot, final @NonNull VertesiaClient vertesiaClient) {
        this.gitHubClient = Objects.requireNonNull(gitHubClient, "gitHubClient must not be null");
        this.discordBot = Objects.requireNonNull(discordBot, "discordBot must not be null");
        this.vertesiaClient = Objects.requireNonNull(vertesiaClient, "vertesiaClient must not be null");
    }

    public void handleContribution(String title, String author, URI contributionUri) {
        log.info("Handling contribution: '{}' from '{}'", title, author);
        final boolean contributor = !gitHubClient.getAllUsersForOrg(HAIRO_HACKERS_ORG).contains(author);
        if (contributor) {
            final String message = "New contribution by " + author + ": " + title + "\n" +
                    "View it here: " + contributionUri.toString();
            discordBot.sendMessageToChannel(discordChannelId, message);
        }
        final JsonNode jsonNode = gitHubClient.doGraphQlQueryForFirstContribution();
        log.info("GraphQL query result: {}", jsonNode);
    }

    public void handleIssueComplexity(JsonNode githubJson, URI contributionUri) {
        vertesiaClient.setIssueComplexity(githubJson, contributionUri);
    }
}
