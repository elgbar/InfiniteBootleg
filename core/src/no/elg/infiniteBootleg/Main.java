package no.elg.infiniteBootleg;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.kotcrab.vis.ui.VisUI;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;

import java.io.File;

import static no.elg.infiniteBootleg.ProgramArgs.executeArgs;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

public class Main extends ApplicationAdapter {

    public static final String EXTERNAL_FOLDER = ".infiniteBootleg" + File.separatorChar;
    public static final String WORLD_FOLDER = EXTERNAL_FOLDER + "worlds" + File.separatorChar;
    public static final String TEXTURES_FOLDER = "textures" + File.separatorChar;
    public static final String TEXTURES_BLOCK_FILE = TEXTURES_FOLDER + "blocks.atlas";
    public static final String VERSION_FILE = "version";

    public static final CancellableThreadScheduler SCHEDULER = new CancellableThreadScheduler();

    private static InputMultiplexer inputMultiplexer;
    private TextureAtlas textureAtlas;

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

    private World world;

    private ConsoleHandler console;

    private SpriteBatch batch;
    private BitmapFont font;
    public static Main inst;
    private String[] args;


    private int mouseBlockX;
    private int mouseBlockY;


    public Main(String[] args) {
        this.args = args;
    }

    @Override
    public void create() {
        inst = this;

        inputMultiplexer = new InputMultiplexer();
        VisUI.load();
        console = new ConsoleHandler();
        Gdx.input.setInputProcessor(inputMultiplexer);

        executeArgs(args);

        batch = new SpriteBatch();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        textureAtlas = new TextureAtlas(TEXTURES_BLOCK_FILE);

        world = new World(new PerlinChunkGenerator(worldSeed), worldSeed);
        font = new BitmapFont(false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            world.getWorldTicker().stop();
            world.save();
        }));

        if (!renderGraphic) {
            console.setVisible(true);
            console.setSizePercent(100, 100);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (!renderGraphic) {
            console.draw();
            return;
        }

        Vector3 unproject = world.getRender().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        mouseBlockX = (int) Math.floor(unproject.x / BLOCK_SIZE);
        mouseBlockY = (int) Math.floor(unproject.y / BLOCK_SIZE);

        //noinspection ConstantConditions
        world.getInput().update();
        world.getRender().render();

        Block block = world.getRawBlock(mouseBlockX, mouseBlockY);

        int[] vChunks = world.getRender().getChunksInView();

        int chunksInView = Math.abs(vChunks[WorldRender.HOR_END] - vChunks[WorldRender.HOR_START]) *
                           Math.abs(vChunks[WorldRender.VERT_END] - vChunks[WorldRender.VERT_START]);
        int h = Gdx.graphics.getHeight();

        Chunk pointChunk = world.getChunkFromWorld(mouseBlockX, mouseBlockY);
        String pointing = String.format("Pointing at %s (%d, %d) (exists? %b) in chunk (%d, %d) (type: %s just air? %b)",//
                                        block != null ? block.getMaterial() : Material.AIR, //
                                        mouseBlockX, mouseBlockY, //
                                        block != null, //
                                        pointChunk.getChunkX(), pointChunk.getChunkY(), //
                                        world.getChunkLoader().getGenerator().getBiome(mouseBlockX), //
                                        pointChunk.isAllAir());

        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, h - 10);
        font.draw(batch, "Delta time: " + Gdx.graphics.getDeltaTime(), 10, h - 25);
        font.draw(batch, pointing, 10, h - 40);
        font.draw(batch,
                  "Viewing " + chunksInView + " chunks (" + chunksInView * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE + " blocks)", 10,
                  h - 55);
        font.draw(batch, "Zoom: " + world.getRender().getCamera().zoom, 10, h - 70);
        Entity player = world.getEntities().iterator().next(); //assume this is the player

        String pos = String.format("p: (%.4f,%.4f) v: (%.4f,%.4f)", player.getPosition().x, player.getPosition().y, 0f, 0f);
        font.draw(batch, "player " + pos, 10, h - 85);

        TextureRegion tr = world.getInput().getSelected().getTextureRegion();
        if (tr != null) {
            batch.draw(tr, Gdx.graphics.getWidth() - BLOCK_SIZE * 3, h - BLOCK_SIZE * 3, BLOCK_SIZE * 2, BLOCK_SIZE * 2);
        }
        batch.end();

        console.draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        textureAtlas.dispose();
        console.dispose();
        font.dispose();
        VisUI.dispose();
    }

    @Override
    public void resize(int width, int height) {
        if (world.getInput() != null) {
            world.getInput().resize(width, height);
        }
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
        console.refresh();
    }

    public static InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }

    public ConsoleLogger getConsoleLogger() {
        return console;
    }

    public TextureAtlas getTextureAtlas() {
        return textureAtlas;
    }

    public int getMouseBlockX() {
        return mouseBlockX;
    }

    public int getMouseBlockY() {
        return mouseBlockY;
    }

    public static Main inst() {
        if (inst == null) { throw new IllegalStateException("Main instance not created"); }
        return inst;
    }

    public World getWorld() {
        return world;
    }

    public ConsoleHandler getConsole() {
        return console;
    }
}
