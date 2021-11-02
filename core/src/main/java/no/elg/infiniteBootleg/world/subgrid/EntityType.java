package no.elg.infiniteBootleg.world.subgrid;

import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlockEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;

public enum EntityType {
  FALLING_BLOCK(FallingBlockEntity.class),
  PLAYER(Player.class);

  private final Class<? extends Entity> impl;

  EntityType(@NotNull Class<? extends Entity> impl) {
    this.impl = impl;
  }
}
