package ru.redstonemaster.client.sync;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import ru.redstonemaster.client.auth.ModWebAuthService;
import ru.redstonemaster.client.gui.tutorial.TutorialCatalog;
import ru.redstonemaster.client.gui.tutorial.TutorialLessonProgress;
import ru.redstonemaster.config.ModConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ModTutorialSyncService {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new Gson();
	private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
	private static final ModTutorialSyncService INSTANCE = new ModTutorialSyncService();
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "redstone-master-sync");
		thread.setDaemon(true);
		return thread;
	});

	private record ProgressPayload(List<String> completedLessons) {
	}

	private record ProfileResponse(
			String username,
			String avatarUrl,
			String syncToken,
			String email,
			String role,
			String createdAt,
			int completedLessons,
			int totalLessons,
			List<String> completedLessonKeys
	) {
	}

	private ModTutorialSyncService() {
	}

	public static ModTutorialSyncService get() {
		return INSTANCE;
	}

	public void pushProgressAsync() {
		EXECUTOR.execute(this::pushProgressBlocking);
	}

	public void pushProgressBlocking() {
		ModConfig config = ModConfig.get();
		if (!config.profileLoggedIn || config.profileSyncToken == null || config.profileSyncToken.isBlank()) {
			return;
		}
		try {
			ProfileResponse response = this.postProgress(config);
			if (response != null) {
				this.applyServerProgress(response);
			}
		} catch (Exception exception) {
			LOGGER.warn("Failed to sync tutorial progress", exception);
		}
	}

	public void applyProfileFromAuth(ModWebAuthService.AuthProfile profile) {
		ModConfig config = ModConfig.get();
		config.profileSyncToken = profile.syncToken() != null ? profile.syncToken() : "";
		config.profileEmail = profile.email() != null ? profile.email() : "";
		config.profileRole = profile.role() != null ? profile.role() : "";
		config.profileCreatedAt = profile.createdAt() != null ? profile.createdAt() : "";
		config.profileCompletedLessons = profile.completedLessons();
		config.profileTotalLessons = profile.totalLessons();
		config.save();
		TutorialLessonProgress.mergeFromServer(profile.completedLessonKeys());
	}

	private ProfileResponse postProgress(ModConfig config) throws IOException, InterruptedException {
		String body = GSON.toJson(new ProgressPayload(new ArrayList<>(config.completedTutorialLessons)));
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ModWebAuthService.normalizeBaseUrl(config.webBaseUrl) + "/api/mod/tutorial/progress"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + config.profileSyncToken)
				.timeout(HTTP_TIMEOUT)
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = createHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() != 200) {
			throw new IOException("Progress sync failed: HTTP " + response.statusCode());
		}
		ProfileResponse parsed = GSON.fromJson(response.body(), ProfileResponse.class);
		if (parsed == null) {
			throw new JsonSyntaxException("Invalid progress sync response");
		}
		return parsed;
	}

	private void applyServerProgress(ProfileResponse response) {
		ModConfig config = ModConfig.get();
		config.profileCompletedLessons = response.completedLessons();
		config.profileTotalLessons = response.totalLessons();
		config.save();
		TutorialLessonProgress.mergeFromServer(response.completedLessonKeys());
		ModWebAuthService.get().markProfileUiStale();
	}

	private static HttpClient createHttpClient() {
		return HttpClient.newBuilder()
				.connectTimeout(HTTP_TIMEOUT)
				.build();
	}

	public static int countTotalLessonsLocal() {
		return TutorialCatalog.getSections().stream()
				.mapToInt(section -> section.lessons().size())
				.sum();
	}

	public static int countCompletedLessonsLocal() {
		Set<String> validKeys = new HashSet<>();
		TutorialCatalog.getSections().forEach(section ->
				section.lessons().forEach(lesson -> validKeys.add(TutorialLessonProgress.lessonKey(section.id(), lesson.id())))
		);
		int count = 0;
		for (String key : ModConfig.get().completedTutorialLessons) {
			if (validKeys.contains(key)) {
				count++;
			}
		}
		return count;
	}
}
