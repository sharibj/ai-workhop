package com.workshop.gemini.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionDeclaration {

    public String name;
    public String description;
    public Schema parameters;

    public FunctionDeclaration(String name, String description, Schema parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Schema {
        public String type;
        public Map<String, Property> properties;
        public List<String> required;

        public Schema(String type, Map<String, Property> properties, List<String> required) {
            this.type = type;
            this.properties = properties;
            this.required = required;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Property {
        public String type;
        public String description;

        public Property(String type, String description) {
            this.type = type;
            this.description = description;
        }
    }
}
