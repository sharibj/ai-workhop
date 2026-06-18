# ai-workhop — Demystifying AI Agents

A 1-hour live-coding workshop. ONE codebase, FIVE stages, ONE story: **"Say My Name."**
We "wake up" the agent gradually by flipping flags in `AgentApp.java`.

## Setup

```bash
export GEMINI_API_KEY=...your key...
mvn -q compile
mvn -q exec:java
```

In a second terminal pane (recommended for the demo):

```bash
tail -f gemini.log    # watch every request/response Gemini sees
```

## Workshop Progression Guide

All stage switches live at the top of `src/main/java/com/workshop/AgentApp.java`:

```java
static final boolean USE_MEMORY            = false;
static final boolean USE_STRUCTURED_OUTPUT = false;
static final boolean USE_TOOLS             = false;
static final boolean USE_AGENT_LOOP        = false;
static final boolean USE_REAL_TTS          = false;  // banner is always shown; flip for audio
```

The story: tell the bot your name, then ask it to "say my name." Each step changes
*who* says it and *how*.

### STEP 1 — Stateless chatbot (all flags false)
Plain HTTP call per turn. No memory.
> **Demo:** "Hi, I'm Lina." → "Say my name." → bot has no idea.
> **Code to read:** `GeminiClient.complete()`.

### STEP 2 — Add memory
Set `USE_MEMORY = true`.
Full conversation history is sent each turn.
> **Demo:** Same prompts as before — now the bot replies *"Your name is Lina."*
> **Code to read:** `Conversation.java`, `GeminiClient.chat()`.

### STEP 3 — Structured output + system prompt + Speaker
Set `USE_STRUCTURED_OUTPUT = true`.
The system prompt forces the model to reply with `{"name": "...", "message": "..."}`.
Our Java code parses the JSON and decides what to do with each field:
- `message` is always shown as the conversational reply.
- `name`, when non-empty, **prompts the user (y/n) to invoke the Speaker** —
  the human is the visible "wire" between the LLM and the action.

The Speaker prints a yellow ASCII banner labeled **`👨‍💻 code-driven`**
(and, if `USE_REAL_TTS=true`, plays Gemini-synthesized audio).
> **Demo:** "Hi, I'm Lina." → casual reply, no banner.
> "Say my name." → JSON appears → press `y` → banner.
> **Code to read:** the `SAY_MY_NAME_SYSTEM_PROMPT` constant, `StructuredParser`,
> the parse/confirm/`speaker.speak(name, Source.CODE)` block in `AgentApp`.

### STEP 4 — One-shot tools
Set `USE_TOOLS = true`.
Now the model sees `write_file`, `read_file`, **and `speak`** declarations
and may emit a `functionCall`. We execute it ONCE and stop — no loop yet.
The Speaker now prints a green banner labeled **`🤖 LLM-driven`** —
the model itself decided to call it. **No `y/n` prompt this time.**
> **Demo:** "Hi, I'm Lina." → "Say my name." → banner appears with no human in the loop.
> Same banner, different color, different label — the disconnect is visible.
> **Code to read:** `Tool.java`, `ToolRegistry.java`, `SpeakTool.java`,
> `GeminiClient.chatWithTools()`, `AgentApp.singleTurnWithTools()`.

### STEP 5 — Full agent loop
Set `USE_AGENT_LOOP = true`.
`AgentLoop.run()` keeps round-tripping until the model returns plain text.
The model can chain calls. (Concrete demo prompt designed separately.)
> **Code to read:** `AgentLoop.java` — the entire while-loop is ~15 lines.

## The "who decided to speak" lesson

The Speaker is called from two places, with different attribution:

| Step | Who calls `speaker.speak()` | Banner color | Header | User pressed `y/n`? |
|------|-----------------------------|--------------|--------|---------------------|
| 3    | `AgentApp` after parsing JSON | yellow | `👨‍💻 code-driven` | yes |
| 4    | `SpeakTool.execute()` from a `functionCall` | green | `🤖 LLM-driven` | no |

Same banner. Same audio (if enabled). The diff between steps 3 and 4 is **who
decided the speaker should fire** — that is what tool-calling actually buys you.

## What students should walk away with

A mental model of an agent as **LLM + memory + tools + a loop**.
Function calling, JSON mode, multi-step planning — all small additions to a
tiny chat program. There is no magic, only a `while` loop.

## File map

```
src/main/java/com/workshop/
├── AgentApp.java           # Entry + flags + STEP 3 parse/confirm logic
├── GeminiClient.java       # HTTP to Gemini (chat + chatWithTools + synthesizeSpeech)
├── Conversation.java       # Memory + system prompt
├── Message.java
├── StructuredParser.java   # JSON extraction (STEP 3)
├── Speaker.java            # Banner + optional Gemini TTS, with Source attribution
├── BannerFont.java         # Hardcoded ASCII font (A–Z, 0–9)
├── AgentLoop.java          # think→act→observe (STEP 5)
└── tools/
    ├── Tool.java
    ├── ToolRegistry.java
    ├── WriteFileTool.java
    ├── ReadFileTool.java
    └── SpeakTool.java      # Exposes the Speaker to the LLM (STEP 4)
```

`gemini.log` is appended by `GeminiClient` on every request/response so you can
`tail -f` it in a split pane during the workshop.
