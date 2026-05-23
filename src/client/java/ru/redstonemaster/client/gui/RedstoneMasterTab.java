package ru.redstonemaster.client.gui;

import net.minecraft.network.chat.Component;
import ru.redstonemaster.config.ModContentLanguage;

public enum RedstoneMasterTab {
	MAIN_MENU("gui.redstone-master.main_menu.body"),
	TUTORIAL("gui.redstone-master.placeholder"),
	SETTINGS(null),
	PROFILE("gui.redstone-master.placeholder");

	private final String translationKey;

	RedstoneMasterTab(String translationKey) {
		this.translationKey = translationKey;
	}

	public Component getContent() {
		if (this.translationKey == null) {
			return Component.empty();
		}
		return ModContentLanguage.translatable(this.translationKey);
	}

	public static RedstoneMasterTab fromName(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		try {
			return RedstoneMasterTab.valueOf(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
