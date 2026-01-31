package de.valcoms.orbisbuddy.entity;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;

public class OrbisBuddyEntity extends Entity {

    public static final BuilderCodec<OrbisBuddyEntity> CODEC = BuilderCodec
            .builder(OrbisBuddyEntity.class, OrbisBuddyEntity::new)
            .build();

    public OrbisBuddyEntity() {
        super();
    }

    public OrbisBuddyEntity(World world) {
        super(world);
    }
}
