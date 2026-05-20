package ru.redstonemaster.client.gui;

import net.minecraft.network.chat.Component;

public enum RedstoneMasterTab {
	MAIN_MENU("gui.redstone_master.welcome"),
	TUTORIAL("gui.redstone_master.placeholder"),
	SETTINGS("gui.redstone_master.placeholder"),
	SEARCH("gui.redstone_master.placeholder");

	private final String translationKey;

	RedstoneMasterTab(String translationKey) {
		this.translationKey = translationKey;
	}

	public Component getContent() {
		return Component.translatable(this.translationKey);
	}
}
