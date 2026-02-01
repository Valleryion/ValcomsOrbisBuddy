package de.valcoms.orbisbuddy.registry;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.ecs.ProximityComponents;
import de.valcoms.orbisbuddy.ecs.ProximityTagComponent;
import de.valcoms.orbisbuddy.entity.OrbisBuddyController;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemService;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventRegistry {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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

        LOGGER.atInfo().log("[ValcomsOrbisBuddy] Registering event listeners...");
        System.out.println("[ValcomsOrbisBuddy] Registering event listeners (stdout)...");

        events.registerGlobal(PlayerChatEvent.class, event -> {
            String rawMessage = event.getContent();
            if (!"!buddy".equalsIgnoreCase(rawMessage)) {
                return;
            }

            PlayerRef sender = event.getSender();
            if (sender == null) {
                return;
            }

            Ref<EntityStore> playerRef = sender.getReference();
            if (playerRef == null) {
                return;
            }

            EntityStore entityStore = playerRef.getStore().getExternalData();

            String ownerId = resolvePlayerId(sender);
            store.setPlayerRef(ownerId, playerRef);
            Object buddyObj = store.getEntity(ownerId);
            if (buddyObj == null) {
                buddyObj = tryRebindBuddy(entityStore, ownerId, store);
            }
            if (buddyObj == null) {
                sender.sendMessage(Message.raw("Kein Buddy gespawnt."));
                return;
            }

            if (buddyObj instanceof Entity buddyEntity) {
                System.out.println("[ValcomsOrbisBuddy] !buddy owner=" + ownerId
                        + " buddyNetworkId=" + buddyEntity.getNetworkId()
                        + " buddyRef=" + store.getEntityRef(ownerId));
            } else {
                System.out.println("[ValcomsOrbisBuddy] !buddy owner=" + ownerId
                        + " buddyType=" + buddyObj.getClass().getName()
                        + " buddyRef=" + store.getEntityRef(ownerId));
            }

            sender.sendMessage(Message.raw("Buddy erkannt. Nutze /golem activate, um ihn zu aktivieren."));
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

    private static String resolvePlayerId(Player player, Ref<EntityStore> playerRef) {
        if (playerRef != null && playerRef.isValid()) {
            EntityStore entityStore = playerRef.getStore().getExternalData();
            PlayerRef ref = entityStore.getStore().getComponent(playerRef, PlayerRef.getComponentType());
            if (ref != null && ref.getUuid() != null) {
                return ref.getUuid().toString();
            }
        }

        return player.getDisplayName();
    }

    private static String resolvePlayerId(PlayerRef playerRef) {
        if (playerRef == null) {
            return "";
        }
        if (playerRef.getUuid() != null) {
            return playerRef.getUuid().toString();
        }
        return playerRef.getUsername();
    }

    private static Object tryRebindBuddy(EntityStore entityStore, String ownerId, GolemInstanceStore store) {
        if (entityStore == null || ownerId == null || ownerId.isBlank()) {
            return null;
        }

        var query = com.hypixel.hytale.component.query.Query.and(ProximityComponents.PROXIMITY_TAG);
        World world = entityStore.getWorld();
        if (world == null) {
            return null;
        }

        try {
            entityStore.getStore().assertThread();
            return scanForBuddy(entityStore, ownerId, store, query);
        } catch (RuntimeException ignored) {
        }

        CompletableFuture<Object> future = new CompletableFuture<>();
        world.execute(() -> future.complete(scanForBuddy(entityStore, ownerId, store, query)));

        try {
            return future.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException executionException) {
            return null;
        }
    }

    private static Object scanForBuddy(EntityStore entityStore,
                                       String ownerId,
                                       GolemInstanceStore store,
                                       com.hypixel.hytale.component.query.Query<EntityStore> query) {
        final Object[] found = new Object[1];
        entityStore.getStore().forEachChunk(query, (chunk, buffer) -> {
            for (int index = 0; index < chunk.size(); index++) {
                try {
                    ProximityTagComponent tag = chunk.getComponent(index, ProximityComponents.PROXIMITY_TAG);
                    if (tag == null || !tag.isBuddy() || !ownerId.equals(tag.getOwnerId())) {
                        continue;
                    }
                    Ref<EntityStore> ref = chunk.getReferenceTo(index);
                    Entity entity = com.hypixel.hytale.server.core.entity.EntityUtils.getEntity(ref, entityStore.getStore());
                    if (entity != null) {
                        OrbisBuddyController controller = new OrbisBuddyController(entity);
                        store.bind(ownerId, ref, entity, controller);
                        found[0] = entity;
                        return;
                    }
                } catch (IndexOutOfBoundsException ignored) {
                    return;
                }
            }
        });
        return found[0];
    }
}
