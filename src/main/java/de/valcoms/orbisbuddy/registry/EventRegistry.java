package de.valcoms.orbisbuddy.registry;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.model.ActivationResult;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventRegistry {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    public static void register(
            com.hypixel.hytale.event.EventRegistry events,
            GolemService golemService,
            GolemInstanceStore store
    ) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(golemService, "golemService");
        Objects.requireNonNull(store, "store");

        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

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
            EntityStore entityStore = playerRef.getStore().getExternalData();
            World world = entityStore.getWorld();
            Runnable activation = () -> {
                ActivationResult result = golemService.tryActivateWithEnergyCore(playerRef, ownerId);
                switch (result) {
                    case SUCCESS -> player.sendMessage(Message.raw("OrbisBuddy aktiviert."));
                    case NEEDS_CORE -> player.sendMessage(Message.raw("Du brauchst einen EnergyCore."));
                    case CONSUME_FAILED -> player.sendMessage(Message.raw("EnergyCore konnte nicht konsumiert werden."));
                    case ALREADY_ACTIVE -> player.sendMessage(Message.raw("OrbisBuddy ist bereits aktiv."));
                }
            };
            if (world != null) {
                world.execute(activation);
            } else {
                activation.run();
            }
        });

        events.registerGlobal(PlayerDisconnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            if (playerRef != null && playerRef.getUuid() != null) {
                store.removeByOwner(playerRef.getUuid().toString());
            }
        });

        events.registerGlobal(EntityRemoveEvent.class, event -> {
            Entity removed = event.getEntity();
            store.removeByEntity(removed);
        });
    }

    private static String resolvePlayerId(Player player) {
        if (player.getUuid() != null) {
            return player.getUuid().toString();
        }
        return player.getDisplayName();
    }
}
