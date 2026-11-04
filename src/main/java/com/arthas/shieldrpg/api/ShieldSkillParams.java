package com.arthas.shieldrpg.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShieldSkillParams {

    public static final ShieldSkillParams EMPTY = new ShieldSkillParams(Map.of());

    private final Map<String, Object> values;

    public ShieldSkillParams(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Map<String, Object> values() {
        return values;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public double getDouble(String key, double defaultValue) {
        return values.get(key) instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        return values.get(key) instanceof Number number ? number.intValue() : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        return values.get(key) instanceof String text ? text : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return values.get(key) instanceof Boolean flag ? flag : defaultValue;
    }
}
