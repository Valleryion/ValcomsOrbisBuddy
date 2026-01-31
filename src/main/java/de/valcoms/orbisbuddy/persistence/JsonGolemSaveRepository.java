package de.valcoms.orbisbuddy.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.valcoms.orbisbuddy.model.GolemData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonGolemSaveRepository implements GolemSaveRepository {

    private static final Type STORE_TYPE = new TypeToken<Map<String, GolemData>>() {}.getType();
    private final Map<String, GolemData> inMemoryStore = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataDir;
    private final Path dataFile;

    public JsonGolemSaveRepository(Path dataDir) {
        this.dataDir = dataDir;
        this.dataFile = SavePaths.golemDataFile(dataDir);
        try {
            Files.createDirectories(dataDir);
        } catch (IOException ignored) {
        }
        loadAll();
    }

    @Override
    public GolemData load(Object worldRef, String ownerId) {
        return inMemoryStore.get(ownerId);
    }

    @Override
    public synchronized void save(Object worldRef, String ownerId, GolemData data) {
        inMemoryStore.put(ownerId, data);
        persistAll();
    }

    private synchronized void loadAll() {
        if (!Files.exists(dataFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Map<String, GolemData> loaded = gson.fromJson(reader, STORE_TYPE);
            if (loaded != null) {
                inMemoryStore.clear();
                inMemoryStore.putAll(loaded);
            }
        } catch (IOException ignored) {
        }
    }

    private synchronized void persistAll() {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException ignored) {
        }

        try (Writer writer = Files.newBufferedWriter(dataFile)) {
            gson.toJson(inMemoryStore, STORE_TYPE, writer);
        } catch (IOException ignored) {
        }
    }
}
