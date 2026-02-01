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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.function.BiConsumer;

public class ProximityTriggerSystem extends TickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final GolemInstanceStore instanceStore;
    private final Query<EntityStore> query;
    private final ConcurrentHashMap<String, Long> lastAttemptNanosByOwner = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    public ProximityTriggerSystem(GolemInstanceStore instanceStore) {
        this.instanceStore = Objects.requireNonNull(instanceStore, "instanceStore");
        this.query = Query.and(TransformComponent.getComponentType(), ProximityComponents.PROXIMITY_TAG);
        this.enabled = false;
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
        if (!enabled) {
            return;
        }

        List<TaggedSnapshot> snapshots = new ArrayList<>();

        store.forEachChunk(query,
                (BiConsumer<ArchetypeChunk<EntityStore>, CommandBuffer<EntityStore>>) (chunk, buffer) ->
                        collectSnapshots(chunk, snapshots));

        for (TaggedSnapshot buddy : snapshots) {
            if (!buddy.tag.isBuddy()) {
                continue;
            }
            String ownerId = buddy.tag.getOwnerId();
            if (ownerId == null || ownerId.isBlank()) {
                continue;
            }

            Ref<EntityStore> playerRef = instanceStore.getPlayerRef(ownerId);
            if (playerRef == null || !playerRef.isValid()) {
                continue;
            }

            TransformComponent playerTransform = safeGetTransform(store, playerRef);
            if (playerTransform == null) {
                continue;
            }

            double dist = buddy.pos.distanceTo(playerTransform.getPosition());
            if (dist > buddy.tag.getRange()) {
                continue;
            }

            if (!shouldAttemptActivation(ownerId)) {
                continue;
            }

            LOGGER.at(Level.INFO)
                    .atMostEvery(1, TimeUnit.SECONDS)
                    .log("[ValcomsOrbisBuddy] Proximity owner=" + ownerId
                            + " buddyNetworkId=" + buddy.networkId
                            + " activatorRef=" + playerRef
                            + " dist=" + dist);
        }
    }

    private boolean shouldAttemptActivation(String ownerId) {
        long now = System.nanoTime();
        long minDelta = TimeUnit.SECONDS.toNanos(1);
        Long last = lastAttemptNanosByOwner.get(ownerId);
        if (last != null && now - last < minDelta) {
            return false;
        }
        lastAttemptNanosByOwner.put(ownerId, now);
        return true;
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
                    .log("[ValcomsOrbisBuddy] Skipping player transform due to concurrent mutation: " + ex.getMessage());
            return null;
        }
    }

    private record TaggedSnapshot(Ref<EntityStore> ref, ProximityTagComponent tag, Vector3d pos, int networkId) {
    }
}
