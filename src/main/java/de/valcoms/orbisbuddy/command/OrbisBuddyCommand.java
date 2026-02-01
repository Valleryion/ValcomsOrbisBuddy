package de.valcoms.orbisbuddy.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.valcoms.orbisbuddy.model.CombatMode;
import de.valcoms.orbisbuddy.model.FollowMode;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OrbisBuddyCommand extends AbstractCommand {

    private final GolemService service;
    private final GolemInstanceStore instanceStore;

    public OrbisBuddyCommand(GolemService service, GolemInstanceStore instanceStore) {
        super("golem", "OrbisBuddy controls");
        this.service = service;
        this.instanceStore = instanceStore;
        setAllowsExtraArguments(true);
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String[] args = parseArgs(context.getInputString());
        if (args.length == 0) {
            context.sendMessage(Message.raw("Usage: /golem status|follow|stay|assist|passive|activate"));
            return CompletableFuture.completedFuture(null);
        }

        if (!context.isPlayer()) {
            context.sender().sendMessage(Message.raw("Nur Spieler können /golem verwenden."));
            return CompletableFuture.completedFuture(null);
        }

        String ownerId = resolveOwnerId(context);
        Ref<EntityStore> playerRef = context.senderAsPlayerRef();
        instanceStore.setPlayerRef(ownerId, playerRef);
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status" -> {
                var data = service.loadOrCreate(ownerId);
                String message = "State=" + data.getState()
                        + ", Follow=" + data.getSettings().getFollowMode()
                        + ", Combat=" + data.getSettings().getCombatMode();
                context.sendMessage(Message.raw(message));
            }
            case "follow" -> service.setFollowMode(ownerId, FollowMode.FOLLOW);
            case "stay" -> service.setFollowMode(ownerId, FollowMode.STAY);
            case "assist" -> service.setCombatMode(ownerId, CombatMode.ASSIST);
            case "passive" -> service.setCombatMode(ownerId, CombatMode.PASSIVE);
            case "activate" -> {
                boolean initial = service.isOffline(ownerId);
                boolean activated = service.tryActivateThreadSafe(playerRef, ownerId, initial);
                if (activated) {
                    context.sender().sendMessage(Message.raw("OrbisBuddy aktiviert."));
                } else {
                    String requirement = initial ? "EnergyCore" : "Lost Core";
                    context.sender().sendMessage(Message.raw("Aktivierung fehlgeschlagen – du brauchst einen " + requirement + "."));
                }
            }
            default -> context.sendMessage(Message.raw("Usage: /golem status|follow|stay|assist|passive|activate"));
        }
        return CompletableFuture.completedFuture(null);
    }

    private String resolveOwnerId(CommandContext context) {
        UUID uuid = context.sender().getUuid();
        if (uuid != null) {
            return uuid.toString();
        }
        return context.sender().getDisplayName();
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
