package no.elg.infiniteBootleg.inventory.container

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.scenes.scene2d.Actor
import no.elg.infiniteBootleg.events.ContainerEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.inventory.container.impl.AutoSortedContainer
import no.elg.infiniteBootleg.inventory.container.impl.ContainerImpl
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.items.Item.Companion.DEFAULT_MAX_STOCK
import no.elg.infiniteBootleg.items.Item.Companion.asProto
import no.elg.infiniteBootleg.items.Item.Companion.fromProto
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ContainerKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.container
import no.elg.infiniteBootleg.protobuf.itemOrNull
import no.elg.infiniteBootleg.world.ContainerElement
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.clientWorld
import java.util.concurrent.CompletableFuture
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Container as ProtoContainer

/**
 * An interface for things that holds items.
 *
 *
 * If the container can only hold valid stacks (checked with [Item.isValid]) is up to the
 * implementation.
 *
 *
 * However for [.getValid] the returned Item <bold>MUST</bold> be a valid stacks (or
 * null) if this is not the desired use [.get] for invalid stacks
 *
 *
 * TODO implement a chest (that can be opened, somehow) TODO let some entities have inventories
 *
 * @author kheba
 */
interface Container : Iterable<IndexedItem> {
  /**
   * @return The name of the container
   */
  /**
   * @param name The new name of the container
   */
  var name: String

  /**
   * @return How many slots this container holds
   */
  val size: Int

  val validOnly: Boolean get() = true

  /**
   * @return The first empty slot in the container, return a negative number if none is found
   */
  fun firstEmpty(): Int

  /**
   * @param tileType The tileType to match against
   * @return The index of the first tile where there are at least [Item.getStock] tiles. If
   * the input is null, this method is identical to [.firstEmpty]
   */
  fun first(containerElement: ContainerElement?): Int {
    if (containerElement == null) {
      return firstEmpty()
    }
    return first(containerElement.toItem(DEFAULT_MAX_STOCK, 0u))
  }

  /**
   * x
   *
   * @param Item The Item to match against
   * @return The index of the first tile where there are at least `Item.getStock` tiles,
   * return negative number if not found. If the input is null, this method is identical to
   * [.firstEmpty]
   */
  fun first(item: Item?): Int

  /**
   * Add an item to the container
   *
   * @return How many of the given tiletype not added
   * @throws IllegalArgumentException if `ContainerElement` is `null` or amount is less
   * than zero
   */
  fun add(tileType: ContainerElement, amount: UInt): UInt

  /**
   * Add one or more items to the container
   *
   * @param Item What to add
   */
  fun add(vararg items: Item): List<Item?>? {
    return add(items.toList())
  }

  /**
   * Add one or more items to the container
   *
   * @param items What to add
   * @return A list of all tiles not added, the returned stack might not be valid.
   * @throws IllegalArgumentException if one of the `Item`s is `null`
   */
  fun add(items: List<Item>): List<Item?>? {
    val collector: MutableMap<ContainerElement, UInt> = HashMap()

    // tally up how many we got of each type
    for (stack in items) {
      //            if (stack == null) { continue; }
      collector[stack.element] = collector.getOrDefault(stack.element, 0u) + stack.stock
    }

    val notAdded: MutableList<Item?> = ArrayList()

    // then add them all type by type
    for ((element, stock) in collector) {
      val failedToAdd = add(element, stock)
      // if any tiles failed to be added, add them here
      if (failedToAdd > 0u) {
        notAdded.add(element.toItem(DEFAULT_MAX_STOCK, failedToAdd))
      }
    }
    return notAdded
  }

  /** Remove all tile stacks with the given tile type  */
  fun removeAll(tileType: ContainerElement)

  /**
   * Remove `amount` of the given tile type
   *
   * @param amount How many to remove
   * @param tileType What tile to remove
   * @return How many tiles that were not removed
   */
  fun remove(tileType: ContainerElement, amount: UInt): UInt

  /**
   * Remove tile stacks in the container that match the given element
   *
   * @param Item The tile validate to remove
   * @throws IllegalArgumentException if one of the `Item`s is `null`
   */
  fun remove(item: Item)

