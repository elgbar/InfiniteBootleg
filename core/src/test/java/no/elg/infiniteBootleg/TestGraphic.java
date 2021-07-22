package no.elg.infiniteBootleg;

import de.tomgrill.gdxtesting.GdxTestRunner;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class TestGraphic {

    static {
        Settings.schedulerThreads = 0;
        Settings.renderGraphic = false;
        Settings.loadWorldFromDisk = false;
        Settings.debug = true;
        new Main(true);
    }

    public World world;

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
