package no.elg.infiniteBootleg;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.Console;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.input.InputHandler;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;

import java.io.File;

import static no.elg.infiniteBootleg.ProgramArgs.executeArgs;

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
        addInputProcessor(new InputHandler(world.getCamera()));
        font = new BitmapFont(true);

    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        world.render();

        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, 10);
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
}
