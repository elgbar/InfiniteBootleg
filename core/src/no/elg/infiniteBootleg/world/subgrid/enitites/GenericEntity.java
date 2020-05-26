package no.elg.infiniteBootleg.world.subgrid.enitites;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.Removable;
import org.jetbrains.annotations.NotNull;

public class GenericEntity extends Entity implements Removable {

    private final int width;
    private final int height;

    public GenericEntity(@NotNull World world, float worldX, float worldY) {
        this(world, worldX, worldY, 1, 1);
    }

    public GenericEntity(@NotNull World world, float worldX, float worldY, int width, int height) {
        this(world, worldX, worldY, width, height, World.ENTITY_FILTER);
    }

    public GenericEntity(@NotNull World world, float worldX, float worldY, int width, int height,
                         @NotNull Filter filter) {
        //Cannot center as the width and height will be 0 anyway
        super(world, worldX, worldY, false);

        this.width = width * Block.BLOCK_SIZE;
        this.height = height * Block.BLOCK_SIZE;

        //make sure the width and height is correct
        synchronized (WorldRender.BOX2D_LOCK) {
            synchronized (this) {
                for (Fixture fixture : getBody().getFixtureList()) {
                    getBody().destroyFixture(fixture);
                }
            }
            createFixture(getBody());
            setFilter(filter);
        }
    }

    @Override
    protected @NotNull BodyDef createBodyDef(float worldX, float worldY) {
        BodyDef def = super.createBodyDef(worldX, worldY);
        def.type = BodyDef.BodyType.StaticBody;
        return def;
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
