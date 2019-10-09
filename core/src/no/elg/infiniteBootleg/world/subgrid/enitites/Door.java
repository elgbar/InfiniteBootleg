package no.elg.infiniteBootleg.world.subgrid.enitites;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.*;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

/**
 * @author Elg
 */
public class Door extends MaterialEntity {

    public static final String OPEN_DOOR_REGION_NAME = "door_open";
    public static final String CLOSED_DOOR_REGION_NAME = "door_closed";

    private TextureRegion openDoorRegion;
    private TextureRegion closedDoorRegion;

    private int open;

    public Door(@NotNull World world, float worldX, float worldY) {
        super(world, worldX, worldY);
        if (Main.renderGraphic) {
            openDoorRegion = Main.inst().getEntityAtlas().findRegion(OPEN_DOOR_REGION_NAME);
            closedDoorRegion = Main.inst().getEntityAtlas().findRegion(CLOSED_DOOR_REGION_NAME);
        }
        open = 0;
    }

    @Override
    public Material getMaterial() {
        return Material.DOOR;
    }

    @Override
    public void contact(@NotNull ContactType type, @NotNull Contact contact) {
        if (type == ContactType.BEGIN_CONTACT) { open++; }
        if (type == ContactType.END_CONTACT) { open--; }
    }

    @NotNull
    @Override
    protected BodyDef createBodyDef(float worldX, float worldY) {
        BodyDef def = super.createBodyDef(worldX, worldY);
        def.type = BodyDef.BodyType.StaticBody;
        return def;
    }

    @Override
    protected void createFixture(@NotNull Body body) {
        PolygonShape box = new PolygonShape();
        box.setAsBox(getHalfBox2dWidth(), getHalfBox2dHeight());
        Fixture fix = body.createFixture(box, 0.0f);
        fix.setFilterData(World.BLOCK_ENTITY_FILTER);
        fix.setSensor(true);
        box.dispose();
    }

    @Override
    public TextureRegion getTextureRegion() {
        return open == 0 ? openDoorRegion : closedDoorRegion;
    }

    @Override
    public int getWidth() {
        return BLOCK_SIZE;
    }

    @Override
    public int getHeight() {
        return 2 * BLOCK_SIZE;
    }
}
