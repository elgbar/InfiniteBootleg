package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class WorldInputHandler extends InputAdapter {

    private final OrthographicCamera camera;
    private final World world;

    public WorldInputHandler(@NotNull World world) {
        this.camera = world.getCamera();
        this.world = world;
    }


}
