package de.valcoms.orbisbuddy.service;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import de.valcoms.orbisbuddy.ecs.ProximityComponents;
import de.valcoms.orbisbuddy.ecs.ProximityTagComponent;
import de.valcoms.orbisbuddy.entity.OrbisBuddyController;
import de.valcoms.orbisbuddy.ids.OrbisBuddyIds;
import de.valcoms.orbisbuddy.model.CombatMode;
import de.valcoms.orbisbuddy.model.FollowMode;
import de.valcoms.orbisbuddy.model.GolemData;
import de.valcoms.orbisbuddy.model.GolemState;

import java.util.Objects;
import java.util.logging.Level;

public class DefaultGolemRuntimeAdapter implements GolemRuntimeAdapter {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final GolemInstanceStore instanceStore;

    public DefaultGolemRuntimeAdapter(GolemInstanceStore store) {
        this.instanceStore = Objects.requireNonNull(store, "store");
    }

    @Override
    public void applyState(String ownerId, GolemData data) {
        if (ownerId == null || ownerId.isBlank() || data == null) {
            return;
        }

        Ref<EntityStore> playerRef = instanceStore.getPlayerRef(ownerId);
        Ref<EntityStore> buddyRef = instanceStore.getEntityRef(ownerId);

        Ref<EntityStore> threadRef = (playerRef != null && playerRef.isValid()) ? playerRef : buddyRef;
        if (threadRef == null || !threadRef.isValid()) {
            return;
        }

        EntityStore entityStore = threadRef.getStore().getExternalData();
        World world = entityStore.getWorld();
        if (world != null) {
            world.execute(() -> applyStateWorldThread(ownerId, data, entityStore));
        } else {
            applyStateWorldThread(ownerId, data, entityStore);
        }
    }

    private void applyStateWorldThread(String ownerId, GolemData data, EntityStore entityStore) {
        Store<EntityStore> store = entityStore.getStore();

        String desiredRoleKey = resolveRoleKey(data);
        String currentRoleKey = instanceStore.getRoleKey(ownerId);

        boolean roleChanged = !Objects.equals(currentRoleKey, desiredRoleKey);
        Ref<EntityStore> buddyRef = instanceStore.getEntityRef(ownerId);
        OrbisBuddyController controller = instanceStore.getController(ownerId);

        if (!roleChanged && buddyRef != null && buddyRef.isValid() && controller != null) {
            controller.applyState(data);
            return;
        }

        TransformComponent spawnTransform = null;
        if (buddyRef != null && buddyRef.isValid()) {
            spawnTransform = store.getComponent(buddyRef, TransformComponent.getComponentType());
        }
        if (spawnTransform == null) {
            Ref<EntityStore> playerRef = instanceStore.getPlayerRef(ownerId);
            if (playerRef != null && playerRef.isValid()) {
                spawnTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
            }
        }
        if (spawnTransform == null) {
            LOGGER.at(Level.WARNING).log("[ValcomsOrbisBuddy] Cannot apply role - missing transform for owner=" + ownerId);
            return;
        }

        Vector3d position = spawnTransform.getPosition().clone().add(1.0, 0.0, 1.0);
        Vector3f rotation = spawnTransform.getRotation();

        Float previousHealth = null;
        if (buddyRef != null && buddyRef.isValid()) {
            previousHealth = readHealth(store, buddyRef);
            tryRemoveEntity(store, buddyRef);
        }

        Ref<EntityStore> newRef = spawnNpc(store, desiredRoleKey, position, rotation);
        if (newRef == null || !newRef.isValid()) {
            LOGGER.at(Level.WARNING).log("[ValcomsOrbisBuddy] Failed to spawn role=" + desiredRoleKey + " for owner=" + ownerId);
            return;
        }

        Entity entity = EntityUtils.getEntity(newRef, store);
        if (entity == null) {
            LOGGER.at(Level.WARNING).log("[ValcomsOrbisBuddy] Spawned ref but entity null for owner=" + ownerId + " role=" + desiredRoleKey);
            return;
        }

        OrbisBuddyController newController = new OrbisBuddyController(entity);
        instanceStore.bind(ownerId, newRef, entity, newController);
        instanceStore.setRoleKey(ownerId, desiredRoleKey);

        try {
            store.addComponent(newRef, ProximityComponents.PROXIMITY_TAG, new ProximityTagComponent(ownerId, true, 4.0f));
        } catch (Throwable ignored) {
        }

        if (previousHealth != null) {
            applyHealth(store, newRef, previousHealth);
        }

        LOGGER.atInfo().log("[ValcomsOrbisBuddy] Applied role owner=" + ownerId
                + " role=" + desiredRoleKey
                + " networkId=" + entity.getNetworkId());

        newController.applyState(data);
    }

