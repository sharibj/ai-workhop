package com.workshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Pre-workshop readiness check.
 *
 * Runs four checks and prints PASS/FAIL for each:
 *   1. Java version  — JDK 17+ available
 *   2. API key       — GEMINI_API_KEY set in the environment
 *   3. LLM call      — Gemini chat endpoint reachable, returns text
 *   4. Audio + TTS   — Gemini TTS returns audio AND your speakers can play it
 *
 * Anything red on the final report → fix it before the workshop.
 */
public class Bootstrap {

    private static final String CHAT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String TTS_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent";

    private static final String GREEN  = "\033[1;32m";
    private static final String RED    = "\033[1;31m";
    private static final String YELLOW = "\033[1;33m";
    private static final String DIM    = "\033[2m";
    private static final String RESET  = "\033[0m";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static int warnings = 0;

    public static void main(String[] args) {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("  AI Workshop — Pre-flight Check");
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println();

        boolean java   = check("Java 17+",          Bootstrap::checkJava);
        boolean key    = check("GEMINI_API_KEY set", Bootstrap::checkApiKey);
        boolean llm    = key && check("LLM call works", Bootstrap::checkLlm);
        boolean audio  = llm && check("TTS + audio playback works", Bootstrap::checkTts);

        System.out.println();
        System.out.println("──────────────────────────────────────────────────────");
        if (java && key && llm && audio) {
            if (warnings > 0) {
                System.out.println(YELLOW + "  ✅ READY (with " + warnings + " warning" +
                        (warnings == 1 ? "" : "s") + ") — see above." + RESET);
                System.out.println("     Setup is fine; quota will reset / can be topped up before the workshop.");
            } else {
                System.out.println(GREEN + "  ✅ READY — see you at the workshop!" + RESET);
            }
        } else {
            System.out.println(RED + "  ❌ NEEDS SETUP — see the messages above." + RESET);
            System.out.println("     Open the README for fix instructions.");
        }
        System.out.println("──────────────────────────────────────────────────────");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────
    // Check runner
    // ─────────────────────────────────────────────────────────────────

    /** Sentinel: throw to mark a check PASS but with a warning (e.g. quota exhausted). */
    private static class QuotaWarning extends RuntimeException {
        QuotaWarning(String msg) { super(msg); }
    }

    private interface Check {
        /** Throw for FAIL. Throw QuotaWarning for PASS-with-warning. Return detail on PASS. */
        String run() throws Exception;
    }

    private static boolean check(String label, Check check) {
        System.out.print("  " + label + " ... ");
        try {
            String detail = check.run();
            System.out.println(GREEN + "PASS" + RESET +
                    (detail == null || detail.isEmpty() ? "" : "  " + DIM + detail + RESET));
            return true;
        } catch (QuotaWarning w) {
            System.out.println(YELLOW + "PASS (with warning)" + RESET);
            System.out.println("       " + YELLOW + w.getMessage() + RESET);
            warnings++;
            return true;
        } catch (Exception e) {
            System.out.println(RED + "FAIL" + RESET);
            System.out.println("       " + RED + e.getMessage() + RESET);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 1. Java
    // ─────────────────────────────────────────────────────────────────

    private static String checkJava() {
        String version = System.getProperty("java.version");
        int major = parseMajor(version);
        if (major < 17) {
            throw new RuntimeException("Java " + version + " detected. Need JDK 17 or higher.");
        }
        return "(" + version + ")";
    }

    private static int parseMajor(String v) {
        // "17.0.9" → 17, "1.8.0_xxx" → 8
        String[] parts = v.split("\\.");
        int first = Integer.parseInt(parts[0]);
        return first == 1 && parts.length > 1 ? Integer.parseInt(parts[1]) : first;
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. API key
    // ─────────────────────────────────────────────────────────────────

    private static String checkApiKey() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new RuntimeException(
                    "Environment variable GEMINI_API_KEY is not set. " +
                    "Get a free key at https://aistudio.google.com/apikey then run: " +
                    "export GEMINI_API_KEY=your-key-here");
        }
        return "(" + key.substring(0, Math.min(6, key.length())) + "…)";
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. LLM call
    // ─────────────────────────────────────────────────────────────────

    private static String checkLlm() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        ObjectNode body = JSON.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode turn = contents.addObject();
        turn.put("role", "user");
        turn.putArray("parts").addObject().put("text", "Reply with the single word: ready.");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> res;
        try {
            res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Network error talking to Gemini: " + e.getMessage());
        }

        if (res.statusCode() == 400 && res.body() != null && res.body().contains("API key not valid")) {
            throw new RuntimeException("Gemini rejected the API key as invalid. " +
                    "Double-check GEMINI_API_KEY (no extra spaces, paste the full key).");
        }
        if (res.statusCode() == 401 || res.statusCode() == 403) {
            throw new RuntimeException("Gemini rejected the API key (HTTP " + res.statusCode() +
                    "). Check that GEMINI_API_KEY is valid.");
        }
        if (res.statusCode() == 429) {
            // Quota exhausted — but auth + routing + API enablement all worked. That's
            // what this check is really verifying. Top up before the workshop.
            throw new QuotaWarning("HTTP 429 — your key works but the daily quota is " +
                    "exhausted. Setup is fine; you'll just need quota again on workshop day.");
        }
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("Gemini returned HTTP " + res.statusCode() + ": " +
                    truncate(res.body(), 200));
        }

        JsonNode root = JSON.readTree(res.body());
        String text = root.path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText("");
        if (text.isBlank()) {
            throw new RuntimeException("Gemini responded but returned no text. Body: " +
                    truncate(res.body(), 200));
        }
        return "(model said: \"" + text.trim().replaceAll("\\s+", " ") + "\")";
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. TTS + audio playback
    // ─────────────────────────────────────────────────────────────────

    private static String checkTts() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");

        ObjectNode body = JSON.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        contents.addObject().putArray("parts").addObject()
                .put("text", "Pre-flight check passed. See you at the workshop.");

        ObjectNode genCfg = body.putObject("generationConfig");
        genCfg.putArray("responseModalities").add("AUDIO");
        ObjectNode speechCfg = genCfg.putObject("speechConfig");
        speechCfg.putObject("voiceConfig")
                 .putObject("prebuiltVoiceConfig").put("voiceName", "Kore");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TTS_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 429) {
            throw new QuotaWarning("HTTP 429 — TTS quota exhausted. Setup is fine; " +
                    "audio playback couldn't be tested end-to-end this run.");
        }
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("Gemini TTS returned HTTP " + res.statusCode() + ": " +
                    truncate(res.body(), 200));
        }

