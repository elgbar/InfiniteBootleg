package no.elg.infiniteBootleg;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.kotcrab.vis.ui.VisUI;
import java.awt.Toolkit;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.screen.ScreenRenderer;
import no.elg.infiniteBootleg.screens.MainMenuScreen;
import no.elg.infiniteBootleg.screens.WorldScreen;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.server.ServerClient;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
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

  @Nullable private Screen screen;

  @Nullable private volatile Player mainPlayer;
  @Nullable private volatile ServerClient serverClient;
  @Nullable protected ClientWorld singleplayerWorld;

  @NotNull
  public static ClientMain inst() {
    if (Settings.client) {
      return inst;
    }
    throw new IllegalStateException("Cannot get client main as a server");
  }

  public ClientMain(boolean test) {
    super(test);

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
            final FileHandle worldFolder = clientWorld.getWorldFolder();
            if (worldFolder != null) {
              worldFolder.deleteDirectory();
            }
          } else if (Main.isClient()) {
            var serverClient = this.serverClient;
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
        final WorldBody worldBody = world.getWorldBody();
        mouseWorldX = screenInputVec.x / BLOCK_SIZE - worldBody.getWorldOffsetX();
        mouseWorldY = screenInputVec.y / BLOCK_SIZE - worldBody.getWorldOffsetY();
        mouseWorldInput.set(mouseWorldX, mouseWorldY);

        mouseBlockX = (int) Math.floor(mouseWorldX);
        mouseBlockY = (int) Math.floor(mouseWorldY);
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
      if (Settings.client) {
        throw new IllegalStateException("Server does not have screens");
      } else {
        throw new IllegalStateException("Client has no screen!");
      }
    }
    return screen;
  }

  /**
   * @return Either the world we're connected to or the singleplayer world, whichever is more
   *     correct. If the client is not in a world null will be returned
   * @throws IllegalStateException If there is a client, but no world attached.
   * @throws IllegalStateException If there is no client, and there is no singleplayer world
   */
  @Nullable
  public ClientWorld getWorld() {
    final ServerClient client = ClientMain.inst().getServerClient();
    if (Main.isSingleplayer()) {
      return ClientMain.inst().getSingleplayerWorld();
    } else if (Main.isServerClient()) {
      final ClientWorld world = client.getWorld();
      if (world == null) {
        PacketExtraKt.fatal(client.ctx, "Failed to get client world when executing command");
        throw new IllegalStateException("Failed to get client world when executing command");
      }
      return world;
    }
    return null;
  }

  /**
   * @return Only use when singleplayer is guaranteed
   * @see #getWorld()
   */
  @Nullable
  public ClientWorld getSingleplayerWorld() {
    return singleplayerWorld;
  }

  public void setSingleplayerWorld(@Nullable ClientWorld singleplayerWorld) {
    if (Main.isMultiplayer()) {
      throw new IllegalStateException("Cannot set the singleplayer world when in multiplayer!");
    }
    synchronized (INST_LOCK) {
      this.singleplayerWorld = singleplayerWorld;
    }
  }

  @Nullable
  public Player getPlayer() {
    if (Main.isServerClient()) {
      return serverClient.getPlayer();
    }
    if (singleplayerWorld == null) {
      return null;
    }
    synchronized (INST_LOCK) {
      if (mainPlayer == null || mainPlayer.isInvalid()) {
        for (LivingEntity entity : singleplayerWorld.getPlayers()) {
          if (entity instanceof Player player
              && !entity.isInvalid()
              && player.getControls() != null) {
            setPlayer(player);
            return mainPlayer;
          }
        }
        return null;
      } else {
        return mainPlayer;
      }
    }
  }

  public void setPlayer(@Nullable Player player) {
    if (Main.isMultiplayer()) {
      // server does not have a main player
      return;
    }
    if (player != null && player.isInvalid()) {
      Main.logger().error("PLR", "Tried to set main player to an invalid entity");
      return;
    }
    synchronized (INST_LOCK) {
      if (mainPlayer != player) {
        // if mainPlayer and player are the same, we would dispose the ''new'' mainPlayer

        if (mainPlayer != null && mainPlayer.hasControls()) {
          mainPlayer.removeControls();
        }
        if (player != null) {
          if (!singleplayerWorld.containsEntity(player.getUuid())) {
            console.error("PLR", "Tried to set main player to an entity that's not in the world!");
            singleplayerWorld.addEntity(player);
          }
          if (!player.hasControls()) {
            player.giveControls();
          }
        }
        mainPlayer = player;
        if (Settings.client) {
          assert singleplayerWorld.getInput() != null;
          singleplayerWorld.getInput().setFollowing(mainPlayer);
        }
        console.debug("PLR", "Changing main player to " + player);
      }
      final WorldInputHandler worldInput = singleplayerWorld.getInput();
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
    return mouseWorldX;
  }

  public float getMouseY() {
    return mouseWorldY;
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
    return serverClient;
  }

  public void setServerClient(@Nullable ServerClient serverClient) {
    this.serverClient = serverClient;
  }
}
