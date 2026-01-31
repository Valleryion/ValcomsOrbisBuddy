package de.valcoms.orbisbuddy;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.logger.HytaleLogger;
import de.valcoms.orbisbuddy.config.ConfigService;
import de.valcoms.orbisbuddy.persistence.JsonGolemSaveRepository;
import de.valcoms.orbisbuddy.registry.CommandRegistry;
import de.valcoms.orbisbuddy.registry.EntityRegistry;
import de.valcoms.orbisbuddy.registry.EventRegistry;
import de.valcoms.orbisbuddy.registry.ItemRegistry;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * ValcomsOrbisBuddy - A Hytale server plugin.
 *
 * @author Valcoms
 * @version 1.0.0
 */
public class ValcomsOrbisBuddyMod extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ValcomsOrbisBuddyMod instance;

    public ValcomsOrbisBuddyMod(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static ValcomsOrbisBuddyMod getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[ValcomsOrbisBuddy] Setting up...");
        var config = new ConfigService().loadOrCreate(null);
        var repo = new JsonGolemSaveRepository();

        CommandRegistry.register();
        ItemRegistry.register();
        EntityRegistry.register();
        EventRegistry.register();

        LOGGER.at(Level.INFO).log("[ValcomsOrbisBuddy] Setup complete!");
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("[ValcomsOrbisBuddy] Started!");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("[ValcomsOrbisBuddy] Shutting down...");
        instance = null;
    }
}