package com.workshop.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NameReply {

    public String name = "";
    public String message = "";
}
