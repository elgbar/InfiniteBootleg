package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Updatable;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public interface EntityControls extends InputProcessor, Updatable, Disposable {


    /**
     * @return The living entity to control
     */
    @NotNull LivingEntity getControlled();

    /**
     * @return The selected material for this entity
     */
    @Nullable Material getSelected();

    /**
     * @param selected
     *     The new selected material
     */
    void setSelected(@Nullable Material selected);

    float getBrushSize();

    void setBrushSize(float brushSize);
}
