package de.valcoms.orbisbuddy.ecs;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ProximityComponents {

    private ProximityComponents() {
    }

    public static ComponentType<EntityStore, ProximityTagComponent> PROXIMITY_TAG;
}
