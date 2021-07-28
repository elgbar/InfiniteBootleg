package no.elg.infiniteBootleg.world.subgrid.enitites;

import box2dLight.ConeLight;
import box2dLight.Light;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.input.KeyboardControls;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import no.elg.infiniteBootleg.world.World;
import static no.elg.infiniteBootleg.world.render.WorldRender.LIGHT_LOCK;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;


public class Player extends LivingEntity {

    public static final String PLAYER_REGION_NAME = "player";

    private final TextureRegion region;
    private final EntityControls controls;
    private final Light torchLight;

    public Player(@NotNull World world) {
        super(world, 0, 0);
        region = new TextureRegion(Main.inst().getEntityAtlas().findRegion(PLAYER_REGION_NAME));
        controls = new KeyboardControls(world.getRender(), this);
        synchronized (LIGHT_LOCK) {
            torchLight = new ConeLight(Main.inst().getWorld().getRender().getRayHandler(), 64, Color.TAN, 48, 5, 5, 0, 30);
            torchLight.setStaticLight(true);
        }
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Override
    public int getWidth() {
        return 2 * BLOCK_SIZE - 1;
    }

    @Override
    public int getHeight() {
        return 4 * BLOCK_SIZE - 1;
    }

    @Override
    public void dispose() {
        super.dispose();
        controls.dispose();
        synchronized (LIGHT_LOCK) {
            torchLight.remove();
        }
    }

    @Override
    public void tick() {
        super.tick();
        
        Vector2 pos = getPosition();
        float angle = Main.inst().getMouse().cpy().sub(pos).angleDeg();
        synchronized (LIGHT_LOCK) {
            torchLight.setDirection(angle);
            torchLight.setPosition(pos);
        }
    }

    @Override
    @NotNull
    public EntityControls getControls() {
        return controls;
    }
}
