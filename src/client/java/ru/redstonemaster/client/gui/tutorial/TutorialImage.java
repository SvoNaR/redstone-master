package ru.redstonemaster.client.gui.tutorial;

import java.util.Locale;

public record TutorialImage(String path, String caption) {
	public TutorialImage {
		path = path != null ? path : "";
		caption = caption != null ? caption : "";
	}

	public String displayCaption() {
		if (!this.caption.isBlank()) {
			return this.caption.trim();
		}
		return defaultCaptionFromPath(this.path);
	}

	private static String defaultCaptionFromPath(String imagePath) {
		if (imagePath == null || imagePath.isBlank()) {
			return "";
		}
		String normalized = imagePath.replace('\\', '/');
		int slash = normalized.lastIndexOf('/');
		String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
		int dot = fileName.lastIndexOf('.');
		if (dot > 0) {
			fileName = fileName.substring(0, dot);
		}
		return fileName.replace('_', ' ');
	}

	public static TutorialImage ofPath(String path) {
		return new TutorialImage(path, "");
	}
}
