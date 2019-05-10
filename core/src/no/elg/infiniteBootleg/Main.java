package no.elg.infiniteBootleg;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.Console;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;

import java.io.File;

import static no.elg.infiniteBootleg.ProgramArgs.executeArgs;
import static no.elg.infiniteBootleg.world.World.*;

public class Main extends ApplicationAdapter {

    public static final String WORLD_FOLDER = "worlds" + File.separatorChar;
    public static final String VERSION_FILE = "version";

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private static InputMultiplexer inputMultiplexer;
    private Console console;
    private BitmapFont font;

    public static boolean HEADLESS;

    public static World world;

    public Main(String[] args) {
        executeArgs(args);
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(true);

        batch = new SpriteBatch();
        inputMultiplexer = new InputMultiplexer();
        VisUI.load();
        console = new ConsoleHandler();
        Gdx.input.setInputProcessor(inputMultiplexer);

        batch.setProjectionMatrix(camera.combined);

        world = new World(new FlatChunkGenerator());
        font = new BitmapFont(true);

    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        world.render();


        final Vector3 unproject = world.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        final int blockX = (int) (unproject.x / World.BLOCK_SIZE);
        final int blockY = (int) (unproject.y / World.BLOCK_SIZE);

        int[] vChunks = world.chunksInView();
//        int chunksInView = (colEnd - colStart) + (rowEnd - vChunks[ROW_START]);
        int chunksInView = Math.abs(vChunks[COL_END] - vChunks[COL_START]) * Math.abs(vChunks[ROW_END] - vChunks[ROW_START]);
        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, 10);
        font.draw(batch, "Pointing at block (" + blockX + ", " + blockY + ") in chunk " +
                         world.getChunkFromWorld(blockX, blockY).getLocation(), 10, 25);
        font.draw(batch, "Viewing " + chunksInView + " chunks", 10, 40);
        batch.end();

        console.draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        VisUI.dispose();
    }

    public static void addInputProcessor(InputProcessor processor) {
        inputMultiplexer.addProcessor(processor);
    }

    public static InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }
}
