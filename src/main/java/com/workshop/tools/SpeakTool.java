package com.workshop.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workshop.Speaker;

/**
 * STEP 4 — the "speak the name" capability, exposed to the LLM as a tool.
 *
 *   Step 3: WE parse JSON and call speaker.speak() — we are the glue.
 *   Step 4: the LLM emits functionCall:speak({name: ...}) and the registry
 *           routes the call into THIS class — the glue dissolves.
 */
public class SpeakTool implements Tool {

    private static final ObjectMapper M = new ObjectMapper();
    private final Speaker speaker;

    public SpeakTool(Speaker speaker) {
        this.speaker = speaker;
    }

    @Override public String name() { return "speak"; }

    @Override public String description() {
        return "Say a person's name out loud. Use this whenever the user asks you to say their name.";
    }

    @Override public ObjectNode parameterSchema() {
        ObjectNode schema = M.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("name").put("type", "string").put("description", "The name to say.");
        schema.putArray("required").add("name");
        return schema;
    }

    @Override public String execute(JsonNode args) {
        String name = args.path("name").asText("");
        speaker.speak(name, Speaker.Source.LLM);
        return "spoke '" + name + "'";
    }
}
