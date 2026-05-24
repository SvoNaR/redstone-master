package ru.redstonemaster.client.gui.tutorial;

import java.util.Collections;
import java.util.List;

public record TutorialSection(
		String id,
		String title,
		String summary,
		String searchTokens,
		String sources,
		List<String> images,
		List<TutorialLesson> lessons
) {
	public List<String> imagePaths() {
		return this.images != null ? this.images : Collections.emptyList();
	}

	public TutorialLesson findLesson(String lessonId) {
		for (TutorialLesson lesson : this.lessons) {
			if (lesson.id().equals(lessonId)) {
				return lesson;
			}
		}
		return null;
	}
}
