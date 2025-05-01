package org.hairo.server.vertesia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.hairo.server.discord.DiscordBot;
import org.hairo.server.github.webhook.GitHubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VertesiaClient {

    private final static Logger log = LoggerFactory.getLogger(VertesiaClient.class);

    @Value("${vertesia.token}")
    private String token;

    @Value("${discord.channel.coc}")
    private String codeOfConductChannel;

    @Value("${discord.channel.maintainers}")
    private String maintainerChannel;

    @Value("${discord.channel.issues}")
    private String issuesChannel;

    private final DiscordBot discordBot;

    private final GitHubClient gitHubClient;

    public VertesiaClient(final DiscordBot discordBot, final GitHubClient gitHubClient) {
        this.discordBot = Objects.requireNonNull(discordBot, "discordBot must not be null");
        this.gitHubClient = Objects.requireNonNull(gitHubClient, "gitHubClient must not be null");
    }

    public void checkCodeOfConduct(String message, URI source) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("message", message);
            final JsonNode json = executePost("CodeOfConductCheck", dataNode);
            final String status = json.get("result").get("status").asText();
            final String description = json.get("result").get("comment").asText();
            discordBot.sendMessageToChannel(discordChannelId,
                    "@everyone\nCode of Conduct Check for " + source + ":\n Result: " + status + "\nDescription: "
                            + description);
        } catch (Exception e) {
            throw new RuntimeException("Error checking code of conduct", e);
        }
    }

    public void checkUserScore(String author) {
        try {
            final JsonNode data = gitHubClient.getUserInfo(author);
            final JsonNode json = executePost("Contributor_profile", data);
            log.info("User score check result: {}", json.toPrettyString());
            discordBot.sendMessageToChannel(discordChannelId,
                    "User Score: " + json.toPrettyString());
        } catch (Exception e) {
            throw new RuntimeException("Error checking code of conduct", e);
        }
    }

    public Boolean determineGoodFirstIssue(JsonNode githubJson, URI source) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("github_json_title", githubJson.get("issue").get("title").asText());
            dataNode.put("github_json_summary", githubJson.get("issue").get("body").asText());

            final JsonNode json = executePost("Issue_Evaluation", dataNode);
            final JsonNode results = json.get("result");
            final String issueTitle = results.get("title").asText();
            final String issueSummary = results.get("summary").asText();
            final JsonNode recommendedContributors = results.get("recommended_contributors");

            String discordMessage =
                    "New Issue Opened: " + issueTitle + "\nSummary: " + issueSummary + "\nRecommended Contributors: ";

            Boolean goodFirstIssue = false;
            if (recommendedContributors != null && recommendedContributors.isArray()) {
                for (JsonNode item : recommendedContributors) {
                    final String contributorName = item.toString();
                    discordMessage += contributorName + ", ";

                    if (contributorName == "New Dev") {
                        goodFirstIssue = true;
                    }
                }
            }

            discordBot.sendMessageToChannel(discordChannelId, discordMessage);
            return goodFirstIssue;

        } catch (Exception e) {
            throw new RuntimeException("Error setting issue complexity", e);
        }
    }

    private JsonNode executePost(final String interaction, JsonNode data) {
        log.info("Executing interaction: {}", interaction);
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("interaction", interaction);
            bodyNode.set("data", data);
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://studio-server-production.api.vertesia.io/api/v1/execute"))
                    .POST(BodyPublishers.ofString(bodyNode.toPrettyString()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();
            final String result = client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            return response.body();
                        } else {
                            throw new RuntimeException("Failed to fetch: " + response.statusCode());
                        }
                    }).get(10, TimeUnit.SECONDS);
            return objectMapper.readTree(result);
        } catch (Exception e) {
            throw new RuntimeException("Error executing request", e);
        }
    }
}
