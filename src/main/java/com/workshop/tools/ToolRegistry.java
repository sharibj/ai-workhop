package com.workshop.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the tools the agent can call, and runs them by name.
 *
 * Already implemented; activated by AgentApp.USE_TOOLS / USE_AGENT_LOOP.
 */
public class ToolRegistry {

    private static final ObjectMapper M = new ObjectMapper();
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    /** Build the `functionDeclarations` array Gemini expects. */
    public ArrayNode declarations() {
        ArrayNode arr = M.createArrayNode();
        for (Tool t : tools.values()) {
            ObjectNode decl = arr.addObject();
            decl.put("name", t.name());
            decl.put("description", t.description());
            decl.set("parameters", t.parameterSchema());
        }
        return arr;
    }

    public String execute(String name, JsonNode args) {
        Tool t = tools.get(name);
        if (t == null) return "ERROR: unknown tool '" + name + "'";
        try {
            return t.execute(args);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
