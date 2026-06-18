package com.workshop.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Anything the agent can DO in the outside world implements this.
 *
 * Each Tool exposes:
 *   - a name the model uses to call it
 *   - a JSON-schema-like declaration so Gemini knows the parameters
 *   - an execute() that consumes parsed args and returns a result string
 */
public interface Tool {
    String name();
    String description();

    /** JSON schema describing the parameters this tool expects. */
    ObjectNode parameterSchema();

    /** Execute the tool. Return a short, human/model-readable result. */
    String execute(JsonNode args) throws Exception;
}
