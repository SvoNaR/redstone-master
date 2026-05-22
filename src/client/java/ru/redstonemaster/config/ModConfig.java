package ru.redstonemaster.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import ru.redstonemaster.client.gui.settings.ModSetting;
import ru.redstonemaster.client.gui.settings.ModSettingDefaults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig instance;

	public double panelScale = 0.8;
	public boolean pauseOnOpen = true;
	public boolean highContrastBorders = false;
	public boolean autoLanguage = true;
	public String manualLanguage = "ru_ru";
	public boolean languageInitialized = false;
	public String defaultModLanguage = "ru_ru";
	public boolean closeOnRepeatKey = true;
	public boolean rememberSession = true;
	public String lastTab = "MAIN_MENU";
	public int settingsScrollOffset = 0;

	public static ModConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static void load() {
		Path path = getConfigPath();
		if (Files.exists(path)) {
			try {
				String json = Files.readString(path);
				instance = GSON.fromJson(json, ModConfig.class);
				if (instance == null) {
					instance = new ModConfig();
				}
				instance.clampPanelScale();
				instance.clampSessionState();
			} catch (IOException | com.google.gson.JsonSyntaxException e) {
				instance = new ModConfig();
			}
		} else {
			instance = new ModConfig();
			instance.save();
		}
	}

	public void save() {
		this.clampPanelScale();
		try {
			Files.writeString(getConfigPath(), GSON.toJson(this));
		} catch (IOException ignored) {
		}
	}

	private void clampPanelScale() {
		this.panelScale = Math.clamp(this.panelScale, 0.6, 1.0);
	}

	private void clampSessionState() {
		if (this.settingsScrollOffset < 0) {
			this.settingsScrollOffset = 0;
		}
	}

	public static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("redstone_master.json");
	}

	public String getEffectiveLanguageCode() {
		if (this.autoLanguage) {
			Minecraft client = Minecraft.getInstance();
			if (client != null) {
				return client.getLanguageManager().getSelected();
			}
			return "ru_ru";
		}
		return this.manualLanguage;
	}

	public void cyclePanelScale() {
		double[] steps = {0.6, 0.7, 0.8, 0.9, 1.0};
		int index = 0;
		for (int i = 0; i < steps.length; i++) {
			if (Math.abs(this.panelScale - steps[i]) < 0.001) {
				index = i;
				break;
			}
		}
		this.panelScale = steps[(index + 1) % steps.length];
		this.save();
	}

	public void cycleManualLanguage() {
		this.manualLanguage = "ru_ru".equals(this.manualLanguage) ? "en_us" : "ru_ru";
		this.save();
	}

	public void syncManualLanguageFromMinecraft() {
		this.manualLanguage = mapMinecraftLanguageToMod(this.getMinecraftSelectedLanguage());
	}

	/**
	 * При первом открытии окна мода запоминает язык Minecraft как стандарт для ручного выбора и сброса.
	 */
	public boolean initializeLanguageOnFirstOpen() {
		if (this.languageInitialized) {
			return false;
		}
		String mapped = mapMinecraftLanguageToMod(this.getMinecraftSelectedLanguage());
		this.manualLanguage = mapped;
		this.defaultModLanguage = mapped;
		this.languageInitialized = true;
		this.save();
		return true;
	}

	public String getDefaultModLanguage() {
		return this.defaultModLanguage;
	}

	private String getMinecraftSelectedLanguage() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return "en_us";
		}
		return client.getLanguageManager().getSelected();
	}

	public void resetSetting(ModSetting setting) {
		ModSettingDefaults.apply(this, setting);
	}

	public void resetAllModSettings() {
		ModSettingDefaults.applyAll(this);
	}

	public boolean isSettingAtDefault(ModSetting setting) {
		return switch (setting) {
			case PANEL_SCALE -> Math.abs(this.panelScale - ModSettingDefaults.PANEL_SCALE) < 0.001;
			case PAUSE_ON_OPEN -> this.pauseOnOpen == ModSettingDefaults.PAUSE_ON_OPEN;
			case HIGH_CONTRAST -> this.highContrastBorders == ModSettingDefaults.HIGH_CONTRAST_BORDERS;
			case AUTO_LANGUAGE -> this.autoLanguage == ModSettingDefaults.AUTO_LANGUAGE
					&& this.manualLanguage.equals(this.getDefaultModLanguage());
			case MANUAL_LANGUAGE -> this.manualLanguage.equals(this.getDefaultModLanguage());
			case REMEMBER_SESSION -> this.rememberSession == ModSettingDefaults.REMEMBER_SESSION;
			case CLOSE_ON_REPEAT -> this.closeOnRepeatKey == ModSettingDefaults.CLOSE_ON_REPEAT_KEY;
		};
	}

	public boolean areAllModSettingsAtDefault() {
		for (ModSetting setting : ModSetting.values()) {
			if (!this.isSettingAtDefault(setting)) {
				return false;
			}
		}
		return true;
	}

	public static String mapMinecraftLanguageToMod(String minecraftCode) {
		if (minecraftCode == null || minecraftCode.isBlank()) {
			return "en_us";
		}
		String lower = minecraftCode.toLowerCase();
		if (lower.startsWith("ru") || "ru_ru".equals(lower)) {
			return "ru_ru";
		}
		if (lower.startsWith("en") || "en_us".equals(lower)) {
			return "en_us";
		}
		return "en_us";
	}
}
