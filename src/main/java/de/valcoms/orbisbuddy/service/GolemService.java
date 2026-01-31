package de.valcoms.orbisbuddy.service;

import de.valcoms.orbisbuddy.ids.OrbisBuddyIds;
import de.valcoms.orbisbuddy.model.CombatMode;
import de.valcoms.orbisbuddy.model.FollowMode;
import de.valcoms.orbisbuddy.model.GolemData;
import de.valcoms.orbisbuddy.model.GolemState;
import de.valcoms.orbisbuddy.persistence.GolemSaveRepository;

public class GolemService {
    private final Object worldRef;
    public final GolemSaveRepository repo;
    public final GolemRuntimeAdapter runtime;
    public final InventoryService inventory;

    public GolemService(
            Object worldRef,
            GolemSaveRepository repo,
            GolemRuntimeAdapter runtime,
            InventoryService inventory
    ) {
        this.worldRef = worldRef;
        this.repo = repo;
        this.runtime = runtime;
        this.inventory = inventory;
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

    public boolean tryActivate(Object playerRef, String ownerId, boolean initialActivation) {
        var data = loadOrCreate(ownerId);

        String required = initialActivation ? OrbisBuddyIds.ITEM_ENERGY_CORE : OrbisBuddyIds.ITEM_LOST_CORE;

        if (!inventory.hasItem(playerRef, required, 1)) {
            return false;
        }
        if (!inventory.consumeItem(playerRef, required, 1)) {
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

}
