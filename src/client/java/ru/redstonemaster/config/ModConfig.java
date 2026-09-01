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
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig instance;

	public double panelScale = 0.8;
	/** Прозрачность фона окна мода: 100 — полностью прозрачный, 75 — стандарт, 0 — непрозрачный чёрный. */
	public double panelBackgroundTransparency = 75.0;
	public boolean pauseOnOpen = true;
	public boolean highContrastBorders = false;
	public boolean autoLanguage = true;
	public String manualLanguage = "ru_ru";
	public boolean languageInitialized = false;
	public String defaultModLanguage = "ru_ru";
	public boolean closeOnRepeatKey = true;
	public boolean tutorialCollapseOtherSections = false;
	public boolean rememberSession = true;
	public String webBaseUrl = "http://localhost:8080";
	public int guestAvatarDefault = 0;
	public boolean profileLoggedIn = false;
	public String profileUsername = "";
	public String profileAvatarUrl = "";
	public String profileSyncToken = "";
	public String profileEmail = "";
	public String profileRole = "";
	public String profileCreatedAt = "";
	public int profileCompletedLessons = 0;
	public int profileTotalLessons = 0;
	public List<String> completedTutorialLessons = new ArrayList<>();

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
				instance.migrateLegacyPanelBackground(json);
				instance.clampPanelBackgroundTransparency();
				if (instance.completedTutorialLessons == null) {
					instance.completedTutorialLessons = new ArrayList<>();
				}
				if (instance.profileSyncToken == null) {
					instance.profileSyncToken = "";
				}
				if (instance.profileEmail == null) {
					instance.profileEmail = "";
				}
				if (instance.profileRole == null) {
					instance.profileRole = "";
				}
				if (instance.profileCreatedAt == null) {
					instance.profileCreatedAt = "";
				}
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
		this.clampPanelBackgroundTransparency();
		try {
			Files.writeString(getConfigPath(), GSON.toJson(this));
		} catch (IOException ignored) {
		}
	}

	private void clampPanelScale() {
		this.panelScale = Math.clamp(this.panelScale, 0.6, 1.0);
	}

	private void clampPanelBackgroundTransparency() {
		this.panelBackgroundTransparency = Math.clamp(Math.round(this.panelBackgroundTransparency), 0.0, 100.0);
	}

	private void migrateLegacyPanelBackground(String json) {
		if (json == null || !json.contains("\"panelBackgroundOpacity\"")
				|| json.contains("\"panelBackgroundTransparency\"")) {
			return;
		}
		try {
			com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
			if (!root.has("panelBackgroundOpacity")) {
				return;
			}
			this.panelBackgroundTransparency = migrateLegacyPanelBackgroundOpacity(
					root.get("panelBackgroundOpacity").getAsDouble()
			);
		} catch (RuntimeException ignored) {
		}
	}

	private static double migrateLegacyPanelBackgroundOpacity(double opacity) {
		return Math.clamp(Math.round((1.0 - opacity) * 100.0), 0.0, 100.0);
	}

	public static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("redstone-master.json");
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
			case BACKGROUND_OPACITY ->
					Math.abs(this.panelBackgroundTransparency - ModSettingDefaults.PANEL_BACKGROUND_TRANSPARENCY) < 0.001;
			case PAUSE_ON_OPEN -> this.pauseOnOpen == ModSettingDefaults.PAUSE_ON_OPEN;
			case HIGH_CONTRAST -> this.highContrastBorders == ModSettingDefaults.HIGH_CONTRAST_BORDERS;
			case AUTO_LANGUAGE -> this.autoLanguage == ModSettingDefaults.AUTO_LANGUAGE;
			case MANUAL_LANGUAGE -> this.autoLanguage
					|| this.manualLanguage.equals(this.getDefaultModLanguage());
			case REMEMBER_SESSION -> this.rememberSession == ModSettingDefaults.REMEMBER_SESSION;
			case CLOSE_ON_REPEAT -> this.closeOnRepeatKey == ModSettingDefaults.CLOSE_ON_REPEAT_KEY;
			case TUTORIAL_COLLAPSE_OTHERS ->
					this.tutorialCollapseOtherSections == ModSettingDefaults.TUTORIAL_COLLAPSE_OTHER_SECTIONS;
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
