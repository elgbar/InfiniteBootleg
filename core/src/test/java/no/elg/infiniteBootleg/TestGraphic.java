package no.elg.infiniteBootleg;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics;
import com.badlogic.gdx.graphics.GL20;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import no.kh498.util.Reflection;
import org.junit.BeforeClass;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class TestGraphic {

    private static final Object LOCK = new Object();

    @BeforeClass
    public static void init() throws NoSuchFieldException, IllegalAccessException {
        synchronized (LOCK) {
            Main.renderGraphic = false;
            Main.loadWorldFromDisk = false; //ensure no loading from disk (to be able to duplicate tests)

            Gdx.gl20 = Mockito.mock(GL20.class);
            Gdx.gl = Gdx.gl20;

            //run postRunnable at once
            Gdx.app = Mockito.mock(Application.class);
            doAnswer(invocation -> {
                invocation.getArgument(0, Runnable.class).run();
                return null;
            }).when(Gdx.app).postRunnable(any(Runnable.class));


            Gdx.graphics = new MockGraphics();
//        Gdx.graphics = Mockito.mock(Graphics.class);
//        when(Gdx.graphics.getWidth()).thenReturn(1);
//        when(Gdx.graphics.getHeight()).thenReturn(1);

            Main.inst = new Main();

            Reflection.modifyField(Main.inst, "scheduler", new CancellableThreadScheduler(0));


            World world = new World(new EmptyChunkGenerator());
            world.getWorldTicker().pause();

            Reflection.modifyField(Main.inst, "world", world);
        }
    }
}
