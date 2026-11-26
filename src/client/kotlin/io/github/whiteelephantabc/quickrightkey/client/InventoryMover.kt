package io.github.whiteelephantabc.quickrightkey.client

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

object InventoryMover {
	const val HOTBAR_TARGET_SLOT = 8

	fun moveToHotbar(client: Minecraft, player: Player, backpackSlot: Int, hotbarSlot: Int): Boolean {
		if (player.inventoryMenu.getSlot(backpackSlot).item.isEmpty) return false
		clickSlot(client, player, backpackSlot, hotbarSlot, ContainerInput.SWAP)
		return true
	}

	fun restoreItem(client: Minecraft, player: Player, backpackSlot: Int, hotbarSlot: Int, targetItem: Item, swapStack: ItemStack) {
		val boxStack = player.inventory.getItem(hotbarSlot)
		if (boxStack.isEmpty || boxStack.item !== targetItem) return
		val backpackStack = player.inventory.getItem(backpackSlot)
		if (!backpackStack.isEmpty && (swapStack.isEmpty || !ItemStack.isSameItemSameComponents(backpackStack, swapStack))) return
		clickSlot(client, player, backpackSlot, hotbarSlot, ContainerInput.SWAP)
	}

	fun switchSelected(client: Minecraft, player: Player, slot: Int) {
		if (player.inventory.selectedSlot == slot) return
		player.inventory.selectedSlot = slot
		client.connection?.send(ServerboundSetCarriedItemPacket(slot))
	}

	private fun clickSlot(client: Minecraft, player: Player, menuSlot: Int, button: Int, input: ContainerInput) {
		client.gameMode?.handleContainerInput(0, menuSlot, button, input, player)
	}
}
