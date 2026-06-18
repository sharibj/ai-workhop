package com.workshop;

import com.workshop.models.Conversation;
import com.workshop.tools.ToolRegistry;
import com.workshop.utilities.GeminiClient;

/**
 * STEP 5 — the agent loop.
 *
 *   while not done:
 *     1. ask the model (with tool declarations + full history)
 *     2. if it returns a tool call → execute it, append the result, repeat
 *     3. if it returns text → that's the final answer, stop
 *
 * Already implemented; activated by AgentApp.USE_AGENT_LOOP.
 */
public class AgentLoop {

    private static final int MAX_STEPS = 6;

    private final GeminiClient gemini;
    private final Conversation memory;
    private final ToolRegistry tools;

    public AgentLoop(GeminiClient gemini, Conversation memory, ToolRegistry tools) {
        this.gemini = gemini;
        this.memory = memory;
        this.tools = tools;
    }

    public String run(String userInput) throws Exception {
        memory.addUser(userInput);

        for (int step = 0; step < MAX_STEPS; step++) {
            GeminiClient.Reply r = gemini.chatWithTools(memory, tools.declarations());

            if (r.toolCall != null) {
                // Model wants to act. Run the tool, log result, loop again.
                System.out.println("  ↳ tool call: " + r.toolCall.name + " " + r.toolCall.args);
                String result = tools.execute(r.toolCall.name, r.toolCall.args);
                System.out.println("  ↳ tool result: " + result);

                memory.addModelToolCall(r.toolCall.name, r.toolCall.args);
                memory.addToolResult(r.toolCall.name, result);
                continue;
            }

            // Plain text reply — we're done.
            memory.addModel(r.text);
            return r.text;
        }
        return "[agent stopped: reached MAX_STEPS=" + MAX_STEPS + "]";
    }
}
