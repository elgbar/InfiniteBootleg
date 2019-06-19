package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.files.FileHandle;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class ChunkLoader {

    private final World world;
    private final ChunkGenerator generator;

    public ChunkLoader(@NotNull World world, @NotNull ChunkGenerator generator) {
        this.world = world;
        this.generator = generator;
    }

    /**
     * @param chunkLoc
     *     The location of the chunk
     *
     * @return If a chunk at the given location exists
     */
    public boolean savedChunk(@NotNull Location chunkLoc) {
        FileHandle chunkFile = Chunk.geChunkFile(world, chunkLoc);
        return chunkFile != null && chunkFile.exists();
    }

    public Chunk load(@NotNull Location chunkLoc) {
        if (savedChunk(chunkLoc)) {
            Chunk chunk = new Chunk(world, chunkLoc);
            //noinspection ConstantConditions
            chunk.assemble(chunk.getChunkFile().readBytes());
            return chunk;
        }
        else {
            return generator.generate(world, chunkLoc);
        }
    }

    public void save(@NotNull Chunk chunk) {
        if (chunk.isModified()) {
            //only save if modified
            FileHandle fh = chunk.getChunkFile();
            if (fh == null) { return; }
            fh.writeBytes(chunk.disassemble(), false);
        }
    }
}
