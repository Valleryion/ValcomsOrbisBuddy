package de.valcoms.orbisbuddy.registry;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.model.ActivationResult;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemService;

import java.util.Objects;

public class EventRegistry {
    public static void register(
            com.hypixel.hytale.event.EventRegistry events,
            GolemService golemService,
            GolemInstanceStore store
    ) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(golemService, "golemService");
        Objects.requireNonNull(store, "store");

        events.registerGlobal(PlayerInteractEvent.class, event -> {
            Entity targetEntity = event.getTargetEntity();
            if (targetEntity == null) {
                return;
            }

            String ownerId = store.findOwnerIdByEntity(targetEntity);
            if (ownerId == null) {
                return;
            }

            Player player = event.getPlayer();
            if (player == null) {
                return;
            }

            String playerId = resolvePlayerId(player);
            if (!ownerId.equals(playerId)) {
                player.sendMessage(Message.raw("Nicht dein Buddy."));
                return;
            }

            Ref<EntityStore> playerRef = event.getPlayerRef();
            ActivationResult result = golemService.tryActivateWithEnergyCore(playerRef, ownerId);
            switch (result) {
                case SUCCESS -> player.sendMessage(Message.raw("OrbisBuddy aktiviert."));
                case NEEDS_CORE -> player.sendMessage(Message.raw("Du brauchst einen EnergyCore."));
                case CONSUME_FAILED -> player.sendMessage(Message.raw("EnergyCore konnte nicht konsumiert werden."));
                case ALREADY_ACTIVE -> {
                }
            }
        });
    }

    private static String resolvePlayerId(Player player) {
        if (player.getUuid() != null) {
            return player.getUuid().toString();
        }
        return player.getDisplayName();
    }
}
