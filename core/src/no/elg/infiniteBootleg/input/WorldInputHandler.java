package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.jetbrains.annotations.NotNull;

import static com.badlogic.gdx.Input.Keys.*;
import static no.elg.infiniteBootleg.world.render.WorldRender.CHUNK_TEXTURE_HEIGHT;
import static no.elg.infiniteBootleg.world.render.WorldRender.CHUNK_TEXTURE_WIDTH;

/**
 * @author Elg
 */
public class WorldInputHandler extends InputAdapter implements Disposable, Updatable {


    private final OrthographicCamera camera;
    private final WorldRender worldRender;
    private Material selected;

    public WorldInputHandler(@NotNull WorldRender world) {
        camera = world.getCamera();
        worldRender = world;

        Main.getInputMultiplexer().addProcessor(this);
        selected = Material.STONE;
    }

    public Material getSelected() {
        return selected;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case NUM_1:
            case NUMPAD_1:
                selected = Material.values()[1];
                break;
            case NUM_2:
            case NUMPAD_2:
                selected = Material.values()[2];
                break;
            case NUM_3:
            case NUMPAD_3:
                selected = Material.values()[3];
                break;
            case NUM_4:
            case NUMPAD_4:
                selected = Material.values()[4];
                break;
            case NUM_5:
            case NUMPAD_5:
                selected = Material.values()[5];
                break;
            case NUM_6:
            case NUMPAD_6:
                selected = Material.values()[6];
                break;
            default:
                return false;
        }
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

    @Override
    public void update() {
        final Vector3 unproject = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        final int blockX = (int) Math.floor(unproject.x / Block.BLOCK_SIZE);
        final int blockY = (int) Math.floor(unproject.y / Block.BLOCK_SIZE);

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            getWorld().setBlock(blockX, blockY, null);
        }
        else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            getWorld().setBlock(blockX, blockY, selected);
        }

        int WVertical = Gdx.input.isKeyPressed(W) ? 1 : 0;
        int SVertical = Gdx.input.isKeyPressed(S) ? -1 : 0;
        int AHorizontal = Gdx.input.isKeyPressed(A) ? 1 : 0;
        int DHorizontal = Gdx.input.isKeyPressed(D) ? -1 : 0;
        camera.position.x -= Gdx.graphics.getDeltaTime() * (AHorizontal + DHorizontal) * CHUNK_TEXTURE_WIDTH * camera.zoom;
        camera.position.y += Gdx.graphics.getDeltaTime() * (WVertical + SVertical) * CHUNK_TEXTURE_HEIGHT * camera.zoom;

        worldRender.update();
    }
}
