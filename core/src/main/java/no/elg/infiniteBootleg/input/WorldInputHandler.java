package no.elg.infiniteBootleg.input;

import static com.badlogic.gdx.Input.Keys.F12;
import static com.badlogic.gdx.Input.Keys.F3;
import static com.badlogic.gdx.Input.Keys.F5;
import static com.badlogic.gdx.Input.Keys.F9;
import static com.badlogic.gdx.Input.Keys.SHIFT_LEFT;
import static no.elg.infiniteBootleg.world.ecs.system.client.FollowEntitySystem.SCROLL_SPEED;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.screens.WorldScreen;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.render.ClientWorldRender;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class WorldInputHandler extends InputAdapter implements Disposable {

  private final ClientWorldRender worldRender;

  public WorldInputHandler(@NotNull ClientWorldRender world) {
    worldRender = world;
  }

  @Override
  public boolean keyDown(int keycode) {
    if (Main.inst().getConsole().isVisible() || Main.isMultiplayer() && keycode != F3) {
      return false;
    }
    ClientWorld world = ClientMain.inst().getWorld();
    if (world == null) {
      return false;
    }
    switch (keycode) {
      case F3:
        Screen screen = ClientMain.inst().getScreen();
        if (screen instanceof WorldScreen worldScreen) {
          HUDRenderer hud = worldScreen.getHud();

          if (!hud.hasMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG)
              && !hud.hasMode(HUDRenderer.DISPLAY_DEBUG)) {
            hud.enableMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG);
          } else if (hud.hasMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG)) {
            hud.enableMode(HUDRenderer.DISPLAY_DEBUG);
            hud.disableMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG);
          } else {
            hud.disableMode(HUDRenderer.DISPLAY_DEBUG);
            hud.disableMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG);
          }

          if (Gdx.input.isKeyPressed(SHIFT_LEFT)) {
            hud.toggleMode(HUDRenderer.DISPLAY_GRAPH_FPS);
          }
        }
        break;
      case F5:
        world.save();
        break;
      case F9:
        world.reload(true);
        break;
      case F12:
        Ticker ticker = world.getWorldTicker();
        if (ticker.isPaused()) {
          ticker.resume();
          Main.logger().log("World", "Ticker resumed by F12");
        } else {
          ticker.pause();
          Main.logger().log("World", "Ticker paused by F12");
        }
        break;
      default:
        return false;
    }
    return true;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    if (Main.inst().getConsole().isVisible()) {
      return false;
    }
    OrthographicCamera camera = worldRender.getCamera();
    camera.zoom += ((amountX + amountY) / 2) * SCROLL_SPEED;
    if (camera.zoom < WorldRender.MIN_ZOOM) {
      camera.zoom = WorldRender.MIN_ZOOM;
    } else if (camera.zoom > WorldRender.MAX_ZOOM) {
      camera.zoom = WorldRender.MAX_ZOOM;
    }
    worldRender.update();
    return true;
  }

  public ClientWorld getWorld() {
    return worldRender.getWorld();
  }

  @Override
  public void dispose() {
    ClientMain.inst().getInputMultiplexer().removeProcessor(this);
  }
}
