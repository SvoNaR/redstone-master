package ru.redstonemaster.client.gui.settings;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

public final class PanelBackgroundTransparencySlider extends AbstractSliderButton {
	private final Runnable onValueCommitted;

	public PanelBackgroundTransparencySlider(int x, int y, int width, int height, Runnable onValueCommitted) {
		super(
				x,
				y,
				width,
				height,
				Component.empty(),
				transparencyToSlider(ModConfig.get().panelBackgroundTransparency)
		);
		this.onValueCommitted = onValueCommitted;
		this.setTooltip(Tooltip.create(
				ModContentLanguage.translatable("gui.redstone-master.settings.background_opacity.tooltip")));
		this.updateMessage();
	}

	@Override
	protected void applyValue() {
		ModConfig.get().panelBackgroundTransparency = sliderToTransparency(this.value);
		this.updateMessage();
	}

	@Override
	protected void updateMessage() {
		int percent = (int) Math.round(ModConfig.get().panelBackgroundTransparency);
		this.setMessage(ModContentLanguage.translatable(
				"gui.redstone-master.settings.background_opacity.value",
				percent
		));
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		super.onRelease(event);
		ModConfig.get().save();
		if (this.onValueCommitted != null) {
			this.onValueCommitted.run();
		}
	}

	public void syncFromConfig() {
		this.setValue(transparencyToSlider(ModConfig.get().panelBackgroundTransparency));
		this.updateMessage();
	}

	public static double transparencyToSlider(double transparency) {
		return Math.clamp((100.0 - transparency) / 100.0, 0.0, 1.0);
	}

	public static double sliderToTransparency(double sliderValue) {
		return Math.clamp(Math.round((1.0 - sliderValue) * 100.0), 0.0, 100.0);
	}
}
