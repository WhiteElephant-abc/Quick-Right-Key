package io.github.whiteelephantabc.quickrightkey.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class OpenBackpackSession {
	private var session: Session? = null

	private enum class Phase {
		WAITING_OPEN,
		WAITING_CLOSE,
		WAITING_RESTORE
	}

	private data class Session(
		val type: BoxType,
		val expectedTitle: String,
		val originalSelected: Int,
		val hotbarSlot: Int,
		val backpackSlot: Int,
		var phase: Phase,
		var ticks: Int = 0
	)

	fun reset() {
		session = null
	}

	fun canStart(client: Minecraft): Boolean {
		if (session != null) return false
		val player = client.player ?: return false
		return client.gameMode != null &&
			client.connection != null &&
			client.gui.screen() == null &&
			!player.isSpectator &&
			!player.isUsingItem &&
			player.isAlive &&
			player.containerMenu === player.inventoryMenu
	}

	fun start(client: Minecraft) {
		val player = client.player ?: return
		val target = TargetFinder.find(player) ?: return
		val targetStack = TargetFinder.stackAt(player, target.location)
		if (targetStack.isEmpty || player.cooldowns.isOnCooldown(targetStack)) return

		val originalSelected = player.inventory.selectedSlot
		val expectedTitle = resolveExpectedTitle(target.type, targetStack)

		when (val location = target.location) {
			Location.MainHand -> {
				session = Session(target.type, expectedTitle, originalSelected, -1, -1, Phase.WAITING_OPEN)
				useItem(client, player, InteractionHand.MAIN_HAND)
			}

			Location.OffHand -> {
				session = Session(target.type, expectedTitle, originalSelected, -1, -1, Phase.WAITING_OPEN)
				useItem(client, player, InteractionHand.OFF_HAND)
			}

			is Location.Hotbar -> {
				session = Session(target.type, expectedTitle, originalSelected, -1, -1, Phase.WAITING_OPEN)
				InventoryMover.switchSelected(client, player, location.slot)
				useItem(client, player, InteractionHand.MAIN_HAND)
			}

			is Location.Backpack -> {
				InventoryMover.moveToHotbar(client, player, location.slot, InventoryMover.HOTBAR_TARGET_SLOT)
				session = Session(
					target.type,
					expectedTitle,
					originalSelected,
					InventoryMover.HOTBAR_TARGET_SLOT,
					location.slot,
					Phase.WAITING_OPEN
				)
				InventoryMover.switchSelected(client, player, InventoryMover.HOTBAR_TARGET_SLOT)
				useItem(client, player, InteractionHand.MAIN_HAND)
			}
		}
	}

	fun tick(client: Minecraft) {
		val active = session ?: return
		active.ticks++
		when (active.phase) {
			Phase.WAITING_OPEN -> if (isTargetScreen(client.gui.screen(), active.type, active.expectedTitle)) {
				active.phase = Phase.WAITING_CLOSE
				active.ticks = 0
			} else if (active.ticks > OPEN_TIMEOUT_TICKS) {
				active.phase = Phase.WAITING_RESTORE
				active.ticks = 0
			}

			Phase.WAITING_CLOSE -> if (!isTargetScreen(client.gui.screen(), active.type, active.expectedTitle)) {
				active.phase = Phase.WAITING_RESTORE
				active.ticks = 0
			}

			Phase.WAITING_RESTORE -> if (restore(client, active)) session = null else if (active.ticks > RESTORE_TIMEOUT_TICKS) session = null
		}
	}

	private fun restore(client: Minecraft, active: Session): Boolean {
		val player = client.player ?: return true
		if (client.gui.screen() != null || player.containerMenu !== player.inventoryMenu) return false
		if (active.backpackSlot >= 0 && active.hotbarSlot in 0 until HOTBAR_COUNT) {
			InventoryMover.restoreItem(client, player, active.backpackSlot, active.hotbarSlot)
		}
		InventoryMover.switchSelected(client, player, active.originalSelected)
		return true
	}

	private fun useItem(client: Minecraft, player: Player, hand: InteractionHand) {
		client.gameMode?.useItem(player, hand)
	}

	private fun resolveExpectedTitle(type: BoxType, stack: ItemStack): String = when (type) {
		BoxType.ENDER_CHEST -> Component.translatable("container.enderchest").string
		BoxType.SHULKER -> stack.get(DataComponents.CUSTOM_NAME)?.plainCopy()?.string
			?: Component.translatable("container.shulkerBox").string
	}

	private fun isTargetScreen(screen: Screen?, type: BoxType, expectedTitle: String): Boolean = when (type) {
		BoxType.ENDER_CHEST -> screen is ContainerScreen && screen.title.string == expectedTitle
		BoxType.SHULKER -> screen is ShulkerBoxScreen && screen.title.string == expectedTitle
	}

	private companion object {
		const val HOTBAR_COUNT = 9
		const val OPEN_TIMEOUT_TICKS = 60
		const val RESTORE_TIMEOUT_TICKS = 200
	}
}
