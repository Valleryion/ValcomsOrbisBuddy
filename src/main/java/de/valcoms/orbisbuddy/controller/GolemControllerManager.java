package de.valcoms.orbisbuddy.controller;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.ids.OrbisBuddyIds;
import de.valcoms.orbisbuddy.model.CombatMode;
import de.valcoms.orbisbuddy.model.FollowMode;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltungsklasse für beide GolemController-ECS-Systeme.
 * Wird vom Plugin erstellt und die Systeme an den EntityStoreRegistry
 * weitergeleitet.
 */
public class GolemControllerManager {

    private static final long LEFT_CLICK_COOLDOWN_MS = 500;

    private final GolemService golemService;
    private final GolemInstanceStore instanceStore;
    private final Map<UUID, Long> leftClickThrottle = new HashMap<>();

    public GolemControllerManager(GolemService golemService, GolemInstanceStore instanceStore) {
        this.golemService = golemService;
        this.instanceStore = instanceStore;
    }

    public EntityEventSystem<EntityStore, DamageBlockEvent> createDamageBlockSystem() {
        return new DamageBlockSystem();
    }

    public void clearThrottle(UUID playerId) {
        if (playerId != null) {
            leftClickThrottle.remove(playerId);
        }
    }

    class DamageBlockSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

        public DamageBlockSystem() {
            super(DamageBlockEvent.class);
        }

        @Override
        public void handle(
                int index,
                @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer,
                @Nonnull DamageBlockEvent event
        ) {
            ItemStack itemInHand = event.getItemInHand();
            String itemId = itemInHand != null ? itemInHand.getItemId() : "<null>";
            System.out.println("[GolemController] DamageBlockEvent received, item=" + itemId);
            if (itemInHand == null || !isControllerItem(itemInHand.getItemId())) {
                return;
            }

            System.out.println("[GolemController] DamageBlockEvent triggered: item=" + itemInHand.getItemId());

            PlayerRef playerRef = resolvePlayerRef(index, archetypeChunk, store);
            if (playerRef == null) {
                return;
            }

            UUID playerId = playerRef.getUuid();
            String ownerId = resolveOwnerId(playerRef);
            if (playerId == null || ownerId == null) {
                return;
            }

            long now = System.currentTimeMillis();
            Long lastTrigger = leftClickThrottle.get(playerId);
            if (lastTrigger != null && (now - lastTrigger) < LEFT_CLICK_COOLDOWN_MS) {
                event.setCancelled(true);
                return;
            }

            leftClickThrottle.put(playerId, now);
            event.setCancelled(true);

            Ref<EntityStore> playerEntityRef = resolvePlayerEntityRef(index, archetypeChunk);
            if (playerEntityRef != null) {
                instanceStore.setPlayerRef(ownerId, playerEntityRef);
            }

            String itemIdLower = itemInHand.getItemId();
            if (isCombatControllerItem(itemIdLower)) {
                CombatMode newMode = golemService.toggleCombatMode(ownerId);
                System.out.println("[GolemController] Combat mode toggled to " + newMode + " for owner=" + ownerId);
                String statusText = buildStatusMessage(ownerId, newMode.name());
                playerRef.sendMessage(Message.raw(statusText));
            } else if (isFollowControllerItem(itemIdLower)) {
                FollowMode newMode = golemService.toggleFollowMode(ownerId);
                System.out.println("[GolemController] Follow mode toggled to " + newMode + " for owner=" + ownerId);
                String statusText = buildStatusMessage(ownerId, newMode.name());
                playerRef.sendMessage(Message.raw(statusText));
            }
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }
    }

    private PlayerRef resolvePlayerRef(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store) {
        Player player = chunk.getComponent(index, Player.getComponentType());
        if (player == null) {
            return null;
        }
        Ref<EntityStore> playerRef = resolvePlayerEntityRef(index, chunk);
        if (playerRef == null) {
            return null;
        }
        return store.getComponent(playerRef, PlayerRef.getComponentType());
    }

    private PlayerRef resolvePlayerRef(Player player) {
        try {
            Object ref = player.getClass().getMethod("getPlayerRef").invoke(player);
            if (ref instanceof PlayerRef) {
                return (PlayerRef) ref;
            }
        } catch (Throwable ignored) {
        }

        try {
            Object ref = player.getClass().getMethod("getRef").invoke(player);
            if (ref instanceof PlayerRef) {
                return (PlayerRef) ref;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private Ref<EntityStore> resolvePlayerEntityRef(int index, ArchetypeChunk<EntityStore> chunk) {
        try {
            return chunk.getReferenceTo(index);
        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    private String resolveOwnerId(PlayerRef playerRef) {
        if (playerRef.getUuid() != null) {
            return playerRef.getUuid().toString();
        }
        return playerRef.getUsername();
    }

    private String buildStatusMessage(String ownerId, String changedModeName) {
        CombatMode currentCombat = golemService.getCurrentCombatMode(ownerId);
        FollowMode currentFollow = golemService.getCurrentFollowMode(ownerId);

        return "Buddy [" + ownerId.substring(0, Math.min(8, ownerId.length())) + "...]: "
                + currentCombat.name() + " | " + currentFollow.name()
                + " (" + changedModeName + ")";
    }

    private boolean isControllerItem(String itemId) {
        if (itemId == null) {
            return false;
        }
        return isCombatControllerItem(itemId) || isFollowControllerItem(itemId);
    }

    private boolean isCombatControllerItem(String itemId) {
        if (OrbisBuddyIds.ITEM_GOLEM_CONTROLLER.equalsIgnoreCase(itemId)) {
            return true;
        }
        return "Golem_Controller".equalsIgnoreCase(itemId) || "golem_controller".equalsIgnoreCase(itemId);
    }

    private boolean isFollowControllerItem(String itemId) {
        if (OrbisBuddyIds.ITEM_GOLEM_FOLLOW_CONTROLLER.equalsIgnoreCase(itemId)) {
            return true;
        }
        return "Golem_Follow_Controller".equalsIgnoreCase(itemId)
                || "golem_follow_controller".equalsIgnoreCase(itemId);
    }
}
