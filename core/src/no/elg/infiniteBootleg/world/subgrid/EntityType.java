package no.elg.infiniteBootleg.world.subgrid;

import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlock;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;

public enum EntityType {

    FALLING_BLOCK(FallingBlock.class),
    PLAYER(Player.class);

    private Class<? extends Entity> impl;

    EntityType(@NotNull Class<? extends Entity> impl) {
        this.impl = impl;
    }
}
