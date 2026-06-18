# ai-workhop — Pre-flight Check

Welcome! This branch is a tiny readiness check for the **"Demystifying AI Agents — By Building One"** workshop.

Before the workshop, please **clone this branch and run it once.** It tells you whether your machine is ready, or what to fix.

## What it checks

1. ✅ Java 17+ is installed
2. ✅ Your `GEMINI_API_KEY` env var is set
3. ✅ A real call to the Gemini LLM succeeds
4. ✅ Gemini TTS audio plays through your laptop's speakers

## Steps

### 1. Get a free Gemini API key (30 seconds)

Go to https://aistudio.google.com/apikey, sign in with any Google account, click **Create API key**, copy it.

### 2. Set it in your shell

```bash
export GEMINI_API_KEY=paste-your-key-here
```

(For convenience, add the line to your `~/.zshrc` or `~/.bashrc` so it sticks across terminals.)

### 3. Clone and run

```bash
git clone -b sj/bootstrap https://github.com/sharibj/ai-workhop.git
cd ai-workhop
mvn -q exec:java
```

That's it. You should see:

```
══════════════════════════════════════════════════════
  AI Workshop — Pre-flight Check
══════════════════════════════════════════════════════

  Java 17+ ... PASS  (17.0.9)
  GEMINI_API_KEY set ... PASS  (AIzaSy…)
  LLM call works ... PASS  (model said: "ready.")
  TTS + audio playback works ... PASS  (played 47 KB through your speakers)

──────────────────────────────────────────────────────
  ✅ READY — see you at the workshop!
──────────────────────────────────────────────────────
```

You should also **hear a short voice** say "Pre-flight check passed. See you at the workshop." If you didn't hear it but the check still passed, double-check your speakers/volume.

## If something fails

The script prints exactly which step broke and a hint. Quick reference:

| Failed step | Most likely fix |
|-------------|-----------------|
| Java 17+    | Install JDK 17+: `brew install openjdk@17` (macOS) or your distro's package manager |
| GEMINI_API_KEY set | `export GEMINI_API_KEY=...` and re-run in the same terminal |
| LLM call works | Check the key is valid (no extra spaces); check your network/proxy |
| TTS + audio playback | Unmute speakers / unplug headphones / try a different audio output |

If you're stuck, ping me — better to debug today than during the workshop.

## What you DON'T need to do

You don't need to read or understand any of the Java code on this branch — it exists only to verify your setup. The actual workshop code lives on a different branch and we'll explore it together on the day.

See you on the 25th!
