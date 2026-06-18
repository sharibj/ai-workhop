package com.workshop;

/** A single chat message. role ∈ {"user", "model", "tool"}. */
public record Message(String role, String content) { }
