package ru.redstonemaster.client.gui.tutorial;

import java.util.Collections;
import java.util.List;

public record TutorialLesson(
		String id,
		String title,
		String body,
		String searchTokens,
		List<String> images
) {
	public List<String> imagePaths() {
		return this.images != null ? this.images : Collections.emptyList();
	}
}
