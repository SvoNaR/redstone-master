package ru.redstonemaster.client.gui.settings;

import ru.redstonemaster.config.ModContentLanguage;

public enum ModSetting {
	PANEL_SCALE(
			"gui.redstone_master.settings.section.interface",
			"gui.redstone_master.settings.panel_scale",
			"интерфейс размер окна мода panel scale window size"
	),
	PAUSE_ON_OPEN(
			"gui.redstone_master.settings.section.interface",
			"gui.redstone_master.settings.pause_on_open",
			"интерфейс пауза при открытии pause singleplayer"
	),
	HIGH_CONTRAST(
			"gui.redstone_master.settings.section.interface",
			"gui.redstone_master.settings.high_contrast",
			"интерфейс высокий контраст рамок contrast border"
	),
	AUTO_LANGUAGE(
			"gui.redstone_master.settings.section.interface",
			"gui.redstone_master.settings.auto_language",
			"интерфейс язык автоподбор языка auto language detect"
	),
	MANUAL_LANGUAGE(
			"gui.redstone_master.settings.section.interface",
			"gui.redstone_master.settings.manual_language",
			"интерфейс выбор языка language manual"
	),
	REMEMBER_SESSION(
			"gui.redstone_master.settings.section.controls",
			"gui.redstone_master.settings.remember_session",
			"управление сохранять вкладку позицию remember session tab scroll"
	),
	CLOSE_ON_REPEAT(
			"gui.redstone_master.settings.section.controls",
			"gui.redstone_master.settings.close_on_repeat",
			"управление закрывать повторное нажатие close key toggle"
	);

	private final String sectionKey;
	private final String nameKey;
	private final String searchTokens;

	ModSetting(String sectionKey, String nameKey, String searchTokens) {
		this.sectionKey = sectionKey;
		this.nameKey = nameKey;
		this.searchTokens = searchTokens;
	}

	public String getSectionKey() {
		return this.sectionKey;
	}

	public String getNameKey() {
		return this.nameKey;
	}

	public boolean matchesSearch(String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String lower = query.toLowerCase().trim();
		if (this.searchTokens.contains(lower)) {
			return true;
		}
		String name = ModContentLanguage.get(this.nameKey).toLowerCase();
		String section = ModContentLanguage.get(this.sectionKey).toLowerCase();
		return name.contains(lower) || section.contains(lower);
	}
}
