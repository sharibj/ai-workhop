package com.workshop.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.workshop.gemini.models.FunctionDeclaration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the tools the agent can call, and runs them by name.
 *
 * Already implemented; activated by AgentApp.USE_TOOLS / USE_AGENT_LOOP.
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public List<FunctionDeclaration> declarations() {
        List<FunctionDeclaration> decls = new ArrayList<>();
        for (Tool t : tools.values()) {
            decls.add(t.declaration());
        }
        return decls;
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
