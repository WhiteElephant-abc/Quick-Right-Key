# Quick-Right-Key

A client-side Fabric mod for Minecraft 26.3. Press `B` (rebindable) to ask a server running Quick-Right-Click to open a shulker box or an ender chest from your inventory.

## Behavior

- Target priority: a shulker box named "Backpack" > ender chest > any shulker box.
- Within one priority the search order is: main hand, off hand, hotbar (selected slot skipped), main inventory.
- A target in the main inventory is moved to hotbar slot 9, opened, and moved back when the screen closes.
- If hotbar slot 9 is occupied, the two stacks are swapped and swapped back on restore. If the original backpack slot is occupied by another stack, the box stays in hotbar slot 9.
- The shortcut is inactive while any screen is open.
- The server must have Quick-Right-Click installed; the client only sends the use-item request.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
