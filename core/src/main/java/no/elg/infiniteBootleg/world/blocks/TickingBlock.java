package no.elg.infiniteBootleg.world.blocks;

import java.util.concurrent.ThreadLocalRandom;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.blocks.traits.TickingTrait;
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

  private volatile long minimumTick;
  private volatile boolean shouldTick;
  private final Object tickLock = new Object();

  public TickingBlock(
      @NotNull World<?> world, Chunk chunk, int localX, int localY, @NotNull Material material) {
    super(world, chunk, localX, localY, material);
    synchronized (tickLock) {
      shouldTick = true;
      // spread ticking over multiple ticks to ease strain on ticker
      minimumTick =
          getWorld().getTick()
              + ThreadLocalRandom.current().nextInt((int) getWorld().getWorldTicker().getTPS() / 3);
    }
  }

  /**
   * Update if the update flag is set to true
   *
   * @param rare If the the rare update should be called instead of the normal update
   */
  public final void tryTick(boolean rare) {
    synchronized (tickLock) {
      // should not tick right away to not spawn multiple entities when spawning f.ex sand
      if (!shouldTick() || minimumTick > getWorld().getTick()) {
        return;
      }
      setShouldTick(false);
    }
    if (rare) {
      tickRare();
    } else {
      tick();
    }
  }

  @Override
  public boolean shouldTick() {
    return shouldTick;
  }

  @Override
  public void setShouldTick(boolean shouldTick) {
    synchronized (tickLock) {
      if (this.shouldTick != shouldTick) {
        this.shouldTick = shouldTick;
        minimumTick = getWorld().getTick();
      }
    }
  }

  @Override
  public void delayedShouldTick(long delayTicks) {
    synchronized (tickLock) {
      setShouldTick(true);
      minimumTick += delayTicks;
    }
  }
}
