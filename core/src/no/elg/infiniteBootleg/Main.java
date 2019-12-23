package no.elg.infiniteBootleg;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.render.HUDRenderer;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

public class Main extends ApplicationAdapter {

    public static final String EXTERNAL_FOLDER = ".infiniteBootleg" + File.separatorChar;
    public static final String WORLD_FOLDER = EXTERNAL_FOLDER + "worlds" + File.separatorChar;

    public static final String TEXTURES_FOLDER = "textures" + File.separatorChar;
    public static final String FONTS_FOLDER = "fonts" + File.separatorChar;

    public static final String TEXTURES_BLOCK_FILE = TEXTURES_FOLDER + "blocks.atlas";
    public static final String TEXTURES_ENTITY_FILE = TEXTURES_FOLDER + "entities.atlas";
    public static final String VERSION_FILE = "version";

    private static InputMultiplexer inputMultiplexer;
    private TextureAtlas blockAtlas;
    private TextureAtlas entityAtlas;
    private CancellableThreadScheduler scheduler;

    /**
     * If worlds should be loaded from disk
     */
    public static boolean loadWorldFromDisk = true;

    /**
     * If graphics should be rendered
     */
    public static boolean renderGraphic = true;

    /**
     * Seed of the world loaded
     */
    public static int worldSeed = 0;

    /**
     * If general debug variable. Use this and-ed with your specific debug variable
     */
    public static boolean debug = false;

    public static int schedulerThreads = 3;

    private World world;
    private ConsoleHandler console;
    private HUDRenderer hud;

    public static Main inst;

    private int mouseBlockX;
    private int mouseBlockY;
    private float mouseX;
    private float mouseY;

    public Main() {
        inst = this;
        console = new ConsoleHandler(false);
    }

    @Override
    public void create() {

        scheduler = new CancellableThreadScheduler(schedulerThreads);

        inputMultiplexer = new InputMultiplexer();
        Gdx.input.setInputProcessor(inputMultiplexer);

        if (renderGraphic) {
            if (Toolkit.getDefaultToolkit().getScreenSize().width > 1920) {
                VisUI.load(VisUI.SkinScale.X2);
            }
            else {
                VisUI.load(VisUI.SkinScale.X1);
            }
        }

        console = new ConsoleHandler();
        console.setAlpha(0.85f);
        console.log(LogLevel.SUCCESS, "Version #" + Util.getVersion());

        console.log("Controls:\n" + //
                    "  WASD to control the camera\n" + //
                    "  arrow-keys to control the player\n" +//
                    "  T to teleport player to current mouse pos\n" + //
                    "  Apostrophe (') to open console (type help for help)");
        console.log(
            "You can also start the program with arguments for '--help' or '-?' as arg to see all possible options");


        Gdx.app.setApplicationLogger(console);
        Gdx.app.setLogLevel(debug ? Application.LOG_DEBUG : Application.LOG_INFO);

        if (renderGraphic) {
            hud = new HUDRenderer();

            blockAtlas = new TextureAtlas(TEXTURES_BLOCK_FILE);
            entityAtlas = new TextureAtlas(TEXTURES_ENTITY_FILE);
        }

        world = new World(new PerlinChunkGenerator(worldSeed), worldSeed);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            world.save();
            scheduler.shutdown(); // we want make sure this thread is dead
        }));
    }

    @Override
    public void render() {
        if (!renderGraphic) { return; }
        Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Vector3 unproject = world.getRender().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        mouseX = unproject.x / BLOCK_SIZE;
        mouseY = unproject.y / BLOCK_SIZE;

        mouseBlockX = MathUtils.floor(mouseX);
        mouseBlockY = MathUtils.floor(mouseY);

        //noinspection ConstantConditions
        world.getInput().update();
        for (LivingEntity entity : world.getLivingEntities()) {
            entity.getControls().update();
        }
        world.getRender().render();

        hud.render();
        console.draw();
    }


    @Override
    public void dispose() {
        if (renderGraphic) {
            hud.dispose();
            blockAtlas.dispose();
            entityAtlas.dispose();
            VisUI.dispose();
        }
        world.dispose();
        console.dispose();
    }

    @Override
    public void resize(int width, int height) {
        if (renderGraphic) {
            world.resize(width, height);
            hud.resize(width, height);
            console.resize(width, height);
        }
    }

    public static InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }

    public ConsoleLogger getConsoleLogger() {
        return console;
    }

    public TextureAtlas getBlockAtlas() {
        return blockAtlas;
    }

    public TextureAtlas getEntityAtlas() {
        return entityAtlas;
    }

    public int getMouseBlockX() {
        return mouseBlockX;
    }

    public int getMouseBlockY() {
        return mouseBlockY;
    }

    public float getMouseX() {
        return mouseX;
    }

    public float getMouseY() {
        return mouseY;
    }

    public static Main inst() {
        if (inst == null) { throw new IllegalStateException("Main instance not created"); }
        return inst;
    }

    @Nullable
    public Player getPlayer() {
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity instanceof Player) {
                return (Player) entity;
            }
        }
        return null;
    }

    public World getWorld() {
        return world;
    }

    public ConsoleHandler getConsole() {
        return console;
    }

    public HUDRenderer getHud() {
        return hud;
    }

    public CancellableThreadScheduler getScheduler() {
        return scheduler;
    }
}
