package de.valcoms.orbisbuddy;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * ValcomsOrbisBuddy - A Hytale server plugin.
 *
 * @author Valcoms
 * @version 1.0.0
 */
public class ValcomsOrbisBuddyPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ValcomsOrbisBuddyPlugin instance;

    public ValcomsOrbisBuddyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static ValcomsOrbisBuddyPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[ValcomsOrbisBuddy] Setting up...");


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