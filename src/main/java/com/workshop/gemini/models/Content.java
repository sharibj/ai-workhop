package com.workshop.gemini.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.workshop.constants.Role;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {

    public Role role;
    public List<Part> parts;

    public Content() {}

    public Content(Role role, String text) {
        this.role = role;
        this.parts = List.of(new Part(text));
    }

    public Content(Role role, List<Part> parts) {
        this.role = role;
        this.parts = parts;
    }
}
