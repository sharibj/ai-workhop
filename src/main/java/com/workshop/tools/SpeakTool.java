package com.workshop.tools;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.JsonNode;
import com.workshop.gemini.models.FunctionDeclaration;
import com.workshop.speaker.Speaker;

/**
 * STEP 4 — the "speak the name" capability, exposed to the LLM as a tool.
 * <p>
 * Step 3: WE parse JSON and call speaker.speak() — we are the glue.
 * Step 4: the LLM emits functionCall:speak({name: ...}) and the registry
 * routes the call into THIS class — the glue dissolves.
 * <p>
 * STEP 5 (BROKEN_SPEAKER=true): the speaker is flaky — half of all calls
 * return "ERROR: speaker offline" instead of producing a banner. The model
 * sees this in the tool result and has to DECIDE on its own whether to retry.
 * That decision-making is what the agent loop is for.
 */
public class SpeakTool implements Tool {
	
	private static final double FAILURE_RATE = 0.7;
	
	private final Speaker speaker;
	
	
	public SpeakTool(Speaker speaker) {
		this.speaker = speaker;
	}
	
	
	@Override
	public String name() {
		return "speak";
	}
	
	
	@Override
	public String description() {
		// Note: we deliberately do NOT mention the flakiness here. We want the
		// model to discover it from the error string, not be told up front.
		return "Say a person's name out loud. Use this only when the user asks you to say their name.";
	}
	
	
	@Override
	public FunctionDeclaration declaration() {
		return new FunctionDeclaration(
				name(),
				description(),
				new FunctionDeclaration.Schema(
						"object",
						Map.of("name", new FunctionDeclaration.Property("string", "The name to say.")),
						List.of("name")
				)
		);
	}
	
	
	@Override
	public String execute(JsonNode args) {
		String name = args.path("name").asText("");
		
		if (speaker.isBroken() && ThreadLocalRandom.current().nextDouble() < FAILURE_RATE) {
			// Failure path: NO banner, NO audio. The audience sees the tool
			// call in the loop log and the error result — and then watches
			// the model decide to call again.
			return "ERROR: speaker offline. Please retry.";
		}
		
		speaker.speak(name, Speaker.Source.LLM);
		return "spoke '" + name + "'";
	}
}

