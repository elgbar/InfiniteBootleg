package no.elg.infiniteBootleg.client.world.render.texture

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.client.world.textureRegion
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion.Companion.disallowedRotation
import java.util.EnumMap

object TextureNeighbor {
  private val logger = KotlinLogging.logger {}

  private val textureNeighbors = mutableMapOf<Material, MutableList<NeighborTexture>>()

  fun getTexture(center: Material, neighbors: EnumMap<Direction, Material>): RotatableTextureRegion? =
    textureNeighbors[center]?.firstOrNull { it.matches(neighbors) }?.textureRegion ?: center.textureRegion

  fun generateNeighborMap(textureAtlasRegions: GdxArray<TextureAtlas.AtlasRegion>) {
    for (region: TextureAtlas.AtlasRegion in textureAtlasRegions) {
      val split = region.name.split('-')
      // If the size is even or only one, then it is not a neighbor texture
      if (split.size % 2 == 0 || split.size == 1) continue

      val centerMaterial = Material.valueOf(split[0].uppercase())
      val wantedNeighbors = split.drop(1).chunked(2).mapTo(mutableSetOf()) { (material, direction) ->
        Direction.valueOf(direction.uppercase()) to Material.valueOf(material.uppercase())
      }

      val neighborTextureSet = textureNeighbors.computeIfAbsent(centerMaterial) { mutableListOf() }
      neighborTextureSet += NeighborTexture(region.disallowedRotation(region.name), wantedNeighbors)
    }
    // We want the texture neighbors with the most directions to be first
    for (textureNeighbor in textureNeighbors.values) {
      textureNeighbor.sortByDescending(NeighborTexture::directions)
    }
    logger.debug { "Generated ${textureNeighbors.map { it.value.size }.sum()} neighbor textures" }
  }

  data class NeighborTexture(
    val textureRegion: RotatableTextureRegion,
    val neighbor: Set<Pair<Direction, Material>>
  ) {
    val directions: Int = neighbor.size
    fun matches(neighbors: EnumMap<Direction, Material>) = neighbor.all { (dir, mat) -> neighbors[dir] == mat }
  }
}
