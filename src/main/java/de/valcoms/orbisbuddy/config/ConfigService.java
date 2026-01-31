package de.valcoms.orbisbuddy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigService {
    private static final String CONFIG_FILE = "config.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path configPath;

    public OrbisBuddyConfig loadOrCreate(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException ignored) {
        }

        configPath = dataDir.resolve(CONFIG_FILE);
        if (Files.exists(configPath)) {
            return load(configPath, true);
        }

        OrbisBuddyConfig created = new OrbisBuddyConfig();
        save(configPath, created);
        return created;
    }

    public OrbisBuddyConfig reload() {
        if (configPath == null) {
            return new OrbisBuddyConfig();
        }
        return load(configPath, true);
    }

    private OrbisBuddyConfig load(Path path, boolean persistIfNormalized) {
        if (!Files.exists(path)) {
            return new OrbisBuddyConfig();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) {
                return new OrbisBuddyConfig();
            }

            OrbisBuddyConfig loaded = gson.fromJson(json, OrbisBuddyConfig.class);
            if (loaded == null) {
                return new OrbisBuddyConfig();
            }

            OrbisBuddyConfig defaults = new OrbisBuddyConfig();
            boolean changed = applyDefaults(json, loaded, defaults);
            changed = migrateIfNeeded(loaded, defaults) || changed;
            if (persistIfNormalized && changed) {
                save(path, loaded);
            }
            return loaded;
        } catch (IOException ignored) {
            return new OrbisBuddyConfig();
        }
    }

    private boolean applyDefaults(JsonObject json, OrbisBuddyConfig target, OrbisBuddyConfig defaults) {
        boolean changed = false;
        for (Field field : OrbisBuddyConfig.class.getDeclaredFields()) {
            if (json.has(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(target, field.get(defaults));
                changed = true;
            } catch (IllegalAccessException ignored) {
            }
        }
        return changed;
    }

    private boolean migrateIfNeeded(OrbisBuddyConfig target, OrbisBuddyConfig defaults) {
        if (target.configVersion >= defaults.configVersion) {
            return false;
        }

        target.debugEnabled = defaults.debugEnabled;
        target.configVersion = defaults.configVersion;
        return true;
    }

    private void save(Path configPath, OrbisBuddyConfig config) {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            gson.toJson(config, writer);
        } catch (IOException ignored) {
        }
    }
}
