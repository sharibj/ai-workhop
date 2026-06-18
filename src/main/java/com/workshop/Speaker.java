package com.workshop;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.ByteArrayInputStream;

/**
 * The "speaker" of our agent — turns a name into output the human can perceive.
 *
 *   Always: print an ASCII banner to the terminal.
 *   If USE_REAL_TTS: also call Gemini TTS and play the audio through the
 *                    JDK's built-in javax.sound.sampled.
 *
 * Each speak() call is attributed to a Source so students can SEE who decided
 * to call the speaker:
 *   STEP 3 → Source.CODE (yellow): our Java code parsed JSON and called speak.
 *   STEP 4 → Source.LLM  (green):  the LLM itself emitted a tool call.
 */
public class Speaker {

    public enum Source {
        CODE("👨‍💻", "code-driven", "[1;33m"),  // yellow
        LLM ("🤖",                     "LLM-driven",  "[1;32m"); // green

        final String emoji;
        final String label;
        final String color;
        Source(String e, String l, String c) { emoji = e; label = l; color = c; }
    }

    private static final String RESET = "[0m";

    private final GeminiClient gemini;
    private final boolean useRealTts;

    public Speaker(GeminiClient gemini, boolean useRealTts) {
        this.gemini = gemini;
        this.useRealTts = useRealTts;
    }

    public void speak(String name, Source source) {
        printBanner(name, source);
        if (useRealTts) {
            try {
                String utterance = "You are " + name + ".";
                byte[] wav = gemini.synthesizeSpeech(utterance);
                playWav(wav);
            } catch (Exception e) {
                System.out.println("[tts failed: " + e.getMessage() + "]");
            }
        }
    }

    private void printBanner(String name, Source source) {
        String c = source.color;
        System.out.println();
        System.out.println(c + "🔊 ──── " + source.emoji + "  " + source.label + " ────────────────" + RESET);
        for (String line : BannerFont.render(name).split("\n")) {
            System.out.println(c + "   " + line + RESET);
        }
        System.out.println(c + "─────────────────────────────────────────" + RESET);
    }

    private void playWav(byte[] wavBytes) throws Exception {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavBytes))) {
            Clip clip = AudioSystem.getClip();
            Object lock = new Object();
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP) {
                    synchronized (lock) { lock.notifyAll(); }
                }
            });
            clip.open(in);
            clip.start();
            synchronized (lock) { lock.wait(); }
            clip.close();
        }
    }
}
