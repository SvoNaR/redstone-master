package ru.redstonemaster.client.gui.tutorial;

import org.jetbrains.annotations.Nullable;

/** Сериализация режима «Изучить» для {@link ru.redstonemaster.config.ModConfig}. */
public final class TutorialSessionPersistence {
	private TutorialSessionPersistence() {
	}

	public static String toStorageKey(@Nullable TutorialStudyTarget target) {
		if (target == null) {
			return "";
		}
		return switch (target) {
			case TutorialStudyTarget.SectionTarget section -> "section:" + section.sectionId();
			case TutorialStudyTarget.LessonTarget lesson ->
					"lesson:" + lesson.sectionId() + ":" + lesson.lessonId();
		};
	}

	@Nullable
	public static TutorialStudyTarget fromStorageKey(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		if (key.startsWith("section:")) {
			String sectionId = key.substring("section:".length());
			if (sectionId.isEmpty()) {
				return null;
			}
			return TutorialStudyTarget.section(sectionId);
		}
		if (key.startsWith("lesson:")) {
			String[] parts = key.substring("lesson:".length()).split(":", 2);
			if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
				return null;
			}
			return TutorialStudyTarget.lesson(parts[0], parts[1]);
		}
		return null;
	}
}
