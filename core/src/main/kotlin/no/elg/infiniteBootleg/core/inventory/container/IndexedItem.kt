package no.elg.infiniteBootleg.core.inventory.container

import no.elg.infiniteBootleg.core.items.Item

/**
 * A slot in a container
 *
 * @author kheba
 */
data class IndexedItem(val index: Int, val content: Item?)
