package no.elg.infiniteBootleg.screen;

import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.screen.hud.CurrentBlock;
import no.elg.infiniteBootleg.screen.hud.DebugGraph;
import no.elg.infiniteBootleg.screen.hud.DebugText;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class HUDRenderer implements Renderer, Resizable {

  public static final int DISPLAY_NOTHING = 0;
  public static final int DISPLAY_CURRENT_BLOCK = 1;
  public static final int DISPLAY_MINIMAL_DEBUG = 2;
  public static final int DISPLAY_DEBUG = 4;
  public static final int DISPLAY_GRAPH_FPS = 8;

  private int modus = DISPLAY_CURRENT_BLOCK;

  @NotNull private final StringBuilder builder = new StringBuilder();

  public HUDRenderer() {
    modus |= Settings.debug ? DISPLAY_DEBUG : DISPLAY_CURRENT_BLOCK;
  }

  @Override
  public void render() {
    if (modus == DISPLAY_NOTHING || Main.isServer()) {
      return;
    }
    ClientMain main = ClientMain.inst();
    @Nullable ClientWorld world = main.getWorld();

    LivingEntity player = ClientMain.inst().getPlayer();
    ScreenRenderer sr = ClientMain.inst().getScreenRenderer();

    reset();
    sr.begin();
    if (hasMode(DISPLAY_MINIMAL_DEBUG) || hasMode(DISPLAY_DEBUG)) {
      DebugText.fpsString(builder, world);
    }
    if (hasMode(DISPLAY_DEBUG) && world != null) {
      int mouseBlockX = main.getMouseBlockX();
      int mouseBlockY = main.getMouseBlockY();

      nl();
      DebugText.pointing(builder, world, mouseBlockX, mouseBlockY);
      nl();
      DebugText.chunk(builder, world, mouseBlockX, mouseBlockY);
      nl();
      DebugText.viewChunk(builder, world);
      nl();
      DebugText.pos(builder, player);
      nl();
      DebugText.time(builder, world);
      nl();
      DebugText.lights(builder);
      nl();
      DebugText.ents(builder, world);
    }
    if (!builder.isEmpty()) {
      sr.drawTop(builder.toString(), 1);
    }

    if (player != null && hasMode(DISPLAY_CURRENT_BLOCK)) {
      CurrentBlock.INSTANCE.render(sr, player);
    }
    if (hasMode(DISPLAY_GRAPH_FPS)) {
      DebugGraph.INSTANCE.render(sr, world);
    }
    sr.end();
  }

  private void reset() {
    builder.setLength(0);
  }

  private void nl() {
    builder.append('\n');
  }

  public boolean hasMode(int mode) {
    return (this.modus & mode) > 0;
  }

  public void displayNothing() {
    this.modus = DISPLAY_NOTHING;
  }

  public void enableMode(int mode) {
    this.modus |= mode;
  }

  public void disableMode(int mode) {
    this.modus &= ~mode;
  }

  public void toggleMode(int mode) {
    this.modus ^= mode;
  }

  @Override
  public void resize(int width, int height) {
    DebugGraph.INSTANCE.resize(width, height);
  }
}
