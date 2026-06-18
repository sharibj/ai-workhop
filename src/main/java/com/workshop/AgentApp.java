package com.workshop;

import java.util.Optional;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.workshop.models.Conversation;
import com.workshop.tools.SpeakTool;
import com.workshop.tools.ToolRegistry;
import com.workshop.utilities.GeminiClient;
import com.workshop.utilities.Speaker;

import static com.workshop.constants.Flags.USE_AGENT_LOOP;
import static com.workshop.constants.Flags.USE_MEMORY;
import static com.workshop.constants.Flags.USE_REAL_TTS;
import static com.workshop.constants.Flags.USE_SYSTEM_PROMPT;
import static com.workshop.constants.Flags.USE_TOOLS;

/**
 * AgentApp — the only entry point.
 */
public class AgentApp {
	
	public static void main(String[] args) throws Exception {
		
		// Initialise
		GeminiClient gemini = getGeminiClient();
		Conversation conversation = new Conversation();
		Speaker speaker = new Speaker(gemini, USE_REAL_TTS);
		
		// STEP 1: Say my name
		
		// STEP 2: Remember my name
		// the memory is nothing but a record of all conversations that forms context
		
		// STEP 3: Say my name... literally
		// the system prompt is how we shape the model's behavior.
		
		/*if (USE_STRUCTURED_OUTPUT) {
			conversation.setSystemPrompt(SAY_MY_NAME_SYSTEM_PROMPT);
		}*/
		
		// Tools are ALWAYS registered — they just stay unused until USE_TOOLS=true.
		ToolRegistry tools = new ToolRegistry();
		tools.register(new SpeakTool(speaker));
		
		AgentLoop agent = new AgentLoop(gemini, conversation, tools);
		
		Scanner in = new Scanner(System.in);
		System.out.println("Say-My-Name agent ready. Type 'exit' to quit.");
		
		while (true) {
			System.out.print("\nyou> ");
			if (!in.hasNextLine()) {
				break;
			}
			String userInput = in.nextLine().trim();
			if (userInput.isEmpty()) {
				continue;
			}
			if (userInput.equalsIgnoreCase("exit")) {
				break;
			}
			String reply;
			
			try {
				if (USE_AGENT_LOOP) {
					// STEP 5: the loop drives multi-turn tool use until the model is done.
					reply = agent.run(userInput);
				} else if (USE_TOOLS) {
					// STEP 4: single-shot tool call — model may emit ONE tool call,
					// we execute it, but we do NOT loop back yet.
					reply = singleTurnWithTools(gemini, conversation, tools, userInput);
				} else if (USE_MEMORY) {
					// STEP 2/3: stateful chat, optionally asking for JSON.
					conversation.addUser(userInput);
					try {
						reply = gemini.chat(conversation, USE_SYSTEM_PROMPT);
						conversation.addModel(reply);
					} catch (Exception ex) {
						conversation.dropLast();
						throw ex;
					}
					if (USE_SYSTEM_PROMPT) {
						// STEP 3: parse the JSON the model gave us, then "say" the name.
						// The model returns {name, message}:
						//   - 'message' is the conversational reply (always shown).
						//   - 'name'    fires the speaker ONLY when non-empty.
						// Students should see: the LLM only returned data; OUR Java code
						// decides what to do with each field.
						Optional<JsonNode> parsed = StructuredParser.tryParse(reply);
						if (parsed.isPresent()) {
							JsonNode p = parsed.get();
							String name = p.path("name").asText("");
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
	
	
	private static GeminiClient getGeminiClient() {
		// Get the API key
		String apiKey = System.getenv("GEMINI_API_KEY");
		if (apiKey == null || apiKey.isBlank()) {
			System.err.println("Please set GEMINI_API_KEY environment variable.");
			System.exit(1);
		}
		
		// Setup Gemini Client
		GeminiClient gemini = new GeminiClient(apiKey);
		return gemini;
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
