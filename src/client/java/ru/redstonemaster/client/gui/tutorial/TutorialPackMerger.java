package ru.redstonemaster.client.gui.tutorial;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import ru.redstonemaster.RedstoneMasterClient;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TutorialPackMerger {
	private static final Gson GSON = new Gson();
	private static final Type ROOT_TYPE = new TypeToken<TutorialCatalog.TutorialCatalogFile>() {
	}.getType();

	private TutorialPackMerger() {
	}

	static List<TutorialSection> mergePacks(List<TutorialSection> baseSections, String fileCode) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return baseSections;
		}
		String suffix = "_" + fileCode + ".json";
		Map<Identifier, Resource> packs = client.getResourceManager().listResources("tutorial/packs", id -> {
			String path = id.getPath();
			return path.endsWith(suffix);
		});
		if (packs.isEmpty()) {
			return baseSections;
		}

		Map<String, TutorialSection> sectionById = new HashMap<>();
		for (TutorialSection section : baseSections) {
			sectionById.put(section.id(), section);
		}

		for (Resource resource : packs.values()) {
			try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
				TutorialCatalog.TutorialCatalogFile file = GSON.fromJson(reader, ROOT_TYPE);
				if (file == null || file.sections == null) {
					continue;
				}
				for (TutorialSection packSection : file.sections) {
					TutorialSection existing = sectionById.get(packSection.id());
					if (existing == null) {
						sectionById.put(packSection.id(), packSection);
						continue;
					}
					sectionById.put(packSection.id(), mergeSection(existing, packSection));
				}
			} catch (Exception ignored) {
			}
		}

		List<TutorialSection> merged = new ArrayList<>(baseSections.size());
		for (TutorialSection section : baseSections) {
			merged.add(sectionById.getOrDefault(section.id(), section));
		}
		for (TutorialSection section : sectionById.values()) {
			if (merged.stream().noneMatch(item -> item.id().equals(section.id()))) {
				merged.add(section);
			}
		}
		return List.copyOf(merged);
	}

	private static TutorialSection mergeSection(TutorialSection base, TutorialSection pack) {
		Map<String, TutorialLesson> lessons = new HashMap<>();
		for (TutorialLesson lesson : base.lessons()) {
			lessons.put(lesson.id(), lesson);
		}
		if (pack.lessons() != null) {
			for (TutorialLesson lesson : pack.lessons()) {
				lessons.put(lesson.id(), lesson);
			}
		}
		return new TutorialSection(
				base.id(),
				pack.title() != null && !pack.title().isBlank() ? pack.title() : base.title(),
				pack.summary() != null && !pack.summary().isBlank() ? pack.summary() : base.summary(),
				pack.searchTokens() != null && !pack.searchTokens().isBlank() ? pack.searchTokens() : base.searchTokens(),
				pack.sources() != null && !pack.sources().isBlank() ? pack.sources() : base.sources(),
				pack.imagePaths().isEmpty() ? base.imagePaths() : pack.imagePaths(),
				List.copyOf(lessons.values())
		);
	}
}
