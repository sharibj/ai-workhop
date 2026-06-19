package com.workshop.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.workshop.gemini.models.Content;
import com.workshop.gemini.models.FunctionDeclaration;
import com.workshop.gemini.models.Part;
import com.workshop.constants.Role;
import com.workshop.models.Message;
import com.workshop.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiRequest {

    public List<Content> contents;
    public SystemInstruction systemInstruction;
    public GenerationConfig generationConfig;
    public List<Tools> tools;

    public void setTools(ToolRegistry registry) {
        this.tools = List.of(new Tools(registry.declarations()));
    }

    public GeminiRequest(String prompt) {
        this.contents = List.of(new Content(Role.USER, prompt));
    }

    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
    }

    // ── request-only nested classes ───────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SystemInstruction {
        public List<Part> parts;

        public SystemInstruction(String text) {
            this.parts = List.of(new Part(text));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerationConfig {
        public String responseMimeType;
        public List<String> responseModalities;
        public SpeechConfig speechConfig;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpeechConfig {
        public VoiceConfig voiceConfig;

        public SpeechConfig(String voiceName) {
            this.voiceConfig = new VoiceConfig(voiceName);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VoiceConfig {
        public PrebuiltVoiceConfig prebuiltVoiceConfig;

        public VoiceConfig(String voiceName) {
            this.prebuiltVoiceConfig = new PrebuiltVoiceConfig(voiceName);
        }
    }

    public static class PrebuiltVoiceConfig {
        public String voiceName;

        public PrebuiltVoiceConfig(String voiceName) {
            this.voiceName = voiceName;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tools {
        public List<FunctionDeclaration> functionDeclarations;

        public Tools(List<FunctionDeclaration> functionDeclarations) {
            this.functionDeclarations = functionDeclarations;
        }
    }

    // ── builder helper ────────────────────────────────────────────────────

    public static List<Content> contentsFromMessages(List<Message> history) {
        List<Content> contents = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Message m : history) {
            Role messageRole = Role.valueOf(m.role().toUpperCase());
            Role wireRole = messageRole == Role.TOOL ? Role.USER : messageRole;
            List<Part> parts = new ArrayList<>();

            if (messageRole == Role.TOOL) {
                String[] split = m.content().split("\\|\\|\\|", 2);
                parts.add(Part.ofFunctionResponse(split[0], split.length > 1 ? split[1] : ""));
            } else if (m.content().startsWith("__TOOLCALL__")) {
                String payload = m.content().substring("__TOOLCALL__".length());
                String[] split = payload.split("\\|\\|\\|", 2);
                try {
                    JsonNode args = mapper.readTree(split.length > 1 ? split[1] : "{}");
                    parts.add(Part.ofFunctionCall(split[0], args));
                } catch (Exception e) {
                    parts.add(Part.ofFunctionCall(split[0], NullNode.getInstance()));
                }
            } else {
                parts.add(new Part(m.content()));
            }
            contents.add(new Content(wireRole, parts));
        }
        return contents;
    }
}
