package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class WorldInputHandler extends InputAdapter implements Disposable {


    private final OrthographicCamera camera;
    private final WorldRender worldRender;

    public WorldInputHandler(@NotNull WorldRender world) {
        camera = world.getCamera();
        this.worldRender = world;

        Main.getInputMultiplexer().addProcessor(this);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        final Vector3 unproject = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        final int blockX = (int) (unproject.x / World.BLOCK_SIZE);
        final int blockY = (int) (unproject.y / World.BLOCK_SIZE);

        if (button == Input.Buttons.LEFT) {
            getWorld().setBlock(blockX, blockY, null);
        }
        else if (button == Input.Buttons.RIGHT) {
            //TODO update with selected block
            getWorld().setBlock(blockX, blockY, Material.STONE);
        }
        else {
            return false;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.position.x -= Gdx.input.getDeltaX() * camera.zoom;
        camera.position.y += Gdx.input.getDeltaY() * camera.zoom;
        worldRender.update();
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        camera.zoom += amount * 0.05f * camera.zoom;
        if (camera.zoom < 0.25) {
            camera.zoom = 0.25f;
        }
        worldRender.update();
        return true;
    }

    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        worldRender.update();
    }

    public World getWorld() {
        return worldRender.getWorld();
    }

    @Override
    public void dispose() {
        Main.getInputMultiplexer().removeProcessor(this);
    }
}
