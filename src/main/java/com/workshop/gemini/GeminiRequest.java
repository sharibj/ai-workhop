package com.workshop.gemini;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.workshop.constants.Role;
import com.workshop.gemini.models.Content;
import com.workshop.gemini.models.FunctionDeclaration;
import com.workshop.gemini.models.Part;
import com.workshop.tools.ToolRegistry;

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
	
}
