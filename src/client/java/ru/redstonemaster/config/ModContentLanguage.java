package ru.redstonemaster.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ModContentLanguage {
	private static final Gson GSON = new Gson();
	private static final Type LANG_MAP_TYPE = new TypeToken<Map<String, String>>() {
	}.getType();

	private static Map<String, String> ruStrings;
	private static Map<String, String> enStrings;

	private ModContentLanguage() {
	}

	public static void clearCache() {
		ruStrings = null;
		enStrings = null;
	}

	public static Component translatable(String key) {
		return Component.literal(get(key));
	}

	public static Component translatable(String key, Object arg) {
		return Component.literal(format(key, arg));
	}

	public static String format(String key, Object arg) {
		String template = get(key);
		return String.format(template, arg);
	}

	public static String get(String key) {
		if (ModConfig.get().autoLanguage) {
			return Language.getInstance().getOrDefault(key, key);
		}
		return getMap(ModConfig.get().manualLanguage).getOrDefault(key, key);
	}

	public static Component getLanguageDisplayName(String languageCode) {
		String nameKey = switch (languageCode) {
			case "ru_ru" -> "gui.redstone_master.settings.language.ru";
			case "en_us" -> "gui.redstone_master.settings.language.en";
			default -> "gui.redstone_master.settings.language.unknown";
		};
		return Component.literal(getMap(languageCode).getOrDefault(nameKey, nameKey));
	}

	public static Component getMinecraftLanguageDisplayName() {
		return getLanguageDisplayName(ModConfig.get().getEffectiveLanguageCode());
	}

	private static Map<String, String> getMap(String languageCode) {
		if ("en_us".equals(languageCode)) {
			if (enStrings == null) {
				enStrings = loadLanguageFile("en_us");
			}
			return enStrings;
		}
		if (ruStrings == null) {
			ruStrings = loadLanguageFile("ru_ru");
		}
		return ruStrings;
	}

	private static Map<String, String> loadLanguageFile(String code) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return Map.of();
		}

		Identifier id = Identifier.fromNamespaceAndPath("redstone_master", "lang/" + code + ".json");
		try {
			Resource resource = client.getResourceManager().getResourceOrThrow(id);
			try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
				Map<String, String> loaded = GSON.fromJson(reader, LANG_MAP_TYPE);
				return loaded != null ? loaded : Map.of();
			}
		} catch (Exception e) {
			return new HashMap<>();
		}
	}
}
