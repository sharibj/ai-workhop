package com.workshop.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.workshop.gemini.models.FunctionDeclaration;

public interface Tool {
    String name();
    String description();

    /** Declaration this tool exposes to the model. */
    FunctionDeclaration declaration();

    /** Execute the tool. Return a short, human/model-readable result. */
    String execute(JsonNode args) throws Exception;
}
