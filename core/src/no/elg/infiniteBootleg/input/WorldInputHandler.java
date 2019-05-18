package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
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
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.position.x -= Gdx.input.getDeltaX() * camera.zoom;
        camera.position.y += Gdx.input.getDeltaY() * camera.zoom;
        worldRender.update();
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        camera.zoom += amount * 0.05f * camera.zoom;
        if (camera.zoom <= 0) {
            camera.zoom = 0.04f;
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
