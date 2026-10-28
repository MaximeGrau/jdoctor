package dev.jdoctor.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Reads the Markdown runbook that documents a finding id. */
public final class Runbooks {

    private Runbooks() {
    }

    public static Optional<String> forFinding(String findingId) {
        String resource = "/runbooks/" + findingId + ".md";
        try (InputStream in = Runbooks.class.getResourceAsStream(resource)) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read runbook " + resource, e);
        }
    }
}
