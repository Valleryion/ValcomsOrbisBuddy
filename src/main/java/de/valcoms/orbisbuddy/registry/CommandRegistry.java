package de.valcoms.orbisbuddy.registry;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import de.valcoms.orbisbuddy.command.DebugCommand;
import de.valcoms.orbisbuddy.command.OrbisBuddyCommand;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemRuntimeAdapter;
import de.valcoms.orbisbuddy.service.GolemService;

public class CommandRegistry {
    public static void register(
            PluginBase plugin,
            GolemService golemService,
            GolemInstanceStore store,
            GolemRuntimeAdapter runtime
    ) {
        OrbisBuddyCommand golemCommand = new OrbisBuddyCommand(golemService);
        DebugCommand debugCommand = new DebugCommand(golemService, store, runtime);

        plugin.getCommandRegistry().registerCommand(golemCommand);
        plugin.getCommandRegistry().registerCommand(debugCommand);
    }
}
