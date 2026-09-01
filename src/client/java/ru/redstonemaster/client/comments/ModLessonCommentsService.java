package ru.redstonemaster.client.comments;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import ru.redstonemaster.client.auth.ModWebAuthService;
import ru.redstonemaster.config.ModConfig;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class ModLessonCommentsService {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new Gson();
	private static final Type COMMENT_LIST_TYPE = TypeToken.getParameterized(List.class, CommentPayload.class).getType();
	private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
	private static final long ONLINE_CACHE_MS = 30_000L;
	private static final ModLessonCommentsService INSTANCE = new ModLessonCommentsService();
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "redstone-master-comments");
		thread.setDaemon(true);
		return thread;
	});

	private volatile Boolean cachedOnline;
	private volatile long cachedOnlineCheckedAt;

	private record CommentPayload(
			long id,
			String username,
			String avatarUrl,
			String body,
			String createdAt,
			Long parentCommentId,
			String replyToUsername
	) {
		ModLessonComment toView(String webBaseUrl) {
			return new ModLessonComment(
					this.id,
					this.username,
					resolveAvatarUrl(webBaseUrl, this.avatarUrl),
					this.body,
					this.replyToUsername
			);
		}
	}

	private ModLessonCommentsService() {
	}

	public static ModLessonCommentsService get() {
		return INSTANCE;
	}

	public boolean isWebsiteReachable() {
		long now = System.currentTimeMillis();
		if (this.cachedOnline != null && now - this.cachedOnlineCheckedAt < ONLINE_CACHE_MS) {
			return this.cachedOnline;
		}
		boolean reachable = this.probeWebsite();
		this.cachedOnline = reachable;
		this.cachedOnlineCheckedAt = now;
		return reachable;
	}

	public void fetchComments(String sectionId, String lessonId, Consumer<List<ModLessonComment>> onSuccess, Runnable onFailure) {
		EXECUTOR.execute(() -> {
			try {
				if (!this.isWebsiteReachable()) {
					onFailure.run();
					return;
				}
				String baseUrl = ModWebAuthService.normalizeBaseUrl(ModConfig.get().webBaseUrl);
				String path = "/api/tutorial/" + sectionId + "/" + lessonId + "/comments";
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(baseUrl + path))
						.timeout(HTTP_TIMEOUT)
						.GET()
						.build();
				HttpResponse<String> response = HttpClient.newHttpClient()
						.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				if (response.statusCode() != 200) {
					LOGGER.warn("Lesson comments HTTP {}", response.statusCode());
					onFailure.run();
					return;
				}
				List<CommentPayload> payloads = GSON.fromJson(response.body(), COMMENT_LIST_TYPE);
				if (payloads == null) {
					onSuccess.accept(List.of());
					return;
				}
				List<ModLessonComment> comments = payloads.stream()
						.map(payload -> payload.toView(baseUrl))
						.toList();
				onSuccess.accept(comments);
			} catch (IOException | InterruptedException | JsonSyntaxException exception) {
				if (exception instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				LOGGER.warn("Failed to load lesson comments", exception);
				onFailure.run();
			}
		});
	}

	private boolean probeWebsite() {
		try {
			String baseUrl = ModWebAuthService.normalizeBaseUrl(ModConfig.get().webBaseUrl);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/api/info"))
					.timeout(HTTP_TIMEOUT)
					.GET()
					.build();
			HttpResponse<Void> response = HttpClient.newHttpClient()
					.send(request, HttpResponse.BodyHandlers.discarding());
			return response.statusCode() == 200;
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}

	private static String resolveAvatarUrl(String webBaseUrl, String avatarUrl) {
		if (avatarUrl == null || avatarUrl.isBlank()) {
			return "";
		}
		if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
			return avatarUrl;
		}
		return webBaseUrl + avatarUrl;
	}
}
