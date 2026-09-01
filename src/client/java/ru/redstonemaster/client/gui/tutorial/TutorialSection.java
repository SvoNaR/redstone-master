package ru.redstonemaster.client.gui.tutorial;

import java.util.List;

public record TutorialSection(
		String id,
		String title,
		String summary,
		String searchTokens,
		String sources,
		List<TutorialImage> images,
		List<TutorialLesson> lessons
) {
	public List<TutorialImage> imageEntries() {
		return this.images != null ? this.images : List.of();
	}

	public List<String> imagePaths() {
		return this.imageEntries().stream().map(TutorialImage::path).toList();
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
