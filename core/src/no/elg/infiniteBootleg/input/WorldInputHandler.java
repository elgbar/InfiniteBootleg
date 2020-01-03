package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.HUDRenderer;
import no.elg.infiniteBootleg.world.render.HUDRenderer.HUDModus;
import no.elg.infiniteBootleg.world.render.Updatable;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.jetbrains.annotations.NotNull;

import static com.badlogic.gdx.Input.Keys.*;

/**
 * @author Elg
 */
public class WorldInputHandler extends InputAdapter implements Disposable, Updatable {

    private final static int CAM_SPEED = 100 * Block.BLOCK_SIZE;
    public static final float SCROLL_SPEED = 0.25f;

    private final WorldRender worldRender;

    public WorldInputHandler(@NotNull WorldRender world) {
        worldRender = world;
        Main.getInputMultiplexer().addProcessor(this);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (Main.inst().getConsole().isVisible()) {
            return false;
        }
        switch (keycode) {
            case F3:
                HUDRenderer hud = Main.inst().getHud();
                if (hud.getModus() == HUDModus.DEBUG) {
                    hud.setModus(HUDModus.NORMAL);
                }
                else {
                    hud.setModus(HUDModus.DEBUG);
                }
                break;
            case F5:
                Main.inst().getWorld().save();
                Main.logger().log("World", "World saved");
                break;
            case F9:
                Main.inst().getWorld().load();
                Main.logger().log("World", "World reloaded last save");
                Main.inst().getWorld().unloadChunks(true, false);
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
        OrthographicCamera camera = worldRender.getCamera();
        camera.zoom += amount * SCROLL_SPEED * camera.zoom;
        if (camera.zoom < WorldRender.MIN_ZOOM) {
            camera.zoom = WorldRender.MIN_ZOOM;
        }
        worldRender.update();
        return true;
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

        int vertical = Gdx.input.isKeyPressed(W) ? 1 : Gdx.input.isKeyPressed(S) ? -1 : 0;
        int horizontal = Gdx.input.isKeyPressed(A) ? 1 : Gdx.input.isKeyPressed(D) ? -1 : 0;
        OrthographicCamera camera = worldRender.getCamera();
        camera.position.x -= Gdx.graphics.getDeltaTime() * horizontal * CAM_SPEED * camera.zoom;
        camera.position.y += Gdx.graphics.getDeltaTime() * vertical * CAM_SPEED * camera.zoom;

        if (vertical != 0 || horizontal != 0) {
            worldRender.update();
        }
    }
}
