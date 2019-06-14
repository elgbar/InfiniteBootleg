package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.files.FileHandle;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

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
        FileHandle chunkFile = chunkFile(chunkLoc);
        return chunkFile != null && chunkFile.exists();
    }

    @Nullable
    public FileHandle chunkFile(@NotNull Location chunkLoc) {
        FileHandle worldFile = world.worldFolder();
        if (worldFile == null) { return null; }
        return worldFile.child(World.CHUNK_FOLDER + File.separator + chunkLoc.x + File.separator + chunkLoc.y);
    }

    public Chunk load(@NotNull Location chunkLoc) {
        if (savedChunk(chunkLoc)) {
            Chunk chunk = new Chunk(world, chunkLoc);
            //noinspection ConstantConditions
            chunk.assemble(chunkFile(chunkLoc).readBytes());
            return chunk;
        }
        else {
            return generator.generate(world, chunkLoc);
        }
    }

    public void save(@NotNull Chunk chunk) {
        if (chunk.isModified()) {
            //only save if modified
            FileHandle fh = chunkFile(chunk.getLocation());
            if (fh == null) { return; }
            fh.writeBytes(chunk.disassemble(), false);
        }
    }
}
