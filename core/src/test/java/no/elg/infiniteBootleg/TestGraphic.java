package no.elg.infiniteBootleg;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import de.tomgrill.gdxtesting.GdxTestRunner;
import no.elg.infiniteBootleg.world.World;
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
        new Main(true);
    }

    public World world;

    @NotNull
    public World createNewWorld() {
        world = new World(new EmptyChunkGenerator(), 0);
        Main.inst().setWorld(world);
        return world;
    }

    @Test
    public void dummy() {
        //This dummy test is needed for @RunWith(GdxTestRunner.class) to work
    }
}
