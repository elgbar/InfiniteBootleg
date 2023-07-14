package no.elg.infiniteBootleg;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.kotcrab.vis.ui.VisUI;
import java.awt.Toolkit;
import no.elg.infiniteBootleg.args.ProgramArgs;
import no.elg.infiniteBootleg.input.GlobalInputListener;
import no.elg.infiniteBootleg.screens.MainMenuScreen;
import no.elg.infiniteBootleg.screens.ScreenRenderer;
import no.elg.infiniteBootleg.screens.WorldScreen;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.server.ServerClient;
import no.elg.infiniteBootleg.world.world.ClientWorld;
import no.elg.infiniteBootleg.world.world.ServerClientWorld;
import no.elg.infiniteBootleg.world.world.SinglePlayerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientMain extends CommonMain {

  /** Only use this when a server is present */
  public static final int SCALE = Toolkit.getDefaultToolkit().getScreenSize().width > 2560 ? 2 : 1;

  public static final float CLEAR_COLOR_R = 0.2f;
  public static final float CLEAR_COLOR_G = (float) (68.0 / 255.0);
  public static final float CLEAR_COLOR_B = 1f;
  public static final float CLEAR_COLOR_A = 1f;

  private static ClientMain inst;
  @NotNull private final InputMultiplexer inputMultiplexer;
  @NotNull private ScreenRenderer screenRenderer;

  @Nullable private Screen screen;
  private boolean singleplayer;
  private boolean multiplayer;

  private String renderThreadName;

  private MouseLocator mouseLocator;

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
    renderThreadName = Thread.currentThread().getName();
    mouseLocator = new MouseLocator();

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
    setScreen(MainMenuScreen.INSTANCE);

    Runnable onShutdown =
        () -> {
          if (Main.isSingleplayer() && screen instanceof WorldScreen worldScreen) {
            var clientWorld = worldScreen.getWorld();
            clientWorld.save();
            clientWorld.dispose();
          } else if (Main.isClient()) {
            var serverClient = getServerClient();
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
  public void resize(int rawWidth, int rawHeight) {
    int width = Math.max(rawWidth, 1);
    int height = Math.max(rawHeight, 1);
    Main.logger().log("Resizing client to " + width + " x " + height);
    getScreen().resize(width, height);
    console.resize(width, height);
    screenRenderer.resize(width, height);
  }

  @Override
  public void render() {
    if (Main.isServer()) {
      return;
    }
    Gdx.gl.glClearColor(CLEAR_COLOR_R, CLEAR_COLOR_G, CLEAR_COLOR_B, CLEAR_COLOR_A);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    if (screen instanceof WorldScreen worldScreen) {
      ClientWorld world = worldScreen.getWorld();
      mouseLocator.update(world);
    }

    if (screen != null) {
      screen.render(Gdx.graphics.getDeltaTime());
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    if (Settings.client) {
      screenRenderer.dispose();
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
    Main.logger()
        .debug(
            "STATUS",
            "World status updated: Multiplayer? "
                + multiplayer
                + ", Singleplayer? "
                + singleplayer);
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

  public @NotNull InputMultiplexer getInputMultiplexer() {
    return inputMultiplexer;
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

  public @NotNull String getRenderThreadName() {
    return renderThreadName;
  }

  public MouseLocator getMouseLocator() {
    return mouseLocator;
  }

  public boolean shouldNotIgnoreWorldInput() {
    return !shouldIgnoreWorldInput();
  }

  public boolean shouldIgnoreWorldInput() {
    boolean debugMenuVisible =
        screen instanceof WorldScreen worldScreen && worldScreen.isDebugMenuVisible();
    boolean consoleVisible = console.isVisible();
    return consoleVisible || debugMenuVisible;
  }
}
