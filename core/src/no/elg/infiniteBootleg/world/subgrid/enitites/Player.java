package no.elg.infiniteBootleg.world.subgrid.enitites;

import box2dLight.PointLight;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.input.KeyboardControls;
import no.elg.infiniteBootleg.util.PointLightPool;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

public class Player extends LivingEntity {

    public static final String PLAYER_REGION_NAME = "player";

    private final TextureRegion region;
    private final EntityControls controls;
    private final PointLight light;

    public Player(@NotNull World world) {
        super(world, 0, 0);
        region = new TextureRegion(Main.inst().getEntityAtlas().findRegion(PLAYER_REGION_NAME));
        controls = new KeyboardControls(world.getRender(), this);

        light = PointLightPool.inst.obtain();
        light.setStaticLight(false);
        light.attachToBody(getBody());
        light.setColor(1, 1, 1, 0.1f);
        light.setDistance(2.5f);
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
    public int getWidth() {
        return BLOCK_SIZE - 3;
    }

    @Override
    public int getHeight() {
        return 2 * BLOCK_SIZE - BLOCK_SIZE / 2;
    }

    @Override
    @NotNull
    public EntityControls getControls() {
        return controls;
    }

    @Override
    public void dispose() {
        super.dispose();
        light.dispose();
    }
}
