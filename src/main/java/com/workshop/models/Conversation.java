package com.workshop.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The agent's working memory. A linear list of messages.
 *
 * Used from STEP 2 onward. Already implemented; just unused in STEP 1.
 */
public class Conversation {

    private final List<Message> history = new ArrayList<>();
    private String systemPrompt;   // STEP 3+: optional preamble sent on every request.

    /**
     * STEP 3 — set the "system prompt": the always-on instruction that shapes
     * how the model behaves. This is how we tell the model to reply in JSON,
     * pretend to be a pirate, refuse certain topics, etc.
     */
    public void setSystemPrompt(String prompt) { this.systemPrompt = prompt; }
    public String systemPrompt()               { return systemPrompt; }

    public void addUser(String text)   { history.add(new Message("user", text)); }
    public void addModel(String text)  { history.add(new Message("model", text)); }

    /** STEP 4+: record the model's tool call so the next request shows full history. */
    public void addModelToolCall(String name, JsonNode args) {
        history.add(new Message("model", "__TOOLCALL__" + name + "|||" + args.toString()));
    }

    /** STEP 4+: record a tool's result. */
    public void addToolResult(String toolName, String result) {
        history.add(new Message("tool", toolName + "|||" + result));
    }

    public List<Message> messages() {
        return Collections.unmodifiableList(history);
    }

    /** Remove the most recent message — used to roll back after a failed API call. */
    public void dropLast() {
        if (!history.isEmpty()) history.remove(history.size() - 1);
    }

    public int size() { return history.size(); }
}
