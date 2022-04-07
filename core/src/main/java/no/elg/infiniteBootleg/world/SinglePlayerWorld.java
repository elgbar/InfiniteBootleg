package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

public class SinglePlayerWorld extends ClientWorld {

  public SinglePlayerWorld(
      @NotNull ChunkGenerator generator, long seed, @NotNull String worldName) {
    super(generator, seed, worldName);
  }
}
