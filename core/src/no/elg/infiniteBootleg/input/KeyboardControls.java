package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.physics.box2d.Body;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.badlogic.gdx.Input.Keys.*;
import static no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK;

/**
 * Control scheme where the user moves the player around with a keyboard
 *
 * @author Elg
 */
public class KeyboardControls extends AbstractEntityControls {

    public static final float JUMP_VERTICAL_IMPULSE = 0.5f;
    public static final float HORIZONTAL_IMPULSE = 1f;

    private Material selected;
    //if objects can be placed on non-air blocks
    public boolean replacePlacement;
    private float breakBrushSize = 2f;
    private float placeBrushSize = 1f;

    public KeyboardControls(@NotNull WorldRender worldRender, @NotNull LivingEntity entity) {
        super(worldRender, entity);
        selected = Material.STONE;
    }

    @Override
    public void update() {
        if (Main.inst().getConsole().isVisible()) {
            return;
        }

        boolean update = false;
        int blockX = Main.inst().getMouseBlockX();
        int blockY = Main.inst().getMouseBlockY();

        float rawX = Main.inst().getMouseX();
        float rawY = Main.inst().getMouseY();
        World world = getWorldRender().getWorld();

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {

            if (breakBrushSize <= 1) {
                world.remove(blockX, blockY, true);
            }
            else {
                for (Block block : world.getBlocksWithin(rawX, rawY, breakBrushSize, true)) {
                    world.remove(block.getWorldX(), block.getWorldY(), true);
                }
            }
            update = true;
        }
        else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || Gdx.input.isKeyJustPressed(Q)) {

            if (replacePlacement || world.isAir(blockX, blockY)) {
                if (placeBrushSize <= 1) {
                    selected.create(world, blockX, blockY);
                }
                else {
                    for (Block block : world.getBlocksWithin(rawX, rawY, placeBrushSize, false)) {
                        selected.create(world, block.getWorldX(), block.getWorldY());
                    }
                }
                update = true;
            }
        }

        Entity entity = getControlled();

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            //teleport the player to the (last) location of the mouse
            entity.teleport(Main.inst().getMouseX(), Main.inst().getMouseY(), true);
            WorldInputHandler input = world.getInput();
            if (input != null) {
                input.setFollowing(entity);
                input.setLockedOn(true);
            }
        }
        else {
            int shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? 2 : 1;
            if (entity.isOnGround() && !entity.isFlying()) {

                if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                    applyImpulse(0, JUMP_VERTICAL_IMPULSE);
                }
            }
            else if (entity.isFlying()) {
                if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                    applyImpulse(0, HORIZONTAL_IMPULSE * shift);
                }
                if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                    applyImpulse(0, -HORIZONTAL_IMPULSE * shift);
                }
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                applyImpulse(-HORIZONTAL_IMPULSE * shift, 0);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                applyImpulse(HORIZONTAL_IMPULSE * shift, 0);
            }
        }

        if (update) {
            getWorldRender().update();
        }
    }

    private void applyImpulse(float impulseX, float impulseY) {
        synchronized (BOX2D_LOCK) {
            Body body = getControlled().getBody();
            body.applyLinearImpulse(impulseX, impulseY, body.getPosition().x, body.getPosition().y, true);
        }
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
    @Nullable
    public Material getSelected() {
        return selected;
    }

    @Override
    public void setSelected(@Nullable Material selected) {
        this.selected = selected;
    }

    @Override
    public float getBreakBrushSize() {
        return breakBrushSize;
    }

    @Override
    public void setBreakBrushSize(float breakBrushSize) {
        this.breakBrushSize = breakBrushSize;
    }

    @Override
    public float getPlaceBrushSize() {
        return placeBrushSize;
    }

    @Override
    public void setPlaceBrushSize(float placeBrushSize) {
        this.placeBrushSize = placeBrushSize;
    }
}
