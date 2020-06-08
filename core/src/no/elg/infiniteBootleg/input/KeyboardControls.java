package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
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

    public static final float JUMP_VERTICAL_IMPULSE = .5f;
    public static final float HORIZONTAL_IMPULSE = .15f;
    public static final int EDIT_TICK_DELAY = 1; //delay in ticks between allowing to place/break blocks

    private Material selected;
    //if objects can be placed on non-air blocks
    public boolean replacePlacement;
    private float breakBrushSize = 2f;
    private float placeBrushSize = 1f;
    private long lastEditTick;

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
            lastEditTick = world.getTick();
            update = true;
        }
        else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || Gdx.input.isKeyJustPressed(Q)) {

            if (placeBrushSize <= 1) {
                update = selected.create(world, blockX, blockY);
            }
            else {
                for (Block block : world.getBlocksWithin(rawX, rawY, placeBrushSize, false)) {
                    update |= selected.create(world, block.getWorldX(), block.getWorldY());
                }
            }
            if (update) {
                lastEditTick = world.getTick();
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
            boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

            if (entity.isFlying()) {

                float modifier = shift ? 2f : 1f;

                if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                    setVel(0, HORIZONTAL_IMPULSE * modifier);
                }
                if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                    setVel(0, -HORIZONTAL_IMPULSE * modifier);
                }


                if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                    setVel(-HORIZONTAL_IMPULSE * modifier, 0);
                }
                if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                    setVel(HORIZONTAL_IMPULSE * modifier, 0);
                }
            }
            else {
                if (entity.isOnGround()) {
                    if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                        setVel(0, JUMP_VERTICAL_IMPULSE);
                    }

                }
                float modifier = (shift ? 2 : 1);

                if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                    setVel(-HORIZONTAL_IMPULSE * modifier, 0);
                }
                if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                    setVel(HORIZONTAL_IMPULSE * modifier, 0);
                }
            }
        }

        if (update) {
            getWorldRender().update();
        }
    }

    private void setVel(float velX, float velY) {
        if (lastEditTick + EDIT_TICK_DELAY <= getWorldRender().getWorld().getTick()) {
            synchronized (BOX2D_LOCK) {
                System.out.println("updating tick (tick " + lastEditTick + ")");
                Body body = getControlled().getBody();
                Vector2 vel = body.getLinearVelocity();
                Vector2 pos = body.getPosition();

                body.applyLinearImpulse(velX == 0 ? vel.x : velX, velY == 0 ? vel.y : velY, pos.x, pos.y, true);
//            body.setLinearVelocity(velX == 0 ? vel.x : velX, velY == 0 ? vel.y : velY);
//            body.setAwake(true);
            }
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
