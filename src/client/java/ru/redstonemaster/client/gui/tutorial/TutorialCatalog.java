package ru.redstonemaster.client.gui.tutorial;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import ru.redstonemaster.config.ModConfig;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TutorialCatalog {
	private static final Gson GSON = new Gson();
	private static final Type ROOT_TYPE = new TypeToken<TutorialCatalogFile>() {
	}.getType();

	private static List<TutorialSection> sections = List.of();
	private static String loadedLanguage = "";

	private TutorialCatalog() {
	}

	public static void clearCache() {
		sections = List.of();
		loadedLanguage = "";
	}

	public static List<TutorialSection> getSections() {
		ensureLoaded();
		return sections;
	}

	public static TutorialSection findSection(String sectionId) {
		for (TutorialSection section : getSections()) {
			if (section.id().equals(sectionId)) {
				return section;
			}
		}
		return null;
	}

	public static TutorialLesson findLesson(String sectionId, String lessonId) {
		TutorialSection section = findSection(sectionId);
		return section != null ? section.findLesson(lessonId) : null;
	}

	public static void ensureLoaded() {
		String language = ModConfig.get().getEffectiveLanguageCode();
		if (!language.equals(loadedLanguage)) {
			sections = loadCatalog(language);
			loadedLanguage = language;
		}
	}

	private static List<TutorialSection> loadCatalog(String languageCode) {
		String fileCode = "en_us".equals(languageCode) ? "en_us" : "ru_ru";
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return List.of();
		}

		Identifier id = Identifier.fromNamespaceAndPath("redstone-master", "tutorial/" + fileCode + ".json");
		try {
			Resource resource = client.getResourceManager().getResourceOrThrow(id);
			try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
				TutorialCatalogFile file = GSON.fromJson(reader, ROOT_TYPE);
				if (file == null || file.sections == null) {
					return List.of();
				}
				return Collections.unmodifiableList(file.sections);
			}
		} catch (Exception e) {
			return List.of();
		}
	}

	public static boolean matchesQuery(String query, String searchTokens, String title) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String lower = query.toLowerCase(Locale.ROOT).trim();
		if (searchTokens != null && searchTokens.toLowerCase(Locale.ROOT).contains(lower)) {
			return true;
		}
		return title != null && title.toLowerCase(Locale.ROOT).contains(lower);
	}

	public static List<FilteredSection> filter(String query) {
		ensureLoaded();
		boolean hasQuery = query != null && !query.isBlank();
		List<FilteredSection> result = new ArrayList<>();

		for (TutorialSection section : sections) {
			boolean sectionMatch = matchesQuery(query, section.searchTokens(), section.title());
			List<TutorialLesson> visibleLessons = new ArrayList<>();

			for (TutorialLesson lesson : section.lessons()) {
				if (matchesQuery(query, lesson.searchTokens(), lesson.title())) {
					visibleLessons.add(lesson);
				}
			}

			if (!hasQuery || sectionMatch || !visibleLessons.isEmpty()) {
				if (hasQuery && sectionMatch) {
					visibleLessons = new ArrayList<>(section.lessons());
				}
				boolean forceExpanded = hasQuery;
				result.add(new FilteredSection(section, visibleLessons, forceExpanded));
			}
		}
		return result;
	}

	public record FilteredSection(TutorialSection section, List<TutorialLesson> lessons, boolean forceExpanded) {
	}

	private static final class TutorialCatalogFile {
		List<TutorialSection> sections;
	}
}
