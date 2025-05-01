package org.hairo.server.comment;

import java.net.URI;
import org.jspecify.annotations.NonNull;

public record Comment(@NonNull String comment, @NonNull URI source) {
}
