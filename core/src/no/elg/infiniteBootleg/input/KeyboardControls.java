package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.badlogic.gdx.Input.Keys.*;

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
    private float brushSize = 1;

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

            if (brushSize <= 1) {
                world.remove(blockX, blockY, true);
            }
            else {
                for (Block block : world.getBlocksWithin(rawX, rawY, brushSize, true)) {
                    world.remove(block.getWorldX(), block.getWorldY(), true);
                }
            }
            update = true;
        }
        else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || Gdx.input.isKeyJustPressed(Q)) {

            if (replacePlacement || world.isAir(blockX, blockY)) {
                if (brushSize <= 1) {
                    selected.create(world, blockX, blockY);
                }
                else {
                    for (Block block : world.getBlocksWithin(rawX, rawY, brushSize, true)) {
                        selected.create(world, block.getWorldX(), block.getWorldY());
                    }
                }
                update = true;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            //teleport the player to the (last) location of the mouse
            getControlled().teleport(Main.inst().getMouseX(), Main.inst().getMouseY(), true);
            if (world.getInput() != null) {
                world.getInput().setLockedOn(true);
            }

        }
        else {
            int shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? 2 : 1;
            //TODO if shift is held, the impulse should be multiplied with 2
            if (getControlled().isOnGround() && !getControlled().isFlying()) {

                if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                    getControlled().getBody().applyLinearImpulse(0, JUMP_VERTICAL_IMPULSE,
                                                                 getControlled().getPosition().x,
                                                                 getControlled().getPosition().y, true);
                }
            }
            else if (getControlled().isFlying()) {
                if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                    getControlled().getBody().applyLinearImpulse(0, HORIZONTAL_IMPULSE * shift,
                                                                 getControlled().getPosition().x,
                                                                 getControlled().getPosition().y, true);
                }
                if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                    getControlled().getBody().applyLinearImpulse(0, -HORIZONTAL_IMPULSE * shift,
                                                                 getControlled().getPosition().x,
                                                                 getControlled().getPosition().y, true);
                }
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                getControlled().getBody().applyLinearImpulse(-HORIZONTAL_IMPULSE * shift, 0,
                                                             getControlled().getPosition().x,
                                                             getControlled().getPosition().y, true);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                getControlled().getBody().applyLinearImpulse(HORIZONTAL_IMPULSE * shift, 0,
                                                             getControlled().getPosition().x,
                                                             getControlled().getPosition().y, true);
            }
        }

        if (update) {
            getWorldRender().update();
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
    public float getBrushSize() {
        return brushSize;
    }

    @Override
    public void setBrushSize(float brushSize) {
        this.brushSize = brushSize;
    }
}
