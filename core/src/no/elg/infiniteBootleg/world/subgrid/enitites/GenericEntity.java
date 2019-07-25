package no.elg.infiniteBootleg.world.subgrid.enitites;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.Removable;
import org.jetbrains.annotations.NotNull;

public class GenericEntity extends Entity implements Removable {

    private final int width;
    private final int height;

    public GenericEntity(@NotNull World world, float worldX, float worldY, int width, int height) {
        super(world, worldX, worldY);
        this.width = width * Block.BLOCK_SIZE;
        this.height = height * Block.BLOCK_SIZE;

        //make sure the width and height is correct
        for (Fixture fixture : getBody().getFixtureList()) {
            getBody().destroyFixture(fixture);
        }
        createFixture(getBody());
    }

    @Override
    protected @NotNull BodyDef createBodyDef(float worldX, float worldY) {
        BodyDef def = super.createBodyDef(worldX, worldY);
        def.type = BodyDef.BodyType.StaticBody;
        return def;
    }

    @Override
    protected boolean isInvalidSpawn() {
        return false;
    }

    @Override
    public TextureRegion getTextureRegion() {
        return null;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
