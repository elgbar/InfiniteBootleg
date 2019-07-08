package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Contact;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.box2d.ContactHandler;
import no.elg.infiniteBootleg.world.subgrid.box2d.ContactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

public class FallingBlock extends Entity implements ContactHandler {

    private final Material material;
    private final TextureRegion region;

    private boolean crashed;

    public FallingBlock(@NotNull World world, float worldX, float worldY, @NotNull Material material) {
        super(world, worldX, worldY);
        this.material = material;
        region = new TextureRegion(material.getTextureRegion());
        region.flip(true, false);
        region.setRegionWidth(getWidth());
        region.setRegionHeight(getHeight());
    }


    @Override
    public void update() {
        super.update();
        //Unload this entity if it entered an unloaded chunk
        //TODO do not _remove_ this entity, just save it to the unloaded chunk
        int chunkX = CoordUtil.worldToChunk(getBlockX());
        int chunkY = CoordUtil.worldToChunk(getBlockY());
        if (!getWorld().isChunkLoaded(chunkX, chunkY)) {
            Gdx.app.postRunnable(() -> getWorld().removeEntity(this));
        }
    }

    @Override
    public void contact(@NotNull ContactType type, @NotNull Contact contact, @Nullable Object data) {
        if (!crashed && type == ContactType.BEGIN_CONTACT && contact.getFixtureB().getBody() == getBody()) {
            crashed = true;
            Gdx.app.postRunnable(() -> {
                int newX = getBlockX();
                int newY = getBlockY();

                if (getWorld().isAir(newX, newY)) {
                    getWorld().setBlock(newX, newY, material, true);
                }
                getWorld().removeEntity(this);
            });
        }
    }


    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Override
    public int getWidth() {
        return BLOCK_SIZE - 1;
    }

    @Override
    public int getHeight() {
        return BLOCK_SIZE - 1;
    }
}
