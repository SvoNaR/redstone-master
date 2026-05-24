package ru.redstonemaster.client.gui;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class RedstoneMasterNavigationHistory {
	private final List<RedstoneMasterNavigationSnapshot> stack = new ArrayList<>();
	private int index = -1;

	void reset(RedstoneMasterNavigationSnapshot initial) {
		this.stack.clear();
		this.stack.add(initial);
		this.index = 0;
	}

	void push(RedstoneMasterNavigationSnapshot snapshot) {
		if (this.index >= 0 && this.stack.get(this.index).equals(snapshot)) {
			return;
		}
		while (this.stack.size() > this.index + 1) {
			this.stack.remove(this.stack.size() - 1);
		}
		this.stack.add(snapshot);
		this.index = this.stack.size() - 1;
	}

	boolean canGoBack() {
		return this.index > 0;
	}

	boolean canGoForward() {
		return this.index >= 0 && this.index < this.stack.size() - 1;
	}

	@Nullable
	RedstoneMasterNavigationSnapshot goBack() {
		if (!this.canGoBack()) {
			return null;
		}
		this.index--;
		return this.stack.get(this.index);
	}

	@Nullable
	RedstoneMasterNavigationSnapshot goForward() {
		if (!this.canGoForward()) {
			return null;
		}
		this.index++;
		return this.stack.get(this.index);
	}
}
