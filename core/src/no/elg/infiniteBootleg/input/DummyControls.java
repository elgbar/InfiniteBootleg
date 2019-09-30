package no.elg.infiniteBootleg.input;

import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Controls for entities that do not move
 *
 * @author Elg
 */
public class DummyControls extends AbstractEntityControls {

    public DummyControls(@NotNull WorldRender worldRender, @NotNull LivingEntity entity) {
        super(worldRender, entity);
    }

    @Override
    public @Nullable Material getSelected() {
        return null;
    }

    @Override
    public void setSelected(@Nullable Material selected) {}

    @Override
    public void update() {

    }
}
