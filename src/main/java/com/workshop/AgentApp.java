package com.workshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.workshop.tools.ReadFileTool;
import com.workshop.tools.SpeakTool;
import com.workshop.tools.ToolRegistry;
import com.workshop.tools.WriteFileTool;

import java.util.Optional;
import java.util.Scanner;

/**
 * AgentApp — the only entry point.
 *
 * Workshop philosophy:
 *   Everything is already wired. Capabilities are GATED by boolean flags.
 *   Flip a flag → unlock the next stage.
 *
 * The whole workshop tells one story: "Say My Name."
 *   STEP 1: tell the bot your name. Ask "say my name." It can't — no memory.
 *   STEP 2: memory on. It remembers and replies in text.
 *   STEP 3: structured output + system prompt. The bot replies in JSON
 *           {"name": "..."} and our code feeds that into Speaker → ASCII banner.
 *   STEP 4: tools on. The LLM itself calls the speak() tool — the banner
 *           appears because the model decided to make it appear.
 *   STEP 5: agent loop (designed separately).
 */
public class AgentApp {

    // ─────────────────────────────────────────────────────────────────────
    // FEATURE FLAGS — flip these during the workshop, one at a time.
    // ─────────────────────────────────────────────────────────────────────

    // STEP 2: turn this on to give the bot conversation memory.
    static final boolean USE_MEMORY = true;

    // STEP 3: turn this on to ask Gemini for structured JSON output.
    static final boolean USE_STRUCTURED_OUTPUT = true;

    // STEP 4: turn this on so the model can call tools (write_file, read_file, speak).
    static final boolean USE_TOOLS = true;

    // STEP 5: turn this on to run the full think→act→observe agent loop.
    static final boolean USE_AGENT_LOOP = false;

    // Audio bonus: banner is ALWAYS shown; flip this to also play TTS audio.
    static final boolean USE_REAL_TTS = true;

    // ─────────────────────────────────────────────────────────────────────

    /** STEP 3 — system prompt that turns the bot into a structured "name speaker." */
    private static final String SAY_MY_NAME_SYSTEM_PROMPT =
            "You are part of a 'Say My Name' demo. " +
            "ALWAYS reply with ONLY a JSON object of the form " +
            "{\"name\": \"<their name or empty string>\", \"message\": \"<your reply to them>\"} " +
            "— no prose outside the JSON, no markdown. " +
            "Set 'name' to the user's name ONLY when they explicitly ask you to say it " +
            "(e.g. 'say my name', 'what's my name'). Otherwise set 'name' to an empty string. " +
            "Always put your conversational reply in 'message'. " +
            "If you don't know their name yet, set name to \"\" and ask for it in 'message'.";

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Please set GEMINI_API_KEY environment variable.");
            System.exit(1);
        }

        GeminiClient gemini = new GeminiClient(apiKey);
        Conversation memory = new Conversation();
        Speaker speaker = new Speaker(gemini, USE_REAL_TTS);

        // STEP 3: the system prompt is how we shape the model's behavior.
        if (USE_STRUCTURED_OUTPUT) {
            memory.setSystemPrompt(SAY_MY_NAME_SYSTEM_PROMPT);
        }

        // Tools are ALWAYS registered — they just stay unused until USE_TOOLS=true.
        ToolRegistry tools = new ToolRegistry();
        tools.register(new WriteFileTool());
        tools.register(new ReadFileTool());
        tools.register(new SpeakTool(speaker));

        AgentLoop agent = new AgentLoop(gemini, memory, tools);

        Scanner in = new Scanner(System.in);
        System.out.println("Say-My-Name agent ready. Type 'exit' to quit.");

        while (true) {
            System.out.print("\nyou> ");
            if (!in.hasNextLine()) break;
            String userInput = in.nextLine().trim();
            if (userInput.isEmpty()) continue;
            if (userInput.equalsIgnoreCase("exit")) break;
            String reply;

            try {
                if (USE_AGENT_LOOP) {
                    // STEP 5: the loop drives multi-turn tool use until the model is done.
                    reply = agent.run(userInput);
                } else if (USE_TOOLS) {
                    // STEP 4: single-shot tool call — model may emit ONE tool call,
                    // we execute it, but we do NOT loop back yet.
                    reply = singleTurnWithTools(gemini, memory, tools, userInput);
                } else if (USE_MEMORY) {
                    // STEP 2/3: stateful chat, optionally asking for JSON.
                    memory.addUser(userInput);
                    try {
                        reply = gemini.chat(memory, USE_STRUCTURED_OUTPUT);
                        memory.addModel(reply);
                    } catch (Exception ex) {
                        memory.dropLast();
                        throw ex;
                    }
                    if (USE_STRUCTURED_OUTPUT) {
                        // STEP 3: parse the JSON the model gave us, then "say" the name.
                        // The model returns {name, message}:
                        //   - 'message' is the conversational reply (always shown).
                        //   - 'name'    fires the speaker ONLY when non-empty.
                        // Students should see: the LLM only returned data; OUR Java code
                        // decides what to do with each field.
                        Optional<JsonNode> parsed = StructuredParser.tryParse(reply);
                        if (parsed.isPresent()) {
                            JsonNode p = parsed.get();
                            String name    = p.path("name").asText("");
                            String message = p.path("message").asText("");
                            System.out.println("model_json> " + reply);
                            if (!message.isBlank()) {
                                reply = message;
                            }
                            if (!name.isBlank()) {
                                System.out.print("👨‍💻 java_code: name=\"" + name + "\" detected. " +
                                        "Invoke speaker? (y/n) ");
                                String confirm = in.hasNextLine() ? in.nextLine().trim().toLowerCase() : "n";
                                if (confirm.equals("y") || confirm.equals("yes")) {
                                    speaker.speak(name, Speaker.Source.CODE);
                                } else {
                                    System.out.println("   skipped.");
                                }
                            } else {
                                System.out.println("👨‍💻 java_code: name is empty → speaker NOT called");
                            }
                        }
                    }
                } else {
                    // STEP 1: stateless single-shot call. The simplest possible chatbot.
                    reply = gemini.complete(userInput);
                }
            } catch (Exception e) {
                System.out.println("bot> [error] " + e.getMessage());
                System.out.println("       (try again — the model may be overloaded)");
                continue;
            }

            System.out.println("bot> " + reply);
        }
    }

    /**
     * STEP 4 helper — one round-trip with tools enabled, no loop.
     * Demonstrates: model returns either text OR a functionCall; we run it once.
     */
    private static String singleTurnWithTools(GeminiClient gemini,
                                              Conversation memory,
                                              ToolRegistry tools,
                                              String userInput) throws Exception {
        memory.addUser(userInput);
        GeminiClient.Reply r;
        try {
            r = gemini.chatWithTools(memory, tools.declarations());
        } catch (Exception ex) {
            memory.dropLast();
            throw ex;
        }
        if (r.toolCall != null) {
            System.out.println("🤖 llm_call: " + r.toolCall.name + "(" + r.toolCall.args + ")");
            String result = tools.execute(r.toolCall.name, r.toolCall.args);
            memory.addModelToolCall(r.toolCall.name, r.toolCall.args);
            memory.addToolResult(r.toolCall.name, result);
            return "[tool " + r.toolCall.name + " executed → " + result + "]";
        }
        memory.addModel(r.text);
        return r.text;
    }
}
