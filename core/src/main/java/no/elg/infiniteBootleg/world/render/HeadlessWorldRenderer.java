package no.elg.infiniteBootleg.world.render;

import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class HeadlessWorldRenderer extends WorldRender {

    public HeadlessWorldRenderer(@NotNull World world) {
        super(world);
    }

    @Override
    public void render() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void update() {

    }
}