  /**
   * Remove tile at index
   *
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to [size]
   */
  fun remove(index: Int)

  /** Clear the container of all tile stacks  */
  fun clear()

  /**
   * @param item The item to check for
   * @return False if `Item` is null, true if this container has the given `Item`
   */
  fun contains(item: Item?): Boolean

  /**
   * @return If this container has the given `Item`, `false` is returned if [ContainerElement] is
   * `null` or if size is less than 0
   */
  fun contains(tileType: ContainerElement?, size: Int): Boolean {
    if (tileType == null || size < 0) {
      return false
    }
    return contains(tileType.toItem(DEFAULT_MAX_STOCK, size.toUInt()))
  }

  /**
   * @return If the container has an item of this ContainerElement
   */
  fun containsAny(tileType: ContainerElement?): Boolean

  /**
   * This method returns the [Item] as is at the given location, there will be no check
   *
   * @return The `Item` at the given location, `null` if there is nothing there
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to [size]
   */
  operator fun get(index: Int): Item?

  /**
   * @param index The index of the item to get
   * @return An array of valid stacks (will pass [Item.isValid]) from the tile stack at the
   * given location
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to [size]
   */
  @Deprecated("")
  fun getValid(index: Int): Array<Item?>

  /**
   * Overwrite a the given `Item` at `index`. If the given tile validate is `null`
   * it is the same as calling [.remove].
   *
   * @param index The index to place the `Item` at
   * @param Item The Item to put at `index`
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to [size]
   */
  fun put(index: Int, item: Item?)

  fun swap(index1: Int, index2: Int)

  /**
   * @return The underlying array of the container
   */
  val content: Array<Item?>

  operator fun plusAssign(item: Item) {
    add(item)
  }

  operator fun minusAssign(item: Item) {
    remove(item)
  }

  val type: ProtoContainer.Type

  companion object : ProtoConverter<Container, ProtoContainer> {

    fun getContainerActor(entity: Entity, container: Container?): CompletableFuture<Actor>? {
      return entity.clientWorld?.render?.getContainerActor(container ?: return null)
    }

    fun Container?.isOpen(entity: Entity): Boolean = this?.let { container -> entity.clientWorld?.render?.isContainerOpen(container) } ?: false

    private fun Container.containerActorOpen(actor: Actor) {
      if (!actor.isVisible) {
        EventManager.dispatchEvent(ContainerEvent.Opening(this))
        actor.isVisible = true
      }
    }

    private fun Container.containerActorClose(actor: Actor) {
      if (actor.isVisible) {
        actor.isVisible = false
        EventManager.dispatchEvent(ContainerEvent.Closed(this))
      }
    }

    fun Container.open(entity: Entity) {
      getContainerActor(entity, this)?.thenApply { containerActorOpen(it) }
    }

    fun Container.close(entity: Entity) {
      getContainerActor(entity, this)?.thenApply { containerActorClose(it) }
    }

    fun Container.toggle(entity: Entity) {
      getContainerActor(entity, this)?.thenApply {
        if (it.isVisible) {
          containerActorClose(it)
        } else {
          containerActorOpen(it)
        }
      }
    }

    override fun ProtoWorld.Container.fromProto(): Container =
      when (type) {
        ProtoWorld.Container.Type.GENERIC -> ContainerImpl(maxSize, name)
        ProtoWorld.Container.Type.AUTO_SORTED -> AutoSortedContainer(maxSize, name)
        else -> ContainerImpl(maxSize, name).also { Main.logger().error("Unknown container type $type") }
      }.apply {
        // note: if an index does not exist in the proto, the slot is implicitly empty
        for (indexedItem in itemsList) {
          content[indexedItem.index] = indexedItem.itemOrNull?.fromProto()
        }
      }

    override fun Container.asProto(): ProtoWorld.Container =
      container {
        maxSize = this@asProto.size
        name = this@asProto.name
        type = this@asProto.type
        // Only add items with a value, indices without a value are implicitly null
        items += this@asProto.content.mapIndexed { containerIndex, maybeItem ->
          maybeItem?.let {
            ContainerKt.indexedItem {
              index = containerIndex
              item = it.asProto()
            }
          }
        }.filterNotNull()
      }
  }
}
