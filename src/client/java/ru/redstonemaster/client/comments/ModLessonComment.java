package ru.redstonemaster.client.comments;

public record ModLessonComment(
		long id,
		String username,
		String avatarUrl,
		String body,
		String replyToUsername
) {
}
