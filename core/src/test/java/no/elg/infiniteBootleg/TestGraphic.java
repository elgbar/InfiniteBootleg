package no.elg.infiniteBootleg;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import de.tomgrill.gdxtesting.GdxTestRunner;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.SinglePlayerWorld;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestRunner.class)
@TestInstance(PER_CLASS)
public class TestGraphic {

  static {
    Settings.schedulerThreads = 0;
    Settings.client = false;
    Settings.loadWorldFromDisk = false;
    Settings.debug = true;
    new ClientMain(true);
  }

  public ClientWorld world;

  @NotNull
  public ClientWorld createNewWorld() {
    world = new SinglePlayerWorld(new EmptyChunkGenerator(), 0, "World");
    return world;
  }

  @Test
  public void dummy() {
    // This dummy test is needed for @RunWith(GdxTestRunner.class) to work
  }
}
