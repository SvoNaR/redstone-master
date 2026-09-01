package ru.redstonemaster.client.gui.tutorial;

import java.util.List;

public record TutorialLesson(
		String id,
		String title,
		String body,
		String searchTokens,
		List<TutorialImage> images,
		List<String> videos
) {
	public List<TutorialImage> imageEntries() {
		return this.images != null ? this.images : List.of();
	}

	public List<String> imagePaths() {
		return this.imageEntries().stream().map(TutorialImage::path).toList();
	}

	public List<String> videoIds() {
		return this.videos != null ? this.videos : List.of();
	}
}
