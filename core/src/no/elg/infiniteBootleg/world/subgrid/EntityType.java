package no.elg.infiniteBootleg.world.subgrid;

import org.jetbrains.annotations.NotNull;

public enum EntityType {

    FALLING_BLOCK(FallingBlock.class),
    ;

    private Class<? extends Entity> impl;

    EntityType(@NotNull Class<? extends Entity> impl) {
        this.impl = impl;
    }
}
