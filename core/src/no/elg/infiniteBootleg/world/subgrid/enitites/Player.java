package no.elg.infiniteBootleg.world.subgrid.enitites;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

public class Player extends LivingEntity {

    public static final String PLAYER_REGION_NAME = "player";

    public static final float JUMP_VERTICAL_IMPULSE = 0.5f;
    public static final float HORIZONTAL_IMPULSE = 1f;
    private final TextureRegion region;

    public Player(@NotNull World world) {
        super(world, 0, 0);
        region = new TextureRegion(Main.inst().getEntityAtlas().findRegion(PLAYER_REGION_NAME));
//        setFlying(true);
    }

    @Override
    protected void createFixture(@NotNull Body body) {
        PolygonShape box = new PolygonShape();
        box.setAsBox(getHalfBox2dWidth(), getHalfBox2dHeight());
        Fixture fix = body.createFixture(box, 1.0f);
        fix.setFilterData(World.ENTITY_FILTER);
        fix.setFriction(0);
        box.dispose();
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Override
    public void update() {
        super.update();
        if (Main.inst().getConsole().isVisible()) {
            return;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.T)) {
            //teleport the player to the (last) location of the mouse
            getBody().setTransform(Main.inst().getMouseX(), Main.inst().getMouseY(), 0);
            getBody().setAngularVelocity(0);
            getBody().setLinearVelocity(0, 0);
            getBody().setAwake(true);
        }
        else {
            int shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? 2 : 1;
            //TODO if shift is held, the impulse should be multiplied with 2
            if (isOnGround() && !isFlying()) {

                if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                    getBody().applyLinearImpulse(0, JUMP_VERTICAL_IMPULSE, getPosition().x, getPosition().y, true);
                }
            }
            else if (isFlying()) {
                if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                    getBody().applyLinearImpulse(0, HORIZONTAL_IMPULSE * shift, getPosition().x, getPosition().y, true);
                }
                if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                    getBody()
                        .applyLinearImpulse(0, -HORIZONTAL_IMPULSE * shift, getPosition().x, getPosition().y, true);
                }
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                getBody().applyLinearImpulse(-HORIZONTAL_IMPULSE * shift, 0, getPosition().x, getPosition().y, true);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                getBody().applyLinearImpulse(HORIZONTAL_IMPULSE * shift, 0, getPosition().x, getPosition().y, true);
            }
        }
    }

    @Override
    public int getWidth() {
        return BLOCK_SIZE - 3;
    }

    @Override
    public int getHeight() {
        return 2 * BLOCK_SIZE - BLOCK_SIZE / 2;
    }
}
