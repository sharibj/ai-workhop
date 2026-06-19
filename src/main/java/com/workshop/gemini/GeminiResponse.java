package com.workshop.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.workshop.gemini.models.Content;
import com.workshop.gemini.models.Part;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

    public List<Candidate> candidates;

    public Candidate firstCandidate() {
        return (candidates != null && !candidates.isEmpty()) ? candidates.get(0) : null;
    }

    public Part firstPart() {
        Candidate c = firstCandidate();
        if (c == null || c.content == null || c.content.parts == null || c.content.parts.isEmpty()) return null;
        return c.content.parts.get(0);
    }

    public String text() {
        Part p = firstPart();
        return (p != null && p.text != null) ? p.text : "";
    }

    public Part.FunctionCall functionCall() {
        Part p = firstPart();
        return (p != null) ? p.functionCall : null;
    }

    public String finishReason() {
        Candidate c = firstCandidate();
        return (c != null && c.finishReason != null) ? c.finishReason : "?";
    }

    public String inlineData() {
        Part p = firstPart();
        return (p != null && p.inlineData != null) ? p.inlineData.data : "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        public Content content;
        public String finishReason;
    }
}
