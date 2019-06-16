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
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.render.WorldRender;

import java.io.File;

import static no.elg.infiniteBootleg.ProgramArgs.executeArgs;

public class Main extends ApplicationAdapter {

    public static final String WORLD_FOLDER = "infiniteBootleg" + File.separatorChar + "worlds" + File.separatorChar;
    public static final String TEXTURES_FOLDER = "textures" + File.separatorChar;
    public static final String TEXTURES_BLOCK_FILE = TEXTURES_FOLDER + "blocks.pack";
    public static final String VERSION_FILE = "version";
    public static final CancellableThreadScheduler SCHEDULER = new CancellableThreadScheduler();

    private static InputMultiplexer inputMultiplexer;
    private TextureAtlas textureAtlas;

    public static boolean renderGraphic = true;
    private World world;

    private ConsoleHandler console;

    private SpriteBatch batch;
    private BitmapFont font;
    public static Main inst;

    public Main(String[] args) {
        executeArgs(args);
    }

    @Override
    public void create() {
        inst = this;

        batch = new SpriteBatch();
        inputMultiplexer = new InputMultiplexer();
        VisUI.load();
        console = new ConsoleHandler();
        Gdx.input.setInputProcessor(inputMultiplexer);

        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        textureAtlas = new TextureAtlas(TEXTURES_BLOCK_FILE);

        int worldSeed = 3;
        world = new World(new PerlinChunkGenerator(worldSeed), worldSeed);
        font = new BitmapFont(false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            world.getWorldTicker().stop();
            world.save();
        }));
    }

    @Override
    public void render() {
        if (!Main.renderGraphic) {
            return;
        }
        int h = Gdx.graphics.getHeight();
        Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        world.getInput().update();
        world.getRender().render();

        Vector3 unproject = world.getRender().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        int blockX = (int) Math.floor(unproject.x / World.BLOCK_SIZE);
        int blockY = (int) Math.floor(unproject.y / World.BLOCK_SIZE);
        Block block = world.getBlock(blockX, blockY);

        int[] vChunks = world.getRender().getChunksInView();

        int chunksInView = Math.abs(vChunks[WorldRender.HOR_END] - vChunks[WorldRender.HOR_START]) *
                           Math.abs(vChunks[WorldRender.VERT_END] - vChunks[WorldRender.VERT_START]);
        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, h - 10);
        font.draw(batch, "Delta time: " + Gdx.graphics.getDeltaTime(), 10, h - 25);
        font.draw(batch, "Pointing at " + block.getMaterial() + " (" + blockX + ", " + blockY + ") in chunk " +
                         world.getChunkFromWorld(blockX, blockY).getLocation(), 10, h - 40);
        font.draw(batch,
                  "Viewing " + chunksInView + " chunks (" + chunksInView * Chunk.CHUNK_WIDTH * Chunk.CHUNK_WIDTH + " blocks)", 10,
                  h - 55);
        font.draw(batch, "Zoom: " + world.getRender().getCamera().zoom, 10, h - 70);

        TextureRegion tr = world.getInput().getSelected().getTextureRegion();
        if (tr != null) {
            batch.draw(tr, Gdx.graphics.getWidth() - 48, h - 48, 32, 32);
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
        world.getInput().resize(width, height);
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

    public static Main inst() {
        if (inst == null) { throw new IllegalStateException("Main instance not created"); }
        return inst;
    }
}
