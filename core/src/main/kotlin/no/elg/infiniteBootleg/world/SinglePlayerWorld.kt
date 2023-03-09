package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.world.generator.ChunkGenerator

class SinglePlayerWorld(generator: ChunkGenerator, seed: Long, worldName: String) : ClientWorld(generator, seed, worldName)
