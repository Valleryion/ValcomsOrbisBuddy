package de.valcoms.orbisbuddy.ecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ProximityTagComponent implements Component<EntityStore> {

    public static final BuilderCodec<ProximityTagComponent> CODEC = BuilderCodec
            .builder(ProximityTagComponent.class, ProximityTagComponent::new)
            .append(new KeyedCodec<>("OwnerId", Codec.STRING), (c, v) -> c.ownerId = v, c -> c.ownerId).add()
            .append(new KeyedCodec<>("Buddy", Codec.BOOLEAN), (c, v) -> c.buddy = v, c -> c.buddy).add()
            .append(new KeyedCodec<>("Range", Codec.FLOAT), (c, v) -> c.range = v, c -> c.range).add()
            .build();

    private String ownerId;
    private boolean buddy;
    private float range;

    public ProximityTagComponent() {
    }

    public ProximityTagComponent(String ownerId, boolean buddy, float range) {
        this.ownerId = ownerId;
        this.buddy = buddy;
        this.range = range;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public boolean isBuddy() {
        return buddy;
    }

    public float getRange() {
        return range;
    }

    @Override
    public Component<EntityStore> clone() {
        return new ProximityTagComponent(ownerId, buddy, range);
    }
}
