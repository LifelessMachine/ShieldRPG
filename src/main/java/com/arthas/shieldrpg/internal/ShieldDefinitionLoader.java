package com.arthas.shieldrpg.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.api.ShieldDescriptor;
import com.arthas.shieldrpg.api.ShieldSkillParams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModFileInfo;

public final class ShieldDefinitionLoader {

    private static final String JAR_MARKER = "/shieldrpg/shields/";

    private ShieldDefinitionLoader() {}

    public static List<ShieldDefinition> loadAll() {
        Map<String, ShieldDefinition> byId = new LinkedHashMap<>();
        Map<String, String> defaults = new LinkedHashMap<>();
        int fromJars = 0;

        try {
            for (IModFileInfo info : ModList.get().getModFiles()) {
                var contents = info.getFile().getContents();
                String owner = info.getFile().getFileName();
                contents.visitContent((relativePath, resource) -> {
                    String path = "/" + relativePath.replace('\\', '/');
                    if (!path.endsWith(".json") || !path.contains(JAR_MARKER)) {
                        return;
                    }
                    try (Reader reader = resource.bufferedReader(StandardCharsets.UTF_8)) {
                        String text = readAll(reader);
                        ShieldDefinition def = parse(JsonParser.parseString(text).getAsJsonObject());
                        if (add(byId, def, owner + path)) {
                            Identifier id = Identifier.parse(def.itemId());
                            defaults.put(id.getNamespace() + "/" + id.getPath() + ".json", text);
                        }
                    } catch (Exception e) {
                        ShieldRPG.LOGGER.error("Skipping shield definition {} in {}: {}", path, owner, e.toString());
                    }
                });
            }
        } catch (Throwable t) {
            ShieldRPG.LOGGER.error("Could not scan mod files for shield definitions", t);
        }
        fromJars = byId.size();

        Path rootDir = FMLPaths.CONFIGDIR.get().resolve("shieldrpg");
        Path configDir = rootDir.resolve("shields");
        try {
            Files.createDirectories(configDir);
            Path readme = configDir.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            ShieldRPG.LOGGER.warn("Could not create {}: {}", configDir, e.toString());
        }
        writeDefaults(rootDir.resolve("defaults"), defaults);
        if (Files.isDirectory(configDir)) {
            try (Stream<Path> files = Files.walk(configDir)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        ShieldDefinition def = parse(JsonParser.parseReader(reader).getAsJsonObject());
                        if (byId.put(def.itemId(), def) != null) {
                            ShieldRPG.LOGGER.info("Config definition {} overrides {}", file.getFileName(), def.itemId());
                        }
                    } catch (Exception e) {
                        ShieldRPG.LOGGER.error("Skipping shield definition {}: {}", file, e.toString());
                    }
                }
            } catch (IOException e) {
                ShieldRPG.LOGGER.error("Could not read {}", configDir, e);
            }
        }

        if (fromJars == 0) {
            ShieldRPG.LOGGER.error("No shield definitions were found in any mod jar. ShieldRPG's own shields are missing; "
                    + "check that data/shieldrpg/shieldrpg/shields/*.json is inside the jar.");
        }
        ShieldRPG.LOGGER.info("Loaded {} shield definition(s)", byId.size());
        return new ArrayList<>(byId.values());
    }

    private static boolean add(Map<String, ShieldDefinition> byId, ShieldDefinition def, String source) {
        if (byId.containsKey(def.itemId())) {
            ShieldRPG.LOGGER.error("Duplicate shield id {} in {}; keeping the first one", def.itemId(), source);
            return false;
        }
        byId.put(def.itemId(), def);
        return true;
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder out = new StringBuilder();
        char[] buffer = new char[4096];
        int n;
        while ((n = reader.read(buffer)) != -1) {
            out.append(buffer, 0, n);
        }
        return out.toString();
    }

    private static void writeDefaults(Path dir, Map<String, String> defaults) {
        if (defaults.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(dir);
            try (Stream<Path> old = Files.walk(dir)) {
                for (Path file : old.filter(f -> f.toString().endsWith(".json")).toList()) {
                    String key = dir.relativize(file).toString().replace('\\', '/');
                    if (!defaults.containsKey(key)) {
                        Files.deleteIfExists(file);
                    }
                }
            }
            for (Map.Entry<String, String> e : defaults.entrySet()) {
                Path file = dir.resolve(e.getKey());
                if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).equals(e.getValue())) {
                    continue;
                }
                Files.createDirectories(file.getParent());
                Files.writeString(file, e.getValue(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            ShieldRPG.LOGGER.warn("Could not write default shield files to {}: {}", dir, e.toString());
        }
    }

    private static final String README = String.join(System.lineSeparator(),
            "ShieldRPG shield overrides",
            "",
            "Any .json file in this folder replaces the shield with the same item_id.",
            "",
            "How to change a shield:",
            "1. Find it in ../defaults/<namespace>/ (for example defaults/shieldrpg/wooden_buckler.json).",
            "2. Copy the file into this folder.",
            "3. Edit the copy. Keep item_id the same.",
            "4. Restart the game.",
            "",
            "The defaults folder is rewritten every time the game starts. Editing files there does nothing.",
            "On a server, every player needs the same files in this folder as the server.",
            "Delete a file here to go back to the mod's own values.",
            "");

    static ShieldDefinition parse(JsonObject json) {
        String itemId = requireString(json, "item_id");
        Identifier id = Identifier.parse(itemId);
        String path = id.getPath();

        String name = string(json, "name", titleCase(path));
        int from = 0xFFFFFF;
        int to = 0xFFFFFF;
        if (json.has("name_gradient")) {
            JsonArray gradient = json.getAsJsonArray("name_gradient");
            from = color(gradient.get(0).getAsString());
            to = gradient.size() > 1 ? color(gradient.get(1).getAsString()) : from;
        }

        String tier = string(json, "tier", "common").toLowerCase(Locale.ROOT);
        String loreKey = string(json, "lore_key", "tooltip." + id.getNamespace() + "." + path + ".lore.1");

        JsonObject guard = obj(json, "guard");
        JsonObject perfect = obj(json, "perfect_guard");

        Map<ShieldDescriptor.Stat, Double> stats = new EnumMap<>(ShieldDescriptor.Stat.class);
        JsonObject statsJson = obj(json, "stats");
        for (Map.Entry<String, JsonElement> e : statsJson.entrySet()) {
            try {
                stats.put(ShieldDescriptor.Stat.valueOf(e.getKey().toUpperCase(Locale.ROOT)), e.getValue().getAsDouble());
            } catch (IllegalArgumentException unknown) {
                ShieldRPG.LOGGER.warn("Shield {}: unknown stat '{}' ignored", itemId, e.getKey());
            }
        }

        List<ShieldDefinition.Skill> skills = new ArrayList<>();
        if (json.has("skills")) {
            for (JsonElement element : json.getAsJsonArray("skills")) {
                skills.add(parseSkill(itemId, element.getAsJsonObject()));
            }
        }

        return new ShieldDefinition(
                itemId, json.has("existing_item") && json.get("existing_item").getAsBoolean(), name, from, to, tier, loreKey,
                (int) clamp(number(json, "durability", 300), 1, 100000),
                clamp(number(guard, "max_stability", 100), 1, 10000),
                clamp(number(guard, "regen_per_second", 20), 0.1, 1000),
                clamp(number(guard, "hold_drain_per_second", 4), 0, 1000),
                clamp(number(guard, "damage_reduction_percent", 50), 0, 100),
                guard.has("projectile_damage_reduction_percent") && !guard.get("projectile_damage_reduction_percent").isJsonNull()
                        ? clamp(guard.get("projectile_damage_reduction_percent").getAsDouble(), 0, 100) : -1,
                clamp(number(perfect, "knockback", 0), 0, 5),
                perfect.has("window_ticks") && !perfect.get("window_ticks").isJsonNull()
                        ? (int) clamp(perfect.get("window_ticks").getAsDouble(), 0, 40) : -1,
                stats, skills,
                !json.has("show_skill_cooldown") || json.get("show_skill_cooldown").isJsonNull() || json.get("show_skill_cooldown").getAsBoolean(),
                clamp(number(guard, "raised_move_speed", 0), -0.9, 0.5));
    }

    private static ShieldDefinition.Skill parseSkill(String itemId, JsonObject json) {
        String trigger = requireString(json, "trigger");

        List<ShieldDefinition.Step> steps = new ArrayList<>();
        if (json.has("steps")) {
            for (JsonElement element : json.getAsJsonArray("steps")) {
                steps.add(parseStep(itemId, element.getAsJsonObject()));
            }
        } else {
            steps.add(parseStep(itemId, json));
        }
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("skill has an empty 'steps' list");
        }

        ShieldDefinition.SoundFx sound = null;
        if (json.has("sound") && json.get("sound").isJsonObject()) {
            JsonObject s = json.getAsJsonObject("sound");
            sound = new ShieldDefinition.SoundFx(requireString(s, "id"),
                    (float) clamp(number(s, "volume", 1), 0, 10), (float) clamp(number(s, "pitch", 1), 0.1, 2));
        }

        return new ShieldDefinition.Skill(trigger, (int) clamp(number(json, "cooldown_ticks", 0), 0, 1_000_000),
                clamp(number(json, "threshold", 0.25), 0.01, 0.99), steps, sound,
                json.has("text_key") ? json.get("text_key").getAsString() : null);
    }

    private static ShieldDefinition.Step parseStep(String itemId, JsonObject json) {
        String action = requireString(json, "action");
        String target = string(json, "target", "attacker");
        if (!target.equals("attacker") && !target.equals("attacker_melee") && !target.equals("self") && !target.equals("nearby")) {
            ShieldRPG.LOGGER.warn("Shield {}: skill target '{}' is not attacker/attacker_melee/self/nearby; using 'attacker'", itemId, target);
            target = "attacker";
        }

        Map<String, Object> params = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : obj(json, "params").entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
                var primitive = e.getValue().getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    params.put(e.getKey(), primitive.getAsDouble());
                } else if (primitive.isBoolean()) {
                    params.put(e.getKey(), primitive.getAsBoolean());
                } else {
                    params.put(e.getKey(), primitive.getAsString());
                }
            }
        }

        ShieldDefinition.ParticleFx particle = null;
        if (json.has("particle") && json.get("particle").isJsonObject()) {
            JsonObject p = json.getAsJsonObject("particle");
            particle = new ShieldDefinition.ParticleFx(requireString(p, "type"),
                    (int) clamp(number(p, "count", 1), 1, 200), clamp(number(p, "spread", 0), 0, 5), clamp(number(p, "speed", 0), 0, 5));
        }

        return new ShieldDefinition.Step(action, target, clamp(number(json, "radius", 4), 0.5, 32),
                clamp(number(json, "max_range", 0), 0, 512), new ShieldSkillParams(params), particle);
    }

    private static JsonObject obj(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static String requireString(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("missing required field '" + key + "'");
        }
        return json.get(key).getAsString();
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : fallback;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int color(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        return Integer.parseInt(hex, 16) & 0xFFFFFF;
    }

    private static String titleCase(String path) {
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_/]")) {
            if (!word.isEmpty()) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return out.toString();
    }
}
