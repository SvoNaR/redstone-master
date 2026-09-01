package ru.redstonemaster.client.gui.tutorial;

public sealed interface TutorialStudyTarget permits TutorialStudyTarget.SectionTarget, TutorialStudyTarget.LessonTarget {
	record SectionTarget(String sectionId) implements TutorialStudyTarget {
	}

	record LessonTarget(String sectionId, String lessonId) implements TutorialStudyTarget {
	}

	static TutorialStudyTarget section(String sectionId) {
		return new SectionTarget(sectionId);
	}

	static TutorialStudyTarget lesson(String sectionId, String lessonId) {
		return new LessonTarget(sectionId, lessonId);
	}
}
