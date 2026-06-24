package com.workshop.constants;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
	USER("user"),
	MODEL("model");
	
	private final String value;
	
	
	Role(String value) {
		this.value = value;
	}
	
	
	@JsonValue
	public String value() {
		return value;
	}
}
