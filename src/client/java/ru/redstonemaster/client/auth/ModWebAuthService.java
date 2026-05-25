package ru.redstonemaster.client.auth;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import ru.redstonemaster.client.profile.ModAvatarManager;
import ru.redstonemaster.config.ModConfig;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class ModWebAuthService {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new Gson();
	private static final ModWebAuthService INSTANCE = new ModWebAuthService();
	private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "redstone-master-auth");
		thread.setDaemon(true);
		return thread;
	});

	public enum AuthPhase {
		IDLE,
		WAITING_BROWSER,
		FAILED
	}

	private record ExchangeResponse(String username, String avatarUrl) {
	}

	private final AtomicReference<AuthPhase> phase = new AtomicReference<>(AuthPhase.IDLE);
	private volatile boolean pendingOpenProfile;
	private volatile boolean pendingLoginSuccess;
	private volatile boolean profileUiStale;
	private volatile String lastErrorKey;
	private ModAuthCallbackServer callbackServer;

	private ModWebAuthService() {
	}

	public static ModWebAuthService get() {
		return INSTANCE;
	}

	public AuthPhase getPhase() {
		return this.phase.get();
	}

	public String getLastErrorKey() {
		return this.lastErrorKey;
	}

	public boolean consumeOpenProfileTab() {
		if (this.pendingOpenProfile) {
			this.pendingOpenProfile = false;
			return true;
		}
		return false;
	}

	public boolean consumeLoginSuccess() {
		if (this.pendingLoginSuccess) {
			this.pendingLoginSuccess = false;
			return true;
		}
		return false;
	}

	public boolean consumeProfileUiStale() {
		if (this.profileUiStale) {
			this.profileUiStale = false;
			return true;
		}
		return false;
	}

	public void beginAuth(String mode) {
		if (this.phase.get() == AuthPhase.WAITING_BROWSER) {
			return;
		}
		this.lastErrorKey = null;
		this.phase.set(AuthPhase.WAITING_BROWSER);
		EXECUTOR.execute(() -> this.runAuthFlow(mode));
	}

	public void tick(net.minecraft.client.Minecraft client) {
		if (!this.pendingOpenProfile || client == null) {
			return;
		}
		boolean showSuccess = this.pendingLoginSuccess;
		this.pendingOpenProfile = false;
		this.pendingLoginSuccess = false;
		if (client.screen instanceof ru.redstonemaster.client.gui.RedstoneMasterScreen screen) {
			screen.openProfileAfterAuth(showSuccess);
			return;
		}
		client.setScreen(new ru.redstonemaster.client.gui.RedstoneMasterScreen(client.screen, true, showSuccess));
	}

	private void runAuthFlow(String mode) {
		ModAuthCallbackServer server = null;
		try {
			this.ensureWebsiteReachable();

			server = new ModAuthCallbackServer();
			this.callbackServer = server;
			int port = server.start();
			String state = UUID.randomUUID().toString();
			String lang = webLangCode();
			String normalizedMode = "register".equals(mode) ? "register" : "login";
			String startUrl = buildStartUrl(state, port, normalizedMode, lang);
			openBrowser(URI.create(startUrl));

			ModAuthCallbackServer.CallbackPayload payload = server.callbackFuture().join();
			ExchangeResponse profile = this.exchange(payload.state(), payload.code());
			this.applyProfile(profile);
			this.phase.set(AuthPhase.IDLE);
			this.pendingOpenProfile = true;
			this.pendingLoginSuccess = true;
			this.profileUiStale = true;
		} catch (Exception exception) {
			LOGGER.error("Mod auth failed", exception);
			this.lastErrorKey = resolveErrorKey(exception);
			this.phase.set(AuthPhase.FAILED);
			this.profileUiStale = true;
		} finally {
			if (server != null) {
				server.close();
			}
			this.callbackServer = null;
		}
	}

	private void ensureWebsiteReachable() throws IOException, InterruptedException {
		ModConfig config = ModConfig.get();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(normalizeBaseUrl(config.webBaseUrl) + "/api/info"))
				.timeout(HTTP_TIMEOUT)
				.GET()
				.build();
		HttpResponse<Void> response = createHttpClient()
				.send(request, HttpResponse.BodyHandlers.discarding());
		if (response.statusCode() != 200) {
			throw new IOException("Website responded with HTTP " + response.statusCode());
		}
	}

	private ExchangeResponse exchange(String state, String code) throws Exception {
		ModConfig config = ModConfig.get();
		String body = GSON.toJson(new ExchangePayload(state, code));
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(normalizeBaseUrl(config.webBaseUrl) + "/api/auth/mod/exchange"))
				.header("Content-Type", "application/json")
				.timeout(HTTP_TIMEOUT)
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = createHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() != 200) {
			throw new IOException("Exchange failed: HTTP " + response.statusCode() + " " + response.body());
		}
		ExchangeResponse parsed = GSON.fromJson(response.body(), ExchangeResponse.class);
		if (parsed == null || parsed.username() == null || parsed.username().isBlank()) {
			throw new JsonSyntaxException("Invalid exchange response");
		}
		return parsed;
	}

	private static HttpClient createHttpClient() {
		return HttpClient.newBuilder()
				.connectTimeout(HTTP_TIMEOUT)
				.build();
	}

	private void applyProfile(ExchangeResponse profile) {
		ModConfig config = ModConfig.get();
		config.profileLoggedIn = true;
		config.profileUsername = profile.username();
		config.profileAvatarUrl = resolveAvatarUrl(profile.avatarUrl());
		config.save();
		ModAvatarManager.loadProfileAvatar();
	}

	private static String resolveErrorKey(Exception exception) {
		if (exception instanceof IOException && exception.getMessage() != null) {
			String message = exception.getMessage().toLowerCase();
			if (message.contains("website") || message.contains("connect") || message.contains("http")) {
				return "gui.redstone-master.profile.auth.error.website";
			}
			if (message.contains("browser")) {
				return "gui.redstone-master.profile.auth.error.browser";
			}
		}
		return "gui.redstone-master.profile.auth.error";
	}

	private static String buildStartUrl(String state, int port, String mode, String lang) {
		ModConfig config = ModConfig.get();
		return normalizeBaseUrl(config.webBaseUrl)
				+ "/auth/mod/start?state=" + URLEncoder.encode(state, StandardCharsets.UTF_8)
				+ "&port=" + port
				+ "&mode=" + mode
				+ "&lang=" + lang;
	}

	private static String resolveAvatarUrl(String avatarUrl) {
		if (avatarUrl == null || avatarUrl.isBlank()) {
			return "";
		}
		if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
			return avatarUrl;
		}
		return normalizeBaseUrl(ModConfig.get().webBaseUrl) + avatarUrl;
	}

	static String normalizeBaseUrl(String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			return "http://localhost:8080";
		}
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	public static String webLangCode() {
		return "ru_ru".equals(ModConfig.get().getEffectiveLanguageCode()) ? "ru" : "en";
	}

	private record ExchangePayload(String state, String code) {
	}

	private static void openBrowser(URI uri) throws IOException {
		String url = uri.toString();
		String os = System.getProperty("os.name", "").toLowerCase();
		IOException lastError = null;

		try {
			if (os.contains("win")) {
				// rundll32 keeps the full query string; cmd "start" would truncate at '&'.
				new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
				return;
			} else if (os.contains("mac")) {
				if (new ProcessBuilder("open", url).start().waitFor() == 0) {
					return;
				}
			} else {
				if (new ProcessBuilder("xdg-open", url).start().waitFor() == 0) {
					return;
				}
			}
		} catch (IOException exception) {
			lastError = exception;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Browser launch interrupted", exception);
		}

		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(uri);
				return;
			}
		}

		if (lastError != null) {
			throw lastError;
		}
		throw new IOException("Cannot open browser");
	}
}
