package org.hairo.server.github.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
public class GitHubClient {

    public String getDiscussionContent(final @NonNull String orgName, final @NonNull String repoName,
            final @NonNull String discussionId) {
        Objects.requireNonNull(orgName, "orgName must not be null");
        Objects.requireNonNull(repoName, "repoName must not be null");
        Objects.requireNonNull(discussionId, "discussionId must not be null");
        if (orgName.isBlank()) {
            throw new IllegalArgumentException("orgName must not be blank");
        }
        if (repoName.isBlank()) {
            throw new IllegalArgumentException("repoName must not be blank");
        }
        if (discussionId.isBlank()) {
            throw new IllegalArgumentException("discussionId must not be blank");
        }
        try {
            final URI uri = new URI("https://api.github.com/repos/" + orgName + "/" + repoName
                    + "/discussions/" + discussionId);
            final JsonNode node = executeGet(uri);
            return node.get("body").asText();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching discussion content for org: " + orgName + ", repo: " + repoName
                    + ", discussionId: " + discussionId, e);
        }
    }

    public String getDiscussionTitle(final @NonNull String orgName, final @NonNull String repoName,
            final @NonNull String discussionId) {
        Objects.requireNonNull(orgName, "orgName must not be null");
        Objects.requireNonNull(repoName, "repoName must not be null");
        Objects.requireNonNull(discussionId, "discussionId must not be null");
        if (orgName.isBlank()) {
            throw new IllegalArgumentException("orgName must not be blank");
        }
        if (repoName.isBlank()) {
            throw new IllegalArgumentException("repoName must not be blank");
        }
        if (discussionId.isBlank()) {
            throw new IllegalArgumentException("discussionId must not be blank");
        }
        try {
            final URI uri = new URI("https://api.github.com/repos/" + orgName + "/" + repoName
                    + "/discussions/" + discussionId);
            final JsonNode node = executeGet(uri);
            return node.get("title").asText();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching discussion title for org: " + orgName + ", repo: " + repoName
                    + ", discussionId: " + discussionId, e);
        }
    }

    private JsonNode executeGet(final @NonNull URI uri) {
        Objects.requireNonNull(uri, "URI must not be null");
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "hAIro-Server")
                    .build();
            final String body = client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            return response.body();
                        } else {
                            throw new RuntimeException("Failed to fetch discussion content: " + response.statusCode());
                        }
                    }).get(10, TimeUnit.SECONDS);
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Error executing GET request to " + uri, e);
        }
    }

}
