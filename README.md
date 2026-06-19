# ai-workhop — Demystifying AI Agents

A 1-hour live-coding workshop. ONE codebase, FIVE stages, ONE story: **"Say My Name."**
We "wake up" the agent gradually by calling step methods in `AgentApp.java`.

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

All stage switches live in `AgentApp.processUserInput()` — comment/uncomment the
`step1()`…`step5()` call to advance through the story.

The story: tell the bot your name, then ask it to "say my name." Each step changes
*who* says it and *how*.

### STEP 1 — Stateless chatbot
Plain HTTP call per turn. No memory.
> **Demo:** "Hi, I'm Lina." → "Say my name." → bot has no idea.
> **Code to read:** `GeminiClient.complete(String)`.

### STEP 2 — Add memory
Full conversation history is sent each turn.
> **Demo:** Same prompts as before — now the bot replies *"Your name is Lina."*
> **Code to read:** the `history` list in `AgentApp`, `GeminiClient.complete(List)`.

### STEP 3 — Structured output + system prompt + Speaker
The system prompt forces the model to reply with `{"name": "...", "message": "..."}`.
Our Java code parses the JSON and decides what to do with each field:
- `message` is always shown as the conversational reply.
- `name`, when non-empty, **prompts the user (y/n) to invoke the Speaker** —
  the human is the visible "wire" between the LLM and the action.

The Speaker prints a yellow ASCII banner labeled **`👨‍💻 code-driven`**
(and, if `USE_REAL_TTS=true`, plays Gemini-synthesized audio).
> **Demo:** "Hi, I'm Lina." → casual reply, no banner.
> "Say my name." → JSON appears → press `y` → banner.
> **Code to read:** `constants/Prompts.java`, `models/NameReply.java`,
> the parse/confirm/`speaker.speak(name, Source.CODE)` block in `AgentApp.step3()`.

### STEP 4 — One-shot tools
The model sees the `speak` tool declaration and may emit a `functionCall`.
We execute it ONCE and stop — no loop yet.
The Speaker prints a green banner labeled **`🤖 LLM-driven`** —
the model itself decided to call it. **No `y/n` prompt this time.**
> **Demo:** "Hi, I'm Lina." → "Say my name." → banner appears with no human in the loop.
> Same banner, different color, different label — the disconnect is visible.
> **Code to read:** `Tool.java`, `ToolRegistry.java`, `SpeakTool.java`,
> `GeminiClient.chatWithTools()`, `AgentApp.step4()`.

### STEP 5 — Full agent loop
A `for` loop in `AgentApp.step5()` keeps round-tripping until the model returns
plain text. The model can chain tool calls.
> **Code to read:** the `for` loop in `AgentApp.step5()` — it's ~20 lines.

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
├── AgentApp.java               # Entry point + step1()…step5() methods
├── constants/
│   ├── Prompts.java            # SAY_MY_NAME_SYSTEM_PROMPT
│   └── Role.java               # "user" / "model" / "tool" string constants
├── gemini/
│   ├── GeminiClient.java       # HTTP to Gemini (complete + chatWithTools + synthesizeSpeech)
│   ├── GeminiRequest.java      # Request builder (contents, tools, systemInstruction)
│   ├── GeminiResponse.java     # Response wrapper
│   └── models/
│       ├── Content.java        # {role, parts[]} conversation turn
│       ├── Part.java           # text / functionCall / functionResponse
│       └── FunctionDeclaration.java
├── models/
│   ├── Conversation.java       # Thin list wrapper (STEP 2 memory)
│   ├── Message.java
│   └── NameReply.java          # {"name": "...", "message": "..."} (STEP 3)
├── speaker/
│   ├── Speaker.java            # ASCII banner + optional Gemini TTS, Source attribution
│   └── BannerFont.java         # Hardcoded ASCII font (A–Z, 0–9)
└── tools/
    ├── Tool.java               # Interface: name / declaration / execute
    ├── ToolRegistry.java       # Registers tools, routes execute() calls
    └── SpeakTool.java          # Exposes Speaker to the LLM (STEP 4+)
```

`gemini.log` is appended by `GeminiClient` on every request/response so you can
`tail -f` it in a split pane during the workshop.