    private String resolveRoleKey(GolemData data) {
        if (data.getState() == GolemState.OFFLINE) {
            return OrbisBuddyIds.NPCROLE_ORBISBUDDY_OFFLINE;
        }

        FollowMode follow = data.getSettings().getFollowMode();
        CombatMode combat = data.getSettings().getCombatMode();

        if (follow == FollowMode.FOLLOW) {
            return combat == CombatMode.ASSIST
                    ? OrbisBuddyIds.NPCROLE_ORBISBUDDY_FOLLOW_ASSIST
                    : OrbisBuddyIds.NPCROLE_ORBISBUDDY_FOLLOW_PASSIVE;
        }
        return combat == CombatMode.ASSIST
                ? OrbisBuddyIds.NPCROLE_ORBISBUDDY_STAY_ASSIST
                : OrbisBuddyIds.NPCROLE_ORBISBUDDY_STAY_PASSIVE;
    }

    private Ref<EntityStore> spawnNpc(Store<EntityStore> store, String roleKey, Vector3d pos, Vector3f rot) {
        for (String candidate : resolveRoleCandidates(roleKey)) {
            try {
                Object result = NPCPlugin.get().spawnNPC(store, candidate, null, pos, rot);
                if (result == null) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Ref<EntityStore> spawnedRef = (Ref<EntityStore>) result.getClass().getMethod("first").invoke(result);
                if (spawnedRef != null) {
                    return spawnedRef;
                }
            } catch (Throwable t) {
                LOGGER.at(Level.WARNING).log("[ValcomsOrbisBuddy] spawnNPC failed role=" + candidate + " error=" + t.getMessage());
            }
        }
        return null;
    }

    private String[] resolveRoleCandidates(String roleKey) {
        return switch (roleKey) {
            case OrbisBuddyIds.NPCROLE_ORBISBUDDY_FOLLOW_ASSIST -> new String[] {
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_FOLLOW_ASSIST,
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_FOLLOW_ASSIST_QUALIFIED
            };
            case OrbisBuddyIds.NPCROLE_ORBISBUDDY_FOLLOW_PASSIVE -> new String[] {
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_FOLLOW_PASSIVE,
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_FOLLOW_PASSIVE_QUALIFIED
            };
            case OrbisBuddyIds.NPCROLE_ORBISBUDDY_STAY_ASSIST -> new String[] {
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_STAY_ASSIST,
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_STAY_ASSIST_QUALIFIED
            };
            case OrbisBuddyIds.NPCROLE_ORBISBUDDY_STAY_PASSIVE -> new String[] {
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_STAY_PASSIVE,
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_STAY_PASSIVE_QUALIFIED
            };
            case OrbisBuddyIds.NPCROLE_ORBISBUDDY_OFFLINE -> new String[] {
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_OFFLINE,
                    OrbisBuddyIds.NPCROLE_ORBISBUDDY_OFFLINE_QUALIFIED
            };
            default -> new String[] { roleKey };
        };
    }

    private void tryRemoveEntity(Store<EntityStore> store, Ref<EntityStore> ref) {
        Entity entity = EntityUtils.getEntity(ref, store);
        if (entity != null) {
            try {
                entity.remove();
                return;
            } catch (Throwable ignored) {
            }
        }

        Object reason = resolveRemoveReason();
        if (reason != null) {
            try {
                store.getClass().getMethod("removeEntity", Ref.class, reason.getClass()).invoke(store, ref, reason);
                return;
            } catch (Throwable ignored) {
            }
        }

        try {
            store.getClass().getMethod("removeEntity", Ref.class).invoke(store, ref);
            return;
        } catch (Throwable ignored) {
        }
        try {
            store.getClass().getMethod("destroyEntity", Ref.class).invoke(store, ref);
            return;
        } catch (Throwable ignored) {
        }
        try {
            store.getClass().getMethod("remove", Ref.class).invoke(store, ref);
        } catch (Throwable ignored) {
        }
    }

    private Object resolveRemoveReason() {
        try {
            Class<?> reasonClass = Class.forName("com.hypixel.hytale.component.RemoveReason");
            try {
                return Enum.valueOf((Class<Enum>) reasonClass, "DELETION");
            } catch (IllegalArgumentException ignored) {
            }
            try {
                return Enum.valueOf((Class<Enum>) reasonClass, "DESPAWN");
            } catch (IllegalArgumentException ignored) {
            }
        } catch (Throwable ignored) {
        }
        return null;
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
        try {
            Object statValue = statMap.getClass().getMethod("get", int.class).invoke(statMap, healthIndex);
            if (statValue == null) {
                return null;
            }
            Object value = statValue.getClass().getMethod("get").invoke(statValue);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void applyHealth(Store<EntityStore> store, Ref<EntityStore> ref, float value) {
        Object statMap = getStatMap(store, ref);
        if (statMap == null) {
            return;
        }
        Integer healthIndex = getHealthIndex();
        if (healthIndex == null) {
            return;
        }
        try {
            statMap.getClass().getMethod("setStatValue", int.class, float.class).invoke(statMap, healthIndex, value);
        } catch (Throwable ignored) {
        }
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
}
