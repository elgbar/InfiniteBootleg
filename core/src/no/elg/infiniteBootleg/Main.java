package no.elg.infiniteBootleg;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.Console;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.input.InputHandler;

import java.io.File;

import static no.elg.infiniteBootleg.ProgramArgs.executeArgs;

public class Main extends ApplicationAdapter {

    public static final String WORLD_FOLDER = "worlds" + File.separatorChar;
    public static final String VERSION_FILE = "version";

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private static InputMultiplexer inputMultiplexer;
    private Console console;

    public static boolean HEADLESS;

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

        addInputProcessor(new InputHandler());

        batch.setProjectionMatrix(camera.combined);

    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batch.begin();
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