        JsonNode root = JSON.readTree(res.body());
        String b64 = root.path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("inlineData").path("data").asText("");
        if (b64.isEmpty()) {
            String finishReason = root.path("candidates").path(0).path("finishReason").asText("?");
            throw new RuntimeException("Gemini TTS returned no audio (finishReason=" + finishReason + ").");
        }

        byte[] pcm = Base64.getDecoder().decode(b64);
        byte[] wav = wrapPcmAsWav(pcm, 24000, 1, 16);

        try {
            playWav(wav);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Got audio bytes from Gemini but your system couldn't play them: " + e.getMessage() +
                    ". Check that your laptop has working audio output.");
        }
        return "(played " + (pcm.length / 1024) + " KB through your speakers)";
    }

    private static void playWav(byte[] wavBytes) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavBytes));
             Clip clip = AudioSystem.getClip()) {
            CountDownLatch done = new CountDownLatch(1);
            clip.addLineListener((LineEvent ev) -> {
                if (ev.getType() == LineEvent.Type.STOP) done.countDown();
            });
            clip.open(ais);
            clip.start();
            if (!done.await(10, TimeUnit.SECONDS)) {
                clip.stop();
                throw new RuntimeException("Audio playback timed out.");
            }
        }
    }

    /** Prepend a 44-byte RIFF/WAV header to raw PCM so AudioSystem can play it. */
    private static byte[] wrapPcmAsWav(byte[] pcm, int sampleRate, int channels, int bitsPerSample) {
        int byteRate   = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize   = pcm.length;
        int chunkSize  = 36 + dataSize;
        ByteBuffer h = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(chunkSize).put("WAVE".getBytes());
        h.put("fmt ".getBytes()).putInt(16).putShort((short) 1)
         .putShort((short) channels).putInt(sampleRate)
         .putInt(byteRate).putShort((short) blockAlign)
         .putShort((short) bitsPerSample);
        h.put("data".getBytes()).putInt(dataSize).put(pcm);
        return h.array();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
