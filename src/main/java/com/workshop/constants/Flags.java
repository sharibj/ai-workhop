package com.workshop.constants;

public class Flags {
	
	/**
	 * Workshop philosophy:
	 * Everything is already wired. Capabilities are GATED by boolean flags.
	 * Flip a flag → unlock the next stage.
	 * <p>
	 * The whole workshop tells one story: "Say My Name."
	 * STEP 1: tell the bot your name. Ask "say my name." It can't — no memory.
	 * STEP 2: memory on. It remembers and replies in text.
	 * STEP 3: structured output + system prompt. The bot replies in JSON
	 * {"name": "..."} and our code feeds that into Speaker → ASCII banner.
	 * STEP 4: tools on. The LLM itself calls the speak() tool — the banner
	 * appears because the model decided to make it appear.
	 * STEP 5: agent loop (designed separately).
	 */
	// ─────────────────────────────────────────────────────────────────────
	// FEATURE FLAGS — flip these during the workshop, one at a time.
	// ─────────────────────────────────────────────────────────────────────
	
	// STEP 2: turn this on to give the bot conversation memory.
	public static final boolean USE_MEMORY = true;
	
	// STEP 3: turn this on to ask Gemini for structured JSON output.
	public static final boolean USE_SYSTEM_PROMPT = true;
	
	// STEP 4: turn this on so the model can call tools (write_file, read_file, speak).
	public static final boolean USE_TOOLS = true;
	
	// STEP 5: turn this on to run the full think→act→observe agent loop.
	public static final boolean USE_AGENT_LOOP = false;
	
	// Audio bonus: banner is ALWAYS shown; flip this to also play TTS audio.
	public static final boolean USE_REAL_TTS = true;
	
	// ─────────────────────────────────────────────────────────────────────
	
}
