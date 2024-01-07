package no.elg.infiniteBootleg.world.world

import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator

class SinglePlayerWorld(generator: ChunkGenerator, seed: Long, worldName: String, forceTransient: Boolean = false) : ClientWorld(generator, seed, worldName, forceTransient)
