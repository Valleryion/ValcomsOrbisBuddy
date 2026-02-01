package de.valcoms.orbisbuddy;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.logger.HytaleLogger;
import de.valcoms.orbisbuddy.config.ConfigService;
import de.valcoms.orbisbuddy.ecs.ProximityComponents;
import de.valcoms.orbisbuddy.ecs.ProximityTagComponent;
import de.valcoms.orbisbuddy.ecs.ProximityTriggerSystem;
import de.valcoms.orbisbuddy.persistence.JsonGolemSaveRepository;
import de.valcoms.orbisbuddy.registry.CommandRegistry;
import de.valcoms.orbisbuddy.registry.EntityRegistry;
import de.valcoms.orbisbuddy.registry.EventRegistry;
import de.valcoms.orbisbuddy.registry.ItemRegistry;

import de.valcoms.orbisbuddy.service.DefaultGolemRuntimeAdapter;
import de.valcoms.orbisbuddy.service.GolemInstanceStore;
import de.valcoms.orbisbuddy.service.GolemService;
import de.valcoms.orbisbuddy.service.InventoryService;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        LOGGER.at(Level.INFO).log("[ValcomsOrbisBuddy] Plugin loaded");
        LOGGER.at(Level.INFO).log("[ValcomsOrbisBuddy] Setting up...");
        Object worldRef = null;
        Path dataDir = Paths.get("plugins", "ValcomsOrbisBuddy");

        var config = new ConfigService().loadOrCreate(dataDir);
        var repo = new JsonGolemSaveRepository(dataDir);
        var store = new GolemInstanceStore();
        var inventory = new InventoryService();
        var runtime = new DefaultGolemRuntimeAdapter(store);
        var golemService = new GolemService(worldRef, repo, runtime, inventory, config.debugEnabled);

        ProximityComponents.PROXIMITY_TAG = getEntityStoreRegistry().registerComponent(
                ProximityTagComponent.class,
                "ProximityTag",
                ProximityTagComponent.CODEC
        );
        var proximitySystem = new ProximityTriggerSystem(store);

        proximitySystem.setEnabled(false);
        getEntityStoreRegistry().registerSystem(proximitySystem);
        ItemRegistry.register();
        EntityRegistry.register(this);
        CommandRegistry.register(this, golemService, store, runtime, proximitySystem);

        LOGGER.at(Level.INFO).log("[ValcomsOrbisBuddy] Registered commands: golem, bdebug");

        EventRegistry.register(getEventRegistry(), golemService, store);

        // TODO(engine): expose services if needed for other engine hooks
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