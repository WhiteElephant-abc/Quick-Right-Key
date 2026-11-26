package io.github.whiteelephantabc.quickrightkey.client

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.EnderChestBlock
import net.minecraft.world.level.block.ShulkerBoxBlock

enum class BoxType {
	SHULKER,
	ENDER_CHEST
}

enum class Priority {
	NAMED_SHULKER,
	ENDER_CHEST,
	ANY_SHULKER
}

sealed interface Location {
	data object MainHand : Location
	data object OffHand : Location
	data class Hotbar(val slot: Int) : Location
	data class Backpack(val slot: Int) : Location
}

data class Target(val type: BoxType, val location: Location)

object TargetFinder {
	private const val HOTBAR_SLOTS = 9
	private const val MAIN_START = 9
	private const val MAIN_END = 35
	private const val BACKPACK_NAME_KEY = "quickrightkey.backpack_name"

	fun find(player: Player): Target? {
		Priority.entries.forEach { priority ->
			scan(player, priority)?.let { return it }
		}
		return null
	}

	private fun scan(player: Player, priority: Priority): Target? {
		match(player.mainHandItem, priority)?.let { return Target(it, Location.MainHand) }
		match(player.offhandItem, priority)?.let { return Target(it, Location.OffHand) }

		val inventory = player.inventory
		(0 until HOTBAR_SLOTS).forEach { slot ->
			if (slot != inventory.selectedSlot) match(inventory.getItem(slot), priority)?.let { return Target(it, Location.Hotbar(slot)) }
		}
		(MAIN_START..MAIN_END).forEach { slot ->
			match(inventory.getItem(slot), priority)?.let { return Target(it, Location.Backpack(slot)) }
		}
		return null
	}

	fun stackAt(player: Player, location: Location): ItemStack = when (location) {
		Location.MainHand -> player.mainHandItem
		Location.OffHand -> player.offhandItem
		is Location.Hotbar -> player.inventory.getItem(location.slot)
		is Location.Backpack -> player.inventory.getItem(location.slot)
	}

	private fun match(stack: ItemStack, priority: Priority): BoxType? {
		if (stack.isEmpty) return null
		val block = (stack.item as? BlockItem)?.block ?: return null
		val type = when (block) {
			is ShulkerBoxBlock -> BoxType.SHULKER
			is EnderChestBlock -> BoxType.ENDER_CHEST
			else -> return null
		}
		return when (priority) {
			Priority.NAMED_SHULKER -> if (type == BoxType.SHULKER && isNamedBackpack(stack)) type else null
			Priority.ENDER_CHEST -> if (type == BoxType.ENDER_CHEST) type else null
			Priority.ANY_SHULKER -> if (type == BoxType.SHULKER) type else null
		}
	}

	private fun isNamedBackpack(stack: ItemStack): Boolean {
		val name: Component = stack.get(DataComponents.CUSTOM_NAME) ?: return false
		return name.plainCopy().string.trim().equals(Component.translatable(BACKPACK_NAME_KEY).string, ignoreCase = true)
	}
}
