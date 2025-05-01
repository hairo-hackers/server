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
import org.jspecify.annotations.NonNull;
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
    private String discordChannelId;

    private final DiscordBot discordBot;

    public VertesiaClient(DiscordBot discordBot) {
        this.discordBot = Objects.requireNonNull(discordBot, "discordBot must not be null");
    }

    public void checkCodeOfConduct(String message, URI source) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("message", message);
            final ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("interaction", "CodeOfConductCheck");
            bodyNode.set("data", dataNode);
            final JsonNode json = executePost(
                    new URI("https://studio-server-production.api.vertesia.io/api/v1/execute"),
                    bodyNode.toPrettyString());
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

            String content = """
                    {
                        "login": "hendrikebbers",
                        "id": 9443847,
                        "node_id": "MDQ6VXNlcjk0NDM4NDc=",
                        "avatar_url": "https://avatars.githubusercontent.com/u/9443847?v=4",
                        "gravatar_id": "",
                        "url": "https://api.github.com/users/hendrikebbers",
                        "html_url": "https://github.com/hendrikebbers",
                        "followers_url": "https://api.github.com/users/hendrikebbers/followers",
                        "following_url": "https://api.github.com/users/hendrikebbers/following{/other_user}",
                        "gists_url": "https://api.github.com/users/hendrikebbers/gists{/gist_id}",
                        "starred_url": "https://api.github.com/users/hendrikebbers/starred{/owner}{/repo}",
                        "subscriptions_url": "https://api.github.com/users/hendrikebbers/subscriptions",
                        "organizations_url": "https://api.github.com/users/hendrikebbers/orgs",
                        "repos_url": "https://api.github.com/users/hendrikebbers/repos",
                        "events_url": "https://api.github.com/users/hendrikebbers/events{/privacy}",
                        "received_events_url": "https://api.github.com/users/hendrikebbers/received_events",
                        "type": "User",
                        "user_view_type": "public",
                        "site_admin": false,
                        "name": "Hendrik Ebbers",
                        "company": "Open Elements GmbH @openelements ",
                        "blog": "https://open-elements.com",
                        "location": "Dortmund, Germany",
                        "email": null,
                        "hireable": null,
                        "bio": "Founder of @openelements - @eclipse-ee4j @adoptium and @AdoptOpenJDK member and Java Champion - working on @hiero-ledger and @hashgraph",
                        "twitter_username": "hendrikEbbers",
                        "public_repos": 99,
                        "public_gists": 265,
                        "followers": 222,
                        "following": 73,
                        "created_at": "2014-10-29T08:04:30Z",
                        "updated_at": "2025-04-28T15:21:31Z"
                    }""";
            final ObjectMapper objectMapper = new ObjectMapper();
            final ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("interaction", "Contributor_profile");
            bodyNode.put("data", content);
            final JsonNode json = executePost(
                    new URI("https://studio-server-production.api.vertesia.io/api/v1/execute"),
                    content);
            log.info("User score check result: {}", json.toPrettyString());
        } catch (Exception e) {
            throw new RuntimeException("Error checking code of conduct", e);
        }
    }

    public void setIssueComplexity(String title, String summary) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("github_json_title", title);
            dataNode.put("github_json_summary", summary);
            final ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("interaction", "Issue_Evaluation");
            bodyNode.set("data", dataNode);
            final JsonNode json = executePost(
                    new URI("https://studio-server-production.api.vertesia.io/api/v1/execute"),
                    bodyNode.toPrettyString());
            final JsonNode results = json.get("result");
            final String issueTitle = results.get("title").asText();
            final String issueSummary = results.get("summary").asText();
            final JsonNode recommendedContributors = results.get("recommended_contributors");

            String discordMessage =
                    "New Issue Opened: " + issueTitle + "\nSummary: " + issueSummary + "\nRecommended Contributors: ";

            if (recommendedContributors != null && recommendedContributors.isArray()) {
                for (JsonNode item : recommendedContributors) {
                    discordMessage += item.toString() + ", ";
                }
            }

            discordBot.sendMessageToChannel(discordChannelId, discordMessage);
        } catch (Exception e) {
            throw new RuntimeException("Error setting issue complexity", e);
        }
    }

    private JsonNode executePost(final @NonNull URI uri, String body) {
        Objects.requireNonNull(uri, "URI must not be null");
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(BodyPublishers.ofString(body))
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
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(result);
        } catch (Exception e) {
            throw new RuntimeException("Error executing request to " + uri, e);
        }
    }
}
