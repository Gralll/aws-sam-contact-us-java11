package com.gralll.sam.service;

import java.util.HashMap;
import java.util.Map;

public class PropertyStorage {

    private final Map<String, String> properties = new HashMap<>();

    public PropertyStorage() {
        init();
    }

    private void init() {
        properties.put("SENDER_EMAIL",
                getOrDefault("SENDER_EMAIL", "someExistingEmail@gmail.com"));
        properties.put("RECIPIENT_EMAIL",
                getOrDefault("RECIPIENT_EMAIL", "someExistingRecepientEmail@gmail.com"));
    }

    private String getOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    public String getValue(String key) {
        return properties.get(key);
    }
}
