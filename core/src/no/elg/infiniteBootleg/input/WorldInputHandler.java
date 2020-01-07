package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Updatable;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.screen.HUDRenderer.HUDModus;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.badlogic.gdx.Input.Keys.*;

/**
 * @author Elg
 */
public class WorldInputHandler extends InputAdapter implements Disposable, Updatable {

    private final static int CAMERA_SPEED = 100 * Block.BLOCK_SIZE;
    public static final float SCROLL_SPEED = 0.25f;
    public static final float CAMERA_LERP = 2.5f;

    private final WorldRender worldRender;
    private Entity following;
    private boolean lockedOn;

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

        OrthographicCamera camera = worldRender.getCamera();

        float vertical = (Gdx.input.isKeyPressed(UP) ? 1 : 0) - (Gdx.input.isKeyPressed(DOWN) ? 1 : 0);
        float horizontal = (Gdx.input.isKeyPressed(LEFT) ? 1 : 0) - (Gdx.input.isKeyPressed(RIGHT) ? 1 : 0);

        if (vertical != 0 || horizontal != 0) {
            camera.position.x -= Gdx.graphics.getDeltaTime() * horizontal * CAMERA_SPEED * camera.zoom;
            camera.position.y += Gdx.graphics.getDeltaTime() * vertical * CAMERA_SPEED * camera.zoom;
            lockedOn = false;
            worldRender.update();
        }
        else if (following != null && lockedOn) {
            Vector2 pos = following.getBody().getPosition();

            camera.position.x +=
                (pos.x * Block.BLOCK_SIZE - camera.position.x) * CAMERA_LERP * Gdx.graphics.getDeltaTime();
            camera.position.y +=
                (pos.y * Block.BLOCK_SIZE - camera.position.y) * CAMERA_LERP * Gdx.graphics.getDeltaTime();

            worldRender.update();
        }
    }


    public Entity getFollowing() {
        return following;
    }

    public void setFollowing(@Nullable Entity following) {
        this.following = following;
        lockedOn = true;
    }

    /**
     * Only applies if {@link #getFollowing()} is not {@code null}
     */
    public boolean isLockedOn() {
        return lockedOn;
    }

    /**
     * Only applies if {@link #getFollowing()} is not {@code null}
     */
    public void setLockedOn(boolean lockedOn) {
        this.lockedOn = lockedOn;
    }
}
