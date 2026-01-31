package de.valcoms.orbisbuddy.persistence;

import de.valcoms.orbisbuddy.model.GolemData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonGolemSaveRepository implements GolemSaveRepository {

    private final Map<String, GolemData> inMemoryStore = new ConcurrentHashMap<>();

    @Override
    public GolemData load(Object worldRef, String ownerId) {
        // TODO(engine): Persist to disk per worldRef. For now keep a session-level map.
        return inMemoryStore.get(ownerId);
    }

    @Override
    public void save(Object worldRef, String ownerId, GolemData data) {
        // TODO(engine): Serialize to JSON. For now store reference for this session.
        inMemoryStore.put(ownerId, data);
    }
}
