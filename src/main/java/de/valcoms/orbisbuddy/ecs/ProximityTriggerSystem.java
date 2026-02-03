package de.valcoms.orbisbuddy.ecs;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.function.BiConsumer;

public class ProximityTriggerSystem extends TickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double TELEPORT_DISTANCE_THRESHOLD = 40.0;
    private static final long TELEPORT_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long HEALTH_SAVE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(2);
    private static final long PROXIMITY_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final float MIN_HEALTH_CHANGE = 0.05f;

    private final GolemInstanceStore instanceStore;
    private final GolemService golemService;
    private final Query<EntityStore> query;
    private final ConcurrentHashMap<String, Long> lastProximityLog = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastTeleport = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastHealthSave = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Float> lastPersistedHealth = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    public ProximityTriggerSystem(GolemInstanceStore instanceStore, GolemService golemService) {
        this.instanceStore = Objects.requireNonNull(instanceStore, "instanceStore");
        this.golemService = Objects.requireNonNull(golemService, "golemService");
        this.query = Query.and(TransformComponent.getComponentType(), ProximityComponents.PROXIMITY_TAG);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(float dt, int systemIndex, Store<EntityStore> store) {
        List<TaggedSnapshot> buddies = collectBuddySnapshots(store);
        for (TaggedSnapshot snapshot : buddies) {
            processSnapshot(store, snapshot);
        }
    }

    private List<TaggedSnapshot> collectBuddySnapshots(Store<EntityStore> store) {
        List<TaggedSnapshot> snapshots = new ArrayList<>();
        store.forEachChunk(query,
                (BiConsumer<ArchetypeChunk<EntityStore>, CommandBuffer<EntityStore>>) (chunk, buffer) ->
                        collectSnapshots(chunk, snapshots));
        return snapshots;
    }

    private void processSnapshot(Store<EntityStore> store, TaggedSnapshot buddy) {
        if (!buddy.tag.isBuddy()) {
            return;
        }

        String ownerId = buddy.tag.getOwnerId();
        if (ownerId == null || ownerId.isBlank()) {
            return;
        }

        Ref<EntityStore> playerRef = instanceStore.getPlayerRef(ownerId);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        TransformComponent playerTransform = safeGetTransform(store, playerRef);
        if (playerTransform == null) {
            return;
        }

        saveBuddyHealthIfNeeded(store, buddy.ref(), ownerId);

        Vector3d playerPos = playerTransform.getPosition().clone();
        double distance = buddy.position.distanceTo(playerPos);

        if (distance > TELEPORT_DISTANCE_THRESHOLD) {
            handleTeleport(store, buddy, ownerId, playerPos, distance);
            return;
        }

        if (!enabled || distance > buddy.tag.getRange()) {
            return;
        }

        handleProximityLog(ownerId, buddy, playerRef, distance);
    }

    private void handleTeleport(Store<EntityStore> store,
                                TaggedSnapshot buddy,
                                String ownerId,
                                Vector3d playerPos,
                                double distance) {
        if (!shouldTeleport(ownerId)) {
            return;
        }

        TransformComponent buddyTransform = safeGetTransform(store, buddy.ref());
        if (buddyTransform == null) {
            return;
        }

        Vector3d target = playerPos.clone().add(1.0, 0.0, 1.0);
        buddyTransform.teleportPosition(target);
        LOGGER.at(Level.INFO)
                .log(String.format(
                        "[ValcomsOrbisBuddy] Auto-teleport owner=%s buddyNetworkId=%d dist=%.2f target=%.2f/%.2f/%.2f",
                        ownerId,
                        buddy.networkId,
                        distance,
                        target.getX(),
                        target.getY(),
                        target.getZ()));
    }

    private void handleProximityLog(String ownerId,
                                    TaggedSnapshot buddy,
                                    Ref<EntityStore> playerRef,
                                    double distance) {
        if (!shouldLogProximity(ownerId)) {
            return;
        }
        LOGGER.at(Level.INFO)
                .atMostEvery(1, TimeUnit.SECONDS)
                .log("[ValcomsOrbisBuddy] Proximity owner=" + ownerId
                        + " buddyNetworkId=" + buddy.networkId
                        + " activatorRef=" + playerRef
                        + " dist=" + distance);
    }

    private boolean shouldLogProximity(String ownerId) {
        long now = System.nanoTime();
        Long last = lastProximityLog.get(ownerId);
        if (last != null && now - last < PROXIMITY_LOG_INTERVAL_NANOS) {
            return false;
        }
        lastProximityLog.put(ownerId, now);
        return true;
    }

    private boolean shouldTeleport(String ownerId) {
        long now = System.nanoTime();
        Long last = lastTeleport.get(ownerId);
        if (last != null && now - last < TELEPORT_INTERVAL_NANOS) {
            return false;
        }
        lastTeleport.put(ownerId, now);
        return true;
    }

    private void saveBuddyHealthIfNeeded(Store<EntityStore> store, Ref<EntityStore> buddyRef, String ownerId) {
        long now = System.nanoTime();
        Long lastSave = lastHealthSave.get(ownerId);
        if (lastSave != null && now - lastSave < HEALTH_SAVE_INTERVAL_NANOS) {
            return;
        }

        Float health = readHealth(store, buddyRef);
        if (health == null) {
            return;
        }

        Float previous = lastPersistedHealth.get(ownerId);
        if (previous != null && Math.abs(previous - health) < MIN_HEALTH_CHANGE) {
            return;
        }

        lastHealthSave.put(ownerId, now);
        lastPersistedHealth.put(ownerId, health);

        try {
            if (health > 0.0f) {
                golemService.saveHealth(ownerId, health);
            } else {
                golemService.handleDowned(ownerId);
            }
        } catch (Exception ex) {
            LOGGER.at(Level.WARNING)
                    .log("[ValcomsOrbisBuddy] Failed to persist buddy health for owner=" + ownerId + ": " + ex.getMessage());
        }
    }

    private void collectSnapshots(ArchetypeChunk<EntityStore> chunk, List<TaggedSnapshot> out) {
        for (int index = 0; ; index++) {
            int currentSize = chunk.size();
            if (index >= currentSize) {
                break;
            }

            TransformComponent transform = safeGetComponent(chunk, index, TransformComponent.getComponentType());
            ProximityTagComponent tag = safeGetComponent(chunk, index, ProximityComponents.PROXIMITY_TAG);
            if (transform == null || tag == null || !tag.isBuddy()) {
                continue;
            }

            Ref<EntityStore> ref = safeGetReference(chunk, index);
            if (ref == null) {
                continue;
            }

            Entity entity = safeGetEntity(chunk, index);
            int networkId = entity != null ? entity.getNetworkId() : Entity.UNASSIGNED_ID;

            out.add(new TaggedSnapshot(ref, tag, transform.getPosition().clone(), networkId));
        }
    }

    private <C extends Component<EntityStore>> C safeGetComponent(ArchetypeChunk<EntityStore> chunk,
                                                                  int index,
                                                                  ComponentType<EntityStore, C> type) {
        try {
            return chunk.getComponent(index, type);
        } catch (IndexOutOfBoundsException ex) {
            LOGGER.at(Level.FINE)
                    .log("[ValcomsOrbisBuddy] Skipping chunk entry due to concurrent mutation: " + ex.getMessage());
            return null;
        }
    }

    private Ref<EntityStore> safeGetReference(ArchetypeChunk<EntityStore> chunk, int index) {
        try {
            return chunk.getReferenceTo(index);
        } catch (IndexOutOfBoundsException ex) {
            LOGGER.at(Level.FINE)
                    .log("[ValcomsOrbisBuddy] Skipping chunk ref due to concurrent mutation: " + ex.getMessage());
            return null;
        }
    }

    private Entity safeGetEntity(ArchetypeChunk<EntityStore> chunk, int index) {
        try {
            return EntityUtils.getEntity(index, chunk);
        } catch (IndexOutOfBoundsException ex) {
            LOGGER.at(Level.FINE)
                    .log("[ValcomsOrbisBuddy] Skipping entity lookup due to concurrent mutation: " + ex.getMessage());
            return null;
        }
    }

    private TransformComponent safeGetTransform(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            return store.getComponent(ref, TransformComponent.getComponentType());
        } catch (IndexOutOfBoundsException ex) {
            LOGGER.at(Level.FINE)
                    .log("[ValcomsOrbisBuddy] Skipping transform due to concurrent mutation: " + ex.getMessage());
            return null;
        }
    }

    private Float readHealth(Store<EntityStore> store, Ref<EntityStore> ref) {
        Object statMap = getStatMap(store, ref);
        if (statMap == null) {
            return null;
        }
        Integer healthIndex = getHealthIndex();
        if (healthIndex == null) {
            return null;
        }
        return readStatValue(statMap, healthIndex);
    }

    private Float readStatValue(Object statMap, int index) {
        Object statValue = readRawStatValue(statMap, index);
        if (statValue == null) {
            return null;
        }
        if (statValue instanceof Number number) {
            return number.floatValue();
        }
        try {
            Object value = statValue.getClass().getMethod("get").invoke(statValue);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable ignored) {
        }
        try {
            Object value = statValue.getClass().getMethod("getValue").invoke(statValue);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object readRawStatValue(Object statMap, int index) {
        try {
            return statMap.getClass().getMethod("get", int.class).invoke(statMap, index);
        } catch (Throwable ignored) {
        }
        try {
            return statMap.getClass().getMethod("getStatValue", int.class).invoke(statMap, index);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object getStatMap(Store<EntityStore> store, Ref<EntityStore> ref) {
        Class<?> statMapClass = loadClass(
                "com.hypixel.hytale.server.core.modules.entity.stats.EntityStatMap",
                "com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap",
                "com.hypixel.hytale.server.core.entity.stats.EntityStatMap"
        );
        if (statMapClass == null) {
            return null;
        }
        try {
            Object componentTypeObj = statMapClass.getMethod("getComponentType").invoke(null);
            @SuppressWarnings("unchecked")
            ComponentType<EntityStore, ?> componentType = (ComponentType<EntityStore, ?>) componentTypeObj;
            return store.getComponent(ref, componentType);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Integer getHealthIndex() {
        Class<?> statTypesClass = loadClass(
                "com.hypixel.hytale.server.core.modules.entity.stats.DefaultEntityStatTypes",
                "com.hypixel.hytale.server.core.modules.entitystats.DefaultEntityStatTypes",
                "com.hypixel.hytale.server.core.entity.stats.DefaultEntityStatTypes"
        );
        if (statTypesClass == null) {
            return null;
        }
        try {
            Object value = statTypesClass.getMethod("getHealth").invoke(null);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Class<?> loadClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private record TaggedSnapshot(Ref<EntityStore> ref, ProximityTagComponent tag, Vector3d position, int networkId) {
    }
}
