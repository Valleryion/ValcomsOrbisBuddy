package de.valcoms.orbisbuddy.service;

import de.valcoms.orbisbuddy.ids.OrbisBuddyIds;
import de.valcoms.orbisbuddy.model.ActivationResult;
import de.valcoms.orbisbuddy.model.CombatMode;
import de.valcoms.orbisbuddy.model.FollowMode;
import de.valcoms.orbisbuddy.model.GolemData;
import de.valcoms.orbisbuddy.model.GolemState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.persistence.GolemSaveRepository;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GolemService {

    private final Object worldRef;
    private final GolemSaveRepository repo;
    private final GolemRuntimeAdapter runtime;
    private final InventoryService inventory;
    private final boolean debugEnabled;
    private final Map<String, Integer> debugEnergyCores = new ConcurrentHashMap<>();

    public GolemService(
            Object worldRef,
            GolemSaveRepository repo,
            GolemRuntimeAdapter runtime,
            InventoryService inventory,
            boolean debugEnabled
    ) {
        this.worldRef = worldRef;
        this.repo = Objects.requireNonNull(repo);
        this.runtime = Objects.requireNonNull(runtime);
        this.inventory = Objects.requireNonNull(inventory);
        this.debugEnabled = debugEnabled;
    }

    public GolemData loadOrCreate(String ownerId) {
        var data = repo.load(worldRef, ownerId);
        if (data != null) {
            return data;
        }

        var created = new GolemData(ownerId);
        repo.save(worldRef, ownerId, created);
        return created;
    }

    public void setFollowMode(String ownerId, FollowMode mode) {
        var data = loadOrCreate(ownerId);
        data.getSettings().setFollowMode(mode);
        repo.save(worldRef, ownerId, data);
        runtime.applyState(ownerId, data);
    }

    public void setCombatMode(String ownerId, CombatMode mode) {
        var data = loadOrCreate(ownerId);
        data.getSettings().setCombatMode(mode);
        repo.save(worldRef, ownerId, data);
        runtime.applyState(ownerId, data);
    }

    public CombatMode toggleCombatMode(String ownerId) {
        var data = loadOrCreate(ownerId);
        CombatMode current = data.getSettings().getCombatMode();
        CombatMode next = current == CombatMode.ASSIST ? CombatMode.PASSIVE : CombatMode.ASSIST;
        data.getSettings().setCombatMode(next);
        repo.save(worldRef, ownerId, data);
        runtime.applyState(ownerId, data);
        return next;
    }

    public FollowMode toggleFollowMode(String ownerId) {
        var data = loadOrCreate(ownerId);
        FollowMode current = data.getSettings().getFollowMode();
        FollowMode next = current == FollowMode.FOLLOW ? FollowMode.STAY : FollowMode.FOLLOW;
        data.getSettings().setFollowMode(next);
        repo.save(worldRef, ownerId, data);
        runtime.applyState(ownerId, data);
        return next;
    }

    public CombatMode getCurrentCombatMode(String ownerId) {
        return loadOrCreate(ownerId).getSettings().getCombatMode();
    }

    public FollowMode getCurrentFollowMode(String ownerId) {
        return loadOrCreate(ownerId).getSettings().getFollowMode();
    }

    public void saveHealth(String ownerId, float health) {
        var data = loadOrCreate(ownerId);
        data.setHealth(health);
        repo.save(worldRef, ownerId, data);
    }

    public void handleDowned(String ownerId) {
        var data = loadOrCreate(ownerId);
        data.setState(GolemState.OFFLINE);
        repo.save(worldRef, ownerId, data);
        runtime.applyState(ownerId, data);
    }

    public ActivationResult tryActivateWithEnergyCore(Object playerRef, String ownerId) {
        var data = loadOrCreate(ownerId);
        if (data.getState() != GolemState.OFFLINE) {
            return ActivationResult.ALREADY_ACTIVE;
        }

        if (!inventory.hasEnergyCore(playerRef) && !hasDebugEnergyCore(ownerId)) {
            return ActivationResult.NEEDS_CORE;
        }
        boolean activated = tryActivate(playerRef, ownerId, true);
        if (!activated) {
            return ActivationResult.CONSUME_FAILED;
        }
        return ActivationResult.SUCCESS;
    }

    public boolean tryActivate(Object playerRef, String ownerId, boolean initialActivation) {
        var data = loadOrCreate(ownerId);
        if (data.getState() == GolemState.ACTIVE) {
            return true;
        }

        String required = initialActivation
                ? OrbisBuddyIds.ITEM_ENERGY_CORE
                : OrbisBuddyIds.ITEM_LOST_CORE;
        boolean hasRequired = inventory.hasItem(playerRef, required, 1)
                || (initialActivation && hasDebugEnergyCore(ownerId));
        if (!hasRequired) {
            return false;
        }
        boolean consumed;
        if (initialActivation) {
            consumed = consumeEnergyCore(playerRef, ownerId);
        } else {
            consumed = inventory.consumeItem(playerRef, required, 1);
        }
        if (!consumed) {
            return false;
        }

        data.setState(GolemState.ACTIVE);
        repo.save(worldRef, ownerId, data);
        runtime.applyState(ownerId, data);
        return true;
    }

    public boolean tryActivateThreadSafe(Object playerRef, String ownerId, boolean initialActivation) {
        if (playerRef instanceof Ref<?> ref) {
            @SuppressWarnings("unchecked")
            Ref<EntityStore> entityRef = (Ref<EntityStore>) ref;
            EntityStore entityStore = entityRef.getStore().getExternalData();
            World world = entityStore.getWorld();
            if (world != null) {
                CompletableFuture<Boolean> future = new CompletableFuture<>();
                world.execute(() -> future.complete(tryActivate(playerRef, ownerId, initialActivation)));
                try {
                    return future.get();
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return false;
                } catch (ExecutionException executionException) {
                    return false;
                }
            }
        }
        return tryActivate(playerRef, ownerId, initialActivation);
    }

    public boolean isOffline(String ownerId) {
        return loadOrCreate(ownerId).getState() == GolemState.OFFLINE;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void grantDebugEnergyCore(String ownerId, int count) {
        if (!debugEnabled) {
            return;
        }
        debugEnergyCores.merge(ownerId, count, Integer::sum);
    }

    private boolean hasDebugEnergyCore(String ownerId) {
        return debugEnabled && debugEnergyCores.getOrDefault(ownerId, 0) > 0;
    }

    private boolean consumeEnergyCore(Object playerRef, String ownerId) {
        if (inventory.consumeEnergyCore(playerRef, 1)) {
            return true;
        }
        return consumeDebugEnergyCore(ownerId, 1);
    }

    private boolean consumeDebugEnergyCore(String ownerId, int count) {
        AtomicBoolean success = new AtomicBoolean(false);
        debugEnergyCores.compute(ownerId, (key, value) -> {
            if (value == null || value < count) {
                return value;
            }
            success.set(true);
            int remaining = value - count;
            return remaining <= 0 ? null : remaining;
        });
        return success.get();
    }
}
