package com.arthas.shieldrpg.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShieldSkillText {

    private ShieldSkillText() {}

    public static List<Object> descriptionArgs(ShieldDefinition.Skill skill) {
        List<Object> args = new ArrayList<>();
        args.add(number(skill.cooldownTicks() / 20.0));
        for (ShieldDefinition.Step step : skill.steps()) {
            for (Map.Entry<String, Object> entry : step.params().values().entrySet()) {
                if (entry.getValue() instanceof Number value) {
                    double v = value.doubleValue();
                    args.add(number(entry.getKey().endsWith("_ticks") ? v / 20.0 : v));
                }
            }
        }
        return args;
    }

    public static String number(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
