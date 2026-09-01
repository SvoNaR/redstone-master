package ru.redstonemaster.client.auth;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class ModAuthCallbackServer implements AutoCloseable {
	private static final Logger LOGGER = LogUtils.getLogger();

	public record CallbackPayload(String state, String code) {
	}

	private ServerSocket serverSocket;
	private int port;
	private CompletableFuture<CallbackPayload> callbackFuture;

	public int start() throws IOException {
		IOException lastError = null;
		for (int attempt = 0; attempt < 25; attempt++) {
			this.port = ThreadLocalRandom.current().nextInt(49152, 65535);
			try {
				this.serverSocket = new ServerSocket();
				this.serverSocket.bind(new InetSocketAddress("127.0.0.1", this.port));
				break;
			} catch (IOException exception) {
				lastError = exception;
				this.serverSocket = null;
			}
		}
		if (this.serverSocket == null) {
			throw lastError != null ? lastError : new IOException("Unable to bind mod auth callback port");
		}

		this.callbackFuture = new CompletableFuture<>();
		Thread acceptThread = new Thread(this::acceptLoop, "redstone-master-auth-callback");
		acceptThread.setDaemon(true);
		acceptThread.start();
		return this.port;
	}

	public CompletableFuture<CallbackPayload> callbackFuture() {
		return this.callbackFuture;
	}

	private void acceptLoop() {
		try {
			while (this.serverSocket != null && !this.serverSocket.isClosed() && !this.callbackFuture.isDone()) {
				Socket client = this.serverSocket.accept();
				try {
					this.handleClient(client);
				} catch (IOException exception) {
					if (!this.callbackFuture.isDone()) {
						this.callbackFuture.completeExceptionally(exception);
						return;
					}
					LOGGER.debug("Mod auth callback: client error after auth completed", exception);
				} finally {
					try {
						client.close();
					} catch (IOException ignored) {
					}
				}
			}
		} catch (IOException exception) {
			if (!this.callbackFuture.isDone()) {
				this.callbackFuture.completeExceptionally(exception);
			}
			LOGGER.debug("Mod auth callback server stopped", exception);
		}
	}

	private void handleClient(Socket client) throws IOException {
		String requestLine = this.readRequestLine(client);
		if (requestLine == null) {
			LOGGER.debug("Mod auth callback: empty request");
			this.tryWriteResponse(client, 404, false);
			return;
		}
		if (requestLine.startsWith("GET /favicon.ico")) {
			this.tryWriteResponse(client, 204, false);
			return;
		}
		if (!requestLine.startsWith("GET /callback")) {
			LOGGER.debug("Mod auth callback: ignored request {}", requestLine);
			this.tryWriteResponse(client, 404, false);
			return;
		}

		int queryStart = requestLine.indexOf('?');
		String query = null;
		if (queryStart >= 0) {
			int pathEnd = requestLine.indexOf(' ', queryStart);
			if (pathEnd < 0) {
				pathEnd = requestLine.length();
			}
			query = requestLine.substring(queryStart + 1, pathEnd);
		}

		String state = readQueryParam(query, "state");
		String code = readQueryParam(query, "code");
		if (state == null || code == null) {
			LOGGER.warn("Mod auth callback: missing state or code in {}", requestLine);
			this.tryWriteResponse(client, 400, false);
			return;
		}

		LOGGER.info("Mod auth callback: received state={}", state);
		if (!this.callbackFuture.isDone()) {
			this.callbackFuture.complete(new CallbackPayload(state, code));
		}
		this.tryWriteResponse(client, 200, true);
	}

	private String readRequestLine(Socket client) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
		String requestLine = reader.readLine();
		while (true) {
			String line = reader.readLine();
			if (line == null || line.isEmpty()) {
				break;
			}
		}
		return requestLine;
	}

	private void tryWriteResponse(Socket client, int statusCode, boolean successPage) {
		try {
			this.writeResponse(client.getOutputStream(), statusCode, successPage);
		} catch (IOException exception) {
			LOGGER.debug("Mod auth callback: could not write HTTP {} response (client disconnected)", statusCode);
		}
	}

	private void writeResponse(OutputStream outputStream, int statusCode, boolean successPage) throws IOException {
		String body = successPage ? this.successHtml() : this.plainHtml("");
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		String status = switch (statusCode) {
			case 200 -> "OK";
			case 204 -> "No Content";
			case 400 -> "Bad Request";
			default -> "Not Found";
		};
		String response = "HTTP/1.1 " + statusCode + " " + status + "\r\n"
				+ "Content-Type: text/html; charset=utf-8\r\n"
				+ "Content-Length: " + bytes.length + "\r\n"
				+ "Connection: close\r\n"
				+ "\r\n";
		outputStream.write(response.getBytes(StandardCharsets.US_ASCII));
		outputStream.write(bytes);
		outputStream.flush();
	}

	private String successHtml() {
		return """
				<!DOCTYPE html>
				<html lang="ru">
				<head>
				<meta charset="UTF-8"/>
				<title>Redstone Master</title>
				<style>
				body { font-family: sans-serif; background: #1a1410; color: #f5f0e8; text-align: center; padding: 2rem; line-height: 1.5; }
				</style>
				</head>
				<body>
				<p>Авторизация завершена. Можете закрыть данную вкладку и вернуться в Minecraft.</p>
				<p>Приятной игры!</p>
				</body>
				</html>
				""";
	}

	private String plainHtml(String message) {
		if (message == null || message.isBlank()) {
			return "";
		}
		return """
				<!DOCTYPE html>
				<html lang="ru"><head><meta charset="UTF-8"/><title>Redstone Master</title></head>
				<body><p>%s</p></body></html>
				""".formatted(message);
	}

	private static String readQueryParam(String query, String name) {
		if (query == null || query.isBlank()) {
			return null;
		}
		for (String part : query.split("&")) {
			int separator = part.indexOf('=');
			if (separator <= 0) {
				continue;
			}
			String key = part.substring(0, separator);
			if (!name.equals(key)) {
				continue;
			}
			return URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8);
		}
		return null;
	}

	@Override
	public void close() {
		if (this.serverSocket != null) {
			try {
				this.serverSocket.close();
			} catch (IOException ignored) {
			}
			this.serverSocket = null;
		}
	}
}
