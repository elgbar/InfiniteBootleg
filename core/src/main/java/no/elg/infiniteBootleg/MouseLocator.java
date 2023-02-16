package no.elg.infiniteBootleg;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import org.jetbrains.annotations.NotNull;

public class MouseLocator {

  private int mouseBlockX;
  private int mouseBlockY;
  private float mouseWorldX;
  private float mouseWorldY;

  private int previousMouseBlockX;
  private int previousMouseBlockY;
  private float previousMouseWorldX;
  private float previousMouseWorldY;

  private final Vector2 mouseWorldInput = new Vector2();
  private final Vector3 screenInputVec = new Vector3();

  void update(ClientWorld world) {
    screenInputVec.set(Gdx.input.getX(), Gdx.input.getY(), 0);
    world.getRender().getCamera().unproject(screenInputVec);
    // Whenever z is not zero unproject returns a very low number
    // I don't know why this is the case, but checking for z to be zero seems to fix the bug
    if (screenInputVec.z == 0f) {

      previousMouseWorldX = mouseWorldX;
      previousMouseWorldY = mouseWorldY;
      previousMouseBlockX = mouseBlockX;
      previousMouseBlockY = mouseBlockY;

      WorldBody worldBody = world.getWorldBody();
      mouseWorldX = screenInputVec.x / BLOCK_SIZE - worldBody.getWorldOffsetX();
      mouseWorldY = screenInputVec.y / BLOCK_SIZE - worldBody.getWorldOffsetY();
      mouseWorldInput.set(mouseWorldX, mouseWorldY);

      mouseBlockX = CoordUtil.worldToBlock(mouseWorldX);
      mouseBlockY = CoordUtil.worldToBlock(mouseWorldY);
    }
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
}
