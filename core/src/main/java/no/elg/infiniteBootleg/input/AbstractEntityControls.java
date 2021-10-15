package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.InputAdapter;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public abstract class AbstractEntityControls extends InputAdapter implements EntityControls {

    private final WorldRender worldRender;
    private final LivingEntity entity;

    public AbstractEntityControls(@NotNull WorldRender worldRender, @NotNull LivingEntity entity) {
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

    public World getWorld() {
        return worldRender.world;
    }

    @Override
    public void dispose() {
        Main.inst().getInputMultiplexer().removeProcessor(this);
    }
}
