package org.hairo.server.vertesia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.hairo.server.discord.DiscordBot;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VertesiaClient {

    @Value("${vertesia.token}")
    private String token;

    private final DiscordBot discordBot;

    public VertesiaClient(DiscordBot discordBot) {
        this.discordBot = Objects.requireNonNull(discordBot, "discordBot must not be null");
    }

    public void checkCodeOfConduct(String message, URI source) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            final String body = objectMapper.createObjectNode()
                    .put("interaction", "CodeOfConductCheck")
                    .put("data", objectMapper.createObjectNode().put("message", message))
                    .toPrettyString();
            final JsonNode json = executePost(
                    new URI("https://studio-server-production.api.vertesia.io/api/v1/execute"), body);
            final String status = json.get("result").get("status").asText();
            final String description = json.get("result").get("comment").asText();
            discordBot.sendMessageToChannel(
                    "Code of Conduct Check for " + source + ":\n Result: " + status + "\nDescription: " + description);
        } catch (Exception e) {
            throw new RuntimeException("Error checking code of conduct", e);
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
                            throw new RuntimeException("Failed to fetch discussion content: " + response.statusCode());
                        }
                    }).get(10, TimeUnit.SECONDS);
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(result);
        } catch (Exception e) {
            throw new RuntimeException("Error executing GET request to " + uri, e);
        }
    }
}
