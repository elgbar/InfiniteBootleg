package no.elg.infiniteBootleg.world.blocks;

import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.blocks.traits.TickingTrait;
import no.elg.infiniteBootleg.world.blocks.traits.TickingTraitHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a block that implements the {@link Ticking} interface.
 *
 * <p>This implementation is set up to automatically call {@link #tick()} and {@link #tickRare()} if
 * {@link #shouldTick()} is {@code true}. To change this behaviour override {@link #shouldTick()}
 *
 * <p>For each chunk has a list of blocks that extends this class and will call {@link
 * #tryTick(boolean)} every tick.
 *
 * @see Chunk
 * @see TntBlock TntBlock (for change of ticking behaviour)
 */
public abstract class TickingBlock extends Block implements TickingTrait {

  private final TickingTraitHandler tickingTrait;

  protected TickingBlock(
      @NotNull World world, Chunk chunk, int localX, int localY, @NotNull Material material) {
    super(world, chunk, localX, localY, material);
    tickingTrait = new TickingTraitHandler(this, world.getWorldTicker());
  }

  public final void tryTick(boolean rare) {
    tickingTrait.tryTick(rare);
  }

  @Override
  public boolean shouldTick() {
    return tickingTrait.getShouldTick();
  }

  @Override
  public void setShouldTick(boolean b) {
    tickingTrait.setShouldTick(b);
  }

  @Override
  public void delayedShouldTick(long delayTicks) {
    tickingTrait.delayedShouldTick(delayTicks);
  }
}
