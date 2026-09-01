package ru.redstonemaster.client;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;

/**
 * Блокирует открытие окна мода, пока игрок вводит текст в Minecraft.
 */
public final class ModOpenKeyGuard {

	private ModOpenKeyGuard() {
	}

	public static boolean shouldBlock(Screen screen) {
		if (screen == null) {
			return false;
		}
		if (screen instanceof ChatScreen
				|| screen instanceof AbstractSignEditScreen
				|| screen instanceof BookEditScreen
				|| screen instanceof BookSignScreen) {
			return true;
		}
		return VanillaTextInputDetector.hasActiveTextInput(screen);
	}
}
