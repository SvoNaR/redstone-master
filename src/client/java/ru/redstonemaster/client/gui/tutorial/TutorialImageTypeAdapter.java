package ru.redstonemaster.client.gui.tutorial;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

final class TutorialImageTypeAdapter implements JsonDeserializer<TutorialImage> {
	@Override
	public TutorialImage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if (json == null || json.isJsonNull()) {
			return TutorialImage.ofPath("");
		}
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
			return TutorialImage.ofPath(json.getAsString());
		}
		if (!json.isJsonObject()) {
			throw new JsonParseException("Tutorial image must be a string or object");
		}
		JsonObject object = json.getAsJsonObject();
		String path = object.has("path") ? object.get("path").getAsString() : "";
		String caption = object.has("caption") ? object.get("caption").getAsString() : "";
		return new TutorialImage(path, caption);
	}
}
