package com.workshop.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;

public class WriteFileTool implements Tool {

    private static final ObjectMapper M = new ObjectMapper();

    @Override public String name() { return "write_file"; }

    @Override public String description() {
        return "Write text content to a file at the given path. Overwrites if it exists.";
    }

    @Override public ObjectNode parameterSchema() {
        ObjectNode schema = M.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("path").put("type", "string").put("description", "File path to write to.");
        props.putObject("content").put("type", "string").put("description", "Text to write.");
        schema.putArray("required").add("path").add("content");
        return schema;
    }

    @Override public String execute(JsonNode args) throws Exception {
        String path = args.path("path").asText();
        String content = args.path("content").asText("");
        Files.writeString(Path.of(path), content);
        return "wrote " + content.length() + " chars to " + path;
    }
}
