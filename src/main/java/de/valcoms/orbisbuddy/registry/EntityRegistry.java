package de.valcoms.orbisbuddy.registry;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import de.valcoms.orbisbuddy.entity.OrbisBuddyEntity;
import de.valcoms.orbisbuddy.ids.OrbisBuddyIds;

public class EntityRegistry {
    public static void register(PluginBase plugin) {
        plugin.getEntityRegistry().registerEntity(
                OrbisBuddyIds.ENTITY_GOLEM,
                OrbisBuddyEntity.class,
                OrbisBuddyEntity::new,
                OrbisBuddyEntity.CODEC
        );
    }
}
