package no.elg.infiniteBootleg;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.kotcrab.vis.ui.VisUI;
import java.awt.Toolkit;
import no.elg.infiniteBootleg.args.ProgramArgs;
import no.elg.infiniteBootleg.input.GlobalInputListener;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.screen.ScreenRenderer;
import no.elg.infiniteBootleg.screens.MainMenuScreen;
import no.elg.infiniteBootleg.screens.WorldScreen;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.server.ServerClient;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.ServerClientWorld;
import no.elg.infiniteBootleg.world.SinglePlayerWorld;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientMain extends CommonMain {

  /** Only use this when a server is present */
  public static final int SCALE = Toolkit.getDefaultToolkit().getScreenSize().width > 2560 ? 2 : 1;

  private static ClientMain inst;
  @NotNull private final InputMultiplexer inputMultiplexer;
  private final Vector2 mouseWorldInput = new Vector2();
  private final Vector3 screenInputVec = new Vector3();
  @NotNull private TextureAtlas blockAtlas;
  @NotNull private TextureAtlas entityAtlas;
  @NotNull private ScreenRenderer screenRenderer;
  private int mouseBlockX;
  private int mouseBlockY;
  private float mouseWorldX;
  private float mouseWorldY;

  private int previousMouseBlockX;
  private int previousMouseBlockY;
  private float previousMouseWorldX;
  private float previousMouseWorldY;

  @Nullable private Screen screen;
  private boolean singleplayer;
  private boolean multiplayer;

  @Nullable private volatile Player mainPlayer;

  @NotNull
  public static ClientMain inst() {
    if (Settings.client) {
      return inst;
    }
    throw new IllegalStateException("Cannot get client main as a server");
  }

  public ClientMain(boolean test, @Nullable ProgramArgs progArgs) {
    super(test, progArgs);

    if (Main.isServer()) {
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
    } else {
      VisUI.load(VisUI.SkinScale.X1);
    }
    // must load VisUI first
    super.create();

    Gdx.input.setInputProcessor(inputMultiplexer);

    KAssets.INSTANCE.load();

    console.log(
        "Controls:\n"
            + //
            "  WASD to control the camera\n"
            + //
            "  arrow-keys to control the player\n"
            + //
            "  T to teleport player to current mouse pos\n"
            + //
            "  Apostrophe (') to open console (type help for help)");
    screenRenderer = new ScreenRenderer();
    blockAtlas = new TextureAtlas(TEXTURES_BLOCK_FILE);
    entityAtlas = new TextureAtlas(TEXTURES_ENTITY_FILE);
    setScreen(MainMenuScreen.INSTANCE);

    Runnable onShutdown =
        () -> {
          if (Main.isSingleplayer() && screen instanceof WorldScreen worldScreen) {
            var clientWorld = worldScreen.getWorld();
            clientWorld.save();
            clientWorld.dispose();
          } else if (Main.isClient()) {
            var serverClient = this.getServerClient();
            if (serverClient != null && serverClient.ctx != null) {
              serverClient.ctx.writeAndFlush(
                  PacketExtraKt.serverBoundClientDisconnectPacket(serverClient, "Client shutdown"));
            }
          }
          scheduler.shutdown(); // make sure scheduler threads are dead
        };
    Runtime.getRuntime().addShutdownHook(new Thread(onShutdown));
  }

  @Override
  public void resize(int width, int height) {
    if (Settings.client) {
      getScreen().resize(width, height);
      console.resize(width, height);
      screenRenderer.resize(width, height);
    }
  }

  @Override
  public void render() {
    if (Main.isServer()) {
      return;
    }
    Gdx.gl.glClearColor(0.2f, 0.3f, 1, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    if (screen instanceof WorldScreen worldScreen) {
      final ClientWorld world = worldScreen.getWorld();

      screenInputVec.set(Gdx.input.getX(), Gdx.input.getY(), 0);
      world.getRender().getCamera().unproject(screenInputVec);
      // Whenever z is not zero unproject returns a very low number
      // I don't know why this is the case, but checking for z to be zero seems to fix the bug
      if (screenInputVec.z == 0f) {

        previousMouseWorldX = mouseWorldX;
        previousMouseWorldY = mouseWorldY;
        previousMouseBlockX = mouseBlockX;
        previousMouseBlockY = mouseBlockY;

        final WorldBody worldBody = world.getWorldBody();
        mouseWorldX = screenInputVec.x / BLOCK_SIZE - worldBody.getWorldOffsetX();
        mouseWorldY = screenInputVec.y / BLOCK_SIZE - worldBody.getWorldOffsetY();
        mouseWorldInput.set(mouseWorldX, mouseWorldY);

        mouseBlockX = CoordUtil.worldToBlock(mouseWorldX);
        mouseBlockY = CoordUtil.worldToBlock(mouseWorldY);
      }
    }

    if (screen != null) {
      screen.render(Gdx.graphics.getDeltaTime());
    }
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  @Override
  public void dispose() {
    super.dispose();
    if (Settings.client) {
      screenRenderer.dispose();
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
    inputMultiplexer.addProcessor(GlobalInputListener.INSTANCE);
    Gdx.input.setOnscreenKeyboardVisible(false);

    Gdx.app.debug("SCREEN", "Loading new screen " + screen.getClass().getSimpleName());
    this.screen = screen;
    if (this.screen instanceof WorldScreen worldScreen) {
      updateStatus(worldScreen.getWorld());
    } else {
      updateStatus(null);
    }
    screen.show();
    screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
  }

  public void updateStatus(@Nullable ClientWorld world) {
    singleplayer = world instanceof SinglePlayerWorld;
    multiplayer = world instanceof ServerClientWorld;
  }

  @NotNull
  public Screen getScreen() {
    if (screen == null) {
      if (Settings.client) {
        throw new IllegalStateException("Client has no screen!");
      } else {
        throw new IllegalStateException("Server does not have screens");
      }
    }
    return screen;
  }

  /**
   * @return Either the world we're connected to or the singleplayer world, whichever is more
   *     correct. If the client is not in a world null will be returned
   */
  @Nullable
  public ClientWorld getWorld() {
    if (screen instanceof WorldScreen worldScreen) {
      return worldScreen.getWorld();
    }
    return null;
  }

  @Nullable
  public Player getPlayer() {
    ServerClient serverClient = getServerClient();
    if (serverClient != null) {
      return serverClient.getPlayer();
    }
    var world = getWorld();
    if (world == null) {
      return null;
    }
    return mainPlayer;
  }

  public void setPlayer(@Nullable Player player) {
    if (Main.isMultiplayer()) {
      // server does not have a main player
      return;
    }
    if (player != null && player.isDisposed()) {
      Main.logger().error("PLR", "Tried to set main player to an invalid entity");
      return;
    }
    var world = getWorld();
    if (world == null) {
      Main.logger().error("PLR", "No world loaded");
      return;
    }
    synchronized (INST_LOCK) {
      final WorldInputHandler worldInput = world.getInput();
      if (mainPlayer != player) {
        // if mainPlayer and player are the same, we would dispose the ''new'' mainPlayer

        Player oldPlayer = this.mainPlayer;
        if (oldPlayer != null && oldPlayer.hasControls()) {
          oldPlayer.removeControls();
        }
        if (player != null) {
          if (!world.containsEntity(player.getUuid())) {
            world.addEntity(player);
          }
          if (!player.hasControls()) {
            player.giveControls();
          }
        }
        this.mainPlayer = player;
        if (Settings.client) {
          world.getInput().setFollowing(player);
        }
        console.debug("PLR", "Changing main player to " + player);
      }
      worldInput.setFollowing(player);
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

  public float getMouseWorldX() {
    return mouseWorldX;
  }

  public float getMouseWorldY() {
    return mouseWorldY;
  }

  public int getPreviousMouseBlockX() {
    return previousMouseBlockX;
  }

  public int getPreviousMouseBlockY() {
    return previousMouseBlockY;
  }

  public float getPreviousMouseWorldX() {
    return previousMouseWorldX;
  }

  public float getPreviousMouseWorldY() {
    return previousMouseWorldY;
  }

  @NotNull
  public Vector2 getMouse() {
    return mouseWorldInput;
  }

  @NotNull
  public ScreenRenderer getScreenRenderer() {
    return screenRenderer;
  }

  @Nullable
  public ServerClient getServerClient() {
    if (screen instanceof WorldScreen worldScreen
        && worldScreen.getWorld() instanceof ServerClientWorld serverClientWorld) {
      return serverClientWorld.getServerClient();
    }
    return null;
  }

  /**
   * @return If the player is singleplayer
   */
  public boolean isSinglePlayer() {
    return singleplayer;
  }

  /**
   * @return If the client is connected to a server
   */
  public boolean isMultiplayer() {
    return multiplayer;
  }
}
