package com.workshop.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReadFileTool implements Tool {

    private static final ObjectMapper M = new ObjectMapper();

    @Override public String name() { return "read_file"; }

    @Override public String description() {
        return "Read the text content of a file at the given path.";
    }

    @Override public ObjectNode parameterSchema() {
        ObjectNode schema = M.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("path").put("type", "string").put("description", "File path to read.");
        schema.putArray("required").add("path");
        return schema;
    }

    @Override public String execute(JsonNode args) throws Exception {
        String path = args.path("path").asText();
        return Files.readString(Path.of(path));
    }
}
