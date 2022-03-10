package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.InputAdapter;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.render.ClientWorldRender;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public abstract class AbstractEntityControls extends InputAdapter implements EntityControls {

  private final ClientWorldRender worldRender;
  private final LivingEntity entity;

  public AbstractEntityControls(
      @NotNull ClientWorldRender worldRender, @NotNull LivingEntity entity) {
    this.worldRender = worldRender;
    this.entity = entity;
  }

  @Override
  @NotNull
  public LivingEntity getControlled() {
    return entity;
  }

  public WorldRender getWorldRender() {
    return worldRender;
  }

  public ClientWorld getWorld() {
    return worldRender.getWorld();
  }

  @Override
  public void dispose() {
    ClientMain.inst().getInputMultiplexer().removeProcessor(this);
  }
}
