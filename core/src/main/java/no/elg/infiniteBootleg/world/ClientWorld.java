package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.render.ClientWorldRender;
import org.jetbrains.annotations.NotNull;

/** @author Elg */
public class ClientWorld extends World {

  private final ClientWorldRender render;
  private final WorldInputHandler input;

  {
    render = new ClientWorldRender(this);
    input = new WorldInputHandler(render);
  }

  public ClientWorld(ProtoWorld.@NotNull World protoWorld) {
    super(protoWorld);
  }

  public ClientWorld(@NotNull ChunkGenerator generator, long seed, @NotNull String worldName) {
    super(generator, seed, worldName);
  }

  @Override
  public @NotNull ClientWorldRender getRender() {
    return render;
  }

  @NotNull
  public WorldInputHandler getInput() {
    return input;
  }

  @Override
  public void reload(boolean force) {
    super.reload(force);
    render.reload();
  }

  @Override
  public void resize(int width, int height) {
    render.resize(width, height);
  }

  @Override
  public void dispose() {
    super.dispose();
    render.dispose();
    input.dispose();
  }
}
