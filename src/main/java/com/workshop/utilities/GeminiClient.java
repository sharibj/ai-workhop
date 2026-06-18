package com.workshop.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workshop.models.Conversation;
import com.workshop.models.Message;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Talks to Gemini over plain HTTP. Intentionally NO abstractions — students
 * should be able to read this top-to-bottom and see exactly what goes on the wire.
 *
 *   Endpoint: POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=...
 */
public class GeminiClient {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final String apiKey;

    public GeminiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /** Reply container — either plain text OR a tool call. (STEP 4+) */
    public static class Reply {
        public final String text;
        public final ToolCall toolCall;
        public Reply(String text, ToolCall toolCall) {
            this.text = text;
            this.toolCall = toolCall;
        }
    }

    public static class ToolCall {
        public final String name;
        public final JsonNode args;
        public ToolCall(String name, JsonNode args) {
            this.name = name;
            this.args = args;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 1 — single-shot, stateless completion.
    // ─────────────────────────────────────────────────────────────────────
    public String complete(String prompt) throws Exception {
        ObjectNode body = json.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode turn = contents.addObject();
        turn.put("role", "user");
        turn.putArray("parts").addObject().put("text", prompt);

        JsonNode response = post(body);
        return extractText(response);
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 2/3 — multi-turn chat. Optionally asks for JSON output.
    //   If conv has a system prompt set, it's attached as `systemInstruction`.
    // ─────────────────────────────────────────────────────────────────────
    public String chat(Conversation conv, boolean structured) throws Exception {
        ObjectNode body = json.createObjectNode();
        attachSystemPrompt(body, conv.systemPrompt());
        body.set("contents", buildContents(conv.messages()));

        if (structured) {
            // Tell Gemini to emit JSON — the parser on our side will read it.
            ObjectNode generationConfig = body.putObject("generationConfig");
            generationConfig.put("responseMimeType", "application/json");
        }

        JsonNode response = post(body);
        return extractText(response);
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 4/5 — chat with tool declarations. Model may return a functionCall.
    // ─────────────────────────────────────────────────────────────────────
    public Reply chatWithTools(Conversation conv, JsonNode toolDeclarations) throws Exception {
        ObjectNode body = json.createObjectNode();
        attachSystemPrompt(body, conv.systemPrompt());
        body.set("contents", buildContents(conv.messages()));
        // Attach tools so Gemini knows what it may call.
        ArrayNode toolsArr = body.putArray("tools");
        ObjectNode toolsObj = toolsArr.addObject();
        toolsObj.set("functionDeclarations", toolDeclarations);

        JsonNode response = post(body);

        // Look at the first candidate's first part: text OR functionCall.
        JsonNode part = response
                .path("candidates").path(0)
                .path("content").path("parts").path(0);

        if (part.has("functionCall")) {
            JsonNode fc = part.get("functionCall");
            String name = fc.get("name").asText();
            JsonNode args = fc.path("args");
            return new Reply(null, new ToolCall(name, args));
        }
        return new Reply(part.path("text").asText(""), null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 3+ (optional) — turn text into spoken audio via Gemini TTS.
    //   Returns a WAV byte stream (24kHz, 16-bit, mono) ready for playback.
    // ─────────────────────────────────────────────────────────────────────
    public byte[] synthesizeSpeech(String text) throws Exception {
        String ttsUrl =
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                "gemini-2.5-flash-preview-tts:generateContent";

        ObjectNode body = json.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        contents.addObject().putArray("parts").addObject().put("text", text);

        ObjectNode genCfg = body.putObject("generationConfig");
        genCfg.putArray("responseModalities").add("AUDIO");
        // Pick a voice — Gemini exposes named prebuilt voices.
        ObjectNode speechCfg = genCfg.putObject("speechConfig");
        ObjectNode voiceCfg = speechCfg.putObject("voiceConfig");
        voiceCfg.putObject("prebuiltVoiceConfig").put("voiceName", "Kore");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ttsUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("Gemini TTS error " + res.statusCode() + ": " + res.body());
        }
        JsonNode root = json.readTree(res.body());
        String b64 = root.path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("inlineData").path("data").asText("");
        if (b64.isEmpty()) {
            String finishReason = root.path("candidates").path(0).path("finishReason").asText("?");
            throw new RuntimeException(
                "Gemini TTS returned no audio (finishReason=" + finishReason + "). " +
                "Common cause: the input text was too short or got filtered. " +
                "Try a fuller utterance.");
        }
        byte[] pcm = java.util.Base64.getDecoder().decode(b64);
        return wrapPcmAsWav(pcm, 24000, 1, 16);
    }

    /** Prepend a 44-byte RIFF/WAV header to raw PCM so AudioSystem can play it. */
    private byte[] wrapPcmAsWav(byte[] pcm, int sampleRate, int channels, int bitsPerSample) {
        int byteRate   = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize   = pcm.length;
        int chunkSize  = 36 + dataSize;
        java.nio.ByteBuffer h = java.nio.ByteBuffer.allocate(44 + dataSize)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(chunkSize).put("WAVE".getBytes());
        h.put("fmt ".getBytes()).putInt(16).putShort((short) 1)        // PCM
         .putShort((short) channels).putInt(sampleRate)
         .putInt(byteRate).putShort((short) blockAlign)
         .putShort((short) bitsPerSample);
        h.put("data".getBytes()).putInt(dataSize).put(pcm);
        return h.array();
    }

    private void attachSystemPrompt(ObjectNode body, String prompt) {
        if (prompt == null || prompt.isBlank()) return;
        ObjectNode sys = body.putObject("systemInstruction");
        sys.putArray("parts").addObject().put("text", prompt);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private ArrayNode buildContents(List<Message> history) {
        ArrayNode contents = json.createArrayNode();
        for (Message m : history) {
            ObjectNode turn = contents.addObject();
            // Gemini accepts roles "user" and "model"; tool results are sent as
            // a "user" turn carrying a functionResponse part (see addToolResult below).
            String role = m.role().equals("tool") ? "user" : m.role();
            turn.put("role", role);
            ArrayNode parts = turn.putArray("parts");

            if (m.role().equals("tool")) {
                // content is encoded as: <toolName>|||<resultText>
                String[] split = m.content().split("\\|\\|\\|", 2);
                ObjectNode fr = parts.addObject().putObject("functionResponse");
                fr.put("name", split[0]);
                ObjectNode resp = fr.putObject("response");
                resp.put("result", split.length > 1 ? split[1] : "");
            } else if (m.content().startsWith("__TOOLCALL__")) {
                // Encoded model-side tool call: __TOOLCALL__<name>|||<argsJson>
                String payload = m.content().substring("__TOOLCALL__".length());
                String[] split = payload.split("\\|\\|\\|", 2);
                ObjectNode fc = parts.addObject().putObject("functionCall");
                fc.put("name", split[0]);
                try {
                    fc.set("args", json.readTree(split.length > 1 ? split[1] : "{}"));
                } catch (Exception e) {
                    fc.putObject("args");
                }
            } else {
                parts.addObject().put("text", m.content());
            }
        }
        return contents;
    }

    private JsonNode post(ObjectNode body) throws Exception {
        appendLog("→ REQUEST\n" + body.toPrettyString() + "\n");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            appendLog("← ERROR " + res.statusCode() + "\n" + res.body() + "\n");
            throw new RuntimeException("Gemini error " + res.statusCode() + ": " + res.body());
        }
        appendLog("← RESPONSE\n" + res.body() + "\n");
        return json.readTree(res.body());
    }

    /** Append to gemini.log so traffic can be tailed in a split terminal. */
    private static void appendLog(String entry) {
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("gemini.log"),
                    entry + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    private String extractText(JsonNode response) {
        return response
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText("");
    }
}
