package de.valcoms.orbisbuddy.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import de.valcoms.orbisbuddy.entity.OrbisBuddyController;
import de.valcoms.orbisbuddy.entity.OrbisBuddyEntity;
import de.valcoms.orbisbuddy.model.GolemData;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemRuntimeAdapter;
import de.valcoms.orbisbuddy.service.GolemService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DebugCommand extends AbstractCommand {

    private final GolemService golemService;
    private final GolemInstanceStore store;
    private final GolemRuntimeAdapter runtime;

    public DebugCommand(
            GolemService golemService,
            GolemInstanceStore store,
            GolemRuntimeAdapter runtime
    ) {
        super("obdebug", "OrbisBuddy debug commands");
        this.golemService = golemService;
        this.store = store;
        this.runtime = runtime;
        setAllowsExtraArguments(true);
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String[] args = parseArgs(context.getInputString());
        if (args.length == 0) {
            context.sendMessage(Message.raw("Usage: /obdebug summon | summonbuddy"));
            return CompletableFuture.completedFuture(null);
        }

        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Only players can use /obdebug summon"));
            return CompletableFuture.completedFuture(null);
        }

        String sub = args[0].toLowerCase();
        if ("givecore".equals(sub)) {
            String ownerId = resolveOwnerId(context);
            golemService.grantDebugEnergyCore(ownerId, 1);
            context.sendMessage(Message.raw("Debug EnergyCore gewährt. Nächste Aktivierung verbraucht ihn."));
            return CompletableFuture.completedFuture(null);
        }

        if ("summon".equals(sub)) {
            context.sendMessage(Message.raw("Spawning Bear_Grizzly (vanilla NPC)..."));
            Ref<EntityStore> playerRef = context.senderAsPlayerRef();
            String ownerId = resolveOwnerId(context);
            SpawnedNpc spawned = spawnNpcNearPlayer(playerRef, "Bear_Grizzly");
            if (spawned == null) {
                context.sendMessage(Message.raw("Failed to spawn Bear_Grizzly."));
            } else {
                Entity entity = spawned.entity();
                OrbisBuddyController controller = new OrbisBuddyController(entity);
                store.setEntity(ownerId, entity);
                store.setController(ownerId, controller);

                GolemData data = golemService.loadOrCreate(ownerId);
                runtime.applyState(ownerId, data);
                context.sendMessage(Message.raw("Spawned Bear_Grizzly."));
            }
            return CompletableFuture.completedFuture(null);
        }

        if ("summonbuddy".equals(sub)) {
            context.sendMessage(Message.raw("Spawning OrbisBuddy (custom entity)..."));
            Ref<EntityStore> playerRef = context.senderAsPlayerRef();
            String ownerId = resolveOwnerId(context);
            OrbisBuddyEntity entity = spawnBuddyNearPlayer(playerRef);
            if (entity == null) {
                context.sendMessage(Message.raw("Failed to spawn OrbisBuddy."));
                return CompletableFuture.completedFuture(null);
            }

            OrbisBuddyController controller = new OrbisBuddyController(entity);
            store.setEntity(ownerId, entity);
            store.setController(ownerId, controller);

            GolemData data = golemService.loadOrCreate(ownerId);
            runtime.applyState(ownerId, data);
            context.sendMessage(Message.raw("Spawned. State=" + data.getState()));
            return CompletableFuture.completedFuture(null);
        }

        context.sendMessage(Message.raw("Usage: /obdebug summon | summonbuddy"));
        return CompletableFuture.completedFuture(null);
    }

    private String resolveOwnerId(CommandContext context) {
        UUID uuid = context.sender().getUuid();
        if (uuid != null) {
            return uuid.toString();
        }
        return context.sender().getDisplayName();
    }

    private SpawnedNpc spawnNpcNearPlayer(Ref<EntityStore> playerRef, String npcType) {
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }

        EntityStore entityStore = playerRef.getStore().getExternalData();
        World world = entityStore.getWorld();
        CompletableFuture<SpawnedNpc> future = new CompletableFuture<>();

        world.execute(() -> {
            try {
                TransformComponent transform = entityStore.getStore().getComponent(playerRef, TransformComponent.getComponentType());
                if (transform == null) {
                    future.complete(null);
                    return;
                }

                Vector3d position = transform.getPosition().clone().add(1.0, 0.0, 1.0);
                Vector3f rotation = transform.getRotation();

                // Spawn a vanilla NPC by its asset key (e.g. "Bear_Grizzly").
                // We intentionally avoid depending on the Pair/INonPlayerCharacter types at compile-time.
                Object result = NPCPlugin.get().spawnNPC(entityStore.getStore(), npcType, null, position, rotation);
                if (result == null) {
                    future.complete(null);
                    return;
                }

                @SuppressWarnings("unchecked")
                Ref<EntityStore> spawnedRef = (Ref<EntityStore>) result.getClass().getMethod("first").invoke(result);
                Entity entity = EntityUtils.getEntity(spawnedRef, entityStore.getStore());
                future.complete(new SpawnedNpc(spawnedRef, entity));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        try {
            return future.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException executionException) {
            return null;
        }
    }

    private record SpawnedNpc(Ref<EntityStore> ref, Entity entity) {}

    private OrbisBuddyEntity spawnBuddyNearPlayer(Ref<EntityStore> playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }

        EntityStore entityStore = playerRef.getStore().getExternalData();
        World world = entityStore.getWorld();
        CompletableFuture<OrbisBuddyEntity> future = new CompletableFuture<>();

        world.execute(() -> {
            try {
                TransformComponent transform = entityStore.getStore().getComponent(playerRef, TransformComponent.getComponentType());
                if (transform == null) {
                    future.complete(null);
                    return;
                }

                Vector3d position = transform.getPosition().clone().add(1.0, 0.0, 1.0);
                Vector3f rotation = transform.getRotation();
                OrbisBuddyEntity spawned = world.spawnEntity(new OrbisBuddyEntity(world), position, rotation);
                future.complete(spawned);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        try {
            return future.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException executionException) {
            return null;
        }
    }

    private String[] parseArgs(String input) {
        if (input == null || input.isBlank()) {
            return new String[0];
        }
        String[] parts = input.trim().split("\\s+");
        if (parts.length <= 1) {
            return new String[0];
        }
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        return args;
    }
}
