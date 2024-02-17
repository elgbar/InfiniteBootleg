package no.elg.infiniteBootleg.inventory.container

import no.elg.infiniteBootleg.items.Item

/**
 * A slot in a container
 *
 * @author kheba
 */
data class IndexedItem(val index: Int, val content: Item?)
