package no.elg.infiniteBootleg;

import de.tomgrill.gdxtesting.GdxTestRunner;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class TestGraphic {

    public World world;

    static {
        Main.schedulerThreads = 0;
        Main.renderGraphic = false;
        Main.loadWorldFromDisk = false;
        Main.debug = true;
        new Main(true);
    }

    //
    @Before
    public void createNewWorld() {
        world = new World(new EmptyChunkGenerator(), 0, false);
        Main.inst().setWorld(world);
    }

    @Test
    public void dummy() {
        //This dummy test is needed for @RunWith(GdxTestRunner.class) to work
    }
}
