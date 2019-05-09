package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class InputHandler implements InputProcessor {

    private final OrthographicCamera camera;

    public InputHandler(@NotNull OrthographicCamera camera) {

        this.camera = camera;
    }

    @Override
    public boolean keyDown(int keycode) {
//        System.out.println("keycode = " + keycode);
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.position.x -= Gdx.input.getDeltaX() * camera.zoom; //* World.BLOCK_SIZE;
        camera.position.y += Gdx.input.getDeltaY() * camera.zoom; //* World.BLOCK_SIZE;

        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        camera.zoom += amount * 0.05f;
//        if (camera.zoom > 1f) {
//            camera.zoom = 1f;
//        }
//        else
        if (camera.zoom <= 0) {
            camera.zoom = 0.04f;
        }
        return false;
    }
}
