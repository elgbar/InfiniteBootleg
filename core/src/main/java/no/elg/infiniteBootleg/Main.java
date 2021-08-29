package no.elg.infiniteBootleg;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Collections;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.LogLevel;
import java.awt.Toolkit;
import java.io.File;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.screen.ScreenRenderer;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Main extends ApplicationAdapter {

    public static final String EXTERNAL_FOLDER = ".infiniteBootleg" + File.separatorChar;
    public static final String WORLD_FOLDER = EXTERNAL_FOLDER + "worlds" + File.separatorChar;

    public static final String TEXTURES_FOLDER = "textures" + File.separatorChar;
    public static final String FONTS_FOLDER = "fonts" + File.separatorChar;

    public static final String TEXTURES_BLOCK_FILE = TEXTURES_FOLDER + "blocks.atlas";
    public static final String TEXTURES_ENTITY_FILE = TEXTURES_FOLDER + "entities.atlas";
    public static final String VERSION_FILE = "version";
    public static final int SCALE = Toolkit.getDefaultToolkit().getScreenSize().width > 2560 ? 2 : 1;

    private static final Object INST_LOCK = new Object();
    private static Main inst;

    private final InputMultiplexer inputMultiplexer;
    private final boolean test;
    private final CancellableThreadScheduler scheduler;
    private final Vector2 mouse = new Vector2();
    private final Vector3 mouseVec = new Vector3();

    private TextureAtlas blockAtlas;
    private TextureAtlas entityAtlas;
    private World world;
    private ConsoleHandler console;
    private HUDRenderer hud;
    private ScreenRenderer screenRenderer;
    private int mouseBlockX;
    private int mouseBlockY;
    private float mouseX;
    private float mouseY;

    private volatile Player mainPlayer;

    public Main(boolean test) {
        synchronized (INST_LOCK) {
            if (inst != null) {
                throw new IllegalStateException("A main instance have already be declared");
            }
            inst = this;
        }

        this.test = test;
        console = new ConsoleHandler(false);
        scheduler = new CancellableThreadScheduler(Settings.schedulerThreads);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (world != null) {
                world.save();
                final FileHandle worldFolder = world.worldFolder();
                if (worldFolder != null) {

                    worldFolder.deleteDirectory();
                }
            }
            scheduler.shutdown(); // we want make sure this thread is dead
        }));
        inputMultiplexer = new InputMultiplexer();
    }

    public static ConsoleLogger logger() {
        return inst().getConsoleLogger();
    }

    public ConsoleLogger getConsoleLogger() {
        return console;
    }

    public static Main inst() {
        return inst;
    }

    @Override
    public void create() {
        Gdx.input.setInputProcessor(inputMultiplexer);

        if (Settings.renderGraphic) {
            if (SCALE > 1) {
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
        console.log("You can also start the program with arguments for '--help' or '-?' as arg to see all possible options");


        Gdx.app.setApplicationLogger(console);
        Gdx.app.setLogLevel(test || Settings.debug ? Application.LOG_DEBUG : Application.LOG_INFO);
        //use unique iterators
        Collections.allocateIterators = true;

        if (Settings.renderGraphic) {
            screenRenderer = new ScreenRenderer();
            hud = new HUDRenderer();

            blockAtlas = new TextureAtlas(TEXTURES_BLOCK_FILE);
            entityAtlas = new TextureAtlas(TEXTURES_ENTITY_FILE);
        }

        world = new World(new PerlinChunkGenerator(Settings.worldSeed), Settings.worldSeed, !test);
    }

    @Override
    public void resize(int width, int height) {
        if (Settings.renderGraphic) {
            world.resize(width, height);
            screenRenderer.resize(width, height);
            console.resize(width, height);
        }
    }

    @Override
    public void render() {
        if (!Settings.renderGraphic) {
            return;
        }
        Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mouseVec.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        world.getRender().getCamera().unproject(mouseVec);
        final WorldBody worldBody = world.getWorldBody();
        mouseX = mouseVec.x / BLOCK_SIZE - worldBody.getWorldOffsetX();
        mouseY = mouseVec.y / BLOCK_SIZE - worldBody.getWorldOffsetY();
        mouse.set(mouseX, mouseY);

        mouseBlockX = MathUtils.floor(mouseX);
        mouseBlockY = MathUtils.floor(mouseY);

        synchronized (INST_LOCK) {
            //noinspection ConstantConditions
            world.getInput().update();
            if (!world.getWorldTicker().isPaused()) {
                //only update controls when we're not paused
                for (LivingEntity entity : world.getLivingEntities()) {
                    entity.update();
                }
            }
            world.getRender().render();
        }

        hud.render();
        console.draw();
    }

    @Override
    public void dispose() {
        if (Settings.renderGraphic) {
            screenRenderer.dispose();
            blockAtlas.dispose();
            entityAtlas.dispose();
            VisUI.dispose();
        }
        world.dispose();
        console.dispose();
    }

    public InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
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

    public Vector2 getMouse() {
        return mouse;
    }

    public ScreenRenderer getScreenRenderer() {
        return screenRenderer;
    }

    @Nullable
    public Player getPlayer() {
        if (mainPlayer == null || mainPlayer.isInvalid()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Player player) {
                    mainPlayer = player;
                    return mainPlayer;
                }
            }
            return null;
        }
        else {
            return mainPlayer;
        }
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(@NotNull World world) {
        synchronized (INST_LOCK) {
            this.world = world;
        }
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
