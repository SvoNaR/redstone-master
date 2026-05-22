package ru.redstonemaster.client.gui.settings;

import ru.redstonemaster.config.ModConfig;

/**
 * Значения настроек мода по умолчанию (как в новом {@link ModConfig}).
 */
public final class ModSettingDefaults {
	public static final double PANEL_SCALE = 0.8;
	public static final boolean PAUSE_ON_OPEN = true;
	public static final boolean HIGH_CONTRAST_BORDERS = false;
	public static final boolean AUTO_LANGUAGE = true;
	public static final boolean CLOSE_ON_REPEAT_KEY = true;
	public static final boolean REMEMBER_SESSION = true;

	private ModSettingDefaults() {
	}

	public static void apply(ModConfig config, ModSetting setting) {
		switch (setting) {
			case PANEL_SCALE -> config.panelScale = PANEL_SCALE;
			case PAUSE_ON_OPEN -> config.pauseOnOpen = PAUSE_ON_OPEN;
			case HIGH_CONTRAST -> config.highContrastBorders = HIGH_CONTRAST_BORDERS;
			case AUTO_LANGUAGE -> {
				config.autoLanguage = AUTO_LANGUAGE;
				config.manualLanguage = config.getDefaultModLanguage();
			}
			case MANUAL_LANGUAGE -> config.manualLanguage = config.getDefaultModLanguage();
			case REMEMBER_SESSION -> config.rememberSession = REMEMBER_SESSION;
			case CLOSE_ON_REPEAT -> config.closeOnRepeatKey = CLOSE_ON_REPEAT_KEY;
		}
	}

	public static void applyAll(ModConfig config) {
		for (ModSetting setting : ModSetting.values()) {
			apply(config, setting);
		}
	}
}
