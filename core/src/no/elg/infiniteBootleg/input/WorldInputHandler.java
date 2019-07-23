package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.jetbrains.annotations.NotNull;

import static com.badlogic.gdx.Input.Keys.*;

/**
 * @author Elg
 */
public class WorldInputHandler extends InputAdapter implements Disposable, Updatable, Resizable {

    private final static int CAM_SPEED = 100 * Block.BLOCK_SIZE;

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
        if (Main.inst().getConsole().isVisible()) {
            return false;
        }
        switch (keycode) {
            case NUM_0:
            case NUMPAD_0:
                selected = Material.values()[0];
                break;
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
            case NUM_7:
            case NUMPAD_7:
                selected = Material.values()[7];
                break;
            case NUM_8:
            case NUMPAD_8:
                selected = Material.values()[8];
                break;
            case NUM_9:
            case NUMPAD_9:
                selected = Material.values()[9];
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        if (Main.inst().getConsole().isVisible()) {
            return false;
        }
        camera.zoom += amount * 0.05f * camera.zoom;
        if (camera.zoom < WorldRender.MIN_ZOOM) {
            camera.zoom = WorldRender.MIN_ZOOM;
        }
        worldRender.update();
        return true;
    }

    @Override
    public void resize(int width, int height) {
        Vector3 old = camera.position.cpy();
        camera.setToOrtho(false, width, height);
        camera.position.set(old);
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
        if (Main.inst().getConsole().isVisible()) {
            return;
        }

        boolean update = false;

        int blockX = Main.inst().getMouseBlockX();
        int blockY = Main.inst().getMouseBlockY();
        World world = getWorld();
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            world.remove(blockX, blockY, true);
            update = true;
        }
        else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || Gdx.input.isKeyJustPressed(Q)) {
            selected.create(world, blockX, blockY);
            update = true;
        }

        int vertical = Gdx.input.isKeyPressed(W) ? 1 : Gdx.input.isKeyPressed(S) ? -1 : 0;
        int horizontal = Gdx.input.isKeyPressed(A) ? 1 : Gdx.input.isKeyPressed(D) ? -1 : 0;
        camera.position.x -= Gdx.graphics.getDeltaTime() * horizontal * CAM_SPEED * camera.zoom;
        camera.position.y += Gdx.graphics.getDeltaTime() * vertical * CAM_SPEED * camera.zoom;

        if (update || vertical != 0 || horizontal != 0) {
            worldRender.update();
        }
    }
}
