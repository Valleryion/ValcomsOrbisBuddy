package de.valcoms.orbisbuddy.service;

import de.valcoms.orbisbuddy.ids.OrbisBuddyIds;
import de.valcoms.orbisbuddy.model.ActivationResult;
import de.valcoms.orbisbuddy.model.CombatMode;
import de.valcoms.orbisbuddy.model.FollowMode;
import de.valcoms.orbisbuddy.model.GolemData;
import de.valcoms.orbisbuddy.model.GolemState;
import de.valcoms.orbisbuddy.persistence.GolemSaveRepository;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GolemService {

    private final Object worldRef;
    private final GolemSaveRepository repo;
    private final GolemRuntimeAdapter runtime;
    private final InventoryService inventory;
    private final Map<String, Integer> debugEnergyCores = new ConcurrentHashMap<>();

    public GolemService(
            Object worldRef,
            GolemSaveRepository repo,
            GolemRuntimeAdapter runtime,
            InventoryService inventory
    ) {
        this.worldRef = worldRef;
        this.repo = Objects.requireNonNull(repo);
        this.runtime = Objects.requireNonNull(runtime);
        this.inventory = Objects.requireNonNull(inventory);
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

    public boolean isOffline(String ownerId) {
        return loadOrCreate(ownerId).getState() == GolemState.OFFLINE;
    }

    public void grantDebugEnergyCore(String ownerId, int count) {
        debugEnergyCores.merge(ownerId, count, Integer::sum);
    }

    private boolean hasDebugEnergyCore(String ownerId) {
        return debugEnergyCores.getOrDefault(ownerId, 0) > 0;
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
