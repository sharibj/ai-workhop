package com.workshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * STEP 3 — turn raw model text into a structured object.
 *
 * Already implemented; activated by AgentApp.USE_STRUCTURED_OUTPUT.
 * The point during the workshop: "the model speaks JSON, our code consumes it."
 */
public class StructuredParser {

    private static final ObjectMapper M = new ObjectMapper();

    public static Optional<JsonNode> tryParse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String trimmed = raw.trim();
        // Tolerate ```json fences if the model adds them.
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            if (firstNl > 0) trimmed = trimmed.substring(firstNl + 1);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        try {
            return Optional.of(M.readTree(trimmed));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
