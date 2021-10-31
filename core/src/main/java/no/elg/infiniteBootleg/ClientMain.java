package no.elg.infiniteBootleg;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.kotcrab.vis.ui.VisUI;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.screen.ScreenRenderer;
import no.elg.infiniteBootleg.screens.MainMenuScreen;
import no.elg.infiniteBootleg.screens.WorldScreen;
import no.elg.infiniteBootleg.server.ServerClient;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientMain extends ServerMain {

    private static ClientMain inst;
    @NotNull
    private final InputMultiplexer inputMultiplexer;
    private final Vector2 mouse = new Vector2();
    private final Vector3 mouseVec = new Vector3();
    @NotNull
    private TextureAtlas blockAtlas;
    @NotNull
    private TextureAtlas entityAtlas;
    @NotNull
    private ScreenRenderer screenRenderer;
    private int mouseBlockX;
    private int mouseBlockY;
    private float mouseX;
    private float mouseY;

    @Nullable
    private Screen screen;

    @Nullable
    private volatile Player mainPlayer;
    @Nullable
    private ServerClient serverClient;

    @NotNull
    public static ClientMain inst() {
        if (Settings.client) {
            return inst;
        }
        throw new IllegalStateException("Cannot get client main as a server");
    }

    public ClientMain(boolean test) {
        super(test);

        if (!Settings.client) {
            throw new IllegalStateException("Cannot create client main as a server!");
        }
        synchronized (INST_LOCK) {
            if (inst != null) {
                throw new IllegalStateException("A main instance have already be declared");
            }
            inst = this;
        }
        if (test) {
            VisUI.load(VisUI.SkinScale.X1);
        }

        inputMultiplexer = new InputMultiplexer();
    }

    @Override
    public void create() {
        if (SCALE > 1) {
            VisUI.load(VisUI.SkinScale.X2);
        }
        else {
            VisUI.load(VisUI.SkinScale.X1);
        }
        // must load VisUI first
        super.create();

        Gdx.input.setInputProcessor(inputMultiplexer);

        KAssets.INSTANCE.load();

        console.log("Controls:\n" + //
                    "  WASD to control the camera\n" + //
                    "  arrow-keys to control the player\n" +//
                    "  T to teleport player to current mouse pos\n" + //
                    "  Apostrophe (') to open console (type help for help)");
        screenRenderer = new ScreenRenderer();
        blockAtlas = new TextureAtlas(TEXTURES_BLOCK_FILE);
        entityAtlas = new TextureAtlas(TEXTURES_ENTITY_FILE);
        setScreen(MainMenuScreen.INSTANCE);
    }

    @Override
    public void resize(int width, int height) {
        if (Settings.client) {
            getScreen().resize(width, height);
            console.resize(width, height);
        }
    }

    @Override
    public void render() {
        if (!Settings.client) {
            return;
        }
        Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (screen instanceof WorldScreen worldScreen) {
            final World world = worldScreen.getWorld();
            mouseVec.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            world.getRender().getCamera().unproject(mouseVec);
            final WorldBody worldBody = world.getWorldBody();
            mouseX = mouseVec.x / BLOCK_SIZE - worldBody.getWorldOffsetX();
            mouseY = mouseVec.y / BLOCK_SIZE - worldBody.getWorldOffsetY();
            mouse.set(mouseX, mouseY);

            mouseBlockX = MathUtils.floor(mouseX);
            mouseBlockY = MathUtils.floor(mouseY);
        }

        if (screen != null) {
            screen.render(Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (Settings.client) {
            if (screenRenderer != null) {
                screenRenderer.dispose();
            }
            blockAtlas.dispose();
            entityAtlas.dispose();
            VisUI.dispose();
            if (screen != null) {
                screen.dispose();
            }
        }
    }

    public void setScreen(@NotNull Screen screen) {
        Screen old = this.screen;
        if (old != null) {
            old.hide();
        }

        // clean up any mess the previous screen have made
        inputMultiplexer.clear();
        Gdx.input.setOnscreenKeyboardVisible(false);

        Gdx.app.debug("SCREEN", "Loading new screen " + screen.getClass().getSimpleName());
        this.screen = screen;
        screen.show();
        screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @NotNull
    public Screen getScreen() {
        if (screen == null) {
            if (Settings.client) { throw new IllegalStateException("Server does not have screens"); }
            else {
                throw new IllegalStateException("Client has no screen!");
            }
        }
        return screen;
    }

    @Nullable
    public Player getPlayer() {
        if (!Settings.client) {
            //server does not have a main player
            return null;
        }
        synchronized (INST_LOCK) {
            if (mainPlayer == null || mainPlayer.isInvalid()) {
                for (LivingEntity entity : world.getPlayers()) {
                    if (entity instanceof Player player && !entity.isInvalid() && player.getControls() != null) {
                        setPlayer(player);
                        return mainPlayer;
                    }
                }
                return null;
            }
            else {
                return mainPlayer;
            }
        }
    }

    public void setPlayer(@Nullable Player player) {
        if (!Settings.client) {
            //server does not have a main player
            return;
        }
        if (player != null && player.isInvalid()) {
            Main.logger().error("PLR", "Tried to set main player to an invalid entity");
            return;
        }
        synchronized (INST_LOCK) {
            if (mainPlayer != player) {
                //if mainPlayer and player are the same, we would dispose the ''new'' mainPlayer

                if (mainPlayer != null && mainPlayer.hasControls()) {
                    mainPlayer.removeControls();
                }
                if (player != null) {
                    if (!world.containsEntity(player.getUuid())) {
                        console.error("PLR", "Tried to set main player to an entity that's not in the world!");
                        world.addEntity(player);
                    }
                    if (!player.hasControls()) {
                        player.giveControls();
                    }
                }
                mainPlayer = player;
                if (Settings.client) {
                    assert world.getInput() != null;
                    world.getInput().setFollowing(mainPlayer);
                }
                console.debug("PLR", "Changing main player to " + player);
            }
            final WorldInputHandler worldInput = world.getInput();
            if (worldInput != null) {
                worldInput.setFollowing(player);
            }
        }
    }

    public @NotNull InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }

    @NotNull
    public TextureAtlas getBlockAtlas() {
        return blockAtlas;
    }

    @NotNull
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

    @NotNull
    public Vector2 getMouse() {
        return mouse;
    }

    @NotNull
    public ScreenRenderer getScreenRenderer() {
        return screenRenderer;
    }

    @Nullable
    public ServerClient getServerClient() {
        return serverClient;
    }

    public void setServerClient(@Nullable ServerClient serverClient) {
        this.serverClient = serverClient;
    }
}