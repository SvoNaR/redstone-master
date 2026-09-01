package ru.redstonemaster.client.gui.tutorial;

import ru.redstonemaster.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public final class TutorialLessonProgress {
	private TutorialLessonProgress() {
	}

	public static String lessonKey(String sectionId, String lessonId) {
		return sectionId + ":" + lessonId;
	}

	public static boolean isCompleted(String sectionId, String lessonId) {
		return completedKeys().contains(lessonKey(sectionId, lessonId));
	}

	public static void markCompleted(String sectionId, String lessonId) {
		List<String> keys = completedKeys();
		String key = lessonKey(sectionId, lessonId);
		if (!keys.contains(key)) {
			keys.add(key);
			ModConfig.get().save();
			ru.redstonemaster.client.sync.ModTutorialSyncService.get().pushProgressAsync();
		}
	}

	public static void mergeFromServer(List<String> serverLessons) {
		if (serverLessons == null || serverLessons.isEmpty()) {
			return;
		}
		List<String> keys = completedKeys();
		boolean changed = false;
		for (String key : serverLessons) {
			if (key != null && !key.isBlank() && !keys.contains(key)) {
				keys.add(key);
				changed = true;
			}
		}
		if (changed) {
			ModConfig.get().save();
		}
	}

	public static int countCompletedTotal() {
		return ru.redstonemaster.client.sync.ModTutorialSyncService.countCompletedLessonsLocal();
	}

	public static int countLessonsTotal() {
		return ru.redstonemaster.client.sync.ModTutorialSyncService.countTotalLessonsLocal();
	}

	public static int countCompleted(String sectionId) {
		TutorialSection section = TutorialCatalog.findSection(sectionId);
		if (section == null) {
			return 0;
		}
		int count = 0;
		for (TutorialLesson lesson : section.lessons()) {
			if (isCompleted(sectionId, lesson.id())) {
				count++;
			}
		}
		return count;
	}

	public static int countLessons(String sectionId) {
		TutorialSection section = TutorialCatalog.findSection(sectionId);
		return section != null ? section.lessons().size() : 0;
	}

	private static List<String> completedKeys() {
		List<String> keys = ModConfig.get().completedTutorialLessons;
		if (keys == null) {
			keys = new ArrayList<>();
			ModConfig.get().completedTutorialLessons = keys;
		}
		return keys;
	}
}
